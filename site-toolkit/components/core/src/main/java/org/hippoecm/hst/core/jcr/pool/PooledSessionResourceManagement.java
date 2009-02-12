package org.hippoecm.hst.core.jcr.pool;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Session;

import org.hippoecm.hst.core.ResourceLifecycleManagement;

public class PooledSessionResourceManagement implements ResourceLifecycleManagement, PoolingRepositoryAware {
    
    private static ThreadLocal<Map<String, boolean []>> tlActiveStates = new ThreadLocal<Map<String, boolean []>>() {
        @Override
        protected synchronized Map<String, boolean []> initialValue() {
            return new HashMap<String, boolean []>();
        }
    };
    
    private static ThreadLocal<Map<String, Set<Session>>> tlPooledSessionsMap = new ThreadLocal<Map<String, Set<Session>>>() {
        @Override
        protected synchronized Map<String, Set<Session>> initialValue() {
            return new HashMap<String, Set<Session>>();
        }
    };

    protected PoolingRepository poolingRepository;
    
    protected String namespace = "";
    
    public PooledSessionResourceManagement() {
        this(null);
    }
    
    public PooledSessionResourceManagement(String namespace) {
        if (namespace != null) {
            this.namespace = namespace;
        }
    }
    
    public void setPoolingRepository(PoolingRepository poolingRepository) {
        this.poolingRepository = poolingRepository;
        
        if ("".equals(this.namespace) && this.poolingRepository != null) {
            this.namespace = this.poolingRepository.toString() + ":" + this.poolingRepository.hashCode(); 
        }
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
        Set<Session> sessions = tlPooledSessionsMap.get().get(this.namespace);
        
        if (sessions == null) {
            sessions = new HashSet<Session>();
            sessions.add((Session) session);
            tlPooledSessionsMap.get().put(this.namespace, sessions);
        } else {
            sessions.add((Session) session);
        }
    }
    
    public void unregisterResource(Object session) {
        Set<Session> sessions = tlPooledSessionsMap.get().get(this.namespace);
        
        if (sessions != null) {
            sessions.remove((Session) session);
        }
    }
    
    public void disposeResource(Object session) {
        this.poolingRepository.returnSession((Session) session);
        unregisterResource(session);
    }
    
    public void disposeAllResources() {
        Set<Session> sessions = tlPooledSessionsMap.get().get(this.namespace);
        
        if (sessions != null) {
            for (Session session : sessions) {
                this.poolingRepository.returnSession((Session) session);
            }
            
            sessions.clear();
        }
    }

}
