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
public interface PoolingRepository extends Repository, PoolingRepositoryMBean {

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
     * Returns pooling counter.
     * @return
     */
    PoolingCounter getPoolingCounter();
    
    /**
     * Returns true if pooling repository is initialized to be available and not closed.
     * 
     * @return
     */
    boolean isActive();
    
    /**
     * Returns true if pooling repository can be marked to be disposed when not in use.
     * For example, if a pooling repository can be disposable at runtime, then this
     * should return true.
     * @return <code>true</code> if it is possible that this {@link PoolingRepository} gets marked to be disposable when it is not in use
     * @see #isMarkedForDisposal
     */
    boolean isDisposableWhenNotInUse();
    
    /**
     * Returns true if this {@link PoolingRepository} is marked for disposal. When a {@link PoolingRepository}
     * is marked for disposal, it should not be used any more
     * This can return true only when {@link #isDisposableWhenNotInUse()} returns true.
     * @return <code>true</code> if this {@link PoolingRepository} is marked for disposal
     * @see #isDisposableWhenNotInUse()
     */
    boolean isMarkedForDisposal();
}
