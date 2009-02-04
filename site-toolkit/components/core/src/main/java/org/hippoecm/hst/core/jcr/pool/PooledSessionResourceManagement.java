package org.hippoecm.hst.core.jcr.pool;

import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;

import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hippoecm.hst.core.ResourceLifecycleManagement;

public class PooledSessionResourceManagement implements ResourceLifecycleManagement {
    
    static Log log = LogFactory.getLog(PooledSessionResourceManagement.class);
    
    private static ThreadLocal<Map<String, Boolean>> tlActiveStates = new ThreadLocal<Map<String, Boolean>>() {
        protected synchronized Map<String, Boolean> initialValue() {
            return new HashMap<String, Boolean>();
        }
    };

    private static ThreadLocal<Map<String, List<Session>>> tlPooledSessionList = new ThreadLocal<Map<String, List<Session>>>() {
        protected synchronized Map<String, List<Session>> initialValue() {
            return new HashMap<String, List<Session>>();
        }
    };
    
    private String name;

    public PooledSessionResourceManagement(String name) {
        this.name = name;
    }
    
    public boolean isActive() {
        Boolean active = tlActiveStates.get().get(this.name);
        return (active != null && active.booleanValue());
    }
    
    public void setActive(boolean active) {
        tlActiveStates.get().put(this.name, Boolean.valueOf(active));
    }

    public void registerResource(Object session) {
        List<Session> sessionList = getSessionList(true);
        sessionList.add((Session) session);
    }
    
    public void unregisterResource(Object session) {
        List<Session> sessionList = getSessionList(false);
        
        if (sessionList != null) {
            sessionList.remove((Session) session);
        }
    }
    
    public void disposeResource(Object session) {
        try {
            ((Session) session).logout();
        } catch (Throwable th) {
        }
        
        unregisterResource(session);
    }
    
    public void disposeAllResources() {
        List<Session> sessionList = getSessionList(false);
        
        if (sessionList != null) {
            for (Session session : sessionList) {
                try {
                    session.logout();
                } catch (Throwable th) {
                }
            }
            
            sessionList.clear();
        }
    }
    
    private List<Session> getSessionList(boolean createNew) {
        List<Session> sessionList = tlPooledSessionList.get().get(this.name);
        
        if (sessionList == null && createNew) {
            sessionList = new LinkedList<Session>();
            tlPooledSessionList.get().put(this.name, sessionList);
        }
        
        return sessionList;
    }
}
