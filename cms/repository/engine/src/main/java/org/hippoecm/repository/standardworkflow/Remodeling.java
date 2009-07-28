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
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import org.apache.jackrabbit.value.ReferenceValue;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Remodeling {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    protected final static Logger log = LoggerFactory.getLogger(Remodeling.class);

    private static class TypeUpdate implements Serializable {
        private static final long serialVersionUID = 1L;

        public String newName;

        public String prototype;

        public Map<FieldIdentifier, FieldIdentifier> renames;
    }

    private static class FieldIdentifier implements Serializable {
        private static final long serialVersionUID = 1L;

        public String path;

        public String type;

        @Override
        public boolean equals(Object object) {
            if (object != null) {
                if (object instanceof FieldIdentifier) {
                    FieldIdentifier id = (FieldIdentifier) object;
                    return id.path.equals(path) && id.type.equals(type);
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            return (path.hashCode() * type.hashCode()) % 1001;
        }
    }

    private static Map<String, TypeUpdate> convertCargo(Object cargo) {
        Map<String, TypeUpdate> updates = new HashMap<String, TypeUpdate>();
        for (Map.Entry<String, Object> entry : ((Map<String, Object>) cargo).entrySet()) {
            Map<String, Object> value = (Map<String, Object>) entry.getValue();
            TypeUpdate update = new TypeUpdate();
            update.newName = (String) value.get("newName");
            update.prototype = (String) value.get("prototype");
            update.renames = new HashMap<FieldIdentifier, FieldIdentifier>();

            Map<Map<String, String>, Map<String, String>> origRenames = (Map<Map<String, String>, Map<String, String>>) value
                    .get("renames");
            for (Map.Entry<Map<String, String>, Map<String, String>> rename : origRenames.entrySet()) {
                FieldIdentifier src = new FieldIdentifier();
                src.path = rename.getKey().get("path");
                src.type = rename.getKey().get("type");

                FieldIdentifier dest = new FieldIdentifier();
                dest.path = rename.getValue().get("path");
                dest.type = rename.getValue().get("type");

                update.renames.put(src, dest);
            }

            updates.put(entry.getKey(), update);
        }
        return updates;
    }

    /** The prefix of the namespace which has been changed
     */
    private String newPrefix;

    /** The prefix of the previous version of the namespace
     */
    private String prefix;

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

    Remodeling(Session session, String newPrefix, String oldUri) throws RepositoryException {
        this(session, newPrefix, oldUri, "", new HashMap<String, TypeUpdate>());
    }

    Remodeling(Session session, String newPrefix, String oldUri, String contentUpdater, Object contentUpdaterCargo)
            throws RepositoryException {
        this.session = session;
        this.newPrefix = newPrefix;
        this.updates = convertCargo(contentUpdaterCargo);

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

    private void copyType(Node source, Node target, NodeType sourceType, List<PropertyDefinition> propDefs,
            List<NodeDefinition> nodeDefs) throws RepositoryException {
        TypeUpdate typeUpdate = updates.get(sourceType.getName());

        for (PropertyDefinition definition : sourceType.getPropertyDefinitions()) {
            if (!definition.isProtected()) {
                String name = definition.getName();

                if (name.equals("*")) {
                    PropertyIterator properties = source.getProperties(name);
                    while (properties.hasNext()) {
                        Property property = properties.nextProperty();
                        if (property.getDefinition().equals(definition)) {
                            copyProperty(target, property, getNewName(property.getName()), propDefs);
                        }
                    }
                } else {
                    if (source.hasProperty(name)) {
                        Property property = source.getProperty(definition.getName());
                        if (typeUpdate != null) {
                            FieldIdentifier fieldId = new FieldIdentifier();
                            fieldId.path = name;
                            fieldId.type = PropertyType.nameFromValue(definition.getRequiredType());

                            FieldIdentifier newId = typeUpdate.renames.get(fieldId);
                            if (newId != null && !newId.path.equals("*")) {
                                name = newId.path;
                            }
                        }
                        copyProperty(target, property, getNewName(name), propDefs);
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
                            copyNode(target, node, getNewName(node.getName()), nodeDefs);
                        }
                    } else {
                        String name = definition.getName();
                        NodeType oldType = node.getPrimaryNodeType();
                        NodeType newType = conversion.get(oldType);
                        if (newType != null) {
                            if (typeUpdate != null) {
                                FieldIdentifier fieldId = new FieldIdentifier();
                                fieldId.path = name;
                                fieldId.type = getOldName(newType.getName());

                                FieldIdentifier newId = typeUpdate.renames.get(fieldId);
                                if (newId != null && !newId.path.equals("*")) {
                                    name = newId.path;
                                }
                            }

                            copyNode(target, node, getNewName(name), nodeDefs);
                        }
                    }
                }
            }
        }
    }

    private void copyPrototype(Node target, NodeType type) throws RepositoryException {
        TypeUpdate typeUpdate = updates.get(getOldName(type.getName()));
        if (typeUpdate != null && typeUpdate.prototype != null) {
            Node prototype = (Node) session.getNodeByUUID(typeUpdate.prototype);

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
                                    if (newType != null) {
                                        Node copy = target.addNode(nodeDef.getName(), newType.getName());
                                        traverse(node, true, copy);
                                    } else {
                                        log.warn("removing node " + node.getPath()
                                                + " as there is no new type defined for type "
                                                + node.getPrimaryNodeType().getName());
                                    }
                                }
                            } else {
                                Node node = prototype.getNode(getOldName(nodeDef.getName()));
                                log.info("pri type: " + node.getPrimaryNodeType().getName());
                                NodeType newType = conversion.get(node.getPrimaryNodeType());
                                if (newType != null) {
                                    Node copy = target.addNode(nodeDef.getName(), newType.getName());
                                    visit(node, copy);
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

    private void addNodeType(NodeType type, List<PropertyDefinition> propDefs, List<NodeDefinition> nodeDefs)
            throws RepositoryException {
        for (PropertyDefinition def : type.getPropertyDefinitions()) {
            propDefs.add(def);
        }
        for (NodeDefinition def : type.getChildNodeDefinitions()) {
            nodeDefs.add(def);
        }
    }

    private void visit(Node source, Node target) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {

        // build a lists of property and node definitions to be able to find
        // target definitions for the existing (source) properties and child nodes.
        List<PropertyDefinition> propDefs = new LinkedList<PropertyDefinition>();
        List<NodeDefinition> nodeDefs = new LinkedList<NodeDefinition>();

        NodeType targetType = target.getPrimaryNodeType();
        addNodeType(targetType, propDefs, nodeDefs);

        NodeType[] targetMixins = target.getMixinNodeTypes();
        for (int i = 0; i < targetMixins.length; i++) {
            addNodeType(targetMixins[i], propDefs, nodeDefs);
        }

        NodeType[] sourceTypes = source.getMixinNodeTypes();
        for (int i = 0; i < sourceTypes.length; i++) {
            NodeType newType = conversion.get(sourceTypes[i]);
            if (newType != null) {
                addNodeType(newType, propDefs, nodeDefs);
            }
        }

        // copy items that are defined in the primary node type to the target
        NodeType sourceType = source.getPrimaryNodeType();
        copyType(source, target, sourceType, propDefs, nodeDefs);

        // copy mixin types
        sourceTypes = source.getMixinNodeTypes();
        for (int i = 0; i < sourceTypes.length; i++) {
            NodeType newType = conversion.get(sourceTypes[i]);
            if (newType != null) {
                target.addMixin(newType.getName());

                copyType(source, target, sourceTypes[i], propDefs, nodeDefs);
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
            ReferenceValue origValue = ReferenceValue.valueOf(source.getUUID());
            ReferenceValue newValue = new ReferenceValue(target);
            while (propIter.hasNext()) {
                Property property = propIter.nextProperty();
                if (!property.getDefinition().isProtected()) {
                    if (property.getDefinition().isMultiple()) {
                        Value[] values = property.getValues();
                        for (int i = 0; i < values.length; i++) {
                            if (values[i].equals(origValue)) {
                                values[i] = newValue;
                            }
                        }
                        property.setValue(values);
                    } else {
                        property.setValue(new ReferenceValue(target));
                    }
                } else {
                    log.warn("Unable to update protected reference");
                }
            }
        }
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
        if (node.hasNode(HippoNodeType.HIPPO_PROTOTYPES)) {
            draft = EditmodelWorkflowImpl.getDraftPrototype(node);
            Node handle = node.getNode(HippoNodeType.HIPPO_PROTOTYPES);

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
            NodeType newType = ntMgr.getNodeType(newPrefix + ":" + NodeNameCodec.decode(node.getName()));
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
                node.checkout();
            }
        }

        if (copy) {
            // copy mixin types and properties
            visit(node, target);
            node.remove();
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
                child = node.getNode(newName + (index > 1 ? "[" + index + "]" : ""));
                traverse(child, false, child);
            }

            if (checkin) {
                node.getSession().save();
                node.checkin();
            }
        }
    }

    protected void updateTypes() throws RepositoryException {
        for (Node node : typeUpdates) {
            visitTemplateType(node);
        }
    }

    public static Remodeling remodel(Session session, String prefix, Reader cnd, String contentUpdater,
            Object contentUpdaterCargo) throws NamespaceException, RepositoryException {
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

            Remodeling remodel = new Remodeling(session, newPrefix, oldNamespaceURI, contentUpdater,
                    contentUpdaterCargo);
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
