package org.hippoecm.hst.core.jcr.pool;

import java.util.HashSet;
import java.util.Set;

import javax.jcr.Session;

import org.hippoecm.hst.core.ResourceLifecycleManagement;

public class PooledSessionResourceManagement implements ResourceLifecycleManagement, PoolingRepositoryAware {
    
    private ThreadLocal<Boolean> tlActiveState = new ThreadLocal<Boolean>();
    private ThreadLocal<Set<Session>> tlPooledSessions = new ThreadLocal<Set<Session>>();

    protected PoolingRepository poolingRepository;
    
    public void setPoolingRepository(PoolingRepository poolingRepository) {
        this.poolingRepository = poolingRepository;
    }
    
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
    
    public void disposeResource(Object session) {
        this.poolingRepository.returnSession((Session) session);
        unregisterResource(session);
    }
    
    public void disposeAllResources() {
        Set<Session> sessions = tlPooledSessions.get();
        
        if (sessions != null) {
            for (Session session : sessions) {
                this.poolingRepository.returnSession((Session) session);
            }
            
            sessions.clear();
        }
    }

}
