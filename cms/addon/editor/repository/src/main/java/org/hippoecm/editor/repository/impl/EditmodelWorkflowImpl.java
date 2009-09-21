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
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.InternalWorkflow;
import org.hippoecm.editor.repository.EditmodelWorkflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated
 */
public class EditmodelWorkflowImpl implements EditmodelWorkflow, InternalWorkflow {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(EditmodelWorkflowImpl.class);

    private Session rootSession;
    private Node subject;

    public EditmodelWorkflowImpl(Session userSession, Session rootSession, Node subject) throws RemoteException {
        this.rootSession = rootSession;
        this.subject = subject;
    }

    public Map<String,Serializable> hints() {
        return new TreeMap<String,Serializable>();
    }

    public String edit() throws WorkflowException, MappingException, RepositoryException {
        if (!subject.isNodeType(HippoNodeType.NT_TEMPLATETYPE))
            throw new MappingException("invalid node type for EditmodelWorkflow");

        Node draft = null;
        if (subject.hasNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE)) {
            NodeIterator iter = subject.getNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE).getNodes(HippoNodeType.HIPPOSYSEDIT_NODETYPE);
            while (iter.hasNext()) {
                Node versionNode = iter.nextNode();
                if (!versionNode.isNodeType(HippoNodeType.NT_REMODEL)) {
                    draft = versionNode;
                    break;
                }
            }
        }
        if (draft != null) {
            return subject.getPath();
        }

        subject.save();
        try {
            checkoutType(subject);

            subject.save();
            return subject.getPath();
        } catch (RepositoryException ex) {
            subject.refresh(false);
            throw ex;
        }
    }

    public void save() throws WorkflowException, MappingException, RepositoryException {
        if (!subject.isNodeType(HippoNodeType.NT_TEMPLATETYPE))
            throw new MappingException("invalid node type for EditmodelWorkflow");

        subject.save();
    }

    public String copy(String name) throws WorkflowException, MappingException, RepositoryException {
        if (!subject.isNodeType(HippoNodeType.NT_TEMPLATETYPE))
            throw new MappingException("invalid node type for EditmodelWorkflow");

        String path = subject.getPath();
        path = path.substring(0, path.lastIndexOf("/") + 1);
        path += name;
        Node target = (Node) rootSession.getItem(subject.getPath());
        target = ((HippoSession) rootSession).copy(target, path);

        checkoutType(target);

        // clean up
        if (target.hasNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE)) {
            NodeIterator nodes = target.getNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE).getNodes(HippoNodeType.HIPPOSYSEDIT_NODETYPE);
            while (nodes.hasNext()) {
                Node child = nodes.nextNode();
                if (child.isNodeType(HippoNodeType.NT_REMODEL)) {
                    child.remove();
                } else if (child.hasProperty(HippoNodeType.HIPPOSYSEDIT_TYPE)) {
                    child.setProperty(HippoNodeType.HIPPOSYSEDIT_TYPE, name);
                }
            }
        }
        if (target.hasNode(HippoNodeType.HIPPO_PROTOTYPES)) {
            NodeIterator nodes = target.getNode(HippoNodeType.HIPPO_PROTOTYPES).getNodes(HippoNodeType.HIPPO_PROTOTYPE);
            while (nodes.hasNext()) {
                Node child = nodes.nextNode();
                if (!child.isNodeType("nt:unstructured")) {
                    child.remove();
                }
            }
        }

        target.getParent().save();
        return target.getPath();
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

    static void checkoutType(Node subject) throws RepositoryException {
        // copy nodetype

        Node draft = null;
        Node current = null;
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
        if (draft == null) {
            if (current == null) {
                throw new ItemNotFoundException("Remodel node " + HippoNodeType.HIPPOSYSEDIT_NODETYPE
                        + ", current version was not found for type " + subject.getPath());
            }
            draft = ((HippoSession) current.getSession()).copy(current, current.getParent().getPath() + "/"
                    + HippoNodeType.HIPPOSYSEDIT_NODETYPE);
            draft.removeMixin(HippoNodeType.NT_REMODEL);
        }

        // use prefix to find matching prototype
        String prefix = null;
        if (current != null) {
            String uri = current.getProperty(HippoNodeType.HIPPO_URI).getString();
            prefix = subject.getSession().getNamespacePrefix(uri);
        }

        // copy prototype
        if (subject.hasNode(HippoNodeType.HIPPO_PROTOTYPES)) {
            draft = null;
            current = null;
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
            if (draft == null) {
                if (current == null) {
                    throw new ItemNotFoundException("Remodel node " + HippoNodeType.HIPPO_PROTOTYPE
                            + ", current version was not found for type " + subject.getPath());
                }
                draft = current.getParent().addNode(HippoNodeType.HIPPO_PROTOTYPE, "nt:unstructured");
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
                    ((HippoSession) current.getSession()).copy(child, draft.getPath() + "/" + child.getName());
                }
            }
        }
    }

}
