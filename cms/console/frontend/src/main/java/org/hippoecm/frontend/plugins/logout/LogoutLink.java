/*
 *  Copyright 2008-2016 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.service.ILogoutService;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogoutLink extends Panel {

    private static final Logger log = LoggerFactory.getLogger(LogoutLink.class);

    public LogoutLink(final String id, final ILogoutService logoutService, final IDialogService dialogService) {
        super(id);

        final AjaxLink logoutLink = new AjaxLink("logout-link") {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                final UserSession userSession = UserSession.get();
                try {
                    final Node rootNode = userSession.getRootNode();
                    if (rootNode != null && rootNode.getSession().hasPendingChanges()) {
                        final LogoutDialog dialog = new LogoutDialog(logoutService);
                        dialogService.show(dialog);
                    } else {
                        logoutService.logout();
                    }
                } catch (final RepositoryException e) {
                    log.error("Error while logging out", e);
                }
            }
        };

        add(logoutLink);
        logoutLink.add(new Label("logout-label", "Logout"));
    }

}
