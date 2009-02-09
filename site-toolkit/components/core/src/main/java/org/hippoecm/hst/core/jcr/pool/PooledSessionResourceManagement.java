package org.hippoecm.hst.core.jcr.pool;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Session;

import org.hippoecm.hst.core.ResourceLifecycleManagement;

public class PooledSessionResourceManagement implements ResourceLifecycleManagement, PoolingRepositoryAware {
    
    private static ThreadLocal<Map<String, boolean []>> tlActiveStates = new ThreadLocal<Map<String, boolean []>>() {
        protected synchronized Map<String, boolean []> initialValue() {
            return new HashMap<String, boolean []>();
        }
    };
    
    private static ThreadLocal<Set<Session>> tlPooledSessions = new ThreadLocal<Set<Session>>();

    protected PoolingRepository poolingRepository;
    
    protected String namespace = "default";
    
    public PooledSessionResourceManagement() {
    }
    
    public PooledSessionResourceManagement(String namespace) {
        if (namespace != null) {
            this.namespace = namespace;
        }
    }
    
    public void setPoolingRepository(PoolingRepository poolingRepository) {
        this.poolingRepository = poolingRepository;
    }
    
    public boolean isActive() {
        boolean [] activeState = tlActiveStates.get().get(this.namespace);
        return (activeState != null && activeState[0]);
    }
    
    public void setActive(boolean active) {
        boolean [] activeState = tlActiveStates.get().get(this.namespace);
        
        if (activeState == null) {
            activeState = new boolean[] { active };
            tlActiveStates.get().put(this.namespace, activeState);
        } else {
            activeState[0] = active;
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
