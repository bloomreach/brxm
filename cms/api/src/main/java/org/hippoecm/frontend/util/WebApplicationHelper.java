/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.RequestCycle;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebResponse;
import org.hippoecm.frontend.PluginApplication;


public class WebApplicationHelper {

    public static final String HIPPO_AUTO_LOGIN_COOKIE_BASE_NAME = "hal";
    public static final String REMEMBERME_COOKIE_BASE_NAME = "rememberme";


    private WebApplicationHelper() {
    }

    public static String getFullyQualifiedCookieName(String cookieBaseName) {
        validateNotBlank(cookieBaseName);

        return getApplicationName() + "." + cookieBaseName;
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

    public static String getApplicationName() {
        return PluginApplication.get().getPluginApplicationName();
    }

    protected static void validateNotBlank(String value) {
        if (value == null || "".equals(value)) {
            throw new IllegalArgumentException("Null or '' are not allowed values!");
        }
    }

}
