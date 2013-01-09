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

import java.util.HashSet;
import java.util.Set;

import javax.jcr.Session;

import org.hippoecm.hst.core.ResourceLifecycleManagement;
import org.hippoecm.hst.core.ResourceVisitor;

public class PooledSessionResourceManagement implements ResourceLifecycleManagement {
    
    private ThreadLocal<Boolean> tlActiveState = new ThreadLocal<Boolean>();
    private ThreadLocal<Set<Session>> tlPooledSessions = new ThreadLocal<Set<Session>>();
    private boolean alwaysActive;

    public boolean isActive() {
        Boolean activeState = tlActiveState.get();
        return (alwaysActive || (activeState != null && activeState.booleanValue()));
    }
    
    public void setActive(boolean active) {
        Boolean activeState = tlActiveState.get();
        
        if (activeState == null || activeState.booleanValue() != active) {
            activeState = Boolean.valueOf(active);
            tlActiveState.set(activeState);
        }
    }
    
    public boolean isAlwaysActive() {
        return alwaysActive;
    }
    
    public void setAlwaysActive(boolean alwaysActive) {
        this.alwaysActive = alwaysActive;
    }

    public void registerResource(Object session) {
        Set<Session> sessions = tlPooledSessions.get();
        
        if (sessions == null) {
            sessions = new HashSet<Session>();
            sessions.add((Session) session);
            tlPooledSessions.set(sessions);
        } else {
            sessions.add((Session) session);
        }
    }
    
    public void unregisterResource(Object session) {
        Set<Session> sessions = tlPooledSessions.get();
        
        if (sessions != null) {
            sessions.remove((Session) session);
        }
    }
    
    public void disposeResource(Object sessionObject) {
        try {
            Session session = (Session) sessionObject;
            session.logout();
        } catch (Exception ignore) {
            // just ignore on pooled session which is already returned to the pool.
        }
        
        unregisterResource(sessionObject);
    }
    
    public void disposeAllResources() {
        Set<Session> sessions = tlPooledSessions.get();
        
        if (sessions != null && !sessions.isEmpty()) {
            // do not iterate through the Set because this will lead to concurrent modification exceptions
            Session [] sessionArray = sessions.toArray(new Session[sessions.size()]);
            
            for (Session session : sessionArray) {
                try {
                    session.logout();
                } catch (Exception ignore) {
                    // just ignore on pooled session which is already returned to the pool.
                }
            }
            
            sessions.clear();
        }
    }
    
    public Object visitResources(ResourceVisitor visitor) {
        if (visitor == null) {
            throw new IllegalArgumentException("argument visitor may not be null");
        }

        Set<Session> sessions = tlPooledSessions.get();
        
        if (sessions != null && !sessions.isEmpty()) {
            // do not iterate through the Set because this will lead to concurrent modification exceptions
            Session [] sessionArray = sessions.toArray(new Session[sessions.size()]);
            
            for (Session session : sessionArray) {
                Object value = null;
    
                // Call visitor
                value = visitor.resource(session);

                // If visitor returns a non-null value, it halts the traversal
                if (value != ResourceVisitor.CONTINUE_TRAVERSAL) {
                    return value;
                }
            }
        }
        
        return null;
    }
}
