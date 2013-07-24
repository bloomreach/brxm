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
package org.onehippo.repository;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.api.WorkflowManager;

/**
 * DO NOT USE, THIS INTERFACE IS NOT YET PART OF THE PUBLIC API.
 * The ManagerService class provides access to additional services
 * related to a JCR session, which are not part of the JCR standard.
 * These additional services are Hippo extensions and/or Jackrabbit
 * internal additions.  This class is intended to be a better alternative
 * for casting a {@link javax.jcr.Workspace} to a
 * {@link org.hippoecm.repository.api.HippoWorkspace} which also provides
 * access to these services.  The HippoWorkspace is really a Hippo
 * specific implementation, while some of the additional services can
 * also be retrieved from a plain Jackrabbit JCR session.
 */
@Deprecated
public interface ManagerService {

    /**
     * Obtains the WorkflowManager.
     * @return the WorkflowManager for the JCR session
     * @throws RepositoryException in case of generic issues accessing the repository
     */
    public WorkflowManager getWorkflowManager() throws RepositoryException;

    /**
     * Obtains the HierarchyResolver.
     * @return the HierarchyResolver for the JCR session
     * @throws RepositoryException
     */
    public HierarchyResolver getHierarchyResolver() throws RepositoryException;

    /**
     * Disposes the ManagerService and releases any resources.  No further calls may
     * be made to this instance afterwards.
     */
    public void close();
}
