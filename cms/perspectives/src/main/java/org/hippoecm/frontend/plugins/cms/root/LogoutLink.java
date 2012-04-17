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

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.Cookie;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebResponse;
import org.hippoecm.frontend.plugins.cms.admin.users.User;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogoutLink extends MarkupContainer {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(LogoutLink.class);

    @SuppressWarnings("unused")
    private String username;

    public LogoutLink(String id) {
        super(id);

        UserSession session = (UserSession) getSession();
        String userID = session.getJcrSession().getUserID();
        username = new User(userID).getDisplayName();
        
        add(new Label("username", new PropertyModel(this, "username")));

        add(new AjaxLink("logout-link") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                LogoutLink.this.logout();
            }
        });
    }

    protected void logout() {
        // Remove the Hippo Auto Login cookie
        Cookie cookie = ((WebRequest) RequestCycle.get().getRequest()).getCookie("hal");

        if (cookie != null) {
            cookie.setMaxAge(0);
            cookie.setValue("");
            ((WebResponse) RequestCycle.get().getResponse()).addCookie(cookie);
        }

        UserSession userSession = (UserSession)getSession();
        try {
            Session session = userSession.getJcrSession();
            if (session != null) {
                session.save();
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        userSession.logout();
    }
}
