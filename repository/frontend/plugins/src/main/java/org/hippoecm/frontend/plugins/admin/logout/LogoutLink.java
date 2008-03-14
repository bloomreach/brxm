/*
 * Copyright 2008 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.admin.logout;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.dialog.DynamicDialogFactory;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.ChannelFactory;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A panel containing a logout link, which on "click", shows a confirmation pop up if there are
 * pending changes, or immediately logs out if there are no pending changes.
 *
 */
public class LogoutLink extends Panel {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(LogoutLink.class);

    public LogoutLink(String id, String linktext, Class clazz, JcrNodeModel model, Channel channel,
            ChannelFactory factory) {
        super(id, model);

        Channel proxy = factory.createChannel();
        final DialogWindow dialogWindow = new DialogWindow("dialog", model, channel, proxy);
        dialogWindow.setPageCreator(new DynamicDialogFactory(dialogWindow, clazz));
        add(dialogWindow);

        AjaxLink logoutLink = new AjaxLink("logout-link") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                UserSession userSession = (UserSession) getSession();
                try {
                    Node rootNode = userSession.getRootNode();
                    if (rootNode != null && rootNode.getSession().hasPendingChanges()) {
                        dialogWindow.show(target);
                    } else {
                        userSession.logout();
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
