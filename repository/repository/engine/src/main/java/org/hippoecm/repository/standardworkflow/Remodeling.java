/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.standardworkflow;

import java.io.InputStream;
import java.io.InputStreamReader;
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
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.compact.CompactNodeTypeDefReader;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.value.ReferenceValue;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.jackrabbit.HippoNamespaceRegistry;
import org.hippoecm.repository.standardworkflow.RemodelWorkflow.FieldIdentifier;
import org.hippoecm.repository.standardworkflow.RemodelWorkflow.TypeUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Remodeling {
    protected final static Logger log = LoggerFactory.getLogger(Remodeling.class);

    /** The prefix of the namespace which has been changed
     */
    private String prefix;

    /** The prefix of the previous version of the namespace
     */
    private String oldPrefix;

    /** lookup map for node types
     */
    private Map<NodeType, NodeType> conversion;

    /** field renames
     */
    private Map<String, TypeUpdate> updates;

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

    Remodeling(Session session, String prefix, String oldUri, Map<String, TypeUpdate> updates)
            throws RepositoryException {
        this.session = session;
        this.prefix = prefix;
        this.updates = updates;

        conversion = new HashMap<NodeType, NodeType>();

        Workspace workspace = session.getWorkspace();
        nsRegistry = workspace.getNamespaceRegistry();
        oldPrefix = nsRegistry.getPrefix(oldUri);

        NodeTypeManagerImpl ntmgr = (NodeTypeManagerImpl) workspace.getNodeTypeManager();
        NodeTypeRegistry ntreg = ntmgr.getNodeTypeRegistry();
        for (Name nodeTypeName : ntreg.getRegisteredNodeTypes()) {
            if (nodeTypeName.getNamespaceURI().equals(oldUri)) {
                String oldName = oldPrefix + ":" + nodeTypeName.getLocalName();
                String newName = prefix + ":" + nodeTypeName.getLocalName();
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
        if (name.startsWith(oldPrefix + ":")) {
            return prefix + name.substring(oldPrefix.length());
        } else {
            return name;
        }
    }

    private String getOldName(String name) {
        if (name.startsWith(prefix)) {
            return oldPrefix + name.substring(prefix.length());
        } else {
            return name;
        }
    }

    private void copyProperty(Node target, Property prop, String name, List<PropertyDefinition> targets)
            throws ValueFormatException, VersionException, LockException, ConstraintViolationException,
            RepositoryException {
        PropertyDefinition definition = prop.getDefinition();
        int propType = definition.getRequiredType();
        boolean found = false;
        for (PropertyDefinition targetDefinition : targets) {
            String targetName = targetDefinition.getName();

            if (targetDefinition.getDeclaringNodeType().getName().equals(HippoNodeType.NT_UNSTRUCTURED)) {
                continue;
            }

            if ((targetName.equals(name) || targetName.equals("*")) && targetDefinition.getRequiredType() == propType) {
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
            }
        }
        if (!found) {
            log.warn("Dropping property " + prop.getName() + " as there is no new definition.");
        }
    }

    private void copyType(Node source, Node target, NodeType sourceType, List<PropertyDefinition> targets)
            throws RepositoryException {
        for (PropertyDefinition definition : sourceType.getPropertyDefinitions()) {
            if (!definition.isProtected()) {
                String name = getNewName(definition.getName());

                if (name.equals("*")) {
                    PropertyIterator properties = source.getProperties(name);
                    while (properties.hasNext()) {
                        Property property = properties.nextProperty();
                        if (property.getDefinition().equals(definition)) {
                            copyProperty(target, property, getNewName(property.getName()), targets);
                        }
                    }
                } else {
                    if (source.hasProperty(definition.getName())) {
                        Property property = source.getProperty(definition.getName());
                        TypeUpdate typeUpdate = updates.get(getNewName(sourceType.getName()));
                        if (typeUpdate != null) {
                            FieldIdentifier fieldId = new FieldIdentifier();
                            fieldId.path = name;
                            fieldId.type = PropertyType.nameFromValue(definition.getRequiredType());

                            FieldIdentifier newId = typeUpdate.getRenames().get(fieldId);
                            if (newId != null && !newId.path.equals("*")) {
                                name = newId.path;
                            }
                        }
                        copyProperty(target, property, name, targets);
                    }
                }
            }
        }

        for (NodeDefinition definition : sourceType.getChildNodeDefinitions()) {
            if (!definition.isProtected()) {
                NodeIterator nodes = source.getNodes(definition.getName());
                while (nodes.hasNext()) {
                    Node node = nodes.nextNode();
                    if (definition.getName().equals("*")) {
                        if (node.getDefinition().equals(definition)) {
                            NodeType newType = conversion.get(node.getPrimaryNodeType());
                            Node copy = target.addNode(getNewName(node.getName()), newType.getName());
                            traverse(node, true, copy);
                        }
                    } else {
                        String name = getNewName(definition.getName());
                        NodeType newType = conversion.get(node.getPrimaryNodeType());

                        TypeUpdate typeUpdate = updates.get(name);
                        if (typeUpdate != null) {
                            FieldIdentifier fieldId = new FieldIdentifier();
                            fieldId.path = name;
                            fieldId.type = newType.getName();

                            FieldIdentifier newId = typeUpdate.getRenames().get(fieldId);
                            if (newId != null && !newId.path.equals("*")) {
                                name = newId.path;
                            }
                        }

                        Node copy = target.addNode(name, newType.getName());
                        traverse(node, true, copy);
                    }
                }
            }
        }
    }

    private void copyPrototype(Node target, NodeType type) throws RepositoryException {
        TypeUpdate typeUpdate = updates.get(type.getName());
        if (typeUpdate != null && typeUpdate.prototype != null) {
            Node prototype = (Node) session.getItem(typeUpdate.prototype);

            // copy properties
            for (PropertyDefinition propDef : type.getPropertyDefinitions()) {
                if (!propDef.getName().equals("*")) {
                    if (propDef.isMandatory()) {
                        if (!target.hasProperty(propDef.getName())) {
                            Property property = prototype.getProperty(getOldName(propDef.getName()));
                            if (propDef.isMultiple()) {
                                target.setProperty(propDef.getName(), property.getValues());
                            } else {
                                target.setProperty(propDef.getName(), property.getValue());
                            }
                        }
                    }
                }
            }

            // copy nodes
            for (NodeDefinition nodeDef : type.getChildNodeDefinitions()) {
                if (!nodeDef.getName().equals("*")) {
                    if (nodeDef.isMandatory()) {
                        if (!target.getNodes(nodeDef.getName()).hasNext()) {
                            if (nodeDef.allowsSameNameSiblings()) {
                                NodeIterator siblings = prototype.getNodes(getOldName(nodeDef.getName()));
                                while (siblings.hasNext()) {
                                    Node node = siblings.nextNode();
                                    NodeType newType = conversion.get(node.getPrimaryNodeType());
                                    Node copy = target.addNode(nodeDef.getName(), newType.getName());
                                    traverse(node, true, copy);
                                }
                            } else {
                                Node node = prototype.getNode(getOldName(nodeDef.getName()));
                                log.info("pri type: " + node.getPrimaryNodeType().getName());
                                NodeType newType = conversion.get(node.getPrimaryNodeType());
                                if (newType != null) {
                                    Node copy = target.addNode(nodeDef.getName(), newType.getName());
                                    traverse(node, true, copy);
                                } else {
                                    log.warn("removing node " + node.getPath()
                                            + " as there is no new type defined for type "
                                            + node.getPrimaryNodeType().getName());
                                }
                            }
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

    private void visit(Node source, Node target) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {

        List<PropertyDefinition> targets = new LinkedList<PropertyDefinition>();

        // copy primary type
        NodeType targetType = target.getPrimaryNodeType();
        for (PropertyDefinition def : targetType.getPropertyDefinitions()) {
            targets.add(def);
        }
        NodeType[] sourceTypes = source.getMixinNodeTypes();
        for (int i = 0; i < sourceTypes.length; i++) {
            NodeType newType = conversion.get(sourceTypes[i]);
            if (newType != null) {
                for (PropertyDefinition def : newType.getPropertyDefinitions()) {
                    targets.add(def);
                }
            }
        }

        NodeType sourceType = source.getPrimaryNodeType();
        copyType(source, target, sourceType, targets);

        // copy mixin types
        sourceTypes = source.getMixinNodeTypes();
        for (int i = 0; i < sourceTypes.length; i++) {
            NodeType newType = conversion.get(sourceTypes[i]);
            if (newType != null) {
                target.addMixin(newType.getName());

                copyType(source, target, sourceTypes[i], targets);
            }
        }

        // copy mandatory properties and child nodes from prototype into target
        copyPrototype(target, targetType);

        NodeType[] targetTypes = target.getMixinNodeTypes();
        for (int i = 0; i < targetTypes.length; i++) {
            copyPrototype(target, targetTypes[i]);
        }

        // validate types
        if (!isType(target, targetType)) {
            throw new ConstraintViolationException("Unable to convert node " + source.getPath() + " to type "
                    + targetType.getName());
        }
        for (int i = 0; i < targetTypes.length; i++) {
            if (!isType(target, targetTypes[i])) {
                throw new ConstraintViolationException("Unable to convert node " + source.getPath() + " to type "
                        + targetTypes[i].getName());
            }
        }

        // update references
        if (source.isNodeType(JcrConstants.MIX_REFERENCEABLE)) {
            PropertyIterator propIter = source.getReferences();
            while (propIter.hasNext()) {
                Property property = propIter.nextProperty();
                if (!property.getDefinition().isProtected()) {
                    property.setValue(new ReferenceValue(target));
                } else {
                    log.warn("Unable to update protected reference");
                }
            }
        }
    }

    private void visitTemplateType(Node node) throws RepositoryException {
        Node draft = getVersion(node, HippoNodeType.HIPPO_NODETYPE, "draft");
        if (draft == null) {
            EditmodelWorkflowImpl.checkoutType(node);
            draft = getVersion(node, HippoNodeType.HIPPO_NODETYPE, "draft");
        }

        // visit node type
        Node current = getVersion(node, HippoNodeType.HIPPO_NODETYPE, "current");
        if (current != null) {
            if (!current.isNodeType(HippoNodeType.NT_REMODEL)) {
                current.addMixin(HippoNodeType.NT_REMODEL);
            }
            current.setProperty(HippoNodeType.HIPPO_REMODEL, "old");
            current.setProperty(HippoNodeType.HIPPO_URI, nsRegistry.getURI(oldPrefix));
        }

        draft = getVersion(node, HippoNodeType.HIPPO_NODETYPE, "draft");
        draft.removeMixin(HippoNodeType.NT_REMODEL);

        // visit prototype
        if (node.hasNode(HippoNodeType.HIPPO_PROTOTYPE)) {
            current = getVersion(node, HippoNodeType.HIPPO_PROTOTYPE, "current");
            if (current != null) {
                if (!current.isNodeType(HippoNodeType.NT_REMODEL)) {
                    current.addMixin(HippoNodeType.NT_REMODEL);
                }
                current.setProperty(HippoNodeType.HIPPO_REMODEL, "old");
                current.setProperty(HippoNodeType.HIPPO_URI, nsRegistry.getURI(oldPrefix));
            }

            draft = getVersion(node, HippoNodeType.HIPPO_PROTOTYPE, "draft");
            Node handle = node.getNode(HippoNodeType.HIPPO_PROTOTYPE);
            NodeTypeManager ntMgr = node.getSession().getWorkspace().getNodeTypeManager();
            NodeType newType = ntMgr.getNodeType(node.getName());
            if (newType != null) {
                Node newChild = handle.addNode(HippoNodeType.HIPPO_PROTOTYPE, newType.getName());
                traverse(draft, true, newChild);
                draft.remove();
                newChild.removeMixin(HippoNodeType.NT_REMODEL);
                newChild.removeMixin(HippoNodeType.NT_UNSTRUCTURED);

                // prepare workflow mixins
                if (newChild.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                    newChild.addMixin(HippoNodeType.NT_HARDDOCUMENT);
                } else if (newChild.isNodeType(HippoNodeType.NT_REQUEST)) {
                    newChild.addMixin(JcrConstants.MIX_REFERENCEABLE);
                }
            }
        }
    }

    private Node getVersion(Node node, String name, String version) throws RepositoryException {
        if (node.hasNode(name)) {
            Node template = node.getNode(name);
            NodeIterator iter = template.getNodes(name);
            while (iter.hasNext()) {
                Node versionNode = iter.nextNode();
                if (versionNode.isNodeType(HippoNodeType.NT_REMODEL)) {
                    if (version.equals(versionNode.getProperty(HippoNodeType.HIPPO_REMODEL).getString())) {
                        return versionNode;
                    }
                } else if (version.equals("current")) {
                    return versionNode;
                }
            }
        }
        return null;
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
            String name = node.getName();
            if (name.startsWith(prefix + ":")) {
                typeUpdates.add(node);
                return;
            }
        }

        if (copy) {
            // copy mixin types and properties
            visit(node, target);
        } else {
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
                        child.remove(); // iter.remove();
                        changes.add(newChild);
                    } else if (child.getName().startsWith(oldPrefix + ":")) {
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
    }

    protected void updateTypes() throws RepositoryException {
        for (Node node : typeUpdates) {
            visitTemplateType(node);
        }
    }

    public static Remodeling remodel(Session userSession, String prefix, InputStream cnd,
            Map<String, TypeUpdate> updates) throws NamespaceException, RepositoryException {
        Workspace workspace = userSession.getWorkspace();
        HippoNamespaceRegistry nsreg = (HippoNamespaceRegistry) workspace.getNamespaceRegistry();

        // obtain namespace URI for prefix as in use
        String oldNamespaceURI = nsreg.getURI(prefix);

        // compute namespace URI for new model to be used
        int pos = oldNamespaceURI.lastIndexOf("/");
        if (pos < 0)
            throw new RepositoryException("Internal error; invalid namespace URI found in repository itself");
        if (oldNamespaceURI.lastIndexOf(".") > pos)
            pos = oldNamespaceURI.lastIndexOf(".");
        int newNamespaceVersion = Integer.parseInt(oldNamespaceURI.substring(pos + 1));
        ++newNamespaceVersion;
        String newNamespaceURI = oldNamespaceURI.substring(0, pos + 1) + newNamespaceVersion;
        String newPrefix = prefix + "_" + newNamespaceURI.substring(newNamespaceURI.lastIndexOf('/') + 1).replace('.', '_');
        String oldPrefix = prefix + "_" + oldNamespaceURI.substring(oldNamespaceURI.lastIndexOf('/') + 1).replace('.', '_');

        // push new node type definition such that it will be loaded
        try {
            nsreg.open();
            nsreg.registerNamespace(newPrefix, newNamespaceURI);
            
            CompactNodeTypeDefReader cndReader = new CompactNodeTypeDefReader(new InputStreamReader(cnd), prefix);
            List ntdList = cndReader.getNodeTypeDefs();
            NodeTypeManagerImpl ntmgr = (NodeTypeManagerImpl) workspace.getNodeTypeManager();
            NodeTypeRegistry ntreg = ntmgr.getNodeTypeRegistry();

            for (Iterator iter = ntdList.iterator(); iter.hasNext();) {
                NodeTypeDef ntd = (NodeTypeDef) iter.next();

                /* EffectiveNodeType effnt = */ ntreg.registerNodeType(ntd);
            }

            
            Remodeling remodel = new Remodeling(userSession, newPrefix, oldNamespaceURI, updates);
            remodel.traverse(userSession.getRootNode(), false, userSession.getRootNode());
            remodel.updateTypes();
            nsreg.commit(newPrefix);

            nsreg.externalRemap(prefix, oldPrefix, oldNamespaceURI);
            nsreg.externalRemap(newPrefix, prefix, newNamespaceURI);

            nsreg.close();
            return remodel;
        } catch (Exception ex) {
            userSession.refresh(false);
            nsreg.unregisterNamespace(newPrefix);
            nsreg.close();
            ex.printStackTrace();
        }
        return null;
    }

    public static void convert(Node node, String prefix, Map<String, TypeUpdate> renames) throws NamespaceException,
            RepositoryException {
        Session session = node.getSession();
        Workspace workspace = session.getWorkspace();
        NamespaceRegistry nsreg = workspace.getNamespaceRegistry();

        // obtain namespace URI for prefix as in use
        String oldNamespaceURI = nsreg.getURI(prefix);

        // get new prefix
        if (prefix.indexOf('_') > 0) {
            prefix = prefix.substring(0, prefix.indexOf('_'));
        }

        try {
            Remodeling remodel = new Remodeling(session, prefix, oldNamespaceURI, renames);
            remodel.traverse(node.getParent(), false, node.getParent());
        } catch (RepositoryException ex) {
            session.refresh(false);
            throw ex;
        }
    }
}
