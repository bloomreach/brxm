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
package org.hippoecm.repository.decorating;

import java.io.InputStream;
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
import javax.jcr.PathNotFoundException;
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
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.spi.Name;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.standardworkflow.RemodelWorkflow.FieldIdentifier;
import org.hippoecm.repository.standardworkflow.RemodelWorkflow.TypeUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Remodeling {
    protected final static Logger log = LoggerFactory.getLogger(Remodeling.class);

    private static final int ERR_NONE = 0;
    private static final int ERR_SOURCE = 1;
    private static final int ERR_TARGET = 2;

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
    private Map<String, TypeUpdate> renames;

    /** namespace registry
     */
    private NamespaceRegistry nsRegistry;

    /** Paths to the changed nodes.
     */
    private Set<Node> changes;

    /** Reference to the session in which the changes are prepared
     */
    transient Session session;

    Remodeling(Session session, String prefix, String oldUri, Map<String, TypeUpdate> renames)
            throws RepositoryException {
        this.session = session;
        this.prefix = prefix;
        this.renames = renames;

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
        if (name.startsWith(oldPrefix)) {
            return prefix + name.substring(oldPrefix.length());
        } else {
            return name;
        }
    }

    private int copyProperty(Node target, Property prop, String name, List<PropertyDefinition> targets)
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
                            return ERR_SOURCE;
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
        return ERR_NONE;
    }

    private int copyType(Node source, Node target, NodeType sourceType, NodeType targetType,
            List<PropertyDefinition> targets) throws RepositoryException {
        for (PropertyDefinition definition : sourceType.getPropertyDefinitions()) {
            if (!definition.isProtected()) {
                String name = getNewName(definition.getName());

                if (name.equals("*")) {
                    PropertyIterator properties = source.getProperties(name);
                    while (properties.hasNext()) {
                        Property property = properties.nextProperty();
                        if (property.getDefinition().equals(definition)) {
                            int result = copyProperty(target, property, getNewName(property.getName()), targets);
                            if (result != ERR_NONE) {
                                return result;
                            }
                        }
                    }
                } else {
                    if (source.hasProperty(definition.getName())) {
                        Property property = source.getProperty(definition.getName());
                        TypeUpdate typeRename = renames.get(getNewName(sourceType.getName()));
                        if (typeRename != null) {
                            FieldIdentifier fieldId = new FieldIdentifier();
                            fieldId.path = name;
                            fieldId.type = PropertyType.nameFromValue(definition.getRequiredType());

                            FieldIdentifier newId = typeRename.getRenames().get(fieldId);
                            if (newId != null && !newId.path.equals("*")) {
                                name = newId.path;
                            }
                        }
                        int result = copyProperty(target, property, name, targets);
                        if (result != ERR_NONE) {
                            return result;
                        }
                    }
                }
            }
        }

        // FIXME: put copying child nodes logic here
        return ERR_NONE;
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

    private int visit(Node source, Node target) throws ValueFormatException, VersionException, LockException,
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
        int result = copyType(source, target, sourceType, targetType, targets);
        if (result != ERR_NONE) {
            return result;
        }

        // copy mixin types
        sourceTypes = source.getMixinNodeTypes();
        for (int i = 0; i < sourceTypes.length; i++) {
            NodeType newType = conversion.get(sourceTypes[i]);
            if (newType != null) {
                target.addMixin(newType.getName());

                result = copyType(source, target, sourceTypes[i], newType, targets);
                if (result != ERR_NONE) {
                    return result;
                }
            }
        }

        // validate types
        if (!isType(target, targetType)) {
            return ERR_TARGET;
        }
        NodeType[] targetTypes = target.getMixinNodeTypes();
        for (int i = 0; i < targetTypes.length; i++) {
            if (!isType(target, targetTypes[i])) {
                return ERR_TARGET;
            }
        }
        return ERR_NONE;
    }

    private void visitTemplateType(Node node) throws RepositoryException {
        String[] names = { HippoNodeType.HIPPO_NODETYPE };
        for (String name : names) {
            Node current = getVersion(node, name, "current");
            Node draft = getVersion(node, name, "draft");
            if (current != null) {
                if (draft == null) {
                    // same as in EditmodelWorkflowImpl
                    draft = ((HippoSession) current.getSession()).copy(current, current.getParent().getPath() + "/"
                            + name);
                    draft.addMixin(HippoNodeType.NT_REMODEL);
                    draft.setProperty(HippoNodeType.HIPPO_REMODEL, "draft");
                }

                if (!current.isNodeType(HippoNodeType.NT_REMODEL)) {
                    current.addMixin(HippoNodeType.NT_REMODEL);
                }
                current.setProperty(HippoNodeType.HIPPO_REMODEL, "old");
                current.setProperty(HippoNodeType.HIPPO_URI, nsRegistry.getURI(oldPrefix));
            }

            if (draft != null) {
                draft.removeMixin(HippoNodeType.NT_REMODEL);
            }
        }

        if (node.hasNode(HippoNodeType.HIPPO_PROTOTYPE)) {
            Node draft = getVersion(node, HippoNodeType.HIPPO_PROTOTYPE, "draft");
            Node current = getVersion(node, HippoNodeType.HIPPO_PROTOTYPE, "current");
            if (draft == null) {
                if (current != null) {
                    // same as in EditmodelWorkflowImpl
                    draft = ((HippoSession) current.getSession()).copy(current, current.getParent().getPath() + "/"
                            + HippoNodeType.HIPPO_PROTOTYPE);
                    draft.addMixin(HippoNodeType.NT_REMODEL);
                    draft.setProperty(HippoNodeType.HIPPO_REMODEL, "draft");
                } else {
                    return;
                }
            }

            if (current != null) {
                if (!current.isNodeType(HippoNodeType.NT_REMODEL)) {
                    current.addMixin(HippoNodeType.NT_REMODEL);
                }
                current.setProperty(HippoNodeType.HIPPO_REMODEL, "old");
                current.setProperty(HippoNodeType.HIPPO_URI, nsRegistry.getURI(oldPrefix));
            }

            Node handle = node.getNode(HippoNodeType.HIPPO_PROTOTYPE);
            NodeType newType = node.getSession().getWorkspace().getNodeTypeManager().getNodeType(node.getName());
            if (newType != null) {
                Node newChild = handle.addNode(HippoNodeType.HIPPO_PROTOTYPE, newType.getName());
                int result = traverse(draft, true, newChild);
                if (result == ERR_NONE) {
                    draft.remove(); // iter.remove();
                } else {
                    newChild.remove();
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

    protected int traverse(Node node, boolean copy, Node target) throws RepositoryException {
        if (node.getPath().equals("/jcr:system")) {
            return ERR_NONE;
        } else if (node.isNodeType(HippoNodeType.NT_TEMPLATETYPE)) {
            String name = node.getName();
            if (name.startsWith(prefix + ":")) {
                visitTemplateType(node);
                return ERR_NONE;
            }
        }

        if (copy) {
            // copy mixin types and properties
            int result = visit(node, target);

            // update workflow state
            switch (result) {
            case ERR_SOURCE:
                if (!node.isNodeType(HippoNodeType.NT_REMODEL)) {
                    node.addMixin(HippoNodeType.NT_REMODEL);
                }
                node.setProperty(HippoNodeType.HIPPO_REMODEL, "error");
                log.error("error in source when converting " + node.getPath()
                        + ".  A multiple field could not be converted to a singular one.");
                return ERR_SOURCE;

            case ERR_TARGET:
                if (!node.isNodeType(HippoNodeType.NT_REMODEL)) {
                    node.addMixin(HippoNodeType.NT_REMODEL);
                }
                node.setProperty(HippoNodeType.HIPPO_REMODEL, "draft");
                log.error("error in target when converting " + node.getPath()
                        + ".  Some mandatory fields are not provided.");

                if (!node.isNodeType(HippoNodeType.NT_UNSTRUCTURED)) {
                    node.addMixin(HippoNodeType.NT_UNSTRUCTURED);
                }
                return ERR_TARGET;

            case ERR_NONE:
                break;
            }
        }

        if (target.isNodeType(HippoNodeType.NT_REMODEL)) {
            String state = target.getProperty(HippoNodeType.HIPPO_REMODEL).getString();
            if ("current".equals(state)) {
                target.setProperty(HippoNodeType.HIPPO_REMODEL, "old");
            } else if ("draft".equals(state)) {
                if (target.isNodeType(HippoNodeType.NT_UNSTRUCTURED)) {
                    target.removeMixin(HippoNodeType.NT_REMODEL);
                    target.removeMixin(HippoNodeType.NT_UNSTRUCTURED);
                } else {
                    target.setProperty(HippoNodeType.HIPPO_REMODEL, "current");
                }
            } else if ("error".equals(state)) {
                target.removeMixin(HippoNodeType.NT_REMODEL);
            }
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
                    int result = traverse(child, true, newChild);
                    if (result == ERR_NONE) {
                        if (!copy) {
                            child.remove(); // iter.remove();
                        }
                        changes.add(newChild);
                    } else {
                        newChild.remove();
                    }
                } else if (copy) {
                    Node newChild = target.addNode(getNewName(child.getName()), newType.getName());
                    int result = traverse(child, true, newChild);
                    if (result != ERR_NONE) {
                        return result;
                    }
                } else if (child.getName().startsWith(oldPrefix)) {
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
        return ERR_NONE;
    }

    public static Remodeling remodel(Session session, String prefix, InputStream cnd, Map<String, TypeUpdate> renames)
            throws NamespaceException, RepositoryException {
        Workspace workspace = session.getWorkspace();
        NamespaceRegistry nsreg = workspace.getNamespaceRegistry();

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

        // push new node type definition such that it will be loaded
        try {
            Node base = session.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH).getNode(
                    HippoNodeType.INITIALIZE_PATH);
            Node node;
            if (base.hasNode(prefix)) {
                node = base.getNode(prefix);
            } else {
                node = base.addNode(prefix, HippoNodeType.NT_INITIALIZEITEM);
            }
            node.setProperty(HippoNodeType.HIPPO_NAMESPACE, newNamespaceURI);
            node.setProperty(HippoNodeType.HIPPO_NODETYPES, cnd);
            session.save();

            // wait for node types to be reloaded
            session.refresh(true);
            while (base.getNode(prefix).hasProperty(HippoNodeType.HIPPO_NODETYPES)
                    || base.getNode(prefix).hasProperty(HippoNodeType.HIPPO_NODETYPESRESOURCE)) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                }
                session.refresh(true);
            }
        } catch (ConstraintViolationException ex) {
            throw new RepositoryException("Hippo repository configuration not in order");
        } catch (LockException ex) {
            throw new RepositoryException("Hippo repository configuration not in order");
        } catch (ValueFormatException ex) {
            throw new RepositoryException("Hippo repository configuration not in order");
        } catch (VersionException ex) {
            throw new RepositoryException("Hippo repository configuration not in order");
        } catch (PathNotFoundException ex) {
            throw new RepositoryException("Hippo repository configuration not in order");
        }

        try {
            Remodeling remodel = new Remodeling(session, prefix, oldNamespaceURI, renames);
            remodel.traverse(session.getRootNode(), false, session.getRootNode());
            return remodel;
        } catch (RepositoryException ex) {
            session.refresh(false);
            throw ex;
        }
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
