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

import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Repository;

import org.hippoecm.hst.core.ResourceLifecycleManagement;

/**
 * Interface extending {@link javax.jcr.Repository} to allow
 * transparent access to internal multiple repositories based on credentials
 * given by the caller.
 * 
 * @version $Id$
 */
public interface MultipleRepository extends Repository {
    
    /**
     * Adds an internal repository with a credentials as key
     * @param credentials
     * @param repository
     */
    void addRepository(Credentials credentials, Repository repository);
    
    /**
     * Removes an internal repository with a credentials as key
     * @param credentials
     * @return removed
     */
    boolean removeRepository(Credentials credentials);
    
    /**
     * Returns the internal repository map.
     * 
     * @return
     */
    Map<Credentials, Repository> getRepositoryMap();
    
    /**
     * Checks if it contains the internal repository which has the specified credentials.
     * 
     * @param credentials
     * @return
     */
    boolean containsRepositoryByCredentials(Credentials credentials);
    
    /**
     * Returns the internal repository which has the specified credentials as its default credentials.
     * 
     * @param credentials
     * @return
     */
    Repository getRepositoryByCredentials(Credentials credentials);
    
    /**
     * Returns the resource lifecycle management implementation.
     * @see {@link ResourceLifecycleManagement}
     * @return
     */
    ResourceLifecycleManagement [] getResourceLifecycleManagements();

}
