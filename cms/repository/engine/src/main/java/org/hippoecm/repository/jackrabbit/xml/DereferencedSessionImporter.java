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
package org.hippoecm.repository.jackrabbit.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinition;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.xml.Importer;
import org.apache.jackrabbit.core.xml.NodeInfo;
import org.apache.jackrabbit.core.xml.PropInfo;
import org.apache.jackrabbit.core.xml.SessionImporter;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.uuid.UUID;
import org.apache.jackrabbit.value.ReferenceValue;
import org.hippoecm.repository.api.ImportMergeBehavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DereferencedSessionImporter implements Importer {

    private static Logger log = LoggerFactory.getLogger(SessionImporter.class);

    private final SessionImpl session;
    private final NodeImpl importTargetNode;
    private final int uuidBehavior;
    private final int referenceBehavior;
    private final int mergeBehavior;

    private Stack<NodeImpl> parents;

    /**
     * helper object that keeps track of remapped uuid's and imported reference
     * properties that might need correcting depending on the uuid mappings
     */
    private final ReferenceChangeTracker refTracker;

    /**
     * Creates a new <code>SessionImporter</code> instance.
     *
     * @param importTargetNode
     * @param session
     * @param uuidBehavior     any of the constants declared by
     *                         {@link ImportUUIDBehavior}
     */
    public DereferencedSessionImporter(NodeImpl importTargetNode,
                           SessionImpl session,
                           int uuidBehavior, int referenceBehavior, int mergeBehavior) {

        System.err.println("USING DereferencedSessionImporter");
        this.importTargetNode = importTargetNode;
        this.session = session;
        this.uuidBehavior = uuidBehavior;
        this.mergeBehavior = mergeBehavior;
        this.referenceBehavior = referenceBehavior;

        refTracker = new ReferenceChangeTracker();

        parents = new Stack<NodeImpl>();
        parents.push(importTargetNode);
    }

    protected NodeImpl createNode(NodeImpl parent,
                                  Name nodeName,
                                  Name nodeTypeName,
                                  Name[] mixinNames,
                                  NodeId id)
            throws RepositoryException {
        NodeImpl node;

        // add node
        UUID uuid = (id == null) ? null : id.getUUID();
        node = parent.addNode(nodeName, nodeTypeName, uuid);
        // add mixins
        if (mixinNames != null) {
            for (int i = 0; i < mixinNames.length; i++) {
                node.addMixin(mixinNames[i]);
            }
        }
        return node;
    }

    /**
     * Resolve uuid conflict
     * @param parent the parent of the conflicting node
     * @param conflicting the conflicting node
     * @param nodeInfo the info of the node to be added
     * @return NodeImpl of the created node
     * @throws RepositoryException
     */
    protected NodeImpl resolveUUIDConflict(NodeImpl parent,
                                           NodeImpl conflicting,
                                           NodeInfo nodeInfo)
            throws RepositoryException {
        NodeImpl node;
        if (uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW) {
            // create new with new uuid
            node = createNode(parent, nodeInfo.getName(),
                    nodeInfo.getNodeTypeName(), nodeInfo.getMixinNames(), null);
            // remember uuid mapping
            if (node.isNodeType(NameConstants.MIX_REFERENCEABLE)) {
                refTracker.mappedUUID(nodeInfo.getId().getUUID(), node.getNodeId().getUUID());
            }
        } else if (uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW) {
            String msg = "a node with uuid " + nodeInfo.getId() + " already exists!";
            log.debug(msg);
            throw new ItemExistsException(msg);
        } else if (uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING) {
            // make sure conflicting node is not importTargetNode or an ancestor thereof
            if (importTargetNode.getPath().startsWith(conflicting.getPath())) {
                String msg = "cannot remove ancestor node";
                log.debug(msg);
                throw new ConstraintViolationException (msg);
            }
            // remove conflicting
            conflicting.remove();
            // create new with given uuid
            node = createNode(parent, nodeInfo.getName(),
                    nodeInfo.getNodeTypeName(), nodeInfo.getMixinNames(),
                    nodeInfo.getId());
        } else if (uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING) {
            if (conflicting.getDepth() == 0) {
                String msg = "root node cannot be replaced";
                log.debug(msg);
                throw new RepositoryException(msg);
            }
            // 'replace' current parent with parent of conflicting
            parent = (NodeImpl) conflicting.getParent();

            // replace child node
            node = parent.replaceChildNode(nodeInfo.getId(), nodeInfo.getName(),
                    nodeInfo.getNodeTypeName(), nodeInfo.getMixinNames());
        } else {
            String msg = "unknown uuidBehavior: " + uuidBehavior;
            log.debug(msg);
            throw new RepositoryException(msg);
        }
        return node;
    }
    
    /**
     * Resolve merge conflict
     * @param parent the parent of the conflicting node
     * @param conflicting the conflicting node
     * @param nodeInfo the info of the node to be added
     * @return nodeInfo of the node to create or null if the node is to be skipped
     * @throws RepositoryException
     */
    protected NodeInfo resolveMergeConflict(NodeImpl parent, NodeImpl conflicting, NodeInfo nodeInfo)
            throws RepositoryException {

        NodeDefinition def = conflicting.getDefinition();
        Name ntName = nodeInfo.getNodeTypeName();
        
        if (def.isAutoCreated() && conflicting.isNodeType(ntName)) {
            // this node has already been auto-created, no need to create it
            log.debug("skipping autocreated node " + conflicting.safeGetJCRPath());
            return null;
        }
        if (mergeBehavior == ImportMergeBehavior.IMPORT_MERGE_SKIP) {
            String msg = "merge_drop node " + conflicting.safeGetJCRPath();
            log.debug(msg);
            return null;
        }
        if (mergeBehavior == ImportMergeBehavior.IMPORT_MERGE_THROW) {
            String msg = "a node already exists add " + conflicting.safeGetJCRPath() + "!";
            log.debug(msg);
            throw new ItemExistsException(msg);
        }
        if (mergeBehavior == ImportMergeBehavior.IMPORT_MERGE_OVERWRITE) {
            // check for potential conflicts
            if (def.isProtected() && conflicting.isNodeType(ntName)) {
                // skip protected node
                parents.push(null); // push null onto stack for skipped node
                log.warn("merge_overwrite, skipping protected node " + conflicting.safeGetJCRPath());
                return null;
            } else {
                conflicting.remove();
                return nodeInfo;
            }
        }
        if (mergeBehavior == ImportMergeBehavior.IMPORT_MERGE_ADD_OR_SKIP) {
            if (def.allowsSameNameSiblings()) {
                return nodeInfo;
            } else {
                String msg = "merge add, skipped node" + conflicting.safeGetJCRPath() + " a node alread !";
                log.debug(msg);
                return null;   
            }
        }
        if (mergeBehavior == ImportMergeBehavior.IMPORT_MERGE_ADD_OR_OVERWRITE) {
            if (def.allowsSameNameSiblings()) {
                return nodeInfo;
            } else {
                // check for potential conflicts
                if (def.isProtected() && conflicting.isNodeType(ntName)) {
                    // skip protected node
                    log.warn("merge_add_or_overwrite, skipping protected node " + conflicting.safeGetJCRPath());
                    return null;
                } else {
                    conflicting.remove();
                    return nodeInfo;
                }
            }
        }

        String msg = "unknown mergeBehavior: " + mergeBehavior;
        log.debug(msg);
        throw new RepositoryException(msg);
    }

    //-------------------------------------------------------------< Importer >
    /**
     * {@inheritDoc}
     */
    public void start() throws RepositoryException {
        // nop
    }

    /**
     * {@inheritDoc}
     */
    public void startNode(NodeInfo nodeInfo, List propInfos)
            throws RepositoryException {
        NodeImpl parent = (NodeImpl) parents.peek();

        // process node

        NodeImpl node = null;
        NodeId id = nodeInfo.getId();
        Name nodeName = nodeInfo.getName();
        Name ntName = nodeInfo.getNodeTypeName();
        Name[] mixins = nodeInfo.getMixinNames();

        if (parent == null) {
            // parent node was skipped, skip this child node too
            parents.push(null); // push null onto stack for skipped node
            log.debug("skipping node " + nodeName);
            return;
        }
        if (parent.hasNode(nodeName)) {
            // a node with that name already exists...
            NodeImpl existing = parent.getNode(nodeName);
            NodeDefinition def = existing.getDefinition();
            if (!def.allowsSameNameSiblings()) {
                // existing doesn't allow same-name siblings,
                // check for potential conflicts
                if (def.isProtected() && existing.isNodeType(ntName)) {
                    // skip protected node
                    parents.push(null); // push null onto stack for skipped node
                    log.debug("skipping protected node " + existing.safeGetJCRPath());
                    return;
                }
                if (def.isAutoCreated() && existing.isNodeType(ntName)) {
                    // this node has already been auto-created, no need to create it
                    node = existing;
                } else {
                    // edge case: colliding node does have same uuid
                    // (see http://issues.apache.org/jira/browse/JCR-1128)
                    if (! (existing.getId().equals(id)
                            && (uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING
                            || uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING))) {
                        throw new ItemExistsException(existing.safeGetJCRPath());
                    }
                    // fall through
                }
            }
        }

        if (node == null) {
            // create node
            if (id == null) {
                // no potential uuid conflict, always add new node
                node = createNode(parent, nodeName, ntName, mixins, null);
            } else {
                // potential uuid conflict
                NodeImpl conflicting;
                try {
                    conflicting = session.getNodeById(id);
                } catch (ItemNotFoundException infe) {
                    conflicting = null;
                }
                if (conflicting != null) {
                    // resolve uuid conflict
                    node = resolveUUIDConflict(parent, conflicting, nodeInfo);
                } else {
                    // create new with given uuid
                    node = createNode(parent, nodeName, ntName, mixins, id);
                }
            }
        }

        // process properties

        Iterator iter = propInfos.iterator();
        while (iter.hasNext()) {
            PropInfo pi = (PropInfo) iter.next();
            pi.apply(node, session.getNamePathResolver(), refTracker);
        }

        parents.push(node);
    }

    /**
     * {@inheritDoc}
     */
    public void endNode(NodeInfo nodeInfo) throws RepositoryException {
        parents.pop();
    }

    /**
     * {@inheritDoc}
     */
    public void end() throws RepositoryException {
        /**
         * adjust references that refer to uuid's which have been mapped to
         * newly generated uuid's on import
         */
        Iterator<NodeImpl> iter = refTracker.getProcessedReferences();
        while (iter.hasNext()) {
            Property prop = (Property) iter.next();
            // being paranoid...
            if (prop.getType() != PropertyType.REFERENCE) {
                continue;
            }
            if (prop.getDefinition().isMultiple()) {
                Value[] values = prop.getValues();
                Value[] newVals = new Value[values.length];
                for (int i = 0; i < values.length; i++) {
                    Value val = values[i];
                    UUID original = UUID.fromString(val.getString());
                    UUID adjusted = refTracker.getMappedUUID(original);
                    if (adjusted != null) {
                        newVals[i] = new ReferenceValue(session.getNodeByUUID(adjusted));
                    } else {
                        // reference doesn't need adjusting, just copy old value
                        newVals[i] = val;
                    }
                }
                prop.setValue(newVals);
            } else {
                Value val = prop.getValue();
                UUID original = UUID.fromString(val.getString());
                UUID adjusted = refTracker.getMappedUUID(original);
                if (adjusted != null) {
                    prop.setValue(session.getNodeByUUID(adjusted));
                }
            }
        }
        refTracker.clear();
    }

    /**
     * Simple helper class that can be used to keep track of nodes with
     * dereferenced references
     */
    public class ReferenceChangeTracker extends org.apache.jackrabbit.core.util.ReferenceChangeTracker{
        /**
         * mapping <original uuid> to <new uuid> of mix:referenceable nodes
         */
        private final HashMap uuidMap = new HashMap();
        /**
         * list of processed reference properties that might need correcting
         */
        private final ArrayList references = new ArrayList();

        /**
         * Creates a new instance.
         */
        public ReferenceChangeTracker() {
        }

        /**
         * Resets all internal state.
         */
        public void clear() {
            uuidMap.clear();
            references.clear();
        }

        /**
         * Store the given uuid mapping for later lookup using
         * <code>{@link #getMappedUUID(UUID)}</code>.
         *
         * @param oldUUID old uuid
         * @param newUUID new uuid
         */
        public void mappedUUID(UUID oldUUID, UUID newUUID) {
            uuidMap.put(oldUUID, newUUID);
        }

        /**
         * Store the given reference property for later retrieval using
         * <code>{@link #getProcessedReferences()}</code>.
         *
         * @param refProp reference property
         */
        public void processedReference(Object refProp) {
            references.add(refProp);
        }

        /**
         * Returns the new UUID to which <code>oldUUID</code> has been mapped
         * or <code>null</code> if no such mapping exists.
         *
         * @param oldUUID old uuid
         * @return mapped new uuid or <code>null</code> if no such mapping exists
         * @see #mappedUUID(UUID, UUID)
         */
        public UUID getMappedUUID(UUID oldUUID) {
            return (UUID) uuidMap.get(oldUUID);
        }

        /**
         * Returns an iterator over all processed reference properties.
         *
         * @return an iterator over all processed reference properties
         * @see #processedReference(Object)
         */
        public Iterator getProcessedReferences() {
            return references.iterator();
        }
    }
}
