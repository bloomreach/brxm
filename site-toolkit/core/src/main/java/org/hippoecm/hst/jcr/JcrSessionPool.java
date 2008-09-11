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
import java.util.LinkedList;
import java.util.NoSuchElementException;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.core.HSTConfiguration;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;

public class JcrSessionPool {

    private static final JcrSessionPool jcrSessionPool = new JcrSessionPool();

    private final LinkedList<ReadOnlyPooledSession> idleSessions = new LinkedList<ReadOnlyPooledSession>();
    private final IdentityHashMap<HttpServletRequest, ReadOnlyPooledSession> activeSessions = new IdentityHashMap<HttpServletRequest, ReadOnlyPooledSession>();

    public static Session getSession(HttpServletRequest request) {
        ReadOnlyPooledSession session = null;

        synchronized (jcrSessionPool.activeSessions) {
            session = jcrSessionPool.activeSessions.get(request);
            if (session != null) {
                if (session.isLive()) {
                    return session;
                } else {
                    jcrSessionPool.activeSessions.remove(request);
                }
            }
        }

        // because both idleSessions and activeSessions modifying/reading, sychronize on jcrSessionPool
        synchronized (jcrSessionPool) {
            while (!jcrSessionPool.idleSessions.isEmpty()) {
                try {
                    session = jcrSessionPool.idleSessions.removeFirst();
                    if (session.isLive() && session.isValid(System.currentTimeMillis())) {
                        jcrSessionPool.activeSessions.put(request, session);
                        return session;
                    } else {
                        // try next in the idleSessions untill none left
                        session.getDelegatee().logout();
                        session = null;
                    }
                } catch (NoSuchElementException e) {
                    // no session idle, create a new one
                }
            }
        }

        // No valid jcrsession we have so far: create a new one.
        ServletContext sc = request.getSession().getServletContext();
        String location = HSTConfiguration.get(sc, HSTConfiguration.KEY_REPOSITORY_ADRESS);
        String username = HSTConfiguration.get(sc, HSTConfiguration.KEY_REPOSITORY_USERNAME);
        String password = HSTConfiguration.get(sc, HSTConfiguration.KEY_REPOSITORY_PASSWORD);

        HippoRepositoryFactory.setDefaultRepository(location);
        Session jcrSession = null;
        try {
            HippoRepository repository = HippoRepositoryFactory.getHippoRepository();
            jcrSession = repository.login(username, (password != null ? password.toCharArray() : null));
        } catch (LoginException e) {
            throw new JCRConnectionException("Cannot login with credentials");
        } catch (RepositoryException e) {
            e.printStackTrace();
            throw new JCRConnectionException("Failed to initialize repository");
        }
        session = new ReadOnlyPooledSession(jcrSession, jcrSessionPool);
        synchronized (jcrSessionPool.activeSessions) {
            jcrSessionPool.activeSessions.put(request, session);
        }
        return session;
    }

    public void release(HttpServletRequest request) {
        synchronized (jcrSessionPool.activeSessions) {
            ReadOnlyPooledSession finishedSession = jcrSessionPool.activeSessions.remove(request);
            if (finishedSession != null) {
                if(!finishedSession.isLive()) {
                    finishedSession.getDelegatee().logout();
                    return;
                }
                
                if (finishedSession.isValid(System.currentTimeMillis())) {
                    idleSessions.add(finishedSession);
                } else {
                    finishedSession.getDelegatee().logout();
                }
            }
        }
    }

}
