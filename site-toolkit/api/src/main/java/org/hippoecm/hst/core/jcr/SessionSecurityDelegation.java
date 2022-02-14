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

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.repository.security.domain.DomainRuleExtension;

/**
 * Component that has facility methods for creating <b>NON POOLED</b> session (security delegates) that are optionally
 * logged out automatically at the end of the hst request processing
 */
public interface SessionSecurityDelegation {

    /**
     * cleans up any sessions that are not yet logged out and were created either through {@link #getOrCreateLiveSecurityDelegate(javax.jcr.Credentials, String)},
     * or through {@link #getOrCreatePreviewSecurityDelegate(javax.jcr.Credentials, String)} or through one of the create methods
     * with <code>autoLogout</code> set to <code>true</code>
     *
     * @param requestContext
     */
    void cleanupSessionDelegates(HstRequestContext requestContext);

    /**
     * @return A non pooled jcr session which is <b>not</b> automatically logged out and is <b>NOT</b> combined with
     * the credentials of any other session: This is a plane delegated repository login, not a security delegate.
     * @throws RepositoryException
     */
    Session getDelegatedSession(Credentials creds) throws RepositoryException;


    /**
     * @deprecated since 13.0.1 and 13.1.0 : Use {@link #createLiveSecurityDelegate(Credentials, boolean)} instead. The
     * {@code key} parameter is not needed any more since we don't support returning same jcr session based on cachekey
     * any more. Use autologout = true if you replace this method
     */
    @Deprecated
    Session getOrCreateLiveSecurityDelegate(Credentials delegate, String key) throws RepositoryException, IllegalStateException;

    /**
     * @param delegate the credentials of the <code>Session</code> to combine the access with the live session
     * @param autoLogout whether the HST should take care of automatically logging out the session at the end of the request
     * @return a security delegated session which combines the access control rules for {@link Session} belonging to <code>delegate</code>
     * and the normal hst live session credentials <b>with</b> the addition of an extra wildcard domain rule hippo:availability = live
     * @throws RepositoryException
     * @throws IllegalStateException if <code>securityDelegationEnabled</code> is false or in case the created sessions are not of type {@link org.hippoecm.repository.api.HippoSession}
     * or when <code>autoLogout</code> is <code>true</code> but there is not <code>HstRequestContext</code> available
     */
    Session createLiveSecurityDelegate(Credentials delegate, boolean autoLogout) throws RepositoryException, IllegalStateException;

    /**
     * @deprecated since 13.0.1 and 13.1.0 : Use {@link #createPreviewSecurityDelegate(Credentials, boolean)} instead. The
     * {@code key} parameter is not needed any more since we don't support returning same jcr session based on cachekey
     * any more. Use autologout = true if you replace this method
     */
    @Deprecated
    Session getOrCreatePreviewSecurityDelegate(Credentials delegate, String key) throws RepositoryException, IllegalStateException;

    /**
     * @param delegate the credentials of the <code>Session</code> to combine the access with the preview session
     * @param autoLogout whether the HST should take care of automatically logging out the session at the end of the request
     * @return a security delegated session which combines the access control rules for {@link Session} belonging to <code>delegate</code>
     * and the normal hst preview session credentials <b>with</b> the addition of an extra wildcard domain rule hippo:availability = preview
     * @throws RepositoryException
     * @throws IllegalStateException if <code>securityDelegationEnabled</code> is false or in case the created sessions are not of type {@link org.hippoecm.repository.api.HippoSession}
     * or when <code>autoLogout</code> is <code>true</code> but there is not <code>HstRequestContext</code> available
     */
    Session createPreviewSecurityDelegate(Credentials delegate, boolean autoLogout) throws RepositoryException, IllegalStateException;

    /**
     *
     * @param cred1 credentials for the first {@link Session}
     * @param cred2 credentials for the second {@link Session}
     * @param autoLogout whether the HST should take care of automatically logging out the session at the end of the request
     * @param domainExtensions optional extra domain rules for the created delegate
     * @return a security delegated session which combines the access control rules for {@link Session} belonging to <code>cred1</code>
     * and <code>cred2</code> with the optional addition of custom domain rules <code>domainExtensions</code>
     * @throws RepositoryException
     * @throws IllegalStateException if <code>securityDelegationEnabled</code> is false or in case the created sessions are not of type {@link org.hippoecm.repository.api.HippoSession}
     * or when <code>autoLogout</code> is <code>true</code> but there is not <code>HstRequestContext</code> available
     */
    Session createSecurityDelegate(Credentials cred1,
                                   Credentials cred2,
                                   boolean autoLogout,
                                   DomainRuleExtension... domainExtensions) throws RepositoryException, IllegalStateException;
}
