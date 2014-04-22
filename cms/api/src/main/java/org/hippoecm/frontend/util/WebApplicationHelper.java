/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.resource.UrlResourceReference;
import org.hippoecm.frontend.PluginApplication;


public class WebApplicationHelper {

    public static final String HIPPO_AUTO_LOGIN_COOKIE_BASE_NAME = "hal";
    public static final String REMEMBERME_COOKIE_BASE_NAME = "rememberme";
    private static final String ANTI_CACHE_KEY = Long.toString(System.currentTimeMillis());

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

    public static ServletWebRequest retrieveWebRequest() {
        return (ServletWebRequest) RequestCycle.get().getRequest();
    }

    public static WebResponse retrieveWebResponse() {
        return (WebResponse) RequestCycle.get().getResponse();
    }

    public static String getApplicationName() {
        return PluginApplication.get().getPluginApplicationName();
    }

    public static boolean isPartOfPage(final Component component) {
        return component.findParent(Page.class) != null;
    }

    public static UrlResourceReference createUniqueUrlResourceReference(Url url) {
        url.addQueryParameter("antiCache", ANTI_CACHE_KEY);
        return new UrlResourceReference(url);
    }

    protected static void validateNotBlank(String value) {
        if (value == null || "".equals(value)) {
            throw new IllegalArgumentException("Null or '' are not allowed values!");
        }
    }

}
