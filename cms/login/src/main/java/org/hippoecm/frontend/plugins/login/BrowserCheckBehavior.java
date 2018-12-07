/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.login;

import java.util.StringTokenizer;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.apache.wicket.util.io.IClusterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrowserCheckBehavior extends Behavior {

    private static final Logger log = LoggerFactory.getLogger(BrowserCheckBehavior.class);

    private final BrowserCheck check;

    static class BrowserCheck implements IClusterable {

        Browser[] browsers;

        BrowserCheck(final String[] init) {
            browsers = new Browser[init.length];
            for (int i = 0; i < browsers.length; i++) {
                browsers[i] = new Browser(init[i]);
            }
        }

        boolean isSupported(final BrowserInfo info) {
            for (final Browser browser : browsers) {
                if (browser.is(info)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Supported browsers can be configured like:
     * ie 6 <=, safari 4 <, chrome, firefox 3.5 <=, opera
     */
    public static class Browser implements IClusterable {

        String agent;
        int majorVersion = -1;
        int minorVersion = -1;
        String modifier = "=";

        Browser(final String init) {
            final StringTokenizer st = new StringTokenizer(init, " ");

            agent = st.nextToken();
            if (st.hasMoreTokens()) {
                String major = st.nextToken(), minor = null;
                final int idx = major.indexOf(".");
                if (idx > -1) {
                    minor = major.substring(idx + 1);
                    major = major.substring(0, idx);
                }
                if (major != null) {
                    majorVersion = new Integer(major);
                }
                if (minor != null) {
                    minorVersion = new Integer(minor);
                }
            }

            if (st.hasMoreTokens()) {
                modifier = st.nextToken();
            }
        }

        public boolean is(final BrowserInfo info) {
            if (!isAgent(info)) {
                return false;
            }
            return isVersion(info);
        }

        private boolean isVersion(final BrowserInfo info) {
            if (majorVersion > -1 && !validVersion(majorVersion, info.getMajor())) {
                return false;
            }
            if (minorVersion > -1 && !validVersion(minorVersion, info.getMinor())) {
                return false;
            }
            return true;
        }

        private boolean validVersion(final int configured, final int provided) {
            switch (modifier) {
                case "=":
                    return configured == provided;
                case "<":
                    return provided < configured;
                case "<=":
                    return provided <= configured;
                case ">":
                    return provided > configured;
                case ">=":
                    return provided >= configured;
                default:
                    return false;
            }
        }

        private boolean isAgent(final BrowserInfo info) {
            switch (agent) {
                case "ie":
                    return info.isInternetExplorer();
                case "firefox":
                    return info.isFirefox();
                case "safari":
                    return info.isSafari();
                case "chrome":
                    return info.isChrome();
                case "opera":
                    return info.isOpera();
                case "edge":
                    return info.isEdge();
                default:
                    return false;
            }
        }

    }

    public interface BrowserInfo {

        boolean isOpera();

        boolean isChrome();

        boolean isSafari();

        boolean isFirefox();

        boolean isInternetExplorer();

        boolean isEdge();

        int getMajor();

        int getMinor();
    }

    public static class WicketBrowserInfo implements BrowserInfo {

        private static final String MSIE = "MSIE";
        private static final String CHROME = "Chrome";
        private static final String SHIRETOKO = "Shiretoko";
        private static final String FIREFOX = "Firefox";
        private static final String EDGE = "Edge";

        private final WebClientInfo info;
        private int major;
        private int minor;

        WicketBrowserInfo(final WebClientInfo info) {
            this.info = info;

            major = info.getProperties().getBrowserVersionMajor();
            minor = info.getProperties().getBrowserVersionMinor();
            if (major == -1) {
                if (isFirefox()) {
                    if (info.getProperties().isBrowserMozillaFirefox()) {
                        setVersions(FIREFOX);
                    } else if (info.getUserAgent().contains(SHIRETOKO)) {
                        setVersions(SHIRETOKO);
                    }
                } else if (isChrome()) {
                    setVersions(CHROME);
                } else if (isSafari() || isOpera()) {
                    setVersions("Version");
                } else if (isInternetExplorer()) {
                    final String ua = info.getUserAgent();
                    if (ua.indexOf(MSIE) > 0) {
                        final String v = ua.substring(ua.indexOf(MSIE));
                        parseMajorMinor(v.substring(5, v.indexOf(';')));
                    }
                } else if(isEdge()) {
                    setVersions(EDGE);
                }
            }
        }

        private void setVersions(final String string) {
            final String ua = info.getUserAgent();
            if (!ua.contains(string)) {
                return;
            }
            parseMajorMinor(ua.substring(ua.indexOf(string) + string.length() + 1));
        }

        private void parseMajorMinor(final String parse) {
            try {
                final StringTokenizer st = new StringTokenizer(parse.trim(), ". ");
                if (st.hasMoreTokens()) {
                    major = Integer.parseInt(st.nextToken());
                    if (st.hasMoreTokens()) {
                        minor = Integer.parseInt(st.nextToken());
                    }
                }
            } catch (final NumberFormatException ex) {
                log.info("Could not parse " + parse + ": " + ex.getMessage());
            }
        }

        public boolean isChrome() {
            return info.getProperties().isBrowserChrome();
        }

        public boolean isFirefox() {
            return info.getProperties().isBrowserMozillaFirefox()
                    || (info.getProperties().isBrowserMozilla() && info.getUserAgent().contains(SHIRETOKO));
        }

        public boolean isInternetExplorer() {
            return info.getProperties().isBrowserInternetExplorer();
        }

        public boolean isOpera() {
            return info.getProperties().isBrowserOpera();
        }

        public boolean isSafari() {
            return info.getProperties().isBrowserSafari() && !isChrome();
        }

        public boolean isEdge() {
            final String userAgent = info.getUserAgent();
            return userAgent.contains(EDGE);
        }

        public int getMajor() {
            return major;
        }

        public int getMinor() {
            return minor;
        }

    }

    BrowserCheckBehavior(final String[] supported) {
        check = new BrowserCheck(supported);
    }

    @Override
    public void bind(final Component component) {
        final WebClientInfo info = WebSession.get().getClientInfo();
        component.setVisible(!check.isSupported(new WicketBrowserInfo(info)));
    }
}
