/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Authentication and Authorization context
 *
 */
public class ManagerContext {

    /** SVN id placeholder */

    /**
     * The system/root session
     */
    private final Session session;

    /**
     * The id of the provider that created the context.
     */
    private final String providerId;


    /**
     * The path of the provider that created the context.
     */
    private final String providerPath;


    /**
     * The target path to expose information for this context eg. hippo:users, hippo:groups, etc.
     */
    private final String path;

    private boolean maintenanceMode;

    /**
     * Initialize the context for the repository based authentication and authorization.
     * @param session Session The system/root session
     * @param path the path for exposing information e.g. hippo:users, hippo:groups, etc.
     * @param providerPath the path to the configuration of this provider
     * @param session the providers own session
     */
    public ManagerContext(Session session, String providerPath, String path, boolean maintenanceMode) throws RepositoryException {
        this.session = session;
        this.providerPath = providerPath;
        this.providerId = providerPath.substring(providerPath.lastIndexOf('/') + 1);
        this.path = path;
        this.maintenanceMode = maintenanceMode;
    }

    /**
     * Get the root Session
     * @return Session the root session
     */
    public Session getSession() {
        return session;
    }

    /**
     * Get the id of the provider that created the context.
     */
    public String getProviderId() {
        return providerId;
    }

    /**
     * Get the path of the provider that created the context.
     */
    public String getProviderPath() {
        return providerPath;
    }

    /**
     * Get the path to expose information for this context eg. hippo:users, hippo:groups, etc.
     * @return
     */
    public String getPath() {
        return path;
    }

    /**
     * Get the path to expose information for this context eg. hippo:users, hippo:groups, etc.
     * @return
     */
    public boolean isMaintenanceMode() {
        return maintenanceMode;
    }
}
