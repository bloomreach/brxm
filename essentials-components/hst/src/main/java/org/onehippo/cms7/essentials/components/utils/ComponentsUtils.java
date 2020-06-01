/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.components.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.HstRequestContext;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;

/**
 * Hippo Essentials component utilities
 *
 * @version $Id$
 */
public final class ComponentsUtils {

    public static final DateTimeFormatter YEAR_FORMAT = DateTimeFormatter.ofPattern("yyyy");
    public static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MM");
    public static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("dd");

    /**
     * Get content bean based on path e.g. {@code "/foo/bar/"}
     *
     * @param path      content path
     * @return null if nothing found
     */
    public static HippoBean getBean(final String path) {

        final HstRequestContext context = RequestContextProvider.get();
        final HippoBean currentBean = context.getContentBean();

        if (path == null) {
            return currentBean;
        }

        if (path.startsWith("/")) {
            return context.getSiteContentBaseBean().getBean(path.substring(1));
        }

        if (currentBean != null) {
            return currentBean.getBean(path);
        }

        return null;
    }

    /**
     * Get a string from a configuration parameter, returning a default value if
     * the parameter is not there.
     */
    public static String getParameter(final BaseHstComponent comp, final HstRequest request, final String paramName, final String defaultValue) {

        final String value = comp.getComponentParameter(paramName);
        return (value != null) ? value.trim() : defaultValue;
    }

    /**
     * Get a string List from comma-separated values of a configuration parameter.
     */
    public static List<String> getParameterList(final BaseHstComponent comp, final HstRequest request, final String paramName) {

        String commaSepValues = comp.getComponentParameter(paramName);

        if (commaSepValues == null) {
            return Collections.emptyList();
        }

        final Iterable<String> iterable = Splitter.on(',').omitEmptyStrings().trimResults().split(commaSepValues);
        List<String> list = new ArrayList<>();
        for (String value : iterable) {
            list.add(CharMatcher.breakingWhitespace().trimFrom(value));
        }
        return list;
    }

    /**
     * Get an int from a configuration parameter, returning a default value in
     * case of error or if the parameter is not there.
     */
    public static int getParameterInt(final BaseHstComponent comp, final HstRequest request, final String paramName, final int defaultValue) {

        final String paramValue = comp.getComponentParameter(paramName);
        if (paramValue != null) {
            try {
                return Integer.parseInt(paramValue.trim());
            } catch (NumberFormatException nfe) {
                return defaultValue;
            }
        }

        return defaultValue;
    }

    /**
     * Get a boolean from a configuration parameter, returning 'false' in case
     * of error or if the parameter is not there.
     */
    public static boolean getParameterBoolean(final BaseHstComponent comp, final HstRequest request, final String paramName) {
        return getParameterBoolean(comp, request, paramName, false);
    }

    /**
     * Get a boolean from a configuration parameter, returning a default value
     * if the parameter is not there, and false if the parsing fails.
     */
    public static boolean getParameterBoolean(final BaseHstComponent comp, HstRequest request, String paramName, boolean defaultValue) {
        final String paramValue = comp.getComponentParameter(paramName);
        if (paramValue != null) {
            return Boolean.valueOf(paramValue.trim());
        }
        return defaultValue;
    }

    /**
     * Get a local configuration parameter, i.e. not overridden by parent
     * components, returning a default value if the parameter is not there.
     */
    public static String getLocalParameter(final BaseHstComponent comp, final HstRequest request,
                                           final String paramName, final String defaultValue) {

        final String value = comp.getComponentLocalParameter(paramName);
        return (value != null) ? value.trim() : defaultValue;
    }

    /**
     * Get a local configuration parameter as integer, i.e. not overridden by
     * parent components, returning a default value if the parameter is not
     * there or if parsing fails.
     */
    public static int getLocalParameterInt(final BaseHstComponent comp, final HstRequest request, final String paramName, final int defaultValue) {

        final String value = comp.getComponentLocalParameter(paramName);
        if (value != null) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException nfe) {
                return defaultValue;
            }
        }

        return defaultValue;
    }

    /**
     * Get a local configuration parameter as boolean, i.e. not overridden by
     * parent components, returning a default value if the parameter is not
     * there.
     */
    public static boolean getLocalParameterBoolean(final BaseHstComponent comp, final HstRequest request,
                                                   final String paramName, final boolean defaultValue) {
        final String paramValue = comp.getComponentLocalParameter(paramName);
        if (paramValue != null) {
            return Boolean.valueOf(paramValue.trim());
        }
        return defaultValue;
    }

    /**
     * Get a public request parameter, returning a default value if the
     * parameter is not there.
     */
    public static String getPublicRequestParameter(final BaseHstComponent comp, final HstRequest request,
                                                   final String paramName, final String defaultValue) {
        final String value = comp.getPublicRequestParameter(request, paramName);
        return (value != null) ? value.trim() : defaultValue;
    }

    /**
     * Get a public request parameter as integer, returning a default value in
     * case of error if the parameter is not there.
     */
    public static int getPublicRequestParameterInt(final BaseHstComponent comp, final HstRequest request,
                                                   final String paramName, final int defaultValue) {
        final String value = comp.getPublicRequestParameter(request, paramName);

        if (value != null) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException nfe) {
                return defaultValue;
            }
        }

        return defaultValue;
    }

    /**
     * Get a request parameter as integer, returning a default value if the
     * parameter is not there or if parsing fails.
     */
    public static int getRequestParameterInt(final HstRequest request, final String paramName, final int defaultValue) {

        final String paramValue = request.getParameter(paramName);
        if (paramValue != null) {
            try {
                return Integer.parseInt(paramValue.trim());
            } catch (NumberFormatException nfe) {
                return defaultValue;
            }
        }

        return defaultValue;
    }


    /**
     * Returns an array of values for a public request parameter. For use of multiple checkboxes
     *
     * @param request
     * @param paramName
     * @return String[] value of the request parameter. null if the parameter does not exist or is empty.
     * @deprecated please use HST native BaseHstComponent#getPublicRequestParameters()
     */
    @Deprecated
    public String[] getPublicRequestParameters(final BaseHstComponent comp, final HstRequest request, final String paramName) {

        final Map<String, String[]> namespaceLessParameters = request.getParameterMap("");
        return namespaceLessParameters.get(paramName);
    }

    /**
     * Adds three request attributes to the request:
     * <ul>
     *     <li>"currentYear" with the current year in yyyy format.</li>
     *     <li>"currentMonth" with the current month in MM format, so with leading zeros.</li>
     *     <li>"currentDay" with the current day of the month in dd format, so with leading zeros.</li>
     * </ul>
     */
    public static void addCurrentDateStrings(final HstRequest request) {
        final LocalDate now = LocalDate.now();
        request.setAttribute("currentYear", now.format(YEAR_FORMAT));
        request.setAttribute("currentMonth", now.format(MONTH_FORMAT));
        request.setAttribute("currentDay", now.format(DAY_FORMAT));
    }

    private ComponentsUtils() {
        // prevent instantiation
    }
}
