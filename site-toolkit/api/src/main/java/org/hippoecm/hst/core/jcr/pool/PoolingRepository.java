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
package org.hippoecm.hst.core.jcr.pool;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.core.ResourceLifecycleManagement;

/**
 * Interface extending {@link javax.jcr.Repository} to allow
 * transparent access to internal session pooling implementation.
 * 
 * @version $Id$
 */
public interface PoolingRepository extends Repository {

    /**
     * When the sessions in the pool are exhausted, the pool will be blocked for the specified interval
     * to wait for available idle session.
     */
    String WHEN_EXHAUSTED_BLOCK = "block";
    
    /**
     * When the sessions in the pool are exhausted, the pool will throw exception instantly without
     * waiting for available idle session. 
     */
    String WHEN_EXHAUSTED_FAIL = "fail";
    
    /**
     * When the sessions in the pool are exhausted, the pool will grow the action session count to serve
     * the request. This option will make the max active count limit meaningless.
     */
    String WHEN_EXHAUSTED_GROW = "grow";

    /**
     * Initializes the pool
     * @throws Exception
     */
    public void initialize() throws Exception;
    
    /**
     * Closes the pool
     */
    public void close() throws Exception;
    
    /**
     * Returns the current active session count in the pool.
     * 
     * @return
     */
    public int getNumActive();

    /**
     * Returns the current idle session count in the pool.
     * @return
     */
    public int getNumIdle();

    /**
     * Returns the session to the pool.
     * 
     * @param session
     */
    public void returnSession(Session session);
    
    /**
     * Returns the resource lifecycle management implementation of this pool.
     * 
     * @see {@link ResourceLifecycleManagement}
     * @return
     */
    public ResourceLifecycleManagement getResourceLifecycleManagement(); 
    
    /**
     * Tries impersonation by the provided the credentials.
     * If this pooling repository is contained in a {@link MultipleRepository} implementation
     * and the containing {@link MultipleRepository} has a proper repository for the provided credentials,
     * then it can return a proper session impersonated.
     * Otherwise, it throws a RepositoryException. 
     * 
     * @param credentials
     * @return
     * @throws LoginException
     * @throws RepositoryException
     */
    public Session impersonate(Credentials credentials) throws LoginException, RepositoryException;

    /**
     * Sets time millis to have each session be refreshed on activation if the session
     * is not refreshed after the specified time millis.
     * 
     * @param sessionsRefreshPendingTimeMillis
     */
    public void setSessionsRefreshPendingAfter(long sessionsRefreshPendingTimeMillis);
    
}
