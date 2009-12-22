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

import java.util.HashSet;
import java.util.Set;

import org.hippoecm.hst.core.ResourceLifecycleManagement;

public class PooledSessionResourceManagement implements ResourceLifecycleManagement {
    
    private ThreadLocal<Boolean> tlActiveState = new ThreadLocal<Boolean>();
    private ThreadLocal<Set<PooledSession>> tlPooledSessions = new ThreadLocal<Set<PooledSession>>();

    public boolean isActive() {
        Boolean activeState = tlActiveState.get();
        return (activeState != null && activeState.booleanValue());
    }
    
    public void setActive(boolean active) {
        Boolean activeState = tlActiveState.get();
        
        if (activeState == null || activeState.booleanValue() != active) {
            activeState = new Boolean(active);
            tlActiveState.set(activeState);
        }
    }

    public void registerResource(Object session) {
        Set<PooledSession> sessions = tlPooledSessions.get();
        
        if (sessions == null) {
            sessions = new HashSet<PooledSession>();
            sessions.add((PooledSession) session);
            tlPooledSessions.set(sessions);
        } else {
            sessions.add((PooledSession) session);
        }
    }
    
    public void unregisterResource(Object session) {
        Set<PooledSession> sessions = tlPooledSessions.get();
        
        if (sessions != null) {
            sessions.remove((PooledSession) session);
        }
    }
    
    public void disposeResource(Object session) {
        try {
            ((PooledSession) session).logout();
        } catch (IllegalStateException e) {
            // just ignore on pooled session which is already returned to the pool.
        }
        
        unregisterResource(session);
    }
    
    public void disposeAllResources() {
        Set<PooledSession> sessions = tlPooledSessions.get();
        
        if (sessions != null) {
            // do not iterate through the Set because this will lead to concurrent modification exceptions
            PooledSession [] sessionArray = sessions.toArray(new PooledSession[sessions.size()]);
            
            for (PooledSession session : sessionArray) {
                try {
                    session.logout();
                } catch (IllegalStateException e) {
                    // just ignore on pooled session which is already returned to the pool.
                }
            }
            
            sessions.clear();
        }
    }

}
