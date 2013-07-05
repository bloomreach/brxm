/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.core.jcr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.onehippo.repository.security.domain.DomainRuleExtension;
import org.onehippo.repository.security.domain.FacetRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SessionSecurityDelegationImpl implements SessionSecurityDelegation {

    private static final Logger log = LoggerFactory.getLogger(SessionSecurityDelegationImpl.class);

    private static final String SESSIONS_KEY_MAP_ATTR_NAME = SessionSecurityDelegationImpl.class.getName() + ".sessions.map";
    private static final String SESSIONS_KEY_LIST_ATTR_NAME = SessionSecurityDelegationImpl.class.getName() + ".sessions.list";

    private Repository repository;
    private Credentials previewCredentials;
    private Credentials liveCredentials;
    private boolean securityDelegationEnabled;


    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setPreviewCredentials(final Credentials credentials) {
        this.previewCredentials = credentials;
    }

    public void setLiveCredentials(final Credentials credentials) {
        this.liveCredentials = credentials;
    }

    public void setSecurityDelegationEnabled(boolean securityDelegationEnabled) {
        this.securityDelegationEnabled = securityDelegationEnabled;
    }

    @Override
    public boolean sessionSecurityDelegationEnabled() {
        return securityDelegationEnabled;
    }

    @Override
    public void cleanupSessionDelegates(HstRequestContext requestContext) {
        List<Session> sessionList = (List<Session>)requestContext.getAttribute(SESSIONS_KEY_LIST_ATTR_NAME);
        if (sessionList != null) {
            for (Session session : sessionList) {
                if (session.isLive()) {
                    session.logout();
                }
            }
            sessionList.clear();
        }
        Map<String, Session> sessionMap = (Map<String, Session>)requestContext.getAttribute(SESSIONS_KEY_MAP_ATTR_NAME);
        if (sessionMap != null) {
            for (Session session : sessionMap.values()) {
                if (session.isLive()) {
                    session.logout();
                }
            }
            sessionMap.clear();
        }
    }

    @Override
    public Session getDelegatedSession(final Credentials creds) throws RepositoryException {
        return repository.login(creds);
    }

    @Override
    public Session getOrCreateLiveSecurityDelegate(final Credentials delegate, final String key) throws RepositoryException, IllegalStateException {
        return createLiveSecurityDelegate(delegate, key, true);
    }

    @Override
    public Session createLiveSecurityDelegate(final Credentials delegate, final boolean autoLogout) throws RepositoryException, IllegalStateException {
        return createLiveSecurityDelegate(delegate, null, autoLogout);
    }

    private Session createLiveSecurityDelegate(final Credentials delegate, final String key, final boolean autoLogout) throws RepositoryException, IllegalStateException {
        final FacetRule facetRule = new FacetRule(HippoNodeType.HIPPO_AVAILABILITY, "live", true, true, PropertyType.STRING);
        final DomainRuleExtension dre = new DomainRuleExtension("*", "*", Arrays.asList(facetRule));
        return createSecurityDelegate(liveCredentials, delegate, key, autoLogout, dre);
    }

    @Override
    public Session getOrCreatePreviewSecurityDelegate(final Credentials delegate, final String key) throws RepositoryException, IllegalStateException {
        return createPreviewSecurityDelegate(delegate, key, true);
    }

    @Override
    public Session createPreviewSecurityDelegate(final Credentials delegate, final boolean autoLogout) throws RepositoryException, IllegalStateException {
        return createPreviewSecurityDelegate(delegate, null, autoLogout);
    }

    private Session createPreviewSecurityDelegate(final Credentials delegate, final String key, final boolean autoLogout) throws RepositoryException, IllegalStateException {
        final FacetRule facetRule = new FacetRule(HippoNodeType.HIPPO_AVAILABILITY, "preview", true, true, PropertyType.STRING);
        final DomainRuleExtension dre = new DomainRuleExtension("*", "*", Arrays.asList(facetRule));
        return createSecurityDelegate(previewCredentials, delegate, key, autoLogout, dre);
    }

    @Override
    public Session createSecurityDelegate(final Credentials cred1,
                                          final Credentials cred2,
                                          final boolean autoLogout,
                                          final DomainRuleExtension... domainExtensions) throws RepositoryException, IllegalStateException {
        return createSecurityDelegate(cred1, cred2, null, autoLogout, domainExtensions);
    }


    private Session createSecurityDelegate(final Credentials cred1,
                                          final Credentials cred2,
                                          final String key,
                                          final boolean autoLogout,
                                          final DomainRuleExtension... domainExtensions) throws RepositoryException, IllegalStateException {
        if (!securityDelegationEnabled) {
            throw new IllegalStateException("Security delegation is not enabled");
        }

        final HstRequestContext requestContext = RequestContextProvider.get();
        if (autoLogout && key != null) {
            if (requestContext == null) {
                throw new IllegalStateException("Cannot automatically logout jcr session since there is no HstRequestContext");
            }
            Map<String, Session> sessionMap = (Map<String, Session>)requestContext.getAttribute(SESSIONS_KEY_MAP_ATTR_NAME);
            if (sessionMap != null) {
                Session existing = sessionMap.get(key);
                if (existing != null) {
                    return existing;
                }
            }
        }

        long start = System.currentTimeMillis();
        Session jcrSession = null;
        Session session1 = repository.login(cred1);
        if (!(session1 instanceof HippoSession)) {
            session1.logout();
            throw new IllegalStateException("Repository returned Session is not a HippoSession.");
        }

        Session session2 = null;
        try {
            session2 = repository.login(cred2);
            jcrSession = ((HippoSession) session1).createSecurityDelegate(session2, domainExtensions);
        } finally {
            if (session1 != null) {
                session1.logout();
            }
            if (session2 != null) {
                session2.logout();
            }
        }
        log.debug("Acquiring security delegate session took '{}' ms.", (System.currentTimeMillis() - start));

        if (autoLogout) {
            if (requestContext == null) {
                throw new IllegalStateException("Cannot automatically logout jcr session since there is no HstRequestContext");
            }
            if (key == null) {
                storeInList(jcrSession, requestContext);
            } else {
                storeInMap(jcrSession, key, requestContext);
            }
        }

        return jcrSession;
    }

    private void storeInList(final Session jcrSession, final HstRequestContext requestContext) {
        List<Session> sessionList = (List<Session>)requestContext.getAttribute(SESSIONS_KEY_LIST_ATTR_NAME);
        if (sessionList == null) {
            sessionList = new ArrayList<Session>();
            requestContext.setAttribute(SESSIONS_KEY_LIST_ATTR_NAME, sessionList);
        }
        sessionList.add(jcrSession);
    }

    private void storeInMap(final Session jcrSession, final String key, final HstRequestContext requestContext) {
        Map<String, Session> sessionMap = (Map<String, Session>)requestContext.getAttribute(SESSIONS_KEY_MAP_ATTR_NAME);
        if (sessionMap == null) {
            sessionMap = new HashMap<String, Session>();
            requestContext.setAttribute(SESSIONS_KEY_MAP_ATTR_NAME, sessionMap);
        }
        sessionMap.put(key, jcrSession);
    }
}
