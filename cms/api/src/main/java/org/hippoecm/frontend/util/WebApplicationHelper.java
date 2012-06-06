/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.util;

import javax.servlet.http.Cookie;

import org.apache.wicket.Application;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebResponse;


public class WebApplicationHelper {

    public final static String PLUGIN_APPLICATION_NAME_PARAMETER = "config";
    public static final String HIPPO_AUTO_LOGIN_COOKIE_BASE_NAME = "hal";
    public static final String REMEMBERME_COOKIE_BASE_NAME = "rememberme";

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private WebApplicationHelper() {
    }

    public static String getConfigurationParameter(String configParamName, String defaultValue) {
        validateNotBlank(configParamName);

        final String returnValue = getConfigurationParameter((WebApplication) Application.get(), configParamName, null);

        return (returnValue == null) ? defaultValue : returnValue;
    }

    public static String getFullyQualifiedCookieName(String cookieBaseName) {
        validateNotBlank(cookieBaseName);

        return getConfigurationParameter(PLUGIN_APPLICATION_NAME_PARAMETER, "cms") + "." + cookieBaseName;
    }

    public static void clearCookie(String cookieName) {
        validateNotBlank(cookieName);

        Cookie cookie = ((WebRequest) RequestCycle.get().getRequest()).getCookie(cookieName);

        if (cookie != null) {
            cookie.setMaxAge(0);
            cookie.setValue("");
            ((WebResponse) RequestCycle.get().getResponse()).addCookie(cookie);
        }
    }

    public static WebRequest retrieveWebRequest() {
        return (WebRequest) RequestCycle.get().getRequest();
    }

    public static WebResponse retrieveWebResponse() {
        return (WebResponse) RequestCycle.get().getResponse();
    }

    public static String getConfigurationParameter(WebApplication application, String parameterName, String defaultValue) {
        String result = application.getInitParameter(parameterName);

        if (result == null || result.equals("")) {
            result = application.getServletContext().getInitParameter(parameterName);
        }

        if (result == null || result.equals("")) {
            result = defaultValue;
        }

        return result;
    }

    protected static void validateNotBlank(String value) {
        if (value == null || "".equals(value)) {
            throw new IllegalArgumentException("Null or '' are not allowed values!");
        }
    }

}
