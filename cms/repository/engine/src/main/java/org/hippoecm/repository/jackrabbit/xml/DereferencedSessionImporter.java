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
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import javax.jcr.AccessDeniedException;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinition;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.xml.Importer;
import org.apache.jackrabbit.core.xml.NodeInfo;
import org.apache.jackrabbit.core.xml.SessionImporter;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.uuid.UUID;
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
    private final String importPath;

    /** this implementation requires a property that can be set on a parent node.  Because this
     * node isn't actually persisted, there will be no constraintviolation, but this property
     * may not clash with any property in the parent node. (FIXME)
     */
   final static String HIPPO_PATHREFERENCE = "hippo:pathreference";
   
   /** '*' is not valid in property name, but can of course be used in value */
   private final static char SEPARATOR = '*';

   /** indicate whether original reference property was a multi valued property */
   private final static String MULTI_VALUE = "m";
   
   /** indicate whether original reference property was a single valued property */
   private final static String SINGLE_VALUE = "s";
   
    /** Keep a list of nodeId's that need revisiting for dereferencing */
    private List<NodeId> derefNodes = new ArrayList<NodeId>();

    private Stack<NodeImpl> parents;


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

        parents = new Stack<NodeImpl>();
        parents.push(importTargetNode);
        importPath = importTargetNode.safeGetJCRPath();
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
            nodeInfo = resolveMergeConflict(parent,parent.getNode(nodeName), nodeInfo);
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
        // TODO: optimize, directly dereference when reference path is not part of import path
        Iterator iter = propInfos.iterator();
        while (iter.hasNext()) {
            PropInfo propInfo = (PropInfo) iter.next();
            propInfo.apply(node, session.getNamePathResolver(), derefNodes, importPath, referenceBehavior);
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
        
        for (NodeId nodeId : derefNodes) {
            try {
                NodeImpl node = session.getNodeById(nodeId);
                // checking again..
                if (!node.hasProperty(HIPPO_PATHREFERENCE)) { 
                    continue;
                }
                
                Value[] refVals = node.getProperty(HIPPO_PATHREFERENCE).getValues();
                for (Value refVal : refVals) {
                    // format ([MULTI_VALUE|SINGLE_VALUE]+REFERENCE_SEPARATOR+
                    // propname+REFERENCE_SEPARATOR+refpath)
                    String ref = refVal.getString();
                    boolean isMulti = false;
                    if (ref.startsWith(MULTI_VALUE)) {
                        isMulti = true;
                    } else if (ref.startsWith(MULTI_VALUE)) {
                        isMulti = false;
                    } else {
                        log.warn("Not dereferencing unknown format for property: " + HIPPO_PATHREFERENCE);
                        continue;
                    }
                    
                    String path = ref.substring(ref.lastIndexOf(SEPARATOR) + 1, ref.length());
                    String propName = ref.substring(ref.indexOf(SEPARATOR) + 1);
                    propName = propName.substring(0, propName.indexOf(SEPARATOR));
                    
                    if (path.startsWith("/")) {
                        path = path.substring(1);
                    }
                    NodeImpl referencedNode = findNode(path, session);
                    if (isMulti) {
                        if (node.hasProperty(propName)) {
                            
                        }
                    } else {
                        if (node.hasProperty(propName)) {
                            // resolve??
                        }
                        
                    }
                    // find nodes and create props..
                }
                
                node.getProperty(HIPPO_PATHREFERENCE).remove();
                
//              while (iter.hasNext()) {
//              Property prop = (Property) iter.next();
//              // being paranoid...
//              if (prop.getType() != PropertyType.REFERENCE) {
//                  continue;
//              }
//              if (prop.getDefinition().isMultiple()) {
            } catch (ItemNotFoundException infe) {
            }
            
        }
        derefNodes.clear();
    }
    
    static NodeImpl findNode(String path, SessionImpl sessionImpl) throws PathNotFoundException, RepositoryException{
        try {
            Path p = sessionImpl.getQPath(path).getNormalizedPath();
            if (!p.isAbsolute()) {
                throw new RepositoryException("not an absolute path: " + path);
            }
            return sessionImpl.getItemManager().getNode(p);
        } catch (NameException e) {
            String msg = path + ": invalid path";
            log.warn(msg);
            throw new RepositoryException(msg, e);
        } catch (NamespaceException e) {
            String msg = path + ": invalid path";
            log.warn(msg);
            throw new RepositoryException(msg, e);
        } catch (AccessDeniedException ade) {
            throw new PathNotFoundException(path);
        } 
    }
}
