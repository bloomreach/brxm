/*
 *  Copyright 2009 Hippo.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.apache.jackrabbit.api.XASession;

import org.datanucleus.OMFContext;
import org.datanucleus.ObjectManager;
import org.datanucleus.Transaction;
import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.store.connection.AbstractConnectionFactory;
import org.datanucleus.store.connection.AbstractManagedConnection;
import org.datanucleus.store.connection.ConnectionFactory;
import org.datanucleus.store.connection.ManagedConnection;
import org.datanucleus.store.connection.ManagedConnectionResourceListener;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;

public class ConnectionFactoryImpl extends AbstractConnectionFactory implements ConnectionFactory {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public ConnectionFactoryImpl(OMFContext omfContext, String resourceType) throws RepositoryException {
        super(omfContext, resourceType);
    }

    @Override
    public ManagedConnection createManagedConnection(Object o, Map transactionOptions) {
        return new JCRManagedConnection(omfContext, transactionOptions);
    }

    /**
     * Implementation of a ManagedConnection for JCR.
     */
    public class JCRManagedConnection extends AbstractManagedConnection implements ManagedConnection {
        private OMFContext omfContext;
        private Map options;
        private List<ManagedConnectionResourceListener> listeners = new ArrayList<ManagedConnectionResourceListener>();

        //private Session session;
        @SuppressWarnings("unused")
        boolean locked = false;
        Session session;

        public JCRManagedConnection(OMFContext omfContext, Map options) {
            //session = null;
            this.omfContext = omfContext;
            this.options = options;
        }

        @Override
        public Object getConnection() {
            session = ((JcrStoreManager)omfContext.getStoreManager()).session;
            if (session == null) {
                try {
                    String location;
                    String url = omfContext.getPersistenceConfiguration()
                            .getStringProperty("datanucleus.ConnectionURL");
                    if (url != null && url.startsWith("jcr:")) {
                        location = url.substring(4); // Omit the jcr prefix
                    } else {
                        throw new NucleusDataStoreException("JCR location invalid. Found datanucleus.ConnectionURL "
                                + url);
                    }
                    HippoRepository repository;
                    if (location != null && !location.equals("")) {
                        repository = HippoRepositoryFactory.getHippoRepository(location);
                    } else {
                        repository = HippoRepositoryFactory.getHippoRepository();
                    }
                    String username = omfContext.getPersistenceConfiguration().getStringProperty(
                            "datanucleus.ConnectionUserName");
                    String password = omfContext.getPersistenceConfiguration().getStringProperty(
                            "datanucleus.ConnectionPassword");
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

        @Override
        public javax.transaction.xa.XAResource getXAResource() {
            if (session instanceof org.apache.jackrabbit.api.XASession) {
                return ((org.apache.jackrabbit.api.XASession) session).getXAResource();
            } else {
                return null;
            }
        }

        @Override
        public void close() {
            /*for (ManagedConnectionResourceListener listener : listeners) {
                listener.managedConnectionPreClose();
            }
            if (session != null) {
                session.logout();
                session = null;
            }*/
        }
    }
}
