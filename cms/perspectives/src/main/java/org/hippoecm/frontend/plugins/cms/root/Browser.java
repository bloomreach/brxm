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
package org.hippoecm.frontend.plugins.cms.root;

import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.apache.wicket.util.io.IClusterable;

public class Browser implements IClusterable {
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
