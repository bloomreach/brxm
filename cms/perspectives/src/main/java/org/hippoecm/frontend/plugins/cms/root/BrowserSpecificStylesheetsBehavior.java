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
package org.hippoecm.frontend.plugins.cms.root;

import org.apache.wicket.IClusterable;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.apache.wicket.request.ClientInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrowserSpecificStylesheetsBehavior extends HeaderContributor {
    private static final long serialVersionUID = 1L;


    static final Logger log = LoggerFactory.getLogger(BrowserSpecificStylesheetsBehavior.class);

    public BrowserSpecificStylesheetsBehavior(final StylesheetConfiguration... configurations) {
        super(new IHeaderContributor() {
            private static final long serialVersionUID = 1L;

            public void renderHead(IHeaderResponse response) {
                Browser userBrowser = getBrowser();
                if (userBrowser != null && userBrowser.userAgent != UserAgent.UNSUPPORTED) {
                    for (StylesheetConfiguration ssc : configurations) {
                        if (loadStylesheets(userBrowser, ssc)) {
                            for (String location : ssc.styleSheets) {
                                HeaderContributor.forCss(location).renderHead(response);
                            }
                        }
                    }
                }
            }

            private boolean loadStylesheets(Browser userBrowser, StylesheetConfiguration conf) {
                if (userBrowser.userAgent != conf.browser.userAgent) {
                    return false;
                }
                if (conf.browser.majorVersion > -1 && conf.browser.majorVersion != userBrowser.majorVersion) {
                    return false;
                }
                if (conf.browser.minorVersion > -1 && conf.browser.minorVersion != userBrowser.minorVersion) {
                    return false;
                }
                return true;
            }

            private Browser getBrowser() {
                ClientInfo info = RequestCycle.get().getClientInfo();
                if (info instanceof WebClientInfo) {
                    return new Browser((WebClientInfo) info);
                }
                return null;
            }
        });
    }

    public static class StylesheetConfiguration implements IClusterable {
        private static final long serialVersionUID = 1L;
        
        Browser browser;
        String[] styleSheets;

        public StylesheetConfiguration(Browser browser, String... styleSheets) {
            this.browser = browser;
            this.styleSheets = styleSheets;
        }

    }

    public static enum UserAgent {
        IE, FIREFOX, SAFARI, OPERA, UNSUPPORTED
    };

    public static class Browser implements IClusterable {
        private static final long serialVersionUID = 1L;

        UserAgent userAgent;
        int majorVersion = -1;
        int minorVersion = -1;

        public Browser(WebClientInfo webInfo) {
            majorVersion = webInfo.getProperties().getBrowserVersionMajor();
            minorVersion = webInfo.getProperties().getBrowserVersionMinor();

            if (webInfo.getProperties().isBrowserInternetExplorer()) {
                userAgent = UserAgent.IE;
            } else if (webInfo.getProperties().isBrowserMozillaFirefox()) {
                userAgent = UserAgent.FIREFOX;
            } else if (webInfo.getProperties().isBrowserSafari()) {
                userAgent = UserAgent.SAFARI;
            } else if (webInfo.getProperties().isBrowserOpera()) {
                userAgent = UserAgent.OPERA;
            } else {
                userAgent = UserAgent.UNSUPPORTED;
            }
        }

        public Browser(UserAgent ua) {
            userAgent = ua;
        }

        public Browser(UserAgent ua, int major) {
            this(ua);
            majorVersion = major;
        }

        public Browser(UserAgent ua, int major, int minor) {
            this(ua, major);
            minorVersion = minor;
        }
    }
}
