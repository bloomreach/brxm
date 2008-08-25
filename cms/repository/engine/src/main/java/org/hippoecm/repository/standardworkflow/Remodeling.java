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
package org.hippoecm.repository.standardworkflow;

import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.core.NamespaceRegistryImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.compact.CompactNodeTypeDefReader;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.jackrabbit.value.ReferenceValue;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Remodeling {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    protected final static Logger log = LoggerFactory.getLogger(Remodeling.class);

    /** The prefix of the namespace which has been changed
     */
    private String newPrefix;

    /** The prefix of the previous version of the namespace
     */
    private String prefix;

    /** lookup map for node types
     */
    private Map<NodeType, NodeType> conversion;

    /** namespace registry
     */
    private NamespaceRegistry nsRegistry;

    /** Paths to the changed nodes.
     */
    private Set<Node> changes;

    /** Types to be updated
     */
    private Set<Node> typeUpdates;

    /** Reference to the session in which the changes are prepared
     */
    transient Session session;

    Remodeling(Session session, String newPrefix, String oldUri)
            throws RepositoryException {
        this(session, newPrefix, oldUri, null, null);
    }

    Remodeling(Session session, String newPrefix, String oldUri, String contentUpdater, Object contentUpdaterCargo)
            throws RepositoryException {
        this.session = session;
        this.newPrefix = newPrefix;

        conversion = new HashMap<NodeType, NodeType>();

        Workspace workspace = session.getWorkspace();
        this.nsRegistry = workspace.getNamespaceRegistry();
        this.prefix = nsRegistry.getPrefix(oldUri);

        NodeTypeManagerImpl ntmgr = (NodeTypeManagerImpl) workspace.getNodeTypeManager();
        NodeTypeRegistry ntreg = ntmgr.getNodeTypeRegistry();
        for (Name nodeTypeName : ntreg.getRegisteredNodeTypes()) {
            if (nodeTypeName.getNamespaceURI().equals(oldUri)) {
                String oldName = prefix + ":" + nodeTypeName.getLocalName();
                String newName = newPrefix + ":" + nodeTypeName.getLocalName();
                try {
                    conversion.put(ntmgr.getNodeType(oldName), ntmgr.getNodeType(newName));
                } catch (NoSuchNodeTypeException ex) {
                    log.warn("Could not find new type for " + newName + "; deleting all nodes of this type");
                }
            } else {
                String name = nsRegistry.getPrefix(nodeTypeName.getNamespaceURI()) + ":" + nodeTypeName.getLocalName();
                NodeType nodeType = ntmgr.getNodeType(name);
                conversion.put(nodeType, nodeType);
            }
        }

        changes = new HashSet<Node>();
        typeUpdates = new HashSet<Node>();
    }

    public NodeIterator getNodes() {
        return new ChangedNodesIterator();
    }

    private class ChangedNodesIterator implements NodeIterator {
        Iterator<Node> iter;
        int index;

        ChangedNodesIterator() {
            iter = changes.iterator();
            index = 0;
        }

        public Node nextNode() {
            Node node = iter.next();
            ++index;
            return node;
        }

        public boolean hasNext() {
            return iter.hasNext();
        }

        public Object next() throws NoSuchElementException {
            Object object = iter.next();
            ++index;
            return object;
        }

        public void remove() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        public void skip(long skipNum) {
            while (skipNum-- > 0) {
                iter.next();
                ++index;
            }
        }

        public long getSize() {
            return changes.size();
        }

        public long getPosition() {
            return index;
        }
    }

    private String getNewName(String name) {
        if (name.startsWith(prefix + ":")) {
            return newPrefix + name.substring(prefix.length());
        } else {
            return name;
        }
    }

    private String getOldName(String name) {
        if (name.startsWith(newPrefix)) {
            return prefix + name.substring(newPrefix.length());
        } else {
            return name;
        }
    }

    private void copyProperty(Node target, Property prop, String name, List<PropertyDefinition> targets)
            throws ValueFormatException, VersionException, LockException, ConstraintViolationException,
            RepositoryException {
        PropertyDefinition definition = prop.getDefinition();
        int propType = prop.getType();

        boolean found = false;
        boolean anySingular = false;
        boolean anyMultiple = false;
        for (PropertyDefinition targetDefinition : targets) {
            String targetName = targetDefinition.getName();
            int targetType = targetDefinition.getRequiredType();

            if (targetName.equals("*")) {
                if (targetType == propType) {
                    if (targetDefinition.isMultiple()) {
                        anyMultiple = true;
                    } else {
                        anySingular = true;
                    }
                } else if (targetType == PropertyType.UNDEFINED) {
                    if (targetDefinition.isMultiple()) {
                        anyMultiple = true;
                    } else {
                        anySingular = true;
                    }
                }
                continue;
            }

            if (targetName.equals(name) && (targetType == PropertyType.UNDEFINED || targetType == propType)) {
                // copy property
                if (definition.isMultiple()) {
                    if (targetDefinition.isMultiple()) {
                        target.setProperty(name, prop.getValues());
                    } else {
                        Value[] values = prop.getValues();
                        if (values.length == 1) {
                            target.setProperty(name, values[0]);
                        } else if (values.length > 1) {
                            throw new ValueFormatException("Property " + prop.getPath()
                                    + " cannot be converted to a single value");
                        }
                    }
                } else {
                    if (targetDefinition.isMultiple()) {
                        target.setProperty(name, new Value[] { prop.getValue() });
                    } else {
                        target.setProperty(name, prop.getValue());
                    }
                }
                found = true;
                break;
            }
        }
        if (!found) {
            if (definition.isMultiple()) {
                Value[] values = prop.getValues();
                if (anyMultiple) {
                    target.setProperty(name, values);
                } else if (anySingular && !target.hasProperty(name)) {
                    if (values.length == 1) {
                        target.setProperty(name, values[0]);
                    } else if (values.length > 1) {
                        throw new ValueFormatException("Property " + prop.getPath()
                                + " cannot be converted to a single value");
                    }
                } else {
                    log.warn("Dropping property " + prop.getName() + " as there is no new definition.");
                }
            } else {
                Value value = prop.getValue();
                if (anySingular) {
                    target.setProperty(name, value);
                } else if (anyMultiple && !target.hasProperty(name)) {
                    Value[] values = new Value[1];
                    values[0] = value;
                    target.setProperty(name, values);
                } else {
                    log.warn("Dropping property " + prop.getName() + " as there is no new definition.");
                }
            }
        }
    }

    private void copyNode(Node target, Node child, String name, List<NodeDefinition> nodeDefs)
            throws ValueFormatException, VersionException, LockException, ConstraintViolationException,
            RepositoryException {

        for (NodeDefinition targetDefinition : nodeDefs) {
            String targetName = targetDefinition.getName();

            if (targetName.equals("*") || targetName.equals(name)) {
                NodeType[] targetTypes = targetDefinition.getRequiredPrimaryTypes();
                NodeType newType = conversion.get(child.getPrimaryNodeType());
                for (NodeType type : targetTypes) {
                    if (newType.isNodeType(type.getName())) {
                        if (targetDefinition.allowsSameNameSiblings() || !target.hasNode(name)) {
                            // copy node
                            Node copy = target.addNode(name, newType.getName());
                            traverse(child, true, copy);
                            return;
                        } else {
                            log.warn("Not copying node " + child.getPath()
                                    + " as the new type doesn't allow same name siblings");
                        }
                    }
                }
            }
        }
    }

    private boolean isType(Node target, NodeType targetType) throws RepositoryException {
        PropertyDefinition[] targets = targetType.getPropertyDefinitions();
        for (PropertyDefinition definition : targets) {
            if (!definition.isProtected()) {
                if (definition.isMandatory()) {
                    if (!definition.getName().equals("*")) {
                        if (!target.hasProperty(definition.getName())) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private void visitTemplateType(Node node) throws RepositoryException {
        Node draft = EditmodelWorkflowImpl.getDraftType(node);
        if (draft == null) {
            EditmodelWorkflowImpl.checkoutType(node);
            draft = EditmodelWorkflowImpl.getDraftType(node);
        }

        // visit node type
        draft.addMixin(HippoNodeType.NT_REMODEL);
        draft.setProperty(HippoNodeType.HIPPO_URI, nsRegistry.getURI(newPrefix));

        // visit prototype
        if (node.hasNode(HippoNodeType.HIPPO_PROTOTYPE)) {
            draft = EditmodelWorkflowImpl.getDraftPrototype(node);
            Node handle = node.getNode(HippoNodeType.HIPPO_PROTOTYPE);

            // remove old prototypes
            NodeIterator children = handle.getNodes(HippoNodeType.HIPPO_PROTOTYPE);
            while (children.hasNext()) {
                Node child = children.nextNode();
                if (!child.isNodeType(JcrConstants.NT_UNSTRUCTURED)) {
                    child.remove();
                }
            }

            // convert draft
            NodeTypeManager ntMgr = node.getSession().getWorkspace().getNodeTypeManager();
            NodeType newType = ntMgr.getNodeType(newPrefix + ":" + ISO9075.decode(node.getName()));
            if (newType != null) {
                Node newChild = handle.addNode(HippoNodeType.HIPPO_PROTOTYPE, newType.getName());

                // prepare workflow mixins
                if (newChild.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                    newChild.addMixin(HippoNodeType.NT_HARDDOCUMENT);
                    newChild.setProperty(HippoNodeType.HIPPO_PATHS, new Value[] {});
                } else if (newChild.isNodeType(HippoNodeType.NT_REQUEST)) {
                    newChild.addMixin(JcrConstants.MIX_REFERENCEABLE);
                }

                traverse(draft, true, newChild);
            }
        }
    }

    private boolean isVirtual(Node node) throws RepositoryException {
        Node canonical;
        try {
            canonical = ((HippoNode) node).getCanonicalNode();
        } catch (ItemNotFoundException e) {
            /*
             * TODO HREPTWO-547 : when a physical node is changed, the virtual equivalence of
             * the former state is still in the LISM. For this virtual node, the physical node
             * cannot be found, throwing a ItemNotFoundException
             */
            return true;
        }
        return (canonical == null || !(canonical.isSame(node)));
    }

    protected void traverse(Node node, boolean copy, Node target) throws RepositoryException {
        if (node.getPath().equals("/jcr:system")) {
            return;
        } else if (node.isNodeType(HippoNodeType.NT_TEMPLATETYPE)) {
            if (node.getParent().getName().equals(prefix)) {
                typeUpdates.add(node);
            }
            return;
        }

        boolean checkin = false;
        if (node.isNodeType(JcrConstants.MIX_VERSIONABLE)) {
            if (!node.isCheckedOut()) {
                checkin = true;
            }
        }

        if (copy) {
            // copy mixin types and properties
            // BERRY visit(node, target);
            node.remove();
        } else {
            if (checkin) {
                target.checkout();
            }
            LinkedList<Node> toRename = new LinkedList<Node>();
            for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                Node child = iter.nextNode();
                if (isVirtual(child)) {
                    continue;
                }

                NodeType oldType = child.getPrimaryNodeType();
                NodeType newType = conversion.get(oldType);
                if (newType != null) {
                    if (newType != oldType) {
                        Node newChild = target.addNode(getNewName(child.getName()), newType.getName());
                        traverse(child, true, newChild);
                        changes.add(newChild);
                    } else if (child.getName().startsWith(prefix + ":")) {
                        toRename.addLast(child);
                    } else {
                        traverse(child, false, child);
                    }
                } else {
                    log.warn("Deleting node " + child.getPath() + " as there is no new type to convert it to");
                    child.remove();
                }
            }
            for (Node child : toRename) {
                String newName = getNewName(child.getName());
                session.move(child.getPath(), node.getPath() + "/" + newName);
                int index = (int) node.getNodes(newName).getSize();
                child = node.getNode(newName + "[" + index + "]");
                traverse(child, false, child);
            }
        }

        if (checkin) {
            target.getSession().save();
            target.checkin();
        }
    }

    protected void updateTypes() throws RepositoryException {
        for (Node node : typeUpdates) {
            visitTemplateType(node);
        }
    }
   
        public static Remodeling remodel(Session session, String prefix, Reader cnd)
            throws NamespaceException, RepositoryException {
        return remodel(session, prefix, cnd, null, null);
        }
    public static Remodeling remodel(Session session, String prefix, Reader cnd, String contentUpdater, Object contentUpdaterCargo)
            throws NamespaceException, RepositoryException {
        Workspace workspace = session.getWorkspace();
        NamespaceRegistryImpl nsreg = (NamespaceRegistryImpl) workspace.getNamespaceRegistry();

        String oldNamespaceURI = nsreg.getURI(prefix);
        String oldPrefix = prefix + "_"
                + oldNamespaceURI.substring(oldNamespaceURI.lastIndexOf('/') + 1).replace('.', '_');

        // push new node type definition such that it will be loaded
        try {
            CompactNodeTypeDefReader cndReader = new CompactNodeTypeDefReader(cnd, "remodeling input stream");
            NamespaceMapping mapping = cndReader.getNamespaceMapping();

            String newNamespaceURI = mapping.getURI(prefix);
            String newPrefix = prefix + "_"
                    + newNamespaceURI.substring(newNamespaceURI.lastIndexOf('/') + 1).replace('.', '_');

            nsreg.registerNamespace(newPrefix, newNamespaceURI);

            List ntdList = cndReader.getNodeTypeDefs();
            NodeTypeManagerImpl ntmgr = (NodeTypeManagerImpl) workspace.getNodeTypeManager();
            NodeTypeRegistry ntreg = ntmgr.getNodeTypeRegistry();

            for (Iterator iter = ntdList.iterator(); iter.hasNext();) {
                NodeTypeDef ntd = (NodeTypeDef) iter.next();

                /* EffectiveNodeType effnt = */ntreg.registerNodeType(ntd);
            }

            Remodeling remodel = new Remodeling(session, newPrefix, oldNamespaceURI);
            remodel.traverse(session.getRootNode(), false, session.getRootNode());
            remodel.updateTypes();

            session.save();

            nsreg.externalRemap(prefix, oldPrefix, oldNamespaceURI);
            nsreg.externalRemap(newPrefix, prefix, newNamespaceURI);

            return remodel;
        } catch (Exception ex) {
            session.refresh(false);
            ex.printStackTrace();
        }
        return null;
    }
}
