/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.security;

import java.security.Principal;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.core.security.JackrabbitSecurityManager;
import org.hippoecm.repository.security.group.GroupManager;
import org.hippoecm.repository.security.user.AbstractUserManager;
import org.hippoecm.repository.security.user.HippoUserManager;

public interface HippoSecurityManager extends JackrabbitSecurityManager {

    /**
     * Configure the SecurityManager.
     * @throws RepositoryException
     */
    void configure() throws RepositoryException;

    /**
     * Get the user, group and facet auth principals to the user. The principals 
     * are added to the Set of principals that is passed along as argument.
     * 
     * @param principals the Set to add the principals to. (pass by reference).
     * @param creds the credentials for the user. If creds.userId() returns null
     *              the user is treated as anonymous. 
     */
    void assignPrincipals(Set<Principal>principals, SimpleCredentials creds);
    
    /**
     * Try to authenticate the user. If the user exists in the repository it will
     * authenticate against the responsible security provider or the internal provider
     * if none is set.
     * If the user is not found in the repository it will try to authenticate against
     * all security providers until a successful authentication is found. It uses the
     * natural node order. If the authentication is successful a user node will be
     * created.
     * @param creds Credentials user provided for authentication
     * @return {@link AuthenticationStatus#SUCCEEDED} only if the authentication is successful or
     * {@link AuthenticationStatus#CREDENTIAL_EXPIRED} if the user credentials are expired or
     * {@link AuthenticationStatus#ACCOUNT_EXPIRED} if the user's account is not active anymore or
     * {@link AuthenticationStatus#FAILED} otherwise
     */
    AuthenticationStatus authenticate(SimpleCredentials creds);

    /**
     * Get a {@link AbstractUserManager} for the given {@link Session} from the required {@link SecurityProvider}.
     * @throws RepositoryException
     */
    HippoUserManager getUserManager(Session session, String providerId) throws RepositoryException;

    /**
     * Get a {@link GroupManager} for the given {@link Session} from the required {@link SecurityProvider}
     * @throws RepositoryException
     */
    GroupManager getGroupManager(Session session, String providerId) throws RepositoryException;

}
