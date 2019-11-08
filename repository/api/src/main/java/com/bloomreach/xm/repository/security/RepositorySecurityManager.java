/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package com.bloomreach.xm.repository.security;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoSession;
import org.onehippo.repository.security.SecurityConstants;
import org.onehippo.repository.security.User;

/**
 * {@link HippoSession} bound manager for accessing, and optionally managing, repository based security configuration.
 * <p>
 *     The provided read-only and thread-safe providers are shared across all {@link RepositorySecurityManager} instances
 *     (e.g. across multiple HippoSessions).
 * </p>
 * <p>
 *     The provided managers are all dedicated and bound to this {@link RepositorySecurityManager} instance and use
 *     (each) a dedicated system session for perform changes. These managers are <em>NOT</em> thread-safe and only to
 *     be used on-behalf of their HippoSession (user).
 * </p>
 */
public interface RepositorySecurityManager {

    /**
     * Provides a read-only and thread-safe provider for accessing repository Role definitions
     * @return the roles provider
     */
    RolesProvider getRolesProvider();

    /**
     * Provides a read-only and thread-safe provider for accessing repository User Role definitions
     * @return the userroles provider
     */
    UserRolesProvider getUserRolesProvider();

    /**
     * The ChangePasswordManager allows the {link HippoSession} user to change its password
     * @throws AccessDeniedException for a {@link HippoSession#isSystemUser()}, a {@link User#isSystemUser()} or a
     * {@link User#isExternal()}.
     * @throws RepositoryException if the underlying HippoSession is no longer live, or something else went wrong
     * @return the ChangePasswordManager
     */
    ChangePasswordManager getChangePasswordManager() throws AccessDeniedException, RepositoryException;

    /**
     * Provides administrative (crud) roles management.
     * <p>
     *     Accessing the {@link RolesManager} requires the HippoSession to be in userrole
     *     {@link SecurityConstants#USERROLE_SECURITY_VIEWER} otherwise an {@link AccessDeniedException} will be raised.
     * </p>
     * <p>
     *     The HippoSession will be attached to a dedicated internal system session for performing the
     *     requested administrative tasks. The HippoSession itself is (only) used for (possibly) additional
     *     authorization checks, depending on the requested administrative task, and for (audit) logging purposes.
     * </p>
     * <p>
     *     All of the managers provided by this RepositorySecurityManager share the same
     *     internal system session for its HippoSession, which is automatically logged out when the
     *     HippoSession logs out.
     * </p>
     * @return the roles manager
     * @throws AccessDeniedException if the HippoSession isn't granted the userrole
     *         {@link SecurityConstants#USERROLE_SECURITY_APPLICATION_ADMIN}
     * @throws RepositoryException if the underlying HippoSession is no longer live, or something else went wrong
     */
    RolesManager getRolesManager() throws AccessDeniedException, RepositoryException;

    /**
     * Provides administrative (crud) userroles management.
     * <p>
     *     Accessing the {@link UserRolesManager} requires the HippoSession to be in userrole
     *     {@link SecurityConstants#USERROLE_SECURITY_VIEWER} otherwise an {@link AccessDeniedException} will be raised.
     * </p>
     * <p>
     *     The HippoSession will be attached to a dedicated internal system session for performing the
     *     requested administrative tasks. The HippoSession itself is (only) used for (possibly) additional
     *     authorization checks, depending on the requested administrative task, and for (audit) logging purposes.
     * </p>
     * <p>
     *     All of the managers provided by this RepositorySecurityManager share the same
     *     internal system session for its HippoSession, which is automatically logged out when the
     *     HippoSession logs out.
     * </p>
     * @return the userroles manager
     * @throws AccessDeniedException if the provided HippoSession isn't granted the userrole
     *         {@link SecurityConstants#USERROLE_SECURITY_APPLICATION_ADMIN}
     * @throws RepositoryException if the underlying HippoSession is no longer live, or something else went wrong
     */
    UserRolesManager getUserRolesManager() throws AccessDeniedException, RepositoryException;

    /**
     * Provides administrative (crud) domain management; currently limited to only {@link AuthRole}s of an existing domain.
     * <p>
     *     Accessing the {@link DomainsManager} requires the HippoSession to be in userRole
     *     {@link SecurityConstants#USERROLE_SECURITY_VIEWER} otherwise an {@link AccessDeniedException} will be raised.
     * </p>
     * <p>
     *     Note that this manager only provides and allows operations on {@link DomainAuth}s in a <em>valid</em> location!
     * </p>
     * <p>
     *     A domain location is valid if:
     * </p>
     * <ul>
     *     <li>it is a domain (directly) under a hipposys:domainfolder parent node below /hippo:configuration/hippo:domains</li>
     *     <li>it is a domain (directly) under a hipposys:federateddomainfolder parent node with depth >= 2</li>
     * </ul>
     * <p>
     *     Likewise, access and operations on {@link AuthRole}s is only provided for authroles directly under a <em>valid</em>
     *     domain location.
     * </p>
     * <p>
     *     All <em>modifying operations</em> require the underlying HippoSession to have userRole
     *     {@link SecurityConstants#USERROLE_SECURITY_APPLICATION_ADMIN}
     * </p>
     * <p>
     *     The HippoSession will be attached to a dedicated internal system session for performing the
     *     requested administrative tasks. The HippoSession itself is (only) used for (possibly) additional
     *     authorization checks, depending on the requested administrative task, and for (audit) logging purposes.
     * </p>
     * <p>
     *     All of the managers provided by this RepositorySecurityManager share the same
     *     internal system session for its HippoSession, which is automatically logged out when the
     *     HippoSession logs out.
     * </p>
     * @return the DomainsManager
     * @throws AccessDeniedException if the provided HippoSession isn't granted the userrole
     *         {@link SecurityConstants#USERROLE_SECURITY_VIEWER}
     * @throws RepositoryException if the underlying HippoSession is no longer live, or something else went wrong
     */
    DomainsManager getDomainsManager() throws AccessDeniedException, RepositoryException;
}
