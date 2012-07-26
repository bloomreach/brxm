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
package org.hippoecm.editor.repository.impl;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.ItemNotFoundException;
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
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;

import org.hippoecm.editor.NamespaceValidator;
import org.hippoecm.editor.repository.EditmodelWorkflow;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.InternalWorkflow;

public class EditmodelWorkflowImpl implements EditmodelWorkflow, InternalWorkflow {

    private static final long serialVersionUID = 1L;

    private class NodeTypeState {

        final String name;
        Node draft = null;
        Node current = null;

        NodeTypeState() throws RepositoryException {
            this.name = subject.getName();

            if (subject.hasNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE)) {
                String[] version = null;

                Node template = subject.getNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE);
                NodeIterator iter = template.getNodes(HippoNodeType.HIPPOSYSEDIT_NODETYPE);
                while (iter.hasNext()) {
                    Node versionNode = iter.nextNode();

                    if (!versionNode.isNodeType(HippoNodeType.NT_REMODEL)) {
                        draft = versionNode;
                    } else {
                        String uri = versionNode.getProperty(HippoNodeType.HIPPO_URI).getString();
                        int pos = uri.lastIndexOf('/');
                        String[] uriVersion = uri.substring(pos + 1).split("\\.");
                        if (version == null || isLater(uriVersion, version)) {
                            current = versionNode;
                            version = uriVersion;
                        }
                    }
                }
            }
        }

        boolean isEditable() throws RepositoryException {
            if ("system".equals(subject.getParent().getName())) {
                return false;
            }
            String ntName = subject.getParent().getName() + ":" + subject.getName();
            NodeTypeManager ntMgr = subject.getSession().getWorkspace().getNodeTypeManager();
            if (current != null) {
                if (!ntMgr.hasNodeType(ntName)) {
                    return false;
                }
                NodeType type = ntMgr.getNodeType(ntName);
                if (!type.isNodeType(HippoStdNodeType.NT_RELAXED)) {
                    return false;
                }
            }
            return true;
        }

        void checkout() throws RepositoryException {
            // copy nodetype
            if (draft == null) {
                if (current == null) {
                    throw new ItemNotFoundException("Remodel node " + HippoNodeType.HIPPOSYSEDIT_NODETYPE
                            + ", current version was not found for type " + subject.getPath());
                }
                draft = ((HippoSession) current.getSession()).copy(current, current.getParent().getPath() + "/"
                        + HippoNodeType.HIPPOSYSEDIT_NODETYPE);
                draft.removeMixin(HippoNodeType.NT_REMODEL);
            }
        }

        List<String> getSupertypes() throws RepositoryException {
            Node reference;
            if (draft != null) {
                reference = draft;
            } else {
                reference = current;
            }

            List<String> superStrings = new LinkedList<String>();
            if (reference.hasProperty(HippoNodeType.HIPPO_SUPERTYPE)) {
                Value[] supers = reference.getProperty(HippoNodeType.HIPPO_SUPERTYPE).getValues();
                for (int i = 0; i < supers.length; i++) {
                    superStrings.add(supers[i].getString());
                }
            }
            return superStrings;
        }
        
        void commit() throws RepositoryException {
            if (draft != null) {
                if (current == null) {
                    // register node type
                    NodeTypeManager ntMgr = subject.getSession().getWorkspace().getNodeTypeManager();
                    if (!ntMgr.hasNodeType(prefix + ":" + name)) {
                        NodeTypeTemplate ntTpl = ntMgr.createNodeTypeTemplate();
                        ntTpl.setName(prefix + ":" + name);
                        if (draft.hasProperty(HippoNodeType.HIPPO_SUPERTYPE)) {
                            Value[] supers = draft.getProperty(HippoNodeType.HIPPO_SUPERTYPE).getValues();
                            String[] superStrings = new String[supers.length];
                            for (int i = 0; i < supers.length; i++) {
                                superStrings[i] = supers[i].getString();
                            }
                            ntTpl.setDeclaredSuperTypeNames(superStrings);
                        }
                        ntTpl.setOrderableChildNodes(true);
                        ntMgr.registerNodeType(ntTpl, false);
                    }
                } else {
                    current.remove();
                }
                draft.addMixin(HippoNodeType.NT_REMODEL);
                draft.setProperty(HippoNodeType.HIPPO_URI, uri);
                current = draft;
                draft = null;
            }
        }

        void revert() throws RepositoryException {
            if (draft != null) {
                if (current == null) {
                    throw new ItemNotFoundException("Remodel node " + HippoNodeType.HIPPOSYSEDIT_NODETYPE
                            + ", current version was not found for type " + subject.getPath());
                }
                draft.remove();
                draft = null;
            }
        }
    }

    private class PrototypeState {
        Node draft = null;
        Node current = null;

        PrototypeState() throws RepositoryException {
            if (subject.hasNode(HippoNodeType.HIPPO_PROTOTYPES)) {
                NodeIterator nodes = subject.getNode(HippoNodeType.HIPPO_PROTOTYPES).getNodes();
                while (nodes.hasNext()) {
                    Node child = nodes.nextNode();
                    if (child.isNodeType("nt:unstructured")) {
                        draft = child;
                    } else if (prefix != null) {
                        NodeType nt = child.getPrimaryNodeType();
                        if (nt.getName().startsWith(prefix + ":")) {
                            current = child;
                        }
                    }
                }
            }
        }

        void checkout() throws RepositoryException {
            // copy prototype
            if (draft == null) {
                if (!subject.hasNode(HippoNodeType.HIPPO_PROTOTYPES)) {
                    subject.addNode(HippoNodeType.HIPPO_PROTOTYPES, HippoNodeType.NT_PROTOTYPESET);
                }
                draft = subject.getNode(HippoNodeType.HIPPO_PROTOTYPES).addNode(HippoNodeType.HIPPO_PROTOTYPE,
                        "nt:unstructured");
                if (current != null) {
                    // add dynamic mixins
                    for (NodeType mixin : current.getMixinNodeTypes()) {
                        draft.addMixin(mixin.getName());
                    }
                    // add static mixins from supertype
                    NodeType primarySuperType = current.getPrimaryNodeType();
                    while (primarySuperType != null && !primarySuperType.getName().equals("nt:base")) {
                        NodeType newSuper = null;
                        for (NodeType superType : primarySuperType.getDeclaredSupertypes()) {
                            if (superType.isMixin()) {
                                draft.addMixin(superType.getName());
                            } else {
                                newSuper = superType;
                            }
                        }
                        primarySuperType = newSuper;
                    }

                    PropertyIterator propIter = current.getProperties();
                    while (propIter.hasNext()) {
                        Property prop = propIter.nextProperty();
                        PropertyDefinition definition = prop.getDefinition();
                        if (!definition.isProtected()) {
                            if (definition.isMultiple()) {
                                draft.setProperty(prop.getName(), prop.getValues());
                            } else {
                                draft.setProperty(prop.getName(), prop.getValue());
                            }
                        }
                    }

                    NodeIterator nodeIter = current.getNodes();
                    while (nodeIter.hasNext()) {
                        Node child = nodeIter.nextNode();
                        if (child.getDefinition().isAutoCreated()) {
                            continue;
                        }
                        ((HippoSession) current.getSession()).copy(child, draft.getPath() + "/" + child.getName());
                    }
                }
            }
        }

        void commit(NodeTypeState nts) throws RepositoryException {
            if (draft != null) {
                if (current != null) {
                    current.remove();
                }

                Node prototypes = subject.getNode(HippoNodeType.HIPPO_PROTOTYPES);
                for (NodeIterator iter = prototypes.getNodes(HippoNodeType.HIPPO_PROTOTYPE); iter.hasNext();) {
                    Node prototype = iter.nextNode();
                    if (!prototype.isNodeType("nt:unstructured")) {
                        prototype.remove();
                    } else {
                        assert (prototype.isSame(draft));
                    }
                }

                String newTypeName = subject.getParent().getName() + ":" + subject.getName();
                NodeType newType = subject.getSession().getWorkspace().getNodeTypeManager().getNodeType(newTypeName);
                Node clone = prototypes.addNode(HippoNodeType.HIPPO_PROTOTYPE, newTypeName);

                if (newType.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                    clone.addMixin(HippoNodeType.NT_HARDDOCUMENT);
                }
                for (String mixinName : nts.getSupertypes()) {
                    if (!newType.isNodeType(mixinName)) {
                        clone.addMixin(mixinName);
                    }
                }
                for (PropertyIterator props = draft.getProperties(); props.hasNext();) {
                    Property prop = props.nextProperty();
                    if (!prop.getDefinition().isProtected()) {
                        if (prop.isMultiple()) {
                            clone.setProperty(prop.getName(), prop.getValues(), prop.getType());
                        } else {
                            clone.setProperty(prop.getName(), prop.getValue());
                        }
                    }
                }
                for (NodeIterator nodes = draft.getNodes(); nodes.hasNext();) {
                    Node child = nodes.nextNode();
                    NodeDefinition definition = child.getDefinition();
                    if (definition.isAutoCreated()) {
                        continue;
                    }
                    if (clone.getPrimaryNodeType().canAddChildNode(child.getName(), child.getPrimaryNodeType().getName())) {
                        subject.getSession().move(child.getPath(), clone.getPath() + "/" + child.getName());
                    }
                }

                if (newType.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                    clone.addMixin(HippoNodeType.NT_HARDDOCUMENT);
                    clone.setProperty(HippoNodeType.HIPPO_PATHS, new Value[] {});
                } else if (newType.isNodeType(HippoNodeType.NT_REQUEST)) {
                    clone.addMixin("mix:referenceable");
                }
                createMandatoryProperties(clone, newType);

                current = draft;
                draft.remove();
                draft = null;
            }
        }

        void revert() throws RepositoryException {
            if (draft != null) {
                draft.remove();
            }
        }
    }

    private static void createMandatoryProperties(Node draftPrototype, NodeType nt) throws RepositoryException,
            ValueFormatException, VersionException, LockException, ConstraintViolationException {
        NodeType[] supers = nt.getSupertypes();
        NodeType[] all = new NodeType[supers.length + 1];
        System.arraycopy(supers, 0, all, 0, supers.length);
        all[supers.length] = nt;
        for (NodeType type : all) {
            for (PropertyDefinition pdef : type.getPropertyDefinitions()) {
                if (pdef.getDeclaringNodeType() == type) {
                    if (pdef.isMandatory() && !pdef.isProtected() && !"*".equals(pdef.getName())
                            && !draftPrototype.hasProperty(pdef.getName())) {
                        if (pdef.isMultiple()) {
                            draftPrototype.setProperty(pdef.getName(), new Value[0]);
                        } else {
                            switch (pdef.getRequiredType()) {
                            case PropertyType.LONG:
                                draftPrototype.setProperty(pdef.getName(), 0);
                                break;
                            case PropertyType.DOUBLE:
                                draftPrototype.setProperty(pdef.getName(), 0.0f);
                                break;
                            case PropertyType.DATE:
                                draftPrototype.setProperty(pdef.getName(), Calendar.getInstance());
                                break;
                            case PropertyType.REFERENCE:
                                draftPrototype.setProperty(pdef.getName(), draftPrototype.getSession().getRootNode());
                                break;
                            case PropertyType.STRING:
                                String[] constraints = pdef.getValueConstraints();
                                if (constraints != null && constraints.length > 0) {
                                    draftPrototype.setProperty(pdef.getName(), constraints[0]);
                                    break;
                                }
                            default:
                                draftPrototype.setProperty(pdef.getName(), "");
                            }
                        }
                    }
                }
            }
        }
    }

    private Session rootSession;
    private Node subject;

    private String prefix;
    private String uri;

    public EditmodelWorkflowImpl(Session userSession, Session rootSession, Node subject) throws RemoteException,
            RepositoryException {
        this.rootSession = rootSession;
        this.subject = subject;
        this.prefix = subject.getParent().getName();

        NamespaceRegistry nsReg = rootSession.getWorkspace().getNamespaceRegistry();
        this.uri = nsReg.getURI(prefix);
    }

    public Map<String, Serializable> hints() throws RepositoryException {
        Map<String, Serializable> hints = new TreeMap<String, Serializable>();
        NodeTypeState state = new NodeTypeState();
        if (!state.isEditable()) {
            hints.put("edit", false);
        }
        if (state.draft == null) {
            hints.put("commit", false);
            hints.put("revert", false);
        }
        return hints;
    }

    public String edit() throws WorkflowException, MappingException, RepositoryException {
        if (!subject.isNodeType(HippoNodeType.NT_TEMPLATETYPE)) {
            throw new MappingException("invalid node type for EditmodelWorkflow");
        }
        NodeTypeState state = new NodeTypeState();
        if (state.draft != null) {
            return subject.getPath();
        }

        try {
            state.checkout();
            PrototypeState prototypeState = new PrototypeState();
            prototypeState.checkout();

            subject.getSession().save();

            return subject.getPath();
        } catch (RepositoryException ex) {
            subject.refresh(false);
            throw ex;
        }
    }

    public void save() throws WorkflowException, MappingException, RepositoryException {
        if (!subject.isNodeType(HippoNodeType.NT_TEMPLATETYPE)) {
            throw new MappingException("invalid node type for EditmodelWorkflow");
        }
        subject.save();
    }

    public String copy(String name) throws WorkflowException, MappingException, RepositoryException {
        if (!subject.isNodeType(HippoNodeType.NT_TEMPLATETYPE)) {
            throw new MappingException("invalid node type for EditmodelWorkflow");
        }

        try {
            NamespaceValidator.checkName(name);
        } catch (Exception e) {
            throw new WorkflowException("Invalid name " + name);
        }

        String path = subject.getPath();
        path = path.substring(0, path.lastIndexOf("/") + 1);
        path += name;
        Node target = (Node) rootSession.getItem(subject.getPath());
        target = ((HippoSession) rootSession).copy(target, path);

        NodeTypeState state = new NodeTypeState();
        state.checkout();

        // clean up
        if (target.hasNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE)) {
            Node draftNode = null;
            NodeIterator nodes = target.getNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE).getNodes(
                    HippoNodeType.HIPPOSYSEDIT_NODETYPE);
            while (nodes.hasNext()) {
                Node child = nodes.nextNode();
                if (child.hasProperty(HippoNodeType.HIPPOSYSEDIT_TYPE)) {
                    child.setProperty(HippoNodeType.HIPPOSYSEDIT_TYPE, name);
                }
                if (draftNode == null) {
                    draftNode = child;
                } else if (!child.isNodeType(HippoNodeType.NT_REMODEL)) {
                    draftNode.remove();
                    draftNode = child;
                } else {
                    draftNode.remove();
                }
            }
            if (draftNode != null && draftNode.isNodeType(HippoNodeType.NT_REMODEL)) {
                draftNode.removeMixin(HippoNodeType.NT_REMODEL);
            }
        }
        if (target.hasNode(HippoNodeType.HIPPO_PROTOTYPES)) {
            NodeIterator nodes = target.getNode(HippoNodeType.HIPPO_PROTOTYPES).getNodes(HippoNodeType.HIPPO_PROTOTYPE);
            Node prototype = null;
            while (nodes.hasNext()) {
                Node child = nodes.nextNode();
                if (prototype == null) {
                    prototype = child;
                } else if (child.isNodeType("nt:unstructured")) {
                    prototype.remove();
                    prototype = child;
                } else {
                    prototype.remove();
                }
            }
            if (prototype != null) {
                prototype.setPrimaryType("nt:unstructured");
            }
        }
        if (target.isNodeType(HippoNodeType.NT_TRANSLATED)) {
            target.removeMixin(HippoNodeType.NT_TRANSLATED);
        }

        rootSession.save();

        return target.getPath();
    }

    public void commit() throws WorkflowException, MappingException, RepositoryException {
        NodeTypeState state = new NodeTypeState();
        if (state.draft == null) {
            throw new WorkflowException("No draft available to publish");
        }
        state.commit();
        PrototypeState prototypeState = new PrototypeState();
        prototypeState.commit(state);

        subject.getSession().save();
    }

    public void revert() throws WorkflowException, MappingException, RepositoryException {
        NodeTypeState state = new NodeTypeState();
        if (state.draft == null) {
            throw new WorkflowException("No draft available to publish");
        }
        state.revert();
        PrototypeState prototypeState = new PrototypeState();
        prototypeState.revert();

        subject.getSession().save();
    }

    private static boolean isLater(String[] one, String[] two) {
        for (int i = 0; i < one.length; i++) {
            if (i < two.length) {
                int oneVersion = Integer.parseInt(one[i]);
                int twoVersion = Integer.parseInt(two[i]);
                if (oneVersion > twoVersion) {
                    return true;
                } else if (oneVersion < twoVersion) {
                    return false;
                }
            } else {
                return true;
            }
        }
        return false;
    }

}
