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
package org.onehippo.cms7.utilities.servlet;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 *     A wrapper class for a {@link Session} which when used as an object in an {@link HttpSession}
 *     attribute will make sure that the wrapped {@link Session} will be logged out when the {@link HttpSession}
 *     passivates or gets logged out.
 * </p>
 * <p>
 *     If a developer logs (which in general (s)he shouldn't do because then better not use this
 *     {@link HttpSessionBoundJcrSessionHolder} at all) out the {@link Session} returned
 *     from {@link #getOrCreateJcrSession(String, HttpSession, SimpleCredentials, JcrSessionCreator)},
 *     then on next invocation of {@link #getOrCreateJcrSession(String, HttpSession, SimpleCredentials, JcrSessionCreator)} a
 *     new {@link Session} will be created and stored on the http session.
 * </p>
 * <p>
 *     When the jcr session has pending changes when being logged out due to deactivation or http session being destroyed,
 *     an error is logged since this should never be the case.
 * </p>
 */

@SuppressWarnings("unused")
public class HttpSessionBoundJcrSessionHolder implements HttpSessionActivationListener, HttpSessionBindingListener {

    private static final Logger log = LoggerFactory.getLogger(HttpSessionBoundJcrSessionHolder.class);

    // transient since not serializable and HttpSessionBoundJcrSessionHolder is stored as http session attr
    private transient Session session;
    private final String httpSessionAttributeName;


    /**
     * <p>
     *     Method for getting hold of a jcr session by either creating it and bind it to the {@link HttpSession} or
     *     by fetching it from the {@link HttpSession}. The {@code httpSessionAttributeName} will be used in combination
     *     with the {@code credentials} userId as http session attribute to store this {@link HttpSessionBoundJcrSessionHolder}
     *     for.
     * </p>
     * @param httpSessionAttributeNamePrefix the attribute name for the http session which will be used as prefix under which this
     * {@link HttpSessionBoundJcrSessionHolder} object will be stored. The final attribute name will use the
     *                                 httpSessionAttributeName + delimiter + the credentials userId.
     *
     * @param httpSession the {@link HttpSession} to store this {@link HttpSessionBoundJcrSessionHolder} object on
     * @param credentials the {@link javax.jcr.Credentials} for the to be created session
     * @param jcrSessionCreator the object that can create a session for the {@code credentials}
     * @return
     * @throws RepositoryException in case the {@code jcrSessionCreator} could not create a {@link Session},
     */
    public static Session getOrCreateJcrSession(final String httpSessionAttributeNamePrefix,
                                                final HttpSession httpSession,
                                                final SimpleCredentials credentials,
                                                final JcrSessionCreator jcrSessionCreator) throws RepositoryException {

        final String httpSessionAttributeName = httpSessionAttributeNamePrefix + '\uFFFF' + credentials.getUserID();

        final HttpSessionBoundJcrSessionHolder holder = (HttpSessionBoundJcrSessionHolder) httpSession.getAttribute(httpSessionAttributeName);
        Session session = null;
        if (holder != null) {
            session = holder.getSession();
        }

        if (session != null && !session.isLive()) {
            log.info("Session '{}' has been logged out, logging in new session", session.getUserID());
            // below jcrSessionCreator.login(credentials) results in a valueUnbound of the previous
            // HttpSessionBoundJcrSessionHolder object bound to the http session, which will trigger a #logoutSession()
            // which will result in nothing because the session
            // instance bound to the previous HttpSessionBoundJcrSessionHolder is already not live any more
        }

        if (session == null || !session.isLive()) {
            session = jcrSessionCreator.login(credentials);
            httpSession.setAttribute(httpSessionAttributeName, new HttpSessionBoundJcrSessionHolder(session, httpSessionAttributeName));
        }

        return session;
    }

    private HttpSessionBoundJcrSessionHolder(final Session session, final String httpSessionAttributeName) {
        this.session = session;
        this.httpSessionAttributeName = httpSessionAttributeName;
    }

    public Session getSession() {
        return session;
    }

    @Override
    public void sessionWillPassivate(final HttpSessionEvent se) {
        logoutSession();
    }

    @Override
    public void valueBound(final HttpSessionBindingEvent event) {
        if (!httpSessionAttributeName.equals(event.getName())) {
            log.error("Unexpected valueBound event for '{}'", event.getName());
        }
    }

    @Override
    public void valueUnbound(final HttpSessionBindingEvent event) {
        if (!httpSessionAttributeName.equals(event.getName())) {
            log.error("Unexpected valueUnbound event for '{}'", event.getName());
            return;
        }

        logoutSession();
    }

    @Override
    public void sessionDidActivate(final HttpSessionEvent se) {
    }

    private void logoutSession() {
        if (session != null && session.isLive()) {
            try {
                if (session.hasPendingChanges()) {
                    log.error("Logging out user session '{}' that has pending changes.", session.getUserID());
                }
            } catch (RepositoryException e) {
                log.error("Repository exception", e);
            }
            session.logout();
        }
        session = null;
    }

    // not using java.util.function.Function since we need to be able to throw a RepositoryException
    @FunctionalInterface
    public interface JcrSessionCreator {
        Session login(Credentials credentials) throws RepositoryException;
    }
}
