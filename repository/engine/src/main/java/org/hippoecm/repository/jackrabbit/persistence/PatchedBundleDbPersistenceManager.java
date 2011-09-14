/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.repository.jackrabbit.persistence;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.persistence.pool.BundleDbPersistenceManager;
import org.apache.jackrabbit.core.persistence.util.NodePropBundle;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.util.db.DbUtility;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PatchedBundleDbPersistenceManager extends BundleDbPersistenceManager {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    /** the default logger */
    private static Logger log = LoggerFactory.getLogger(PatchedBundleDbPersistenceManager.class);

    /**
     * the name of this persistence manager
     */
    private String name = super.toString();

    private byte[] getBytes(Blob blob) throws SQLException, IOException {
        InputStream in = null;
        try {
            long length = blob.length();
            byte[] bytes = new byte[(int) length];
            in = blob.getBinaryStream();
            int read, pos = 0;
            while ((read = in.read(bytes, pos, bytes.length - pos)) > 0) {
                pos += read;
            }
            return bytes;
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * Checks a single bundle for inconsistencies, ie. inexistent child nodes
     * and inexistent parents.
     *
     * @param id node id for the bundle to check
     * @param bundle the bundle to check
     * @param fix if <code>true</code>, repair things that can be repaired
     * @param modifications if <code>fix == true</code>, collect the repaired
     * {@linkplain NodePropBundle bundles} here
     * @param orphans if <code>fix == true</code>, the resulting collection of
     * the orphan nodes to be destroyed
     */
    protected void checkBundleConsistency(NodeId id, NodePropBundle bundle,
                                          boolean fix, Collection<NodePropBundle> modifications, Collection orphans) {
        //log.info(name + ": checking bundle '" + id + "'");

        // skip all system nodes except root node
        if (id.toString().endsWith("babecafebabe")
                && !id.toString().equals("cafebabe-cafe-babe-cafe-babecafebabe")) {
            return;
        }

        // look at the node's children
        Collection<NodePropBundle.ChildNodeEntry> missingChildren = new ArrayList<NodePropBundle.ChildNodeEntry>();
        for (NodePropBundle.ChildNodeEntry entry : bundle.getChildNodeEntries()) {

            // skip check for system nodes (root, system root, version storage, node types)
            if (entry.getId().toString().endsWith("babecafebabe")) {
                continue;
            }

            try {
                // analyze child node bundles
                NodePropBundle child = loadBundle(entry.getId());
                if (child == null) {
                    log.error(
                            "NodeState '" + id + "' references inexistent child"
                            + " '" + entry.getName() + "' with id "
                            + "'" + entry.getId() + "'");
                    missingChildren.add(entry);
                } else {
                    NodeId cp = child.getParentId();
                    if (cp == null) {
                        log.error("ChildNode has invalid parent uuid: <null>");
                    } else if (!cp.equals(id)) {
                        log.error("ChildNode has invalid parent uuid: '" + cp + "' (instead of '" + id + "')");
                    }
                }
            } catch (ItemStateException e) {
                // problem already logged (loadBundle called with logDetailedErrors=true)
            }
        }
        // remove child node entry (if fixing is enabled)
        if (fix && !missingChildren.isEmpty()) {
            for (NodePropBundle.ChildNodeEntry entry : missingChildren) {
                bundle.getChildNodeEntries().remove(entry);
            }
            modifications.add(bundle);
        }

        // check parent reference
        NodeId parentId = bundle.getParentId();
        try {
            // skip root nodes (that point to itself)
            if (parentId != null && !id.toString().endsWith("babecafebabe")) {
                if (loadBundle(parentId) == null) {
                    log.error("NodeState '" + id + "' references inexistent parent uuid '" + parentId + "'");
                    if (fix) {
                        orphans.add(bundle);
                    }
                }
            }
        } catch (ItemStateException e) {
            log.error("Error reading node '" + parentId + "' (parent of '" + id + "'): " + e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkConsistency(String[] uuids, boolean recursive, boolean fix) {
        log.info("{}: checking workspace consistency...", name);

        int count = 0;
        int total = 0;
        Collection<NodePropBundle> modifications = new ArrayList<NodePropBundle>();
        Collection orphans = new ArrayList();
        Collection<NodeId> references = new ArrayList();

        if (uuids == null) {
            // get all node bundles in the database with a single sql statement,
            // which is (probably) faster than loading each bundle and traversing the tree
            ResultSet rs = null;
            try {
                String sql = "select count(*) from " + schemaObjectPrefix + "BUNDLE";
                try {
                    rs = conHelper.exec(sql, new Object[0], false, 0);
                    if (!rs.next()) {
                        log.error("Could not retrieve total number of bundles. empty result set.");
                        return;
                    }
                    total = rs.getInt(1);

                    log.info(name + ": Checking " + total + " bundles...");
                } finally {
                    DbUtility.close(rs);
                }
                if (getStorageModel() == SM_BINARY_KEYS) {
                    sql = "select NODE_ID from " + schemaObjectPrefix + "BUNDLE";
                } else {
                    sql = "select NODE_ID_HI, NODE_ID_LO from " + schemaObjectPrefix + "BUNDLE";
                }
                rs = conHelper.exec(sql, new Object[0], false, 0);

                // iterate over all node bundles in the db
                while (rs.next()) {
                    NodeId id;
                    if (getStorageModel() == SM_BINARY_KEYS) {
                        id = new NodeId(rs.getBytes(1));
                    } else {
                        id = new NodeId(rs.getLong(1), rs.getLong(2));
                    }

                    // issuing 2nd statement to circumvent issue JCR-1474
                    ResultSet bRs = null;
                    byte[] data = null;
                    try {
                        bRs = conHelper.exec(bundleSelectSQL, getKey(id), false, 0);
                        if (!bRs.next()) {
                            //throw new SQLException("bundle cannot be retrieved?");
                            log.error("invalid bundle '" + id + "', bundle cannot be retrieved or empty result?");
                        } else {
                            NodePropBundle bundle = readBundle(id, bRs, 1);
                            checkBundleConsistency(id, bundle, fix, modifications);
                        }
                    } finally {
                        DbUtility.close(bRs);
                    }

                    count++;
                    if (count % 1000 == 0) {
                        log.info(name + ": checked " + count + "/" + total + " bundles...");
                    }
                }
            } catch (Exception e) {
                log.error("Error loading bundle", e);
            } finally {
                DbUtility.close(rs);
                total = count;
            }
        } else {
            // check only given uuids, handle recursive flag

            // 1) convert uuid array to modifiable list
            // 2) for each uuid do
            //     a) load node bundle
            //     b) check bundle, store any bundle-to-be-modified in collection
            //     c) if recursive, add child uuids to list of uuids

            List<NodeId> idList = new ArrayList<NodeId>(uuids.length);
            // convert uuid string array to list of UUID objects
            for (int i = 0; i < uuids.length; i++) {
                try {
                    idList.add(new NodeId(uuids[i]));
                } catch (IllegalArgumentException e) {
                    log.error("Invalid uuid for consistency check, skipping: '" + uuids[i] + "': " + e);
                }
            }

            // iterate over UUIDs (including ones that are newly added inside the loop!)
            for (int i = 0; i < idList.size(); i++) {
                NodeId id = idList.get(i);
                try {
                    // load the node from the database
                    NodePropBundle bundle = loadBundle(id);

                    if (bundle == null) {
                        log.error("No bundle found for uuid '" + id + "'");
                        continue;
                    }

                    checkBundleConsistency(id, bundle, fix, modifications, orphans);

                    if (recursive) {
                        for (NodePropBundle.ChildNodeEntry entry : bundle.getChildNodeEntries()) {
                            idList.add(entry.getId());
                        }
                    }

                    count++;
                    if (count % 1000 == 0) {
                        log.info(name + ": checked " + count + "/" + idList.size() + " bundles...");
                    }
                } catch (ItemStateException e) {
                    // problem already logged (loadBundle called with logDetailedErrors=true)
                }
            }

            total = idList.size();
        }

        // repair collected broken bundles
        if (fix && !modifications.isEmpty()) {
            log.info(name + ": Fixing " + modifications.size() + " inconsistent bundle(s)...");
            for (NodePropBundle bundle : modifications) {
                try {
                    log.info(name + ": Fixing bundle '" + bundle.getId() + "'");
                    bundle.markOld(); // use UPDATE instead of INSERT
                    storeBundle(bundle);
                    evictBundle(bundle.getId());
                } catch (ItemStateException e) {
                    log.error(name + ": Error storing fixed bundle: " + e);
                }
            }
        }

        // remove orphans
        if (fix && !orphans.isEmpty()) {
            log.info(name + ": Removing " + orphans.size() + " orphan bundle(s)...");
            Iterator iterator = orphans.iterator();
            while (iterator.hasNext()) {
                NodePropBundle bundle = (NodePropBundle) iterator.next();
                try {
                    log.info(name + ": Removing orphan bundle and child node bundles of '" + bundle.getId() + "'");
                    destroyOrphansRecursive(bundle);
                } catch (ItemStateException e) {
                    log.error(name + ": Error removing bundle: " + e);
                }
            }
        }
        log.info(name + ": checked " + count + "/" + total + " bundles.");
        
        // Now we should check the references table, since nodes and entries could have been removed
        references = checkReferences(name);
        
        // remove broken references

        if (fix && !references.isEmpty()) {
            log.info(name + ": Fixing " + references.size() + " reference(s)...");
            for (NodeId targetNode : references) {
                checkAndFixReference(targetNode, true);
            }
        }
    }

    protected Collection<NodeId> checkReferences(String name) {

        log.info("{}: checking references consistency...", name);

        int count = 0;
        int total = 0;
        Collection<NodeId> references = new ArrayList();
        
        // Get all references in the database with a single sql statement.
        ResultSet rs = null;
        try {
            String sql = "select count(*) from " + schemaObjectPrefix + "REFS";
            try {
                rs = conHelper.exec(sql, new Object[0], false, 0);
                if (!rs.next()) {
                    log.error("Could not retrieve total number of references. Empty result set.");
                    return new ArrayList();
                }
                total = rs.getInt(1);
                log.info(name + ": checked " + count + "/" + total + " references...");
            } finally {
                DbUtility.close(rs);
            }
            if (getStorageModel() == SM_BINARY_KEYS) {
                sql = "select NODE_ID from " + schemaObjectPrefix + "REFS";
            } else {
                sql = "select NODE_ID_HI, NODE_ID_LO from " + schemaObjectPrefix + "REFS";
            }
            rs = conHelper.exec(sql, new Object[0], false, 0);

            // iterate over all node bundles in the db
            while (rs.next()) {
                NodeId id;
                if (getStorageModel() == SM_BINARY_KEYS) {
                    id = new NodeId(rs.getBytes(1));
                } else {
                    id = new NodeId(rs.getLong(1), rs.getLong(2));
                }
                if(checkAndFixReference(id, false)) {
                    references.add(id);
                }
                count++;
                if (count % 1000 == 0) {
                    log.info(name + ": checked " + count + "/" + total + " references...");
                }
            }
        } catch (Exception e) {
            log.error("Error loading bundle", e);
        } finally {
            DbUtility.close(rs);
            total = count;
            log.info(name + ": checked " + count + "/" + total + " references.");
        }
        return references;
    }

    protected void checkPath(ItemState item) throws NoSuchItemStateException, ItemStateException {
        NodeId parent = item.getParentId();
        if (parent != null) {
            NodeState state = load(parent);
            if (item.isNode()) {
                if (state.getChildNodeEntry(((NodeState)item).getNodeId()) == null) {
                    throw new ItemStateException("failed to build path");
                }
            } else {
                if (!state.getPropertyNames().contains(((PropertyId)item.getId()).getName())) {
                    throw new ItemStateException("failed to build path");
                }
            }
            checkPath(state);
        }
    }

    protected boolean checkAndFixReference(NodeId targetNode, boolean fix) {
        NodeReferences fixedRefs = (fix ? new NodeReferences(targetNode) : null);
        try {
            // try to load reference
            NodeReferences refs = loadReferencesTo(targetNode);
            load(targetNode);
            for (Iterator refIterator = refs.getReferences().iterator(); refIterator.hasNext(); ) {
                PropertyId refPropertyId;
                refPropertyId = (PropertyId)refIterator.next();
                try {
                    Name propertyName = refPropertyId.getName();
                    if(!(propertyName.equals(NameConstants.JCR_VERSIONHISTORY) ||
                         propertyName.equals(NameConstants.JCR_BASEVERSION) ||
                         propertyName.equals(NameConstants.JCR_PREDECESSORS))) {
                        PropertyState propertyState = load(refPropertyId);
                        checkPath(propertyState);
                        if(fix) {
                            log.info("keep reference "+refPropertyId+" to "+targetNode);
                            fixedRefs.addReference(refPropertyId);
                        }
                    }
                } catch (NoSuchItemStateException e) {
                    if (!fix) {
                        log.error("Error in reference to " + targetNode + ": " + e);
                        return true;
                    } else {
                        log.info("drop reference "+refPropertyId+" to "+targetNode);
                    }
                } catch (ItemStateException e) {
                    if (!fix) {
                        log.error("Error in reference to " + targetNode + ": " + e);
                        return true;
                    } else {
                        log.info("drop reference "+refPropertyId+" to "+targetNode);
                    }
                }
            }
        } catch (NoSuchItemStateException e) {
            if (fix) {
                log.info(name + ": Removing references to: '" + targetNode + "'");
                try {
                    conHelper.exec(nodeReferenceDeleteSQL, getKey(targetNode));
                    return false;
                } catch (Exception ex) {
                    String msg = "failed to delete references to: " + targetNode;
                    log.error(msg, e);
                    return true;
                }
            } else {
                log.error("Error in reference to " + targetNode + ": " + e);
                return true;
            }
        } catch (ItemStateException e) {
            if (fix) {
                log.info(name + ": Removing references to: '" + targetNode + "'");
                try {
                    conHelper.exec(nodeReferenceDeleteSQL, getKey(targetNode));
                    return false;
                } catch (Exception ex) {
                    String msg = "failed to delete references to: " + targetNode;
                    log.error(msg, e);
                }
            } else {
                log.error("Error in reference to " + targetNode + ": " + e);
                return true;
            }
        }
        if (fix) {
            try {
                log.info(name + ": Fix references to: '" + targetNode + "'");
                new NodeReferences(targetNode);
                store(fixedRefs);
            } catch (ItemStateException ex) {
                String msg = "failed to fix references to: " + targetNode;
                log.error(msg, ex);
                return true;
            }
        }
        return false;
    }

    protected void destroyOrphansRecursive(NodePropBundle bundle) throws ItemStateException {
        Iterator iterator = bundle.getChildNodeEntries().iterator();
        while(iterator.hasNext()) {
            NodePropBundle.ChildNodeEntry entry = (NodePropBundle.ChildNodeEntry) iterator.next();
            NodePropBundle child = loadBundle(entry.getId());
            destroyOrphansRecursive(child); 
        }
        log.info(name + ": Removing orphan bundle '{}'", bundle.getId());
        bundle.markOld(); 
        destroyBundle(bundle);
        evictBundle(bundle.getId());
      }

    /**
     * Reads and parses a bundle from the BLOB in the given column of the
     * current row of the given result set. This is a helper method to
     * circumvent issues JCR-1039 and JCR-1474.
     *
     * @param id bundle identifier
     * @param rs result set
     * @param column BLOB column
     * @return parsed bundle
     * @throws SQLException if the bundle can not be read or parsed
     */
    private NodePropBundle readBundle(NodeId id, ResultSet rs, int column)
            throws SQLException {
        try {
            InputStream in;
            if (rs.getMetaData().getColumnType(column) == Types.BLOB) {
                in = rs.getBlob(column).getBinaryStream();
            } else {
                in = rs.getBinaryStream(column);
            }
            try {
                return binding.readBundle(in, id);
            } finally {
                in.close();
            }
        } catch (IOException e) {
            SQLException exception =
                new SQLException("Failed to parse bundle " + id);
            exception.initCause(e);
            throw exception;
        }
    }
}
