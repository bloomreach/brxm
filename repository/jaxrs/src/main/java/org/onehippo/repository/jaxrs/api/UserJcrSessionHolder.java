/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.jaxrs.api;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserJcrSessionHolder implements HttpSessionActivationListener, HttpSessionBindingListener {

    private static final Logger log = LoggerFactory.getLogger(UserJcrSessionHolder.class);

    public static String JCR_SESSION_HOLDER_ATTR = UserJcrSessionHolder.class.getName() +".session";

    // transient since not serializable and UserJcrSessionHolder is stored as http session attr
    private transient Session userSession;

    public UserJcrSessionHolder(final Session userSession) {
        this.userSession = userSession;
    }

    public Session getSession() {
        return userSession;
    }

    @Override
    public void sessionWillPassivate(final HttpSessionEvent se) {
        logoutSession(se.getSession().getAttribute(JCR_SESSION_HOLDER_ATTR));
    }

    @Override
    public void valueBound(final HttpSessionBindingEvent event) {
        if (!JCR_SESSION_HOLDER_ATTR.equals(event.getName())) {
            log.error("Unexpected valueUnbound event for '{}'", event.getName());
        }
    }

    @Override
    public void valueUnbound(final HttpSessionBindingEvent event) {
        if (!JCR_SESSION_HOLDER_ATTR.equals(event.getName())) {
            log.error("Unexpected valueUnbound event for '{}'", event.getName());
            return;
        }

        logoutSession(event.getValue());
    }

    @Override
    public void sessionDidActivate(final HttpSessionEvent se) {
    }

    private void logoutSession(final Object value) {
        if (!(value instanceof UserJcrSessionHolder)) {
            log.error("Unexpected object '{}'.", value);
            return;
        }
        final UserJcrSessionHolder sessionHolder = (UserJcrSessionHolder)value;
        if (sessionHolder == null) {
            return;
        }
        final Session session = sessionHolder.getSession();
        if (sessionHolder != null && session.isLive()) {
            try {
                if (userSession.hasPendingChanges()) {
                    log.error("Logging out user session '{}' that has pending changes.", userSession.getUserID());
                }
            } catch (RepositoryException e) {
                log.error("Repository exception", e);
            }
            session.logout();
        }
    }

}
