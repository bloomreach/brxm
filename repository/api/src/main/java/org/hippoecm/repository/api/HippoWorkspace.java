/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.api;

import javax.jcr.RepositoryException;
import javax.jcr.Workspace;

import com.bloomreach.xm.repository.security.RepositorySecurityManager;

/**
 * Any instance of a {@link Workspace} returned by a HippoRepository may be cast to a HippoWorkspace to expose some
 * additional services from the Hippo repository.  These services are bound to the session from which this
 * workspace was obtained.
 */
public interface HippoWorkspace extends Workspace {

    /**
     * The {@link WorkflowManager} service allows access to workflows operations that are available on documents stored in the repository.
     * @return the workflow manager
     * @throws RepositoryException indicates an unspecified error from the repository
     */
    WorkflowManager getWorkflowManager() throws RepositoryException;

    /**
     * The {@link HierarchyResolver} service allows you to navigate though the repository using some context knowledge of Hippo document
     * types.
     * @return the hierarchy service
     * @throws RepositoryException indicates an unspecified error from the repository
     */
    HierarchyResolver getHierarchyResolver() throws RepositoryException;

    /**
     * The {@link RepositorySecurityManager} allows accessing, and optionally managing, repository based security configuration.
     * @return the security manager
     */
    RepositorySecurityManager getSecurityManager();
}
