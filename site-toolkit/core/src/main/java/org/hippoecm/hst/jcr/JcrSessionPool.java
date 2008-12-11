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
package org.hippoecm.hst.jcr;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.http.HttpSession;

import org.hippoecm.hst.core.Stats;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrSessionPool {
    private static final Logger log = LoggerFactory.getLogger(JcrSessionPoolManager.class);

    private static final boolean MUST_REFRESH = true;
    private SimpleCredentials simpleCredentials;
    private String repositoryLocation;
    private LinkedList<ReadOnlyPooledSession> idleSessions = new LinkedList<ReadOnlyPooledSession>();
    private IdentityHashMap<HttpSession, ReadOnlyPooledSession> activeSessions = new IdentityHashMap<HttpSession, ReadOnlyPooledSession>();

    public JcrSessionPool(SimpleCredentials simpleCredentials, String repositoryLocation) {
        log.debug("Create a new jcr session pool for '" + simpleCredentials.getUserID() + "'");
        this.simpleCredentials = simpleCredentials;
        this.repositoryLocation = repositoryLocation;

    }

    public ReadOnlyPooledSession getSession(HttpSession httpSession) {
        if(Stats.log.isDebugEnabled()) {
            logPoolStats();
        }
        log.debug("fetching a session from the pool.");
        ReadOnlyPooledSession session = null;

        synchronized (this.activeSessions) {
            session = this.activeSessions.get(httpSession);
            if (session != null) {
                if (session.isLive()) {
                    session.increaseRefCount();
                    log.debug("return found active session in pool for the request. ");
                    return session;
                } else {
                    log.debug("found a session which is not alive: logout and remove from active sessions");
                    session.getDelegatee().logout();
                    this.activeSessions.remove(httpSession);
                }
            }
        }

        log.debug("trying to get a session from the 'idle sessions' list");
        // because both idleSessions and activeSessions modifying/reading, sychronize on jcrSessionPool
        synchronized (this) {
            while (!this.idleSessions.isEmpty()) {
                //  try {
                session = this.idleSessions.removeFirst();
                if (session.isLive() && session.isValid()) {
                    this.activeSessions.put(httpSession, session);
                    log.debug("Return  found idle session.");
                    session.increaseRefCount();
                    return session;
                } else {
                    // try next in the idleSessions untill none left
                    log.debug("Found idle session is expired. Remove this one, and try next idle");
                    session.getDelegatee().logout();
                    session = null;
                }

            }
        }

        log.debug("No valid idle session found in the pool. Create a new one, and add to the pool when finished.");
        // No valid jcrsession we have so far: create a new one.

        HippoRepositoryFactory.setDefaultRepository(repositoryLocation);
        Session jcrSession = null;
        try {
            HippoRepository repository = HippoRepositoryFactory.getHippoRepository();
            jcrSession = repository.login(simpleCredentials);
        } catch (LoginException e) {
            throw new JcrConnectionException("Cannot login with credentials");
        } catch (RepositoryException e) {
            e.printStackTrace();
            throw new JcrConnectionException("Failed to initialize repository");
        }
        session = new ReadOnlyPooledSession(jcrSession, this);
        synchronized (this.activeSessions) {
            session.increaseRefCount();
            this.activeSessions.put(httpSession, session);
        }
        return session;
    }

    public Session getWriteableSession(){
        Session jcrSession = null;
        try {
            HippoRepository repository = HippoRepositoryFactory.getHippoRepository();
            jcrSession = repository.login(simpleCredentials);
        } catch (LoginException e) {
            throw new JcrConnectionException("Cannot login with credentials");
        } catch (RepositoryException e) {
            e.printStackTrace();
            throw new JcrConnectionException("Failed to initialize repository");
        }
        return jcrSession;
    }

    public void release(HttpSession httpSession) {
        synchronized (this) {
            ReadOnlyPooledSession finishedSession  = this.activeSessions.get(httpSession);
            if (finishedSession != null) {
                finishedSession.decreaseRefCount();
                if(finishedSession.getRefCount() == 0) {
                    this.activeSessions.remove(httpSession);
                    if (!finishedSession.isLive()) {
                        log.debug("Used session is not live anymore: log out");
                        finishedSession.getDelegatee().logout();
                        return;
                    }
                    if (finishedSession.isValid()) {
                        log.debug("Return the used session to the pool");
                        refresh(finishedSession);
                        idleSessions.add(finishedSession);
                    } else {
                        log.debug("Used session is expired. Log out");
                        finishedSession.getDelegatee().logout();
                    }
                }
             }
        }
    }


    private void refresh(ReadOnlyPooledSession finishedSession) {
        // while no invalidation, refresh sessions after a release
        if(MUST_REFRESH) {
            try {
                finishedSession.refresh(false);
            } catch (RepositoryException e) {
                log.error("RepositoryException " + e.getMessage());
            }
        }
    }


    private void logPoolStats() {
        synchronized(this) {
        Stats.log.debug("-------POOL STATS-------");       
        Stats.log.debug("Pool name: " + this.simpleCredentials.getUserID());
        Stats.log.debug("Idle sessions : " + this.idleSessions.size());
        Stats.log.debug("ActiveSessions sessions : " + this.activeSessions.size());
        Stats.log.debug("------------------------");
        }
    }


    public void dispose() {
        synchronized(this) {
            for( Iterator<ReadOnlyPooledSession> si = this.activeSessions.values().iterator(); si.hasNext() ; ){
                si.next().getDelegatee().logout();
            } 
            for( Iterator<ReadOnlyPooledSession> si = this.idleSessions.iterator(); si.hasNext() ; ){
                si.next().getDelegatee().logout();
            }
        }
    }
}
