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

import javax.servlet.http.Cookie;

import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebResponse;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

public class PersistentLogoutPlugin extends LogoutPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public PersistentLogoutPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(new AjaxLink("logout-force-link") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                Cookie[] cookies = ((WebRequest)RequestCycle.get().getRequest()).getHttpServletRequest().getCookies();
                for (int i = 0; i < cookies.length; i++) {
                    if ("org.hippoecm.frontend.plugins.login.RememberMeLoginPlugin".equals(cookies[i].getName()) ||
                            getClass().getName().equals(cookies[i].getName())) {
                        ((WebResponse)RequestCycle.get().getResponse()).clearCookie(cookies[i]);
                    }
                }
                PersistentLogoutPlugin.this.logout();
            }
        });
    }
}
