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
package org.hippoecm.repository.ocm;

import java.util.Map;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.jpox.ConnectionFactory;
import org.jpox.ManagedConnection;
import org.jpox.OMFContext;
import org.jpox.ObjectManager;
import org.jpox.exceptions.JPOXException;

class ConnectionFactoryImpl implements ConnectionFactory {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private HippoRepository repository;
    private Session session;
    private String location;
    private String username;
    private String password;
    private OMFContext omfContext;

    public ConnectionFactoryImpl(OMFContext omfContext) throws RepositoryException {
        this.omfContext = omfContext;
        location = null;
        String url = omfContext.getPersistenceConfiguration().getConnectionURL();
        if (url != null) {
            if (!url.startsWith("jcr")) {
                throw new JPOXException("JCR location invalid");
            }
            location = url.substring(4); // Omit the jcr prefix
        }
        if (location != null && !location.equals("")) {
            repository = HippoRepositoryFactory.getHippoRepository(location);
        } else {
            repository = HippoRepositoryFactory.getHippoRepository();
        }
        session = null;
        username = null; // FIXME
        password = null; // FIXME
    }

    public ConnectionFactoryImpl(OMFContext omfContext, Session session) throws RepositoryException {
        this.omfContext = omfContext;
        location = null;
        String url = omfContext.getPersistenceConfiguration().getConnectionURL();
        if (url != null) {
            if (!url.startsWith("jcr")) {
                throw new JPOXException("JCR location invalid");
            }
        }
        repository = null;
        this.session = session;
        username = null; // FIXME
        password = null; // FIXME
    }

    ConnectionFactoryImpl(String username, String password) throws RepositoryException {
        repository = HippoRepositoryFactory.getHippoRepository();
        this.username = username;
        this.password = password;
    }

    public ManagedConnection getConnection(ObjectManager om, Map options) {
        ManagedConnection mconn = omfContext.getConnectionManager().allocateConnection(this, om, options);
        return mconn;
    }

    public ManagedConnection createManagedConnection(Map transactionOptions) {
        if (session != null)
            return new ExistingConnection();
        else
            return new JCRManagedConnection();
    }

    class JCRManagedConnection implements ManagedConnection {
        private Session session;
        @SuppressWarnings("unused") private boolean transactional;

        public JCRManagedConnection() {
            this.session = null;
            transactional = false;
        }

        public JCRManagedConnection(Session session) {
            this.session = session;
            transactional = false;
        }

        public Object getConnection() {
            if (session == null) {
                try {
                    session = repository.login(username, password.toCharArray());
                } catch (LoginException ex) {
                    // FIXME: log something
                    return null;
                } catch (RepositoryException ex) {
                    // FIXME: log something
                    return null;
                }
            }
            return session;
        }

        public javax.transaction.xa.XAResource getXAResource() {
            if (session instanceof org.apache.jackrabbit.api.XASession) {
                return ((org.apache.jackrabbit.api.XASession) session).getXAResource();
            } else {
                return null;
            }
        }

        public void release() {
        }

        public void close() {
            if (session != null) {
                session.logout();
                session = null;
            }
        }

        public void setTransactional() {
            transactional = true;
        }
    }

    class ExistingConnection implements ManagedConnection {
        private boolean transactional;

        public ExistingConnection() {
            transactional = false;
        }

        public Object getConnection() {
            return session;
        }

        public javax.transaction.xa.XAResource getXAResource() {
            /* FIXME
              if (session instanceof org.apache.jackrabbit.api.XASession) {
                  return ((org.apache.jackrabbit.api.XASession) session).getXAResource();
                  } else */{
                return null;
            }
        }

        public void release() {
        }

        public void close() {
        }

        public void setTransactional() {
            transactional = true;
        }
    }
}
