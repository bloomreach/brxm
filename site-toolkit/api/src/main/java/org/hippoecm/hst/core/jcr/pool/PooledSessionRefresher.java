/*
 *  Copyright 2012 Hippo.
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

import javax.jcr.RepositoryException;

/**
 * PooledSessionRefresher
 * This interface is responsible for refreshing the states of the pooled session.
 * For example, if a pooled session refresher can simply invoke 
 * <code>javax.jcr.Session#refresh();</code>.
 * Or, a custom implementation can do something in more optimized way in order to
 * clean the virtual states only.
 * 
 * @version $Id$
 */
public interface PooledSessionRefresher {

    /**
     * Refreshes the pooled session.
     * This can make the pooled session refreshed or
     * a custom implementation can do something in more optimized way in order to
     * clean the virtual states only.
     *
     * @param pooledSession a pooled session
     * @param keepChanges a boolean
     * @throws RepositoryException if an error occurs.
     */
    void refresh(PooledSession pooledSession, boolean keepChanges) throws RepositoryException;

}
