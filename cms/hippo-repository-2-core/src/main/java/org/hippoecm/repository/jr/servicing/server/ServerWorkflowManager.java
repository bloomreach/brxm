/*
  THIS CODE IS UNDER CONSTRUCTION, please leave as is until
  work has proceeded to a stable level, at which time this comment
  will be removed.  -- Berry
*/

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
package org.hippoecm.repository.jr.servicing.server;

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.rmi.server.ServerObject;

import org.hippoecm.repository.workflow.Workflow;
import org.hippoecm.repository.workflow.WorkflowDescriptor;
import org.hippoecm.repository.jr.servicing.WorkflowManager;
import org.hippoecm.repository.jr.servicing.remote.RemoteWorkflowManager;
import org.hippoecm.repository.jr.servicing.remote.RemoteServicingAdapterFactory;

public class ServerWorkflowManager extends ServerObject
  implements RemoteWorkflowManager
{
  private WorkflowManager workflowManager;

  public ServerWorkflowManager(WorkflowManager manager, RemoteServicingAdapterFactory factory) throws RemoteException {
    super(factory);
    this.workflowManager = manager;
  }

  public WorkflowDescriptor getWorkflowDescriptor(String category, String absPath)
    throws RepositoryException, RemoteException
  {
    try {
      Node node = workflowManager.getSession().getRootNode().getNode(absPath);
      return workflowManager.getWorkflowDescriptor(category, node);
    } catch(RepositoryException ex) {
      throw getRepositoryException(ex);
    }

  }

  public Workflow getWorkflow(String category, String absPath)
    throws RepositoryException, RemoteException
  {
    try {
      absPath = absPath.substring(1); // skip leading slash
      Node node = workflowManager.getSession().getRootNode().getNode(absPath);
      return workflowManager.getWorkflow(category, node);
    } catch(RepositoryException ex) {
      throw getRepositoryException(ex);
    }
  }

  public Workflow getWorkflow(WorkflowDescriptor descriptor)
    throws RepositoryException, RemoteException
  {
    try {
      return workflowManager.getWorkflow(descriptor);
    } catch(RepositoryException ex) {
      throw getRepositoryException(ex);
    }
  }
}
