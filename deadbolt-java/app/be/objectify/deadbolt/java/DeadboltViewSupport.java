/*
 * Copyright 2012 Steve Chaloner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package be.objectify.deadbolt.java;

import be.objectify.deadbolt.core.PatternType;
import be.objectify.deadbolt.core.models.RoleHolder;
import be.objectify.deadbolt.java.utils.PluginUtils;
import be.objectify.deadbolt.java.utils.RequestUtils;
import play.Logger;
import play.cache.Cache;
import play.mvc.Http;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

/**
 * Provides the entry point for view-level annotations.
 *
 * @author Steve Chaloner (steve@objectify.be)
 */
public class DeadboltViewSupport
{
    /**
     * Used for restrict tags in the template.
     *
     * @param roles a list of String arrays.  Within an array, the roles are ANDed.  The arrays in the list are OR'd.
     * @return true if the view can be accessed, otherwise false
     */
    public static boolean viewRestrict(List<String[]> roles) throws Throwable
    {
        boolean roleOk = false;
        RoleHolder roleHolder = RequestUtils.getRoleHolder(PluginUtils.getDeadboltHandler(),
                                                           Http.Context.current());
        for (int i = 0; !roleOk && i < roles.size(); i++)
        {
            roleOk = JavaDeadboltAnalyzer.checkRole(roleHolder,
                                                roles.get(i));
        }

        return roleOk;
    }

    /**
     * Used for dynamic tags in the template.
     *
     * @param name the name of the resource
     * @param meta meta information on the resource
     * @return true if the view can be accessed, otherwise false
     */
    public static boolean viewDynamic(String name,
                                      String meta) throws Throwable
    {
        Http.Context context = Http.Context.current();
        DynamicResourceHandler resourceHandler = PluginUtils.getDeadboltHandler().getDynamicResourceHandler(context);
        boolean allowed = false;
        if (resourceHandler == null)
        {
            throw new RuntimeException("A dynamic resource is specified but no dynamic resource handler is provided");
        }
        else
        {
            if (resourceHandler.isAllowed(name,
                                          meta,
                                          PluginUtils.getDeadboltHandler(),
                                          context))
            {
                allowed = true;
            }
        }

        return allowed;
    }

    /**
     * Used for roleHolderPresent tags in the template.
     *
     * @return true if the view can be accessed, otherwise false
     */
    public static boolean viewRoleHolderPresent() throws Throwable
    {
        RoleHolder roleHolder = RequestUtils.getRoleHolder(PluginUtils.getDeadboltHandler(),
                                                           Http.Context.current());
        return roleHolder != null;
    }

    /**
     * Used for roleHolderNotPresent tags in the template.
     *
     * @return true if the view can be accessed, otherwise false
     */
    public static boolean viewRoleHolderNotPresent() throws Throwable
    {
        RoleHolder roleHolder = RequestUtils.getRoleHolder(PluginUtils.getDeadboltHandler(),
                                                           Http.Context.current());
        return roleHolder == null;
    }

    public static boolean viewPattern(String value,
                                      PatternType patternType) throws Exception
    {
        boolean allowed = false;

        Http.Context context = Http.Context.current();
        RoleHolder roleHolder = RequestUtils.getRoleHolder(PluginUtils.getDeadboltHandler(),
                                                           context);
        switch (patternType)
        {
            case EQUALITY:
                allowed = JavaDeadboltAnalyzer.checkPatternEquality(roleHolder,
                                                                    value);
                break;
            case REGEX:
                allowed = JavaDeadboltAnalyzer.checkRegexPattern(roleHolder,
                                                                 getPattern(value));
                break;
            case CUSTOM:
                allowed = JavaDeadboltAnalyzer.checkCustomPattern(roleHolder,
                                                                  PluginUtils.getDeadboltHandler(),
                                                                  context,
                                                                  value);
                break;
            default:
                Logger.error("Unknown pattern type: " + patternType);
        }

        return allowed;
    }

    public static Pattern getPattern(final String patternValue) throws Exception
    {
        return Cache.getOrElse("Deadbolt." + patternValue,
                               new Callable<Pattern>()
                               {
                                   public Pattern call() throws Exception
                                   {
                                       return Pattern.compile(patternValue);
                                   }
                               },
                               0);
    }
}