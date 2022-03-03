/*
 *  Copyright 2008-2022 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.frontend.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.basjes.parse.useragent.UserAgent;

public class BrowserCheckBehavior extends Behavior {

    private static final Logger log = LoggerFactory.getLogger(BrowserCheckBehavior.class);

    private final BrowserCheck check;

    BrowserCheckBehavior(final String[] supported) {
        check = new BrowserCheck(supported);
    }

    @Override
    public void bind(final Component component) {
        final Main main = (Main) Main.get();
        final WebClientInfo clientInfo = WebSession.get().getClientInfo();
        final UserAgent.ImmutableUserAgent userAgent = main.getUserAgentAnalyzer().parse(clientInfo.getUserAgent());

        component.setVisible(!check.isSupported(new BrowserInfoImpl(userAgent)));
    }

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
     * edge 6 <=, safari 4 <, chrome, firefox 3 <=, opera
     */
    public static class Browser implements IClusterable {

        String agent;
        int majorVersion = -1;
        String modifier = "=";

        Browser(final String init) {
            final StringTokenizer st = new StringTokenizer(init, " ");

            agent = st.nextToken();
            if (st.hasMoreTokens()) {
                String major = st.nextToken();
                final int idx = major.indexOf(".");
                if (idx > -1) {
                    major = major.substring(0, idx);
                }
                majorVersion = Integer.parseInt(major);
            }

            if (st.hasMoreTokens()) {
                modifier = st.nextToken();
            }
        }

        public boolean is(final BrowserInfo info) {
            return isAgent(info) && isVersion(info);
        }

        private boolean isAgent(final BrowserInfo info) {
            return info.getAgentName().equalsIgnoreCase(agent);
        }

        private boolean isVersion(final BrowserInfo info) {
            return majorVersion <= -1 || validVersion(majorVersion, info.getMajorVersion());
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
    }

    public interface BrowserInfo {

        String getAgentName();

        int getMajorVersion();
    }

    private static class BrowserInfoImpl implements BrowserInfo {

        final UserAgent ua;

        public BrowserInfoImpl(final UserAgent ua) {
            this.ua = ua;
        }

        @Override
        public String getAgentName() {
            return ua.getValue(UserAgent.AGENT_NAME);
        }

        @Override
        public int getMajorVersion() {
            try {
                return Integer.parseInt(ua.getValue(UserAgent.AGENT_VERSION_MAJOR));
            } catch (NumberFormatException e) {
                log.warn("Failed to parse UserAgentMajorVersion as integer: {}", e.getMessage());
                return -1;
            }
        }
    }

}
