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

public class StoreManagerImpl extends StoreManager {
    private String username;
    private String password;

    public StoreManagerImpl(ClassLoaderResolver clr, ObjectManagerFactoryImpl omf, String username, String password)
            throws RepositoryException {
        super(clr, omf, username, password);
        // FIXME: Provide a default AutoStartMechanism
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
            return new ConnectionFactoryImpl(getOMFContext()); // return new ConnectionFactoryImpl(username, password);
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
            } catch (ClassNotResolvedException cnre) {
                // Class not found so ignore it
            }
        }
        for (Iterator iter = cmds.iterator(); iter.hasNext();) {
            ClassMetaData cmd = (ClassMetaData) iter.next();
            if (cmd.getPersistenceModifier() != ClassPersistenceModifier.PERSISTENCE_CAPABLE)
                return; // FIXME: shouldn't this be a break?
            StoreData sd = (StoreData) storeDataByClass.get(cmd.getFullClassName());
            if (sd == null) {
                sd = new StoreData(cmd.getFullClassName(), cmd, StoreData.FCO_TYPE, null);
                //sd = new TableStoreData(cmd, new MyDatastoreContainerObject(), true);
                registerStoreData(sd);
                sd = (StoreData) storeDataByClass.get(cmd.getFullClassName());
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

    public void insert(StateManager sm) {
        ObjectManager om = sm.getObjectManager();
        ManagedConnection mconn = getConnection(om);
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
            node = node.addNode(nodeName);
            node.setProperty("classname", cmd.getFullClassName());
            node.addMixin("mix:referenceable"); // FIXME: should be per node type definition
            sm.provideFields(cmd.getAllFieldNumbers(), new FieldManagerImpl(sm, session, node));
        } catch (PathNotFoundException ex) {
            System.err.println("PathNotFoundException :" + ex.getMessage());
        } catch (ItemExistsException ex) {
            System.err.println("ItemExistsException :" + ex.getMessage());
        } catch (NoSuchNodeTypeException ex) {
            System.err.println("NoSuchNodeTypeException :" + ex.getMessage());
        } catch (VersionException ex) {
            System.err.println("VersionException :" + ex.getMessage());
        } catch (ConstraintViolationException ex) {
            System.err.println("ConstraintViolationException :" + ex.getMessage());
        } catch (LockException ex) {
            System.err.println("LockException :" + ex.getMessage());
        } catch (RepositoryException ex) {
            System.err.println("RepositoryException :" + ex.getMessage());
        } finally {
            mconn.release();
        }
    }

    public void updateObject(StateManager sm, int fieldNumbers[]) {
        updateObject(sm, fieldNumbers);
    }

    public void update(StateManager sm, int fieldNumbers[]) {
        ManagedConnection mconn = getConnection(sm.getObjectManager());
        try {
            Session session = (Session) mconn.getConnection();
            sm.provideFields(fieldNumbers, new FieldManagerImpl(sm, session));
        } finally {
            mconn.release();
        }
    }

    //public void fetchObject(StateManager sm, int fieldNumbers[]) { fetch(sm, fieldNumbers); }
    public void fetch(StateManager sm, int fieldNumbers[]) {
        ManagedConnection mconn = getConnection(sm.getObjectManager());
        try {
            Session session = (Session) mconn.getConnection();
            AbstractClassMetaData cmd = sm.getClassMetaData();
            sm.replaceFields(fieldNumbers, new FieldManagerImpl(sm, session));
        } finally {
            mconn.release();
        }
    }

    public void locate(StateManager sm) {
        super.locate(sm);
    }

    public Object newObjectID(ObjectManager om, String className, PersistenceCapable pc) {
        return super.newObjectID(om, className, pc);
    }

    public void flush(ObjectManager om) {
        ManagedConnection mconn = getConnection(om);
        try {
            Session session = (Session) mconn.getConnection();
            session.save();
        } catch (RepositoryException ex) {
            System.err.println("RepositoryException: " + ex.getMessage());
        } finally {
            mconn.release();
        }
    }

    public boolean usesDatastoreClass() {
        return false;
    }

    public String getClassNameForObjectID(Object id, ClassLoaderResolver clr, ObjectManager om) {
        ManagedConnection mconn = getConnection(om);
        try {
            Session session = (Session) mconn.getConnection();
            JCROID oid = (JCROID) id;
            Node node = oid.getNode(session);
            String className = node.getProperty("classname").getString();
            return className;
        } catch (PathNotFoundException ex) {
            throw new JPOXDataStoreException("PathNotFoundException", ex, id);
        } catch (ValueFormatException ex) {
            throw new JPOXDataStoreException("ValueFormatException", ex, id);
        } catch (RepositoryException ex) {
            throw new JPOXDataStoreException("RepositoryException", ex, id);
        } finally {
            mconn.release();
        }
    }

    public boolean isStrategyDatastoreAttributed(IdentityStrategy identityStrategy, boolean datastoreIdentityField) {
        return true;
    }

    public Object getStrategyValue(ObjectManager om, DatastoreClass table, AbstractClassMetaData cmd,
            int absoluteFieldNumber) {
        return new JCROID();
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
