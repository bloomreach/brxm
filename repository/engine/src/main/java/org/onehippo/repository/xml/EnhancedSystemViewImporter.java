/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.xml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinition;

import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.xml.Importer;
import org.apache.jackrabbit.core.xml.NodeInfo;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.hippoecm.repository.api.ImportReferenceBehavior;
import org.hippoecm.repository.jackrabbit.InternalHippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.repository.util.JcrConstants.MIX_REFERENCEABLE;

public class EnhancedSystemViewImporter implements Importer {


    private static Logger log = LoggerFactory.getLogger(EnhancedSystemViewImporter.class);

    private final InternalHippoSession session;
    private final NodeImpl importTargetNode;
    private final int uuidBehavior;
    private final int referenceBehavior;
    private final String importPath;
    private final NamePathResolver resolver;
    private final ImportContext importContext;
    private boolean isRootReferenceable;
    private long startTime;

    /** Keep a list of nodeId's that need revisiting for dereferencing */
    private final Map<NodeId, List<Reference>> derefNodes = new HashMap<NodeId, List<Reference>>();

    private final Stack<NodeImpl> parents;

    public EnhancedSystemViewImporter(NodeImpl importTargetNode, ImportContext importContext, InternalHippoSession session) {
        this.importTargetNode = importTargetNode;
        this.importContext = importContext;
        this.session = session;

        this.uuidBehavior = importContext.getUuidBehaviour();
        this.referenceBehavior = importContext.getReferenceBehaviour();
        this.resolver = session;

        isRootReferenceable = false;
        try {
            isRootReferenceable = session.getRootNode().isNodeType(MIX_REFERENCEABLE);
        } catch (RepositoryException e) {
            // guess not..
        }

        parents = new Stack<>();
        parents.push(importTargetNode);
        importPath = importTargetNode.safeGetJCRPath();

    }

    protected NodeImpl mergeOrCreateNode(NodeImpl parent, Name nodeName, Name nodeTypeName, Name[] mixinNames, NodeId id, EnhancedNodeInfo nodeInfo)
            throws RepositoryException {
        NodeImpl node;
        if (nodeInfo.mergeCombine()) {
            node = nodeInfo.getOrigin();
            importContext.addContextPath(node.safeGetJCRPath());
        } else if (nodeInfo.mergeOverlay()) {
            node = nodeInfo.getOrigin();
            importContext.addContextPath(node.safeGetJCRPath());
            final Collection<Name> oldMixins = node.getMixinTypeNames();
            final Collection<Name> newMixins = mixinNames != null ?
                    new ArrayList<>(Arrays.asList(mixinNames)) : Collections.<Name>emptyList();
            removeMixins(node, oldMixins, newMixins);
            final Name oldPrimaryType = node.getPrimaryNodeTypeName();
            if (nodeTypeName != null && !nodeTypeName.equals(oldPrimaryType)) {
                node.setPrimaryType(nodeTypeName.toString());
            }
        } else {
            node = parent.addNode(nodeName, nodeTypeName, id);
            if (importPath.equals(parent.safeGetJCRPath())) {
                importContext.addContextPath(node.safeGetJCRPath());
            }
        }

        if (mixinNames != null) {
            for (final Name mixinName : mixinNames) {
                node.addMixin(mixinName);
            }
        }
        if (importPath.equals(parent.safeGetJCRPath())) {
            importContext.setBaseNode(node);
        }
        return node;
    }

    private boolean removeMixins(final NodeImpl node, final Collection<Name> oldMixins, final Collection<Name> newMixins) throws RepositoryException {
        boolean mixinsChanged = false;
        for (Name oldMixin : oldMixins) {
            if (newMixins.contains(oldMixin)) {
                newMixins.remove(oldMixin);
            } else {
                mixinsChanged = true;
                node.removeMixin(oldMixin);
            }
        }
        return mixinsChanged && newMixins.isEmpty();
    }

    /**
     * Resolve uuid conflict
     * @param parent the parent of the conflicting node
     * @param conflicting the conflicting node
     * @param nodeInfo the info of the node to be added
     * @return NodeImpl of the created node
     * @throws RepositoryException
     */
    protected NodeImpl resolveUUIDConflict(NodeImpl parent, NodeImpl conflicting, EnhancedNodeInfo nodeInfo)
            throws RepositoryException {
        NodeImpl node;
        if (uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW) {
            // create new with new uuid
            node = mergeOrCreateNode(parent, nodeInfo.getName(), nodeInfo.getNodeTypeName(), nodeInfo.getMixinNames(), null, nodeInfo);
        } else if (uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW) {
            String msg = "a node with uuid " + nodeInfo.getId() + " already exists!";
            log.debug(msg);
            throw new ItemExistsException(msg);
        } else if (uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING) {
            // make sure conflicting node is not importTargetNode or an ancestor thereof
            if (importTargetNode.getPath().startsWith(conflicting.getPath())) {
                String msg = "cannot remove ancestor node";
                log.debug(msg);
                throw new ConstraintViolationException(msg);
            }
            // remove conflicting
            conflicting.remove();
            // create new with given uuid
            node = mergeOrCreateNode(parent, nodeInfo.getName(), nodeInfo.getNodeTypeName(), nodeInfo.getMixinNames(),
                    nodeInfo.getId(), nodeInfo);
        } else if (uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING) {
            if (conflicting.getDepth() == 0) {
                String msg = "root node cannot be replaced";
                log.debug(msg);
                throw new RepositoryException(msg);
            }
            // 'replace' current parent with parent of conflicting
            parent = (NodeImpl) conflicting.getParent();

            // replace child node
            node = parent.replaceChildNode(nodeInfo.getId(), nodeInfo.getName(), nodeInfo.getNodeTypeName(), nodeInfo
                    .getMixinNames());
        } else {
            String msg = "unknown uuidBehavior: " + uuidBehavior;
            log.debug(msg);
            throw new RepositoryException(msg);
        }
        return node;
    }

    /**
     * Resolve merge conflict
     * @return nodeInfo of the node to create or null if the node is to be skipped
     */
    protected EnhancedNodeInfo resolveMergeConflict(NodeImpl conflicting, EnhancedNodeInfo nodeInfo) throws RepositoryException {

        NodeDefinition def = conflicting.getDefinition();
        Name ntName = nodeInfo.getNodeTypeName();

        if (nodeInfo.mergeCombine() || nodeInfo.mergeOverlay()) {
            nodeInfo.setOrigin(conflicting);
            return nodeInfo;
        }
        if (def.isAutoCreated() && ntName != null && conflicting.isNodeType(ntName)) {
            log.debug("Overwriting autocreated node {}", conflicting.safeGetJCRPath());
            conflicting.remove();
            return nodeInfo;
        }
        if (nodeInfo.mergeSkip()) {
            log.debug("Skipping {} ", conflicting.safeGetJCRPath());
            return null;
        }

        if (def.allowsSameNameSiblings()) {
            return nodeInfo;
        } else {
            importTargetNode.refresh(false);
            String errorMessage = "A node already exists at "
                    + conflicting.safeGetJCRPath()
                    + " and same-name siblings are not allowed."
                    + " Conflicting node name is: " + nodeInfo.getName();
            throw new ItemExistsException(errorMessage);
        }
    }

    /**
     * resolveReferenceConflict
     */
    public String resolveReferenceConflict(NodeImpl node, String name, String path) throws RepositoryException {
        StringBuilder sb = new StringBuilder();
        sb.append("Reference not found for property ");
        sb.append('\'').append(node.safeGetJCRPath()).append('/').append(name).append('\'');
        sb.append(" : ");
        if (referenceBehavior == ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE) {
            sb.append("skipping.");
            log.warn(sb.toString());
            return null;
        }
        if (referenceBehavior == ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_THROW) {
            sb.append("throw error.");
            log.warn(sb.toString());
            importTargetNode.refresh(false);
            throw new RepositoryException(sb.toString());
        }
        if (referenceBehavior == ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_TO_ROOT) {
            if (isRootReferenceable) {
                sb.append("trying to set reference to root node.");
                log.warn(sb.toString());
                return session.getRootNode().getIdentifier();
            } else {
                sb.append("root not referenceable.");
                log.warn(sb.toString());
                importTargetNode.refresh(false);
                throw new RepositoryException(sb.toString());
            }
        }

        String msg = "unknown reference behaviour: " + referenceBehavior;
        log.debug(msg);
        throw new RepositoryException(msg);
    }

    public void start() throws RepositoryException {
        startTime = System.currentTimeMillis();
    }

    public void startNode(NodeInfo info, List propInfos) throws RepositoryException {
        EnhancedNodeInfo nodeInfo = (EnhancedNodeInfo) info;
        NodeImpl parent = parents.peek();

        // process node
        NodeImpl node;
        NodeId id = nodeInfo.getId();
        Name nodeName = nodeInfo.getName();
        int index = nodeInfo.getIndex();
        Name ntName = nodeInfo.getNodeTypeName();
        Name[] mixins = nodeInfo.getMixinNames();

        if (parent == null) {
            // parent node was skipped, skip this child node too
            parents.push(null);
            return;
        }
        final NodeImpl existing = getExistingNode(parent, nodeInfo);
        if (existing != null) {
            nodeInfo = resolveMergeConflict(existing, nodeInfo);
            if (nodeInfo == null) {
                parents.push(null);
                return;
            }
        } else {
            if (nodeInfo.mergeCombine() || nodeInfo.mergeOverlay()) {
                final String msg = "No such node to merge with: " + parent.safeGetJCRPath() + "/" + nodeName;
                throw new RepositoryException(msg);
            }
        }

        if (id == null || !hasConflictingIdentifier(id)) {
            node = mergeOrCreateNode(parent, nodeName, ntName, mixins, id, nodeInfo);
        } else {
            node = resolveUUIDConflict(parent, session.getNodeById(id), nodeInfo);
        }

        String insertBeforeLocation = nodeInfo.mergeInsertBefore();
        if (insertBeforeLocation != null) {
            String relPath = node.getName();
            if (node.getIndex() > 1) {
                relPath += "[" + node.getIndex() + "]";
            }
            if (insertBeforeLocation.equals("")) {
                NodeIterator iter = parent.getNodes();
                if (iter.hasNext()) {
                    Node firstChild = iter.nextNode();
                    if (iter.hasNext()) {
                        // Note that if there is just one node, it isn't the node before which to insert, but actually the
                        // node we just created.  In this case the created node is already the first and there is nothing to do.
                        parent.orderBefore(relPath, firstChild.getName());
                    }
                }
            } else {
                if (parent.hasNode(insertBeforeLocation)) {
                    parent.orderBefore(relPath, insertBeforeLocation);
                } else {
                    throw new ItemNotFoundException(parent.getPath() + "/" + insertBeforeLocation);
                }
            }
        }

        // process properties
        for (final Object propInfo : propInfos) {
            ((EnhancedPropInfo) propInfo).apply(node, resolver, derefNodes);
        }

        parents.push(node);
    }

    private NodeImpl getExistingNode(final NodeImpl parent, final EnhancedNodeInfo nodeInfo) throws RepositoryException {
        try {
            if (nodeInfo.getIndex() == -1) {
                return parent.getNode(nodeInfo.getName());
            } else {
                return parent.getNode(nodeInfo.getName(), nodeInfo.getIndex());
            }
        } catch (ItemNotFoundException e) {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void endNode(org.apache.jackrabbit.core.xml.NodeInfo nodeInfo) throws RepositoryException {
        parents.pop();
    }

    /**
     * {@inheritDoc}
     */
    public void end() throws RepositoryException {
        // loop over all nodeIds with references
        for (Map.Entry<NodeId, List<Reference>> nodeRef : derefNodes.entrySet()) {
            NodeImpl node = session.getNodeById(nodeRef.getKey());

            // loop over all the references for this node
            List<Reference> references = nodeRef.getValue();
            for (Reference ref : references) {
                ref.setBasePath(importPath);
                ref.resolveUUIDs(session);

                // set the references
                String[] uuids = ref.getUUIDs();
                String[] paths = ref.getPaths();
                List<Value> vals = new ArrayList<Value>(paths.length);
                for (int i = 0; i < uuids.length; i++) {
                    if (uuids[i] == null) {
                        String uuid = resolveReferenceConflict(node, ref.getPropertyName(), paths[i]);
                        if (uuid != null) {
                            vals.add(session.getValueFactory().createValue(uuid, PropertyType.REFERENCE));
                        }
                    } else {
                        vals.add(session.getValueFactory().createValue(uuids[i], PropertyType.REFERENCE));
                    }
                }

                // set property
                if (ref.isMulti()) {
                    node.setProperty(ref.getName(), vals.toArray(new Value[vals.size()]), PropertyType.REFERENCE);
                } else {
                    if (vals.size() > 0) {
                        node.setProperty(ref.getName(), vals.get(0));
                    }
                }
            }
        }

        // done, cleanup
        derefNodes.clear();
        if (log.isDebugEnabled()) {
            log.debug("end(), import ran for " + (System.currentTimeMillis() - startTime) + " ms.");
        }
    }

    private boolean hasConflictingIdentifier(NodeId nodeId) throws RepositoryException {
        try {
            return session.getNodeById(nodeId) != null;
        } catch (ItemNotFoundException infe) {
            return false;
        }

    }
}
