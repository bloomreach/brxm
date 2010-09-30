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

import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.core.ResourceLifecycleManagement;
import org.hippoecm.hst.statistics.Counter;

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
     * The key name of the counter which counts session creation.
     */
    String COUNTER_SESSION_CREATED = "Created";
    
    /**
     * The key name of the counter which counts session activation.
     */
    String COUNTER_SESSION_ACTIVATED = "Activated";
    
    /**
     * The key name of the counter which counts session obtained by login.
     */
    String COUNTER_SESSION_OBTAINED = "Obtained";
    
    /**
     * The key name of the counter which counts session returned by logout.
     */
    String COUNTER_SESSION_RETURNED = "Returned";
    
    /**
     * The key name of the counter which counts session passivation.
     */
    String COUNTER_SESSION_PASSIVATED = "Passivated";
    
    /**
     * The key name of the counter which counts session destroying.
     */
    String COUNTER_SESSION_DESTROYED = "Destroyed";
    
    /**
     * Initializes the pool
     * @throws Exception
     */
    void initialize() throws Exception;
    
    /**
     * Clears any sessions sitting idle in the pool by removing them from the idle instance pool.
     */
    void clear();
    
    /**
     * Closes the pool
     */
    void close() throws Exception;
    
    /**
     * Returns the current active session count in the pool.
     * 
     * @return
     */
    int getNumActive();

    /**
     * Returns the current idle session count in the pool.
     * @return
     */
    int getNumIdle();

    /**
     * Returns the session to the pool.
     * <p><strong>Note: </strong> There is no guard to prevent an object
     * being returned to the pool multiple times. Clients are expected to
     * discard references to returned objects and ensure that an object is not
     * returned to the pool multiple times in sequence (i.e., without being
     * borrowed again between returns). Violating this contract will result in
     * the same object appearing multiple times in the pool and pool counters
     * (numActive, numIdle) returning incorrect values.</p>
     * 
     * @param session
     */
    void returnSession(Session session);
    
    /**
     * Returns the resource lifecycle management implementation of this pool.
     * 
     * @see {@link ResourceLifecycleManagement}
     * @return
     */
    ResourceLifecycleManagement getResourceLifecycleManagement(); 
    
    /**
     * Tries impersonation by the provided the credentials.
     * If this pooling repository is contained in a {@link MultipleRepository} implementation
     * and the containing {@link MultipleRepository} has a proper repository for the provided credentials,
     * then it can return a proper session impersonated.
     * Otherwise, it returns null.
     * 
     * @param credentials
     * @return
     * @throws LoginException
     * @throws RepositoryException
     */
    Session impersonate(Credentials credentials) throws LoginException, RepositoryException;

    /**
     * Sets time millis to have each session be refreshed on activation if the session
     * is not refreshed after the specified time millis.
     * 
     * @param sessionsRefreshPendingTimeMillis
     */
    void setSessionsRefreshPendingAfter(long sessionsRefreshPendingTimeMillis);
    
    /**
     * Returns the initial size of the connection pool.
     * @return
     */
    int getInitialSize();
    
    /**
     * Returns the maximum number of active connections that can be allocated at the same time.
     * @return
     */
    int getMaxActive();
    
    /**
     * Returns the maximum number of connections that can remain idle in the pool. 
     * @return
     */
    int getMaxIdle();
    
    /**
     * Returns the minimum number of idle connections in the pool
     * @return
     */
    int getMinIdle();
    
    /**
     * Returns counters map in which available counters are associated by keys.
     * @return
     */
    Map<String, Counter> getCounters();
    
}
