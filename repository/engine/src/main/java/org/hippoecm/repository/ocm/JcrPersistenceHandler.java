/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Arrays;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.datanucleus.ObjectManager;
import org.datanucleus.StateManager;
import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.exceptions.NucleusObjectNotFoundException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.identity.OIDImpl;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.IdentityType;
import org.datanucleus.store.ExecutionContext;
import org.datanucleus.store.ObjectProvider;
import org.datanucleus.store.StoreManager;
import org.datanucleus.store.StorePersistenceHandler;
import org.datanucleus.store.connection.ManagedConnection;
import org.datanucleus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrPersistenceHandler implements StorePersistenceHandler {

    private final Logger log = LoggerFactory.getLogger(JcrPersistenceHandler.class);
    
    private JcrStoreManager storeMgr;

    public JcrPersistenceHandler(StoreManager storeMgr) {
        this.storeMgr = (JcrStoreManager) storeMgr;
    }
    
    /**
     * Method to close the handler and release any resources.
     */
    public void close() {
    }

    /**
     * Locates the object managed by the passed StateManager into the JCR repository.
     * @param sm StateManager
     * @throws NucleusObjectNotFoundException if the object cannot be located
     */
    public void locateObject(StateManager sm) {
        final AbstractClassMetaData cmd = sm.getClassMetaData();
        if (cmd.getIdentityType() == IdentityType.APPLICATION) {
            ManagedConnection mconn = storeMgr.getConnection(sm.getObjectManager().getExecutionContext());
            try {
                Session session = (Session) mconn.getConnection();
                JcrOID oid = (JcrOID) sm.getExternalObjectId(sm.getObject());
                Node node = oid.getNode(session);
                if (node == null) {
                    throw new NucleusObjectNotFoundException("Object not found", sm.getExternalObjectId(sm.getObject()));
                }
                if (log.isDebugEnabled()) {
                    log.debug("JCR.Locate {}", oid.getKeyValue());
                }
                return;
            } finally {
                mconn.release();
            }
        } else if (cmd.getIdentityType() == IdentityType.DATASTORE) {
            throw new NucleusUserException("JCR.DatastoreID");
        }
    }

    /**
     * Updates a persistent object in the datastore.
     * @param op The state manager of the object to be updated.
     * @param fieldNumbers The numbers of the fields to be updated.
     * @throws NucleusDataStoreException when an error occurs in the datastore communication
     */
    @Override
    public void updateObject(ObjectProvider op, int[] fieldNumbers) {
        // Check if read-only so update not permitted
        storeMgr.assertReadOnlyForUpdateOfObject(op);

        // TODO Implement version checking?
        ManagedConnection mconn = storeMgr.getConnection(op.getExecutionContext());

        try {
            long startTime = System.currentTimeMillis();
            if (log.isDebugEnabled()) {
                AbstractClassMetaData cmd = op.getClassMetaData();
                StringBuffer fieldStr = new StringBuffer();
                for (int i = 0; i < fieldNumbers.length; i++) {
                    if (i > 0) {
                        fieldStr.append(",");
                    }
                    fieldStr.append(cmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldNumbers[i]).getName());
                }
                log.debug("JCR.Update.Start {} {} {}", new Object[] {StringUtils.toJVMIDString(op
                        .getObject()), op.getInternalObjectId(), fieldStr.toString()});
            }

            // update
            Session session = (Session) mconn.getConnection();
            Node node = null;
            Object objectId = op.getExternalObjectId();
            if(objectId instanceof JcrOID) {
                node = ((JcrOID) objectId).getNode(session);
            } else if(objectId instanceof OIDImpl) {
                Object objectKey = ((OIDImpl) objectId).getKeyValue();
                if(objectKey instanceof String) {
                    node = JcrOID.getNode(session, (String) objectKey);
                } else {
                    throw new NucleusDataStoreException("OID");
                }
            } else {
                throw new NucleusDataStoreException("OID");
            }

            if (node == null) {
                throw new NucleusDataStoreException("Object not found", op.getExternalObjectId());
            }

            op.provideFields(fieldNumbers, new UpdateFieldManager(op, session, storeMgr.columnResolver, storeMgr.typeResolver, node));

            if (log.isDebugEnabled()) {
                log.debug("JCR.ExecutionTime {}",
                        (System.currentTimeMillis() - startTime));
            }
            if (storeMgr.getRuntimeManager() != null) {
                storeMgr.getRuntimeManager().incrementUpdateCount();
            }
        } finally {
            mconn.release();
        }
    }

    public void deleteObject(StateManager sm) {
        // Check if read-only so update not permitted
        storeMgr.assertReadOnlyForUpdateOfObject(sm.getObjectProvider());
    }

    /**
     * Accessor for the object with the specified identity (if present). Since we don't manage the memory instantiation
     * of objects this just returns null.
     * @param om ObjectManager in use
     * @param id Identity of the object
     * @return The object
     */
    public Object findObject(ObjectManager om, Object id) {
        return null;
    }

    @Override
    public void batchStart(ExecutionContext ec) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void batchEnd(ExecutionContext ec) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Inserts a persistent object into the database.
     * @param sm The state manager of the object to be inserted.
     * @throws NucleusDataStoreException when an error occurs in the datastore communication
     */
    @Override
    public void insertObject(ObjectProvider op) {
        // Check if read-only so update not permitted
        storeMgr.assertReadOnlyForUpdateOfObject(op);

        // pre-insert
        // for hierarchical mapping: ensure that parent is created before child entry
        AbstractClassMetaData cmd = op.getClassMetaData();

        if (cmd.getIdentityType() == IdentityType.APPLICATION) {
            // FIXME not needed since jcr creates it's own uuids
            // Check existence of the object
            try {
                locateObject(op);
                throw new NucleusUserException("JCR.Insert.ObjectWithIdAlreadyExists " + StringUtils
                        .toJVMIDString(op.getObject()), op.getInternalObjectId());
            } catch (NucleusObjectNotFoundException onfe) {
                // Do nothing since object with this id doesn't exist
            }
        } else if (cmd.getIdentityType() == IdentityType.DATASTORE) {
            updateObject(op, cmd.getAllMemberPositions());
            return;
        }

        // insert
        ManagedConnection mconn = storeMgr.getConnection(op.getExecutionContext());
        try {
            long startTime = System.currentTimeMillis();
            if (log.isDebugEnabled()) {
                log.debug("JCR.Insert.Start {} {}", StringUtils.toJVMIDString(op.getObject()), op.getInternalObjectId());
            }
            Session session = (Session) mconn.getConnection();
            Node node = session.getRootNode();

            String table = cmd.getTable();
            if (table != null && !table.equals("")) {
                node = node.getNode(table);
            } else {
                throw new NucleusDataStoreException("Table not found from meta data");
            }

            // FIXME: cmd.getColumn();
            String nodeName = null;
            if (nodeName == null || nodeName.equals("")) {
                nodeName = cmd.getEntityName();
            }
            if (!node.isCheckedOut()) {
                node.getSession().getWorkspace().getVersionManager().checkout(node.getPath());
            }
            String[] nodeTypes = storeMgr.typeResolver.resolve(cmd.getFullClassName());
            if (nodeTypes.length > 0) {
                node = node.addNode(nodeName, nodeTypes[0]);
                for (int i = 1; i < nodeTypes.length; i++) {
                    node.addMixin(nodeTypes[i]);
                }
            } else {
                node = node.addNode(nodeName);
            }

            int[] fieldNumbers = op.getClassMetaData().getAllMemberPositions();
            op.provideFields(fieldNumbers, new InsertFieldManager(op, session, storeMgr.columnResolver, storeMgr.typeResolver, node));

            // TODO Implement version retrieval
            if (log.isDebugEnabled()) {
                log.debug("JCR.ExecutionTime {}",
                        (System.currentTimeMillis() - startTime));
            }
            if (storeMgr.getRuntimeManager() != null) {
                storeMgr.getRuntimeManager().incrementInsertCount();
            }
            if (log.isDebugEnabled()) {
                log.debug("JCR.Insert.ObjectPersisted {} {}", StringUtils.toJVMIDString(op.getObject()), op.getInternalObjectId());
            }
        } catch (RepositoryException ex) {
            throw new NucleusDataStoreException(ex.getClass().getName() + ": " + ex.getMessage(), ex);
        } finally {
            mconn.release();
        }
    }

    @Override
    public void deleteObject(ObjectProvider op) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Fetches a persistent object from the JCR Repository.
     * @param sm The state manager of the object to be fetched.
     * @param fieldNumbers The numbers of the fields to be fetched.
     * @throws NucleusObjectNotFoundException if the object doesn't exist
     * @throws NucleusDataStoreException when an error occurs in the datastore communication
     */
    @Override
    public void fetchObject(ObjectProvider op, int[] fieldNumbers) {
        if (op.getLifecycleState().isDeleted()) {
            return;
        }

        fieldNumbers = op.getClassMetaData().getAllMemberPositions();

        AbstractClassMetaData cmd = op.getClassMetaData();
        if (log.isDebugEnabled()) {
            // Debug information about what we are retrieving
            StringBuffer str = new StringBuffer("Fetching object \"");
            str.append(StringUtils.toJVMIDString(op.getObject())).append("\" (id=");
            str.append(op.getExecutionContext().getApiAdapter().getObjectId(op)).append(")").append(" fields [");
            for (int i = 0; i < fieldNumbers.length; i++) {
                if (i > 0) {
                    str.append(",");
                }
                str.append(cmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldNumbers[i]).getName());
            }
            str.append("]");
            log.debug(str.toString());
        }

        ManagedConnection mconn = storeMgr.getConnection(op.getExecutionContext());
        try {
            long startTime = System.currentTimeMillis();
            if (log.isDebugEnabled()) {
                log.debug("JCR.Fetch.Start {} {}", StringUtils.toJVMIDString(op
                        .getObject()), op.getInternalObjectId());
            }

            Session session = (Session) mconn.getConnection();
            op.replaceFields(fieldNumbers, new FetchFieldManager(op, session, storeMgr.columnResolver, storeMgr.typeResolver, null));

            if (log.isDebugEnabled()) {
                log.debug("JCR.ExecutionTime {}",
                        (System.currentTimeMillis() - startTime));
            }
            if (storeMgr.getRuntimeManager() != null) {
                storeMgr.getRuntimeManager().incrementFetchCount();
            }
        } finally {
            mconn.release();
        }
    }

    @Override
    public void locateObject(ObjectProvider op) {
    }

    @Override
    public void locateObjects(ObjectProvider[] ops) {
    }

    /**
     * Accessor for the object with the specified identity (if present). Since we don't manage the memory instantiation
     * of objects this just returns null.
     * @param om ObjectManager in use
     * @param id Identity of the object
     * @return The object
     */
    @Override
    public Object findObject(ExecutionContext ec, Object o) {
        return null;
    }

    @Override
    public Object[] findObjects(ExecutionContext ec, Object[] os) {
        Arrays.fill(os, null);
        return os;
    }
}
