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

import java.rmi.RemoteException;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowException;

public class EditmodelWorkflowImpl implements EditmodelWorkflow {
    private static final long serialVersionUID = 1L;

    private Session rootSession;
    private Node node;

    public EditmodelWorkflowImpl(Session userSession, Session rootSession, Node subject) throws RemoteException {
        this.rootSession = rootSession;
        this.node = subject;
    }

    public String edit() throws WorkflowException, MappingException, RepositoryException {
        if (!node.isNodeType(HippoNodeType.NT_TEMPLATETYPE))
            throw new MappingException("invalid node type for EditmodelWorkflow");

        Node draft = getVersion(node, HippoNodeType.HIPPO_NODETYPE, "draft");
        if (draft != null) {
            return node.getPath();
        }

        node.save();
        try {
            String[] names = { HippoNodeType.HIPPO_TEMPLATE, HippoNodeType.HIPPO_NODETYPE,
                    HippoNodeType.HIPPO_PROTOTYPE };
            for (String name : names) {
                Node current = getVersion(node, name, "current");
                if (current == null) {
                    throw new ItemNotFoundException("Remodel node " + name
                            + ", current version was not found for type " + node.getPath());
                }
                Node copy = ((HippoSession) current.getSession()).copy(current, current.getParent().getPath() + "/"
                        + name);
                if (!name.equals(HippoNodeType.HIPPO_TEMPLATE)) {
                    copy.addMixin(HippoNodeType.NT_REMODEL);
                    copy.setProperty(HippoNodeType.HIPPO_REMODEL, "draft");
                    if (name.equals(HippoNodeType.HIPPO_PROTOTYPE)) {
                        copy.addMixin(HippoNodeType.NT_UNSTRUCTURED);
                    }
                }
            }
            node.save();
            return node.getPath();
        } catch (RepositoryException ex) {
            node.refresh(false);
            throw ex;
        }
    }

    public void save() throws WorkflowException, MappingException, RepositoryException {
        if (node.isNodeType(HippoNodeType.NT_TEMPLATE) || node.isNodeType(HippoNodeType.HIPPO_NODETYPE)
                || node.isNodeType(HippoNodeType.HIPPO_PROTOTYPE)) {
            Node type = node;
            while (type != null && !type.isNodeType(HippoNodeType.NT_TEMPLATETYPE)) {
                type = type.getParent();
            }
            if (type != null) {
                type.save();
                String[] names = { HippoNodeType.HIPPO_TEMPLATE, HippoNodeType.HIPPO_NODETYPE,
                        HippoNodeType.HIPPO_PROTOTYPE };
                for (String name : names) {
                    Node draft = getVersion(type, name, "draft");
                    if (draft != null) {
                        draft.save();
                    }
                }
            }
        }
    }

    public String copy(String name) throws WorkflowException, MappingException, RepositoryException {
        if (!node.isNodeType(HippoNodeType.NT_TEMPLATETYPE))
            throw new MappingException("invalid node type for EditmodelWorkflow");

        String path = node.getPath();
        path = path.substring(0, path.lastIndexOf("/") + 1);
        path += name;
        Node target = (Node) rootSession.getItem(node.getPath());
        target = ((HippoSession) rootSession).copy(target, path);
        target.getParent().save();
        return target.getPath();
    }

    private Node getVersion(Node node, String name, String version) throws RepositoryException {
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
        return null;
    }
}
