/*
 * Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.repository;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.hippoecm.repository.security.HippoSecurityManager;
import org.onehippo.repository.journal.ExternalRepositorySyncRevisionService;

/**
 * Internal Hippo Repository implementation methods
 */
public interface InternalHippoRepository extends Repository {

    /**
     * Provides access to the ExternalRepositorySyncRevisionService for this repository.
     * @return the ExternalRepositorySyncRevisionService for this repository
     * @throws RepositoryException
     */
    ExternalRepositorySyncRevisionService getExternalRepositorySyncRevisionService() throws RepositoryException;

    /**
     * Provides access to the NodeTypeRegistry, for example to allow adding a NodeTypeRegistryListener
     * @return the NodeTypeRegistry
     */
    NodeTypeRegistry getNodeTypeRegistry();

    /**
     * Provides access to the HippoSecurityManager
     * @return the HippoSecurityManager
     */
    HippoSecurityManager getHippoSecurityManager();
}
