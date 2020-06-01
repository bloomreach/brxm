/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.cache.esi;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.map.LazyMap;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.util.HstRequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.PropertyPlaceholderHelper.PlaceholderResolver;

/**
 * ESIVarsPlaceholderResolver
 */
public class ESIVarsPlaceholderResolver implements PlaceholderResolver {

    private static final String USER_AGENT_BROWSER_MSIE = "MSIE";
    private static final String USER_AGENT_BROWSER_OPERA = "OPERA";
    private static final String USER_AGENT_BROWSER_CHROME = "CHROME";
    private static final String USER_AGENT_BROWSER_FIREFOX = "FIREFOX";
    private static final String USER_AGENT_BROWSER_MOZILLA = "MOZILLA";

    private static final String USER_AGENT_OS_WINDOWS = "WIN";
    private static final String USER_AGENT_OS_MAC = "MAC";
    private static final String USER_AGENT_OS_LINUX = "LINUX";
    private static final String USER_AGENT_OS_UNIX = "UNIX";
    private static final String USER_AGENT_OS_OTHER = "OTHER";

    private static Logger log = LoggerFactory.getLogger(ESIVarsPlaceholderResolver.class);

    private static final Pattern DICTIONARY_VAR_PATTERN = Pattern.compile("([^\\{\\}]+)\\{([^\\{\\}]+)\\}");

    private static final Pattern UA_MSIE_PATTERN = Pattern.compile("\\sMSIE ([\\d\\.]+);");
    private static final Pattern UA_OPERA_PATTERN = Pattern.compile("^Opera/([\\d\\.]+)\\s");
    private static final Pattern UA_CHROME_PATTERN = Pattern.compile("\\sChrome/([\\d\\.]+)");
    private static final Pattern UA_FIREFOX_PATTERN = Pattern.compile("\\sFirefox/([\\d\\.]+)");
    private static final Pattern UA_MOZILLA_PATTERN = Pattern.compile("^Mozilla/([\\d\\.]+)\\s");

    private HttpServletRequest request;
    private Map<String, Object> varsMap = new HashMap<String, Object>();

    private UserAgentInfo uai = new UserAgentInfo();

    public ESIVarsPlaceholderResolver(HttpServletRequest servletRequest) {
        this.request = servletRequest;

        List<String> acceptLanguageList = 
                Arrays.asList(StringUtils.split(StringUtils.defaultIfEmpty(request.getHeader("Accept-Language"), ""), " ,"));
        varsMap.put("HTTP_ACCEPT_LANGUAGE", acceptLanguageList);

        Map cookiesMap = LazyMap.decorate(new HashMap<String, String>(), new Transformer() {
            @Override
            public Object transform(Object cookieName) {
                Cookie cookie = getCookie((String) cookieName);

                if (cookie != null) {
                    return cookie.getValue();
                }

                return null;
            }
        });

        varsMap.put("HTTP_COOKIE", cookiesMap);

        varsMap.put("HTTP_HOST", HstRequestUtils.getFarthestRequestHost(request));

        varsMap.put("HTTP_REFERER", request.getHeader("Referer"));

        Map uaMap = LazyMap.decorate(new HashMap<String, String>(), new Transformer() {
            @Override
            public Object transform(Object key) {
                if (StringUtils.isEmpty(uai.getBrowser())) {
                    parseUserAgent(request, uai);
                }

                if ("browser".equals(key)) {
                    return uai.getBrowser();
                } else if ("version".equals(key)) {
                    return uai.getVersion();
                } else if ("os".equals(key)) {
                    return uai.getOs();
                }

                return null;
            }
        });

        varsMap.put("HTTP_USER_AGENT", uaMap);

        Map queryStringMap = LazyMap.decorate(new HashMap<String, String>(), new Transformer() {
            @Override
            public Object transform(Object paramName) {
                return request.getParameter((String) paramName);
            }
        });

        varsMap.put("QUERY_STRING", queryStringMap);
    }

    @Override
    public String resolvePlaceholder(String varName) {
        Object value = null;
        Matcher m = DICTIONARY_VAR_PATTERN.matcher(varName);

        if (m.find()) {
            Object col = varsMap.get(m.group(1));

            if (col instanceof Map) {
                value = ((Map) col).get(m.group(2));
            } else if (col instanceof List) {
                value = Boolean.valueOf(((List) col).contains(m.group(2)));
            }
        } else {
            value = varsMap.get(varName);
        }

        if (value == null) {
            log.warn("No value found for the ESI variable name: '{}'", varName);
            return null;
        }

        return value.toString();
    }

    private Cookie getCookie(String cookieName) {
        Cookie [] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (StringUtils.equals(cookie.getName(), cookieName)) {
                return cookie;
            }
        }

        return null;
    }

    protected UserAgentInfo parseUserAgent(HttpServletRequest request, UserAgentInfo uai) {
        String userAgent = request.getHeader("User-Agent");

        if (userAgent == null) {
            return null;
        }

        if (StringUtils.contains(userAgent, "Windows")) {
            uai.setOs(USER_AGENT_OS_WINDOWS);
        } else if (StringUtils.contains(userAgent, "Macintosh")) {
            uai.setOs(USER_AGENT_OS_MAC);
        } else if (StringUtils.contains(userAgent, "Linux")) {
            uai.setOs(USER_AGENT_OS_LINUX);
        } else if (StringUtils.contains(userAgent, "X11; U;")) {
            uai.setOs(USER_AGENT_OS_UNIX);
        } else {
            uai.setOs(USER_AGENT_OS_OTHER);
        }

        boolean done = false;
        Matcher m = UA_MSIE_PATTERN.matcher(userAgent);

        if (m.find()) {
            uai.setBrowser(USER_AGENT_BROWSER_MSIE);
            uai.setVersion(m.group(1));
            done = true;
        }

        if (!done) {
            m = UA_OPERA_PATTERN.matcher(userAgent);

            if (m.find()) {
                uai.setBrowser(USER_AGENT_BROWSER_OPERA);
                uai.setVersion(m.group(1));
                done = true;
            }
        }

        if (!done) {
            m = UA_CHROME_PATTERN.matcher(userAgent);

            if (m.find()) {
                uai.setBrowser(USER_AGENT_BROWSER_CHROME);
                uai.setVersion(m.group(1));
                done = true;
            }
        }

        if (!done) {
            m = UA_FIREFOX_PATTERN.matcher(userAgent);

            if (m.find()) {
                uai.setBrowser(USER_AGENT_BROWSER_FIREFOX);
                uai.setVersion(m.group(1));
                done = true;
            }
        }

        if (!done) {
            m = UA_MOZILLA_PATTERN.matcher(userAgent);

            if (m.find()) {
                uai.setBrowser(USER_AGENT_BROWSER_MOZILLA);
                uai.setVersion(m.group(1));
                done = true;
            }
        }

        return uai;
    }

    public static class UserAgentInfo {

        private String browser;
        private String version;
        private String os;

        public String getBrowser() {
            return browser;
        }

        public void setBrowser(String browser) {
            this.browser = browser;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getOs() {
            return os;
        }

        public void setOs(String os) {
            this.os = os;
        }
    }
}
