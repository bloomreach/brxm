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

import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import javax.jdo.spi.PersistenceCapable;

import org.jpox.ClassLoaderResolver;
import org.jpox.ConnectionFactory;
import org.jpox.ConnectionManager;
import org.jpox.ManagedConnection;
import org.jpox.ObjectManager;
import org.jpox.ObjectManagerFactoryImpl;
import org.jpox.StateManager;
import org.jpox.exceptions.ClassNotResolvedException;
import org.jpox.exceptions.JPOXDataStoreException;
import org.jpox.exceptions.NoPersistenceInformationException;
import org.jpox.metadata.AbstractClassMetaData;
import org.jpox.metadata.AbstractPropertyMetaData;
import org.jpox.metadata.ClassMetaData;
import org.jpox.metadata.ClassPersistenceModifier;
import org.jpox.metadata.IdentityStrategy;
import org.jpox.metadata.SequenceMetaData;
import org.jpox.store.DatastoreClass;
import org.jpox.store.DatastoreContainerObject;
import org.jpox.store.DatastoreObject;
import org.jpox.store.Extent;
import org.jpox.store.FetchStatement;
import org.jpox.store.JPOXConnection;
import org.jpox.store.JPOXSequence;
import org.jpox.store.StoreData;
import org.jpox.store.StoreManager;
import org.jpox.store.scostore.ArrayStore;
import org.jpox.store.scostore.CollectionStore;
import org.jpox.store.scostore.MapStore;
import org.jpox.util.ClassUtils;
import org.jpox.util.MacroString.IdentifierMacro;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hippoecm.repository.api.HippoNodeType;

public class StoreManagerImpl extends StoreManager {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    protected final Logger log = LoggerFactory.getLogger(StoreManagerImpl.class);

    private String username;
    private String password;
    private Session session;
    private Node types;

    public StoreManagerImpl(ClassLoaderResolver clr, ObjectManagerFactoryImpl omf) throws RepositoryException {
        super(clr, omf);
        this.session = null;
        // FIXME: Provide a default AutoStartMechanism
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public void setTypes(Node types) {
        this.types = types;
    }

    public void close() {
        super.close();
    }

    public Date getDatastoreDate() {
        return null;
    }

    public JPOXSequence getJPOXSequence(ObjectManager om, SequenceMetaData seqmd) {
        return null;
    }

    public JPOXConnection getJPOXConnection(final ObjectManager om) {
        return null;
    }

    public ConnectionFactory getConnectionFactory() {
        try {
            // new ConnectionFactoryImpl(username, password);
            if (session == null)
                return new ConnectionFactoryImpl(getOMFContext());
            else
                return new ConnectionFactoryImpl(getOMFContext(), session);
        } catch (RepositoryException ex) {
            // FIXME: log something
            return null;
        }
    }

    public void addClasses(String[] classes, ClassLoaderResolver clr, Writer writer, boolean completeDdl) {
        if (classes == null)
            return;
        String[] classNames = ClassUtils.getUnsupportedClassNames(getOMFContext().getTypeManager(), classes);
        List cmds = new ArrayList();
        for (int i = 0; i < classNames.length; i++) {
            Class cls = null;
            try {
                cls = clr.classForName(classNames[i]);
                if (!cls.isInterface()) {
                    AbstractClassMetaData cmd = getMetaDataManager().getMetaDataForClass(classNames[i], clr);
                    if (cmd == null) {
                        throw new NoPersistenceInformationException(classNames[i]);
                    }
                    cmds.addAll(getMetaDataManager().getReferencedClassMetaData(cmd, null, clr));
                }
            } catch (ClassNotResolvedException ex) {
                // Class not found so ignore it
            }
        }
        for (Iterator iter = cmds.iterator(); iter.hasNext();) {
            ClassMetaData cmd = (ClassMetaData) iter.next();
            if (cmd.getPersistenceModifier() != ClassPersistenceModifier.PERSISTENCE_CAPABLE)
                return; // FIXME: shouldn't this be a break?
            StoreData sd = (StoreData) storeDataMgr.get(cmd.getFullClassName());
            if (sd == null) {
                sd = new StoreData(cmd.getFullClassName(), cmd, StoreData.FCO_TYPE, null);
                //sd = new TableStoreData(cmd, new MyDatastoreContainerObject(), true);
                registerStoreData(sd);
                sd = (StoreData) storeDataMgr.get(cmd.getFullClassName());
            }
        }
    }

    public void removeAllClasses(ClassLoaderResolver clr) {
    }

    ManagedConnection getConnection(ObjectManager om) {
        ConnectionManager manager = omfContext.getConnectionManager();
        ConnectionFactory factory = omfContext.getConnectionFactoryRegistry().lookupConnectionFactory("jcr");
        ManagedConnection connection = manager.allocateConnection(factory, om, null);
        return connection;
    }

    public void insertObject(StateManager sm) {
        ObjectManager om = sm.getObjectManager();
        ManagedConnection mconn = getConnection(om);
        if (log.isDebugEnabled())
            log.debug("insert object");
        try {
            Session session = (Session) mconn.getConnection();
            AbstractClassMetaData cmd = sm.getClassMetaData();
            Node node = session.getRootNode();
            String table = cmd.getTable();
            if (table != null && !table.equals(""))
                node = node.getNode(table);
            String nodeName = null; // FIXME: cmd.getColumn();
            if (nodeName == null || nodeName.equals(""))
                nodeName = cmd.getEntityName();
            Node nodetypeNode = types.getNode(cmd.getFullClassName());
            if(!node.isCheckedOut()) {
                node.checkout();
            }
            node = node.addNode(nodeName, nodetypeNode.getProperty(HippoNodeType.HIPPOSYS_NODETYPE).getString());
            if(node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                node.addMixin(HippoNodeType.NT_HARDDOCUMENT);
            } else if(node.isNodeType(HippoNodeType.NT_REQUEST)) {
                node.addMixin("mix:referenceable");
            }
            sm.provideFields(cmd.getAllFieldNumbers(), new FieldManagerImpl(sm, session, types, node));
        } catch (PathNotFoundException ex) {
            log.error(ex.getClass().getName()+": "+ex.getMessage());
        } catch (ItemExistsException ex) {
            log.error(ex.getClass().getName()+": "+ex.getMessage());
        } catch (NoSuchNodeTypeException ex) {
            log.error(ex.getClass().getName()+": "+ex.getMessage());
        } catch (VersionException ex) {
            log.error(ex.getClass().getName()+": "+ex.getMessage());
        } catch (ConstraintViolationException ex) {
            log.error(ex.getClass().getName()+": "+ex.getMessage());
        } catch (LockException ex) {
            log.error(ex.getClass().getName()+": "+ex.getMessage());
        } catch (RepositoryException ex) {
            log.error(ex.getClass().getName()+": "+ex.getMessage());
        } finally {
            mconn.release();
        }
    }

    public void updateObject(StateManager sm, int fieldNumbers[]) {
        ManagedConnection mconn = getConnection(sm.getObjectManager());
        if (log.isDebugEnabled())
            log.debug("update object");
        try {
            Session session = (Session) mconn.getConnection();
            JCROID oid = (JCROID) sm.getExternalObjectId(null);
            Node node = oid.node;
            if (node == null) {
                node = session.getNodeByUUID(oid.key);
            }
            sm.provideFields(fieldNumbers, new FieldManagerImpl(sm, session, types, node));
        } catch (ItemNotFoundException ex) {
            log.error(ex.getClass().getName()+": "+ex.getMessage());
        } catch (RepositoryException ex) {
            log.error(ex.getClass().getName()+": "+ex.getMessage());
        } finally {
            mconn.release();
        }
    }

    public void fetchObject(StateManager sm, int fieldNumbers[]) {
        ManagedConnection mconn = getConnection(sm.getObjectManager());
        if (log.isDebugEnabled())
            log.debug("fetch object");
        try {
            Session session = (Session) mconn.getConnection();
            AbstractClassMetaData cmd = sm.getClassMetaData();
            sm.replaceFields(fieldNumbers, new FieldManagerImpl(sm, session, types));
        } finally {
            mconn.release();
        }
    }

    public void locateObject(StateManager sm) {
        if (log.isDebugEnabled())
            log.debug("locate object");
        /* For some reason, the following line causes problems.
         * getDatastoreClass(sm.getObject().getClass().getName(), sm.getObjectManager().getClassLoaderResolver())
         *     .locate(sm);
         */
    }

    public Object findObject(ObjectManager om, Object id) {
        if (log.isDebugEnabled())
            log.debug("find object");
        return null;
    }

    public void deleteObject(StateManager sm) {
        if (log.isDebugEnabled())
            log.debug("delete object");
        // FIXME
    }

    public Object newObjectID(ObjectManager om, String className, PersistenceCapable pc) {
        if (log.isDebugEnabled())
            log.debug("new object id");
        return super.newObjectID(om, className, pc);
    }

    public void flush(ObjectManager om) {
        if (log.isDebugEnabled())
            log.debug("flush");
    }

    public boolean usesDatastoreClass() {
        if (log.isDebugEnabled())
            log.debug("uses datastore class");
        return false;
    }

    public String getClassNameForObjectID(Object id, ClassLoaderResolver clr, ObjectManager om) {
        JCROID oid = (JCROID) id;
        return oid.classname;
    }

    public boolean isStrategyDatastoreAttributed(IdentityStrategy identityStrategy, boolean datastoreIdentityField) {
        if (log.isDebugEnabled())
            log.debug("is datastore attributed");
        return true;
    }

    public Object getStrategyValue(ObjectManager om, DatastoreClass table, AbstractClassMetaData cmd,
            int absoluteFieldNumber) {
        throw new JPOXDataStoreException("Unsupported method");
    }

    public Extent getExtent(ObjectManager om, Class c, boolean subclasses) {
        return null;
    }

    public boolean supportsQueryLanguage(String language) {
        return false;
    }

    public ArrayStore getBackingStoreForArray(AbstractPropertyMetaData fmd, DatastoreObject datastoreTable,
            ClassLoaderResolver clr) {
        return null;
    }

    public CollectionStore getBackingStoreForCollection(AbstractPropertyMetaData fmd, DatastoreObject datastoreTable,
            ClassLoaderResolver clr, boolean instantiated, boolean listBased) {
        return null;
    }

    public MapStore getBackingStoreForMap(AbstractPropertyMetaData fmd, DatastoreObject datastoreTable,
            ClassLoaderResolver clr) {
        return null;
    }

    public DatastoreContainerObject newJoinDatastoreContainerObject(AbstractPropertyMetaData fmd,
            ClassLoaderResolver clr) {
        return null;
    }

    public FetchStatement getFetchStatement(DatastoreContainerObject table) {
        return null;
    }

    public void resolveIdentifierMacro(IdentifierMacro im, ClassLoaderResolver clr) {
    }

    public void outputDatastoreInformation(PrintStream ps) throws Exception {
    }

    public void outputSchemaInformation(PrintStream ps) throws Exception {
    }
}
