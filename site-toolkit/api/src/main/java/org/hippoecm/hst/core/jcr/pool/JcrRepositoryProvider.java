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
package org.hippoecm.hst.core.jcr.pool;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;

/**
 * Interface to define the contract between service provider and jcr session pool.
 * The jcr session pool expects the service provider to give a implementation for this.
 * 
 * @version $Id$
 */
public interface JcrRepositoryProvider {

    /**
     * Retrieves JCR Repository from this provider by specifying a repository URI.
     * 
     * @param repositoryURI the URI for the repository
     * @return javax.jcr.Repository
     * @throws RepositoryException
     */
    Repository getRepository(String repositoryURI) throws RepositoryException;
    
    /**
     * Returns the repository back to the provider
     * After returning, the caller cannot use the repository any more.
     * 
     * @param repository
     */
    void returnRepository(Repository repository);
    
}
