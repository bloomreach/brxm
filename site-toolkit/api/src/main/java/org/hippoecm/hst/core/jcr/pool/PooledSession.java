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

import javax.jcr.Session;

/**
 * PooledSession interface.
 * This interface extends <CODE>javax.jcr.Session</CODE>, allowing to set additional attributes
 * for internal use.
 * For example, if a pooled session is needed to refresh just before borrowing from the pool,
 * the pool implementation can set a specific attribute to check it later.
 * <P><EM>Note: If a pooled session is already returned to the pool, then any method invocation on this
 * pooled session will throw <CODE>java.lang.IllegalStateException</CODE>.</EM></P>
 * 
 * @version $Id$
 */
public interface PooledSession extends Session {
    
    /**
     * Invokes logout() of the underlying session.
     */
    void logoutSession();
    
    /**
     * Returns the last refreshed time millis.
     * @return
     */
    long lastRefreshed();
    
}
