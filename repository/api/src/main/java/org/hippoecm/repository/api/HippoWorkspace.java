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
package org.hippoecm.repository.api;

import javax.jcr.RepositoryException;
import javax.jcr.Workspace;

/**
 * Any instance of a {@link Workspace} returned by a HippoRepository may be cast to a HippoWorkspace to expose some
 * additional services from the Hippo repository.  These services are bound to the session from which this
 * workspace was obtained.
 */
public interface HippoWorkspace extends Workspace {

    /**
     * The document manager service allows the representation of a document stored as a subtree of a {link javax.jcr.Node} as a Java object.
     * @return the document manager
     * @throws javax.jcr.RepositoryException indicates an unspecified error from the repository
     */
    public DocumentManager getDocumentManager() throws RepositoryException;

    /**
     * The workflow manager service allows access to workflows operations that are available on documents stored in the repository.
     * @return the workflow manager
     * @throws javax.jcr.RepositoryException indicates an unspecified error from the repository
     */
    public WorkflowManager getWorkflowManager() throws RepositoryException;

    /**
     * The hierarchy resolver service allows you to navigate though the repository using some context knowledge of Hippo document
     * types.
     * @return the hierarchy service
     * @throws javax.jcr.RepositoryException indicates an unspecified error from the repository
     */
    public HierarchyResolver getHierarchyResolver() throws RepositoryException;
}
