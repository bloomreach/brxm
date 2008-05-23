/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.security;

import javax.jcr.Session;

/**
 * Authentication and Authorization context for the Repository implementations
 *
 */
public class RepositoryAAContext implements AAContext {

    /** SVN id placeholder */
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    
    /**
     * The system/root session
     */
    private final Session rootSession;

    /**
     * The path from the root containing the information for this context
     */
    private final String path;

    /**
     * Initialize the context for the repository based authentication and authorization
     * @param rootSession Session The system/root session
     * @param usersPath String The path containing the users
     * @param groupsPath String The path containing the groups
     * @param rolesPath String The path containing the roles
     */
    public RepositoryAAContext(Session rootSession, String path) {
        this.rootSession = rootSession;
        this.path = path;
    }

    /**
     * Get the root Session
     * @return Session the root session
     */
    public Session getRootSession() {
        return rootSession;
    }

    /**
     * Get the path in of the node that holds the configuration
     * @return
     */
    public String getPath() {
        return path;
    }
}
