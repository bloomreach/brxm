/*
 * Copyright 2013-2022 Hippo B.V. (http://www.onehippo.com)
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
import java.util.List;

import javax.jcr.Credentials;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
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

    private static final String AUTO_LOGOUT_SESSIONS_LIST_ATTR_NAME = SessionSecurityDelegationImpl.class.getName() + ".auto.logout.sessions.list";

    private Repository repository;
    private Credentials previewCredentials;
    private Credentials liveCredentials;


    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setPreviewCredentials(final Credentials credentials) {
        this.previewCredentials = credentials;
    }

    public void setLiveCredentials(final Credentials credentials) {
        this.liveCredentials = credentials;
    }

    @Override
    public void cleanupSessionDelegates(HstRequestContext requestContext) {
        List<Session> autoLogoutList = getAutoLogoutSessionList(requestContext);
        if (autoLogoutList != null) {
            for (Session session : autoLogoutList) {
                if (session.isLive()) {
                    session.logout();
                }
            }
            autoLogoutList.clear();
        }
    }

    @Override
    public Session getDelegatedSession(final Credentials creds) throws RepositoryException {
        return repository.login(creds);
    }

    @Deprecated
    @Override
    public Session getOrCreateLiveSecurityDelegate(final Credentials delegate, final String key) throws RepositoryException, IllegalStateException {
        return createLiveSecurityDelegate(delegate, true);
    }

    @Override
    public Session createLiveSecurityDelegate(final Credentials delegate, final boolean autoLogout) throws RepositoryException, IllegalStateException {
        return doCreateLiveSecurityDelegate(delegate, autoLogout);
    }

    private Session doCreateLiveSecurityDelegate(final Credentials delegate, final boolean autoLogout) throws RepositoryException, IllegalStateException {
        final FacetRule facetRule = new FacetRule(HippoNodeType.HIPPO_AVAILABILITY, "live", true, true, PropertyType.STRING);
        final DomainRuleExtension dre = new DomainRuleExtension("*", "*", Arrays.asList(facetRule));
        return doCreateSecurityDelegate(liveCredentials, delegate, autoLogout,  false, dre).getLeft();
    }

    @Deprecated
    @Override
    public Session getOrCreatePreviewSecurityDelegate(final Credentials delegate, final String key) throws RepositoryException, IllegalStateException {
        return doCreatePreviewSecurityDelegate(delegate, true,  false).getLeft();
    }

    @Override
    public Session createPreviewSecurityDelegate(final Credentials delegate, final boolean autoLogout) throws RepositoryException, IllegalStateException {
        return doCreatePreviewSecurityDelegate(delegate, autoLogout, false).getLeft();
    }

    /**
     * @param cmsUserCreds the credentials of the user with which to combine the HST preview user
     * @return Returns a {@link Pair} object where left contains the Session security delegate being a combination of p
     * review user and cmsUser authorization and right containe the Session for the cmsUserCreds
     */
    public Pair<Session, Session> createCmsUserAndChannelMgrPreviewUser(final Credentials cmsUserCreds) throws RepositoryException, IllegalStateException {
        return doCreatePreviewSecurityDelegate(cmsUserCreds, false, true);
    }

    private Pair<Session, Session> doCreatePreviewSecurityDelegate(final Credentials delegate, final boolean autoLogout,
                                                                   final boolean keepDelegateSession) throws RepositoryException, IllegalStateException {
        final FacetRule facetRule = new FacetRule(HippoNodeType.HIPPO_AVAILABILITY, "preview", true, true, PropertyType.STRING);
        final DomainRuleExtension dre = new DomainRuleExtension("*", "*", Arrays.asList(facetRule));
        return doCreateSecurityDelegate(previewCredentials, delegate, autoLogout, keepDelegateSession, dre);
    }

    @Override
    public Session createSecurityDelegate(final Credentials cred1,
                                          final Credentials delegatee,
                                          final boolean autoLogout,
                                          final DomainRuleExtension... domainExtensions) throws RepositoryException, IllegalStateException {
        return doCreateSecurityDelegate(cred1, delegatee, autoLogout, false, domainExtensions).getRight();
    }


    /**
     * returns a Pair for which the left contains the 'combined session authorization for cred1 and delegatee' and
     * right is null if {@code keepDelegateSession = false} or right contains the session for {@code delegatee} is
     * {@code keepDelegateSession = true}. Note the session for {@code delegatee} will never be logged out automatically
     * so 'autoLogout' only concerns the combined session
     */
    private Pair<Session, Session> doCreateSecurityDelegate(final Credentials cred1,
                                                final Credentials delegatee,
                                                final boolean autoLogout,
                                                final boolean keepDelegateSession,
                                                final DomainRuleExtension... domainExtensions) throws RepositoryException, IllegalStateException {

        final HstRequestContext requestContext = RequestContextProvider.get();

        long start = System.currentTimeMillis();
        Session combinedSession;

        Session session1 = null;
        try {
            session1 = repository.login(cred1);
        } catch (javax.jcr.LoginException e) {
            logWarningAndRethrow(cred1, e);
        }

        if (!(session1 instanceof HippoSession)) {
            session1.logout();
            throw new IllegalStateException("Repository returned Session is not a HippoSession.");
        }

        Session delegateeSession = null;
        try {
            try {
                delegateeSession = session1.impersonate(delegatee);
            } catch (javax.jcr.LoginException e) {
                logWarningAndRethrow(delegatee, e);

            }
            combinedSession = ((HippoSession) session1).createSecurityDelegate(delegateeSession, domainExtensions);
        } finally {
            session1.logout();
            if (delegateeSession != null && !keepDelegateSession) {
                delegateeSession.logout();
            }
        }
        log.debug("Acquiring security delegate session took '{}' ms.", (System.currentTimeMillis() - start));

        if (autoLogout) {
            if (requestContext == null) {
                throw new IllegalStateException("Cannot automatically logout jcr session since there is no HstRequestContext");
            }
            storeInAutoLogoutList(combinedSession, requestContext);

        }

        if (keepDelegateSession) {
            return new ImmutablePair<>(combinedSession, delegateeSession);
        } else {
            return new ImmutablePair<>(combinedSession, null);
        }
    }

    private void logWarningAndRethrow(final Credentials cred, final javax.jcr.LoginException e) throws javax.jcr.LoginException {
        if (cred == previewCredentials ) {
            log.error("Cannot create security delegate due to LoginException due to invalid preview credentials : {}", e.toString());
        } else if (cred == liveCredentials) {
            log.error("Cannot create security delegate due to LoginException due to invalid live credentials : {}", e.toString());
        } else {
            log.info("Cannot create security delegate due to LoginException : {}", e.toString());
        }
        throw e;
    }

    private void storeInAutoLogoutList(final Session jcrSession, final HstRequestContext requestContext) {
        List<Session> sessionList = getAutoLogoutSessionList(requestContext);
        if (sessionList == null) {
            sessionList = new ArrayList<>();
            requestContext.setAttribute(AUTO_LOGOUT_SESSIONS_LIST_ATTR_NAME, sessionList);
        }
        sessionList.add(jcrSession);
    }

    private List<Session> getAutoLogoutSessionList(final HstRequestContext requestContext) {
        return (List<Session>)requestContext.getAttribute(AUTO_LOGOUT_SESSIONS_LIST_ATTR_NAME);
    }

}
