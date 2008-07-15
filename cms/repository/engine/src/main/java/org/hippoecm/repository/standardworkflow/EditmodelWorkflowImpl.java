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

import java.rmi.RemoteException;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.jackrabbit.JcrConstants;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.InternalWorkflow;
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

    public String edit() throws WorkflowException, MappingException, RepositoryException {
        if (!subject.isNodeType(HippoNodeType.NT_TEMPLATETYPE))
            throw new MappingException("invalid node type for EditmodelWorkflow");

        Node draft = getDraft(subject, HippoNodeType.HIPPO_NODETYPE);
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
        for (String sub : new String[] { HippoNodeType.HIPPO_NODETYPE, HippoNodeType.HIPPO_PROTOTYPE }) {
            if (target.hasNode(sub)) {
                NodeIterator nodes = target.getNode(sub).getNodes(sub);
                while (nodes.hasNext()) {
                    Node child = nodes.nextNode();
                    if (child.isNodeType(HippoNodeType.NT_REMODEL)) {
                        child.remove();
                    } else if (sub.equals(HippoNodeType.HIPPO_NODETYPE)) {
                        if (child.hasProperty(HippoNodeType.HIPPO_TYPE)) {
                            child.setProperty(HippoNodeType.HIPPO_TYPE, name);
                        }
                    }
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

    static Node getLatest(Node node, String name) throws RepositoryException {
        if (node.hasNode(name)) {
            Node latestNode = null;
            String[] version = null;

            Node template = node.getNode(name);
            NodeIterator iter = template.getNodes(name);
            while (iter.hasNext()) {
                Node versionNode = iter.nextNode();
                if (versionNode.isNodeType(HippoNodeType.NT_REMODEL)) {
                    String uri = versionNode.getProperty(HippoNodeType.HIPPO_URI).getString();
                    int pos = uri.lastIndexOf('/');
                    String[] uriVersion = uri.substring(pos + 1).split("\\.");
                    if (version == null || isLater(uriVersion, version)) {
                        latestNode = versionNode;
                        version = uriVersion;
                    }
                }
            }
            return latestNode;
        }
        return null;
    }

    static Node getDraft(Node node, String name) throws RepositoryException {
        if (node.hasNode(name)) {
            Node template = node.getNode(name);
            NodeIterator iter = template.getNodes(name);
            while (iter.hasNext()) {
                Node versionNode = iter.nextNode();

                if (!versionNode.isNodeType(HippoNodeType.NT_REMODEL)) {
                    return versionNode;
                }
            }
        }
        return null;
    }

    static void checkoutType(Node subject) throws RepositoryException {
        // copy nodetype

        Node current;
        Node draft = getDraft(subject, HippoNodeType.HIPPO_NODETYPE);
        String name = HippoNodeType.HIPPO_NODETYPE;
        if (draft == null) {
            current = getLatest(subject, name);
            if (current == null) {
                throw new ItemNotFoundException("Remodel node " + name + ", current version was not found for type "
                        + subject.getPath());
            }
            draft = ((HippoSession) current.getSession()).copy(current, current.getParent().getPath() + "/" + name);
            draft.removeMixin(HippoNodeType.NT_REMODEL);
        }

        // copy prototype

        name = HippoNodeType.HIPPO_PROTOTYPE;
        draft = getDraft(subject, name);
        if (draft == null) {
            current = getLatest(subject, name);
            if (current == null) {
                throw new ItemNotFoundException("Remodel node " + name + ", current version was not found for type "
                        + subject.getPath());
            }
            draft = current.getParent().addNode(name, JcrConstants.NT_UNSTRUCTURED);
            draft.addMixin("mix:referenceable");

            PropertyIterator propIter = current.getProperties();
            while (propIter.hasNext()) {
                Property prop = propIter.nextProperty();
                PropertyDefinition definition = prop.getDefinition();
                if (!definition.isProtected()
                        && !definition.getDeclaringNodeType().isNodeType(HippoNodeType.NT_REMODEL)) {
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
