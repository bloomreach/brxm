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

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrowserSpecificStylesheetsBehavior extends Behavior {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(BrowserSpecificStylesheetsBehavior.class);

    private final StylesheetConfiguration[] configurations;

    public BrowserSpecificStylesheetsBehavior(final StylesheetConfiguration... configurations) {
        this.configurations = configurations;
    }

    public void renderHead(Component component, IHeaderResponse response) {
        Browser userBrowser = getBrowser();
        if (userBrowser != null && userBrowser.userAgent != UserAgent.UNSUPPORTED) {
            for (StylesheetConfiguration ssc : configurations) {
                if (loadStylesheets(userBrowser, ssc)) {
                    for (String location : ssc.styleSheets) {
                        response.render(CssHeaderItem.forUrl(location));
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
        final WebClientInfo clientInfo = WebSession.get().getClientInfo();
        return new Browser(clientInfo);
    }
}
