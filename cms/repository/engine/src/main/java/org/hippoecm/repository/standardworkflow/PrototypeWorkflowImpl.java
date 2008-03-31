/*
 * Copyright 2008 Hippo
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
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.WorkflowException;

public class PrototypeWorkflowImpl implements PrototypeWorkflow {
    private static final long serialVersionUID = 1L;

    private Session userSession;
    private Node subject;

    public PrototypeWorkflowImpl(Session userSession, Session rootSession, Node subject) throws RemoteException {
        this.subject = subject;
        this.userSession = userSession;
    }

    public String addDocument(String name) throws WorkflowException, RepositoryException, RemoteException {
        if (!subject.isNodeType(HippoNodeType.NT_PROTOTYPED))
            throw new WorkflowException("Invalid node type for workflow");

        String path =  subject.getProperty(HippoNodeType.HIPPO_PROTOTYPE).getString();
        Node prototype = userSession.getRootNode().getNode(path.substring(1));
        Node result = ((HippoSession) userSession).copy(prototype, subject.getPath() + "/" + name);
        if (result.isNodeType(HippoNodeType.NT_HANDLE)) {
            NodeIterator children = result.getNodes(prototype.getName());
            while (children.hasNext()) {
                Node child = children.nextNode();
                if (child.getName().equals(prototype.getName())) {
                    userSession.move(child.getPath(), result.getPath() + "/" + name);
                }
            }
        }
        subject.save();
        return result.getPath();
    }
}