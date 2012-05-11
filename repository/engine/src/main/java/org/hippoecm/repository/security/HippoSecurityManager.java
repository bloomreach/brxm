/*
 *  Copyright 2012 Hippo.
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
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.core.security.JackrabbitSecurityManager;

public interface HippoSecurityManager extends JackrabbitSecurityManager {

    @SuppressWarnings("unused")
    static final String SVN_ID = "$Id$";

    /**
     * Initialize the SecurityManager.
     * This method is a temporary fix for REPO-368 and will be removed in the future.
     * @throws RepositoryException
     */
    void init() throws RepositoryException;

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
     * @param creds
     * @return true only if the authentication is successful
     */
    boolean authenticate(SimpleCredentials creds);
}
