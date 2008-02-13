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

import java.io.StringBufferInputStream;
import java.rmi.RemoteException;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.servicing.Remodeling;

public class RemodelWorkflowImpl implements RemodelWorkflow {
    private Session userSession;
    private Node subject;

    public RemodelWorkflowImpl(Session userSession, Session rootSession, Node subject) throws RemoteException {
        this.userSession = userSession;
        this.subject = subject;
    }

    public String[] remodel(String cnd) throws WorkflowException, MappingException, RepositoryException {
        if (!subject.isNodeType(HippoNodeType.NT_NAMESPACE))
            throw new MappingException("invalid node type for RemodelWorkflow");

        try {
            String prefix = subject.getName();

            StringBufferInputStream istream = new StringBufferInputStream(cnd);
            Remodeling remodel = Remodeling.remodel(userSession, prefix, istream);
            NodeIterator iter = remodel.getNodes();
            String[] paths = new String[(int) iter.getSize()];
            for (int i = 0; iter.hasNext(); i++) {
                paths[i] = iter.nextNode().getPath();
            }
            return paths;
        } catch (NamespaceException ex) {
            throw new RepositoryException(ex);
        }
    }
}
