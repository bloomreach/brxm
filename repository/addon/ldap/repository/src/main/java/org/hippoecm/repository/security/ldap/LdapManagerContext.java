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
package org.hippoecm.repository.security.ldap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.security.ManagerContext;


/**
 * Authentication and Authorization context for the managers
 *
 */
public class LdapManagerContext extends ManagerContext {

    /** SVN id placeholder */
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    /**
     * The ldap context factory
     */
    private final LdapContextFactory lcf;

    /**
     * Initialize the context for the repository based authentication and authorization.
     * @param session Session The system/root session
     * @param path the path for exposing information e.g. hippo:users, hippo:groups, etc.
     * @param providerPath the path to the configuration of this provider
     */
    public LdapManagerContext(LdapContextFactory lcf, Node providerNode, String path) throws RepositoryException {
        super(providerNode, path);
        this.lcf = lcf;
    }

    /**
     * Get the Ldap context factory
     * @return LdapContextFactory
     */
    public LdapContextFactory getLdapContextFactory() {
        return lcf;
    }
}
