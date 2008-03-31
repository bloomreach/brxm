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

import javax.jcr.Node;
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

        Node template = node.getNode(HippoNodeType.HIPPO_TEMPLATE);
        template = template.getNode(HippoNodeType.HIPPO_TEMPLATE);
        return template.getPath();
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
}
