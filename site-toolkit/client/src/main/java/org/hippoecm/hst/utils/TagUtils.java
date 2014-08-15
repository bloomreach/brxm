/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.ResolvedVirtualHost;
import org.hippoecm.hst.util.HstRequestUtils;

public class TagUtils {

    private static final char DOUBLE_QUOTE = '"';

    /**
     * Returns the given map as a JSON map, with the keys and values in double quotes. Keys and values will be converted
     * to strings by calling {@link Object#toString()}.
     *
     * @param map a map (<code>null</code> or empty are allowed)
     * @return string representation of a JSON map.
     */
    public static String toJSONMap(Map<?, ?> map) {
        if (MapUtils.isEmpty(map)) {
            return "{}";
        }
        final StringBuilder builder = new StringBuilder("{");
        for (Map.Entry<?, ?> each : map.entrySet()) {
            doubleQuote(builder, each.getKey()).append(':');
            doubleQuote(builder, each.getValue()).append(',');
        }
        final int length = builder.length();
        return builder.replace(length - 1, length, "}").toString();
    }

    /**
     * Returns the string enclosed in HTML comments. Before enclosing the string will be whitespace trimmed.
     *
     * @param string a string (<code>null</code> is allowed)
     * @return trimmed string enclosed in HTML comments.
     */
    public static String encloseInHTMLComment(String string) {
        return String.format("<!-- %s -->", StringUtils.stripToEmpty(string));
    }

    private static StringBuilder doubleQuote(StringBuilder builder, Object value) {
        return builder.append(DOUBLE_QUOTE).append(value).append(DOUBLE_QUOTE);
    }

    public static void writeOrSetVar(final String url, final String var, final PageContext pageContext, final String scope) throws JspException {
        if (var == null) {
            try {
                JspWriter writer = pageContext.getOut();
                writer.print(url);
            } catch (IOException ioe) {
                throw new JspException(
                        "ResourceURL-Tag Exception: cannot write to the output writer.");
            }
        } else {
            int varScope = PageContext.PAGE_SCOPE;
            if (scope != null) {
                if ("request".equals(scope)) {
                    varScope = PageContext.REQUEST_SCOPE;
                } else if ("session".equals(scope)) {
                    varScope = PageContext.SESSION_SCOPE;
                } else if ("application".equals(scope)) {
                    varScope = PageContext.APPLICATION_SCOPE;
                }
            }
            pageContext.setAttribute(var, url, varScope);
        }
    }

    public static String createPathInfoWithoutRequestContext(final String path,
                                                             final Map<String, List<String>> parametersMap,
                                                             final List<String> removedParametersList,
                                                             final HttpServletRequest servletRequest) throws JspException {
        String pathInfo = path;
        if (!pathInfo.equals("") && !pathInfo.startsWith("/") && !pathInfo.startsWith("http://")
                && !pathInfo.startsWith("https://")) {
            pathInfo = "/" + pathInfo;
        }
        if (!parametersMap.isEmpty()) {
            try {
                String queryString = getQueryString(HstRequestUtils.getCharacterEncoding(servletRequest), parametersMap, removedParametersList);
                pathInfo += queryString;
            } catch (UnsupportedEncodingException e) {
                throw new JspException(e);
            }

        }
        ResolvedVirtualHost host = (ResolvedVirtualHost) servletRequest.getAttribute(ContainerConstants.VIRTUALHOSTS_REQUEST_ATTR);
        if (host != null) {
            if (host.getVirtualHost().isContextPathInUrl()) {
                return servletRequest.getContextPath() + pathInfo;
            } else {
                // skip the contextPath
                return pathInfo;
            }
        } else {
            // There is no VirtualHost on the request. Link will include the contextPath as we cannot do a
            // lookup in a virtual host whether the contextPath should be included or not;
            return servletRequest.getContextPath() + pathInfo;
        }
    }

    public static String getQueryString(String characterEncoding,
                                        final Map<String, List<String>> parametersMap,
                                        final List<String> removedParametersList) throws UnsupportedEncodingException {
        boolean firstParamDone = false;
        StringBuilder queryString = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : parametersMap.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                String name = entry.getKey();
                if(removedParametersList.contains(name)) {
                    // set to null by hst:param tag, thus skip
                    continue;
                }
                if (entry.getValue() != null) {
                    for (String value : entry.getValue()) {
                        if(value != null) {
                            queryString.append(firstParamDone ? "&" : "?").append(name).append("=").append(URLEncoder.encode(value, characterEncoding));
                            firstParamDone = true;
                        }
                    }
                }
            }
        }
        return queryString.toString();
    }
}
