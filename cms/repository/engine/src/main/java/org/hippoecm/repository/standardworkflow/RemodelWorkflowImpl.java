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

import java.io.StringReader;
import java.rmi.RemoteException;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.InternalWorkflow;

public class RemodelWorkflowImpl implements RemodelWorkflow, InternalWorkflow {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private Session session;
    private Node subject;

    public RemodelWorkflowImpl(Session userSession, Session rootSession, Node subject) throws RemoteException, RepositoryException {
        this.session = rootSession;
        this.subject = rootSession.getRootNode().getNode(subject.getPath().substring(1));
    }

    public void createNamespace(String prefix, String namespace) throws WorkflowException, MappingException,
            RepositoryException {
        try {
           // push new node type definition such that it will be loaded
           Node node, base = session.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH).getNode(HippoNodeType.INITIALIZE_PATH);
            if (base.hasNode(prefix)) {
                node = base.getNode(prefix);
            } else {
                node = base.addNode(prefix, HippoNodeType.NT_INITIALIZEITEM);
            }
            node.setProperty(HippoNodeType.HIPPO_NAMESPACE, namespace + "/0.0");
            session.save();

            // wait for node types to be reloaded
            session.refresh(true);
            while (node.hasProperty(HippoNodeType.HIPPO_NAMESPACE)) {
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
    }

    public String[] updateModel(String prefix, String cnd) throws WorkflowException, MappingException,
            RepositoryException, RemoteException
    {
         try {
            StringReader istream = new StringReader(cnd);
            Remodeling remodel = Remodeling.remodel(session, prefix, istream);
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

    public String[] updateModel(String prefix, String cnd, String contentUpdater, Object contentUpdaterCargo) throws WorkflowException, MappingException,
            RepositoryException, RemoteException
    {
        try {
            StringReader istream = new StringReader(cnd);
            Remodeling remodel = Remodeling.remodel(session, prefix, istream, contentUpdater, contentUpdaterCargo);
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
