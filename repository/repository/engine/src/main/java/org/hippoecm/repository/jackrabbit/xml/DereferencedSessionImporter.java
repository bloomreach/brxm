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
import java.util.Map;
import java.util.Stack;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
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
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.uuid.UUID;
import org.hippoecm.repository.api.ImportMergeBehavior;
import org.hippoecm.repository.api.ImportReferenceBehavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DereferencedSessionImporter implements Importer {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static Logger log = LoggerFactory.getLogger(DereferencedSessionImporter.class);

    private final SessionImpl session;
    private final NodeImpl importTargetNode;
    private final int uuidBehavior;
    private final int referenceBehavior;
    private final int mergeBehavior;
    private final String importPath;
    private NamePathResolver resolver;

    private boolean isRootReferenceable;

    private long startTime;

    /** Keep a list of nodeId's that need revisiting for dereferencing */
    private final Map<NodeId, List<Reference>> derefNodes = new HashMap<NodeId, List<Reference>>();

    private final Stack<NodeImpl> parents;

    /**
     * Creates a new <code>SessionImporter</code> instance.
     *
     * @param importTargetNode
     * @param session
     * @param uuidBehavior     any of the constants declared by
     *                         {@link ImportUUIDBehavior}
     */
    public DereferencedSessionImporter(NodeImpl importTargetNode, SessionImpl session, int uuidBehavior,
            int referenceBehavior, int mergeBehavior) {

        this.importTargetNode = importTargetNode;
        this.session = session;
        this.uuidBehavior = uuidBehavior;
        this.mergeBehavior = mergeBehavior;
        this.referenceBehavior = referenceBehavior;
        this.resolver = new DefaultNamePathResolver(session, true);

        isRootReferenceable = false;
        try {
            isRootReferenceable = ((NodeImpl)session.getRootNode()).isNodeType(NameConstants.MIX_REFERENCEABLE);
        } catch (RepositoryException e) {
            // guess not..
        }

        parents = new Stack<NodeImpl>();
        parents.push(importTargetNode);
        importPath = importTargetNode.safeGetJCRPath();

        if (log.isDebugEnabled())
            log.debug("Importing to: " + importPath + " u:" + uuidBehavior + " r:" + referenceBehavior + " m:"
                    + mergeBehavior);
    }

    protected NodeImpl createNode(NodeImpl parent, Name nodeName, Name nodeTypeName, Name[] mixinNames, NodeId id)
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
    protected NodeImpl resolveUUIDConflict(NodeImpl parent, NodeImpl conflicting, NodeInfo nodeInfo)
            throws RepositoryException {
        NodeImpl node;
        if (uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW) {
            // create new with new uuid
            node = createNode(parent, nodeInfo.getName(), nodeInfo.getNodeTypeName(), nodeInfo.getMixinNames(), null);
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
            node = createNode(parent, nodeInfo.getName(), nodeInfo.getNodeTypeName(), nodeInfo.getMixinNames(),
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
            log.debug("Overwriting autocreated node " + conflicting.safeGetJCRPath());
            conflicting.remove();
            return nodeInfo;
        }
        if (mergeBehavior == ImportMergeBehavior.IMPORT_MERGE_SKIP) {
            String msg = "Import merge skip node " + conflicting.safeGetJCRPath();
            log.debug(msg);
            return null;
        }
        if (mergeBehavior == ImportMergeBehavior.IMPORT_MERGE_THROW) {
            String msg = "A node already exists add " + conflicting.safeGetJCRPath() + "!";
            log.debug(msg);
            importTargetNode.refresh(false);
            throw new ItemExistsException(msg);
        }
        if (mergeBehavior == ImportMergeBehavior.IMPORT_MERGE_OVERWRITE) {
            // check for potential conflicts
            if (def.isProtected() && conflicting.isNodeType(ntName)) {
                // skip protected node
                parents.push(null); // push null onto stack for skipped node
                log.warn("Import merge overwrite, skipping protected node " + conflicting.safeGetJCRPath());
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
                String msg = "Import merge add or skip, skipped node " + conflicting.safeGetJCRPath() + " a node alread !";
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
                    log.warn("Imoprt merge add or overwrite, skipping protected node " + conflicting.safeGetJCRPath());
                    return null;
                } else {
                    conflicting.remove();
                    return nodeInfo;
                }
            }
        }

        String msg = "unknown mergeBehavior: " + mergeBehavior;
        log.warn(msg);
        throw new RepositoryException(msg);
    }

    /**
     * resolveReferenceConflict
     */
    public String resolveReferenceConflict(NodeImpl node, String name, String path) throws RepositoryException {
        StringBuffer buf = new StringBuffer();
        buf.append("Reference not found for property ");
        buf.append('\'').append(node.safeGetJCRPath()).append('/').append(name).append('\'');
        buf.append(" : ");
        if (referenceBehavior == ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE) {
            buf.append("skipping.");
            log.warn(buf.toString());
            return null;
        }
        if (referenceBehavior == ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_THROW) {
            buf.append("throw error.");
            log.warn(buf.toString());
            importTargetNode.refresh(false);
            throw new RepositoryException(buf.toString());
        }
        if (referenceBehavior == ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_TO_ROOT) {
            if (isRootReferenceable) {
                buf.append("trying to set reference to root node.");
                log.warn(buf.toString());
                return session.getRootNode().getUUID();
            } else {
                buf.append("root not referenceable.");
                log.warn(buf.toString());
                importTargetNode.refresh(false);
                throw new RepositoryException(buf.toString());
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
        startTime = System.currentTimeMillis();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void startNode(NodeInfo nodeInfo, List propInfos) throws RepositoryException {
        NodeImpl parent = parents.peek();

        // process node
        NodeImpl node = null;
        NodeId id = nodeInfo.getId();
        Name nodeName = nodeInfo.getName();
        Name ntName = nodeInfo.getNodeTypeName();
        Name[] mixins = nodeInfo.getMixinNames();

        if (parent == null) {
            // parent node was skipped, skip this child node too
            parents.push(null); // push null onto stack for skipped node
            if (log.isDebugEnabled())
                log.debug("skipping node " + nodeName);
            return;
        }
        if (parent.hasNode(nodeName)) {
            if (importPath.equals(parent.safeGetJCRPath())) {
                // this is the root target node, decided by the user self
                // only throw an error on the most strict import
                if (mergeBehavior == ImportMergeBehavior.IMPORT_MERGE_SKIP) {
                    String msg = "A node already exists add " + parent.safeGetJCRPath() + "!";
                    log.warn(msg);
                    importTargetNode.refresh(false);
                    throw new ItemExistsException(msg);
                } else {
                    String msg = "Import base node already exists. skipping: " + parent.safeGetJCRPath();
                    log.debug(msg);
                    parents.push(parent.getNode(nodeName));
                    return;
                }
            }
            nodeInfo = resolveMergeConflict(parent, parent.getNode(nodeName), nodeInfo);
            if (nodeInfo == null) {
                parents.push(null); // push null onto stack for skipped node
                return;
            }
        }

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

        // process properties
        Iterator iter = propInfos.iterator();
        while (iter.hasNext()) {
            PropInfo propInfo = (PropInfo) iter.next();
            propInfo.apply(node, resolver, derefNodes, importPath, referenceBehavior);
        }

        parents.push(node);
        if (log.isDebugEnabled()) {
            log.debug("startNode: " + parents.peek().safeGetJCRPath());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void endNode(NodeInfo nodeInfo) throws RepositoryException {
        if (parents.peek() != null)
            log.debug("endNode: " + parents.peek().safeGetJCRPath());
        parents.pop();
    }

    /**
     * {@inheritDoc}
     */
    public void end() throws RepositoryException {
        // loop over all nodeIds with references
        for (Iterator<Map.Entry<NodeId, List<Reference>>> it = derefNodes.entrySet().iterator(); it.hasNext();) {
            Map.Entry<NodeId, List<Reference>> nodeRef = it.next();
            NodeImpl node = session.getNodeById(nodeRef.getKey());

            // loop over all the references for this node
            List<Reference> references = nodeRef.getValue();
            for(Reference ref : references) {
                ref.setBasePath(importPath);
                ref.resolveUUIDs(session);

                // set the references
                String[] uuids = ref.getUUIDs();
                String[] paths = ref.getPaths();
                List<Value> vals = new ArrayList<Value>(paths.length);
                for (int i = 0; i < uuids.length; i++) {
                    if (uuids[i] == null) {
                        String uuid = resolveReferenceConflict(node, ref.getPropertyName(), paths[i]);
                        if (uuid == null) {
                            //skip
                        } else {
                            vals.add(session.getValueFactory().createValue(uuid, PropertyType.REFERENCE));
                        }
                    } else {
                        vals.add(session.getValueFactory().createValue(uuids[i], PropertyType.REFERENCE));
                    }
                }

                // set property
                if (ref.isMulti()) {
                    node.setProperty(ref.getName(), vals.toArray(new Value[vals.size()]));
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
}
