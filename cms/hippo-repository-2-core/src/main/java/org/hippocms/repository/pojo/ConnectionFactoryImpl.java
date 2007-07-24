/*
 * Copyright 2007 Hippo.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippocms.repository.pojo;

import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import javax.jdo.identity.SingleFieldIdentity;
import javax.jdo.spi.PersistenceCapable;

import org.jpox.ClassLoaderResolver;
import org.jpox.ConnectionFactory;
import org.jpox.ManagedConnection;
import org.jpox.ConnectionManager;
import org.jpox.ObjectManager;
import org.jpox.ObjectManagerFactoryImpl;
import org.jpox.PersistenceConfiguration;
import org.jpox.StateManager;
import org.jpox.exceptions.ClassNotResolvedException;
import org.jpox.exceptions.JPOXDataStoreException;
import org.jpox.exceptions.JPOXException;
import org.jpox.exceptions.JPOXObjectNotFoundException;
import org.jpox.exceptions.JPOXOptimisticException;
import org.jpox.exceptions.JPOXUserException;
import org.jpox.exceptions.NoPersistenceInformationException;
import org.jpox.metadata.AbstractClassMetaData;
import org.jpox.metadata.AbstractPropertyMetaData;
import org.jpox.metadata.ClassMetaData;
import org.jpox.metadata.ClassPersistenceModifier;
import org.jpox.metadata.ExtensionMetaData;
import org.jpox.metadata.IdentityStrategy;
import org.jpox.metadata.IdentityType;
import org.jpox.metadata.IdentityMetaData;
import org.jpox.metadata.SequenceMetaData;
import org.jpox.metadata.VersionStrategy;
import org.jpox.plugin.ConfigurationElement;
import org.jpox.sco.SCO;
import org.jpox.store.DatastoreClass;
import org.jpox.store.DatastoreContainerObject;
import org.jpox.store.DatastoreObject;
import org.jpox.store.Extent;
import org.jpox.store.FetchStatement;
import org.jpox.store.JPOXConnection;
import org.jpox.store.JPOXSequence;
import org.jpox.store.OID;
import org.jpox.store.OIDFactory;
import org.jpox.store.SCOID;
import org.jpox.store.StoreData;
import org.jpox.store.TableStoreData;
import org.jpox.store.StoreManager;
import org.jpox.store.StoreManagerFactory;
import org.jpox.store.exceptions.DatastorePermissionException;
import org.jpox.store.exceptions.NoExtentException;
import org.jpox.store.fieldmanager.DeleteFieldManager;
import org.jpox.store.fieldmanager.PersistFieldManager;
import org.jpox.store.poid.PoidConnectionProvider;
import org.jpox.store.poid.PoidGenerator;
import org.jpox.store.scostore.ArrayStore;
import org.jpox.store.scostore.CollectionStore;
import org.jpox.store.scostore.MapStore;
import org.jpox.util.AIDUtils;
import org.jpox.util.ClassUtils;
import org.jpox.util.JPOXLogger;
import org.jpox.util.Localiser;
import org.jpox.util.StringUtils;
import org.jpox.util.TypeConversionHelper;
import org.jpox.util.MacroString.IdentifierMacro;
import org.jpox.OMFContext;

import javax.jcr.Session;
import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ValueFormatException;
import javax.jcr.version.VersionException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.PathNotFoundException;
import javax.jcr.ItemExistsException;
import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.version.VersionException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

import org.hippocms.repository.jr.embedded.HippoRepository;
import org.hippocms.repository.jr.embedded.HippoRepositoryFactory;

class ConnectionFactoryImpl implements ConnectionFactory {
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
            if (!url.startsWith("jcr"))
                throw new JPOXException("JCR location invalid");
            location = url.substring(4); // Omit the jcr prefix
        }
        if (location != null && !location.equals(""))
            repository = (new HippoRepositoryFactory()).getHippoRepository(location);
        else
            repository = (new HippoRepositoryFactory()).getHippoRepository();
	session  = null;
        username = null; // FIXME
        password = null; // FIXME
    }

    public ConnectionFactoryImpl(OMFContext omfContext, Session session) throws RepositoryException {
        this.omfContext = omfContext;
        location = null;
        String url = omfContext.getPersistenceConfiguration().getConnectionURL();
        if (url != null) {
            if (!url.startsWith("jcr"))
                throw new JPOXException("JCR location invalid");
        }
	repository = null;
	this.session = session;
        username = null; // FIXME
        password = null; // FIXME
    }

    ConnectionFactoryImpl(String username, String password) throws RepositoryException {
        repository = (new HippoRepositoryFactory()).getHippoRepository();
        this.username = username;
        this.password = password;
    }

    public ManagedConnection getConnection(ObjectManager om, Map options) {
        ManagedConnection mconn = omfContext.getConnectionManager().allocateConnection(this, om, options);
        return mconn;
    }

    public ManagedConnection createManagedConnection(Map transactionOptions) {
        return new JCRManagedConnection();
    }

    class JCRManagedConnection implements ManagedConnection {
        private Session session;
        private boolean transactional;

        public JCRManagedConnection() {
            session = null;
            transactional = false;
        }

        public Object getConnection() {
            if (session == null) {
                try {
                    session = repository.login(username, password);
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
            if (session instanceof org.apache.jackrabbit.core.XASession)
                return ((org.apache.jackrabbit.core.XASession) session).getXAResource();
            else
                return null;
        }

        public void release() {
            try {
                session.save();
            } catch (AccessDeniedException ex) {
                System.err.println(ex.getMessage());
            } catch (ItemExistsException ex) {
                System.err.println(ex.getMessage());
            } catch (ConstraintViolationException ex) {
                System.err.println(ex.getMessage());
            } catch (InvalidItemStateException ex) {
                System.err.println(ex.getMessage());
            } catch (VersionException ex) {
                System.err.println(ex.getMessage());
            } catch (LockException ex) {
                System.err.println(ex.getMessage());
            } catch (NoSuchNodeTypeException ex) {
                System.err.println(ex.getMessage());
            } catch (RepositoryException ex) {
                System.err.println(ex.getMessage());
            }
            /* FIXME:
             if(!transactional)
             close();
             */
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
}
