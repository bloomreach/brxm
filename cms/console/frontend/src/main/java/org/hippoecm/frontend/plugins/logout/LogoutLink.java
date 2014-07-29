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
package org.hippoecm.frontend.plugins.logout;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.protocol.http.WebApplication;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.util.WebApplicationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogoutLink extends Panel {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(LogoutLink.class);

    public LogoutLink(String id, IPluginContext context) {
        super(id);

        final DialogWindow dialogWindow = new DialogWindow("dialog");
        add(dialogWindow);

        AjaxLink logoutLink = new AjaxLink("logout-link") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                // Remove the Hippo Auto Login cookie
                WebApplicationHelper.clearCookie(WebApplicationHelper.getFullyQualifiedCookieName(WebApplicationHelper.HIPPO_AUTO_LOGIN_COOKIE_BASE_NAME));

                UserSession userSession = UserSession.get();
                try {
                    Node rootNode = userSession.getRootNode();
                    if (rootNode != null && rootNode.getSession().hasPendingChanges()) {
                        final LogoutDialog dialog = new LogoutDialog();
                        dialogWindow.show(dialog);
                    } else {
                        userSession.logout();
                        if (WebApplication.exists()) {
                            throw new RestartResponseException(WebApplication.get().getHomePage());
                        }
                    }
                } catch (RepositoryException e) {
                    log.error(e.getMessage());
                }
            }
        };

        add(logoutLink);
        logoutLink.add(new Label("logout-label", "Logout"));
    }

}
