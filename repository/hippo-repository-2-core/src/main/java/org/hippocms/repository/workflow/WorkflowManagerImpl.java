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
package org.hippocms.repository.workflow;

import java.rmi.RemoteException;

import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.ValueFormatException;
import javax.jcr.RepositoryException;

import org.hippocms.repository.jr.servicing.ServicingNode;
import org.hippocms.repository.jr.servicing.WorkflowManager;
 
import org.hippocms.repository.workflow.Workflow;
import org.hippocms.repository.workflow.WorkflowDescriptor;

public class WorkflowManagerImpl
  implements WorkflowManager
{
  Session session;
  String configuration;
  public WorkflowManagerImpl(Session session) {
    this.session = session;
    // FIXME
    try {
      configuration = session.getRootNode().getNode("configuration/workflows").getUUID();
    } catch(RepositoryException ex) {
    }
  }

  private Node getWorkflowNode(String category, Node item) {
    try {
      Node node = session.getNodeByUUID(configuration);
      node = node.getNode(category);
      Node workflowNode = null;
      for(NodeIterator iter = node.getNodes(); iter.hasNext(); ) {
        workflowNode = iter.nextNode();
        if(item.isNodeType(workflowNode.getProperty("nodetype").getString())) {
          return workflowNode;
        }
      }
    } catch(ItemNotFoundException ex) {
      // FIXME
    } catch(PathNotFoundException ex) {
      // FIXME
    } catch(ValueFormatException ex) {
      // FIXME
    } catch(RepositoryException ex) {
      // FIXME
    }
    return null;
  }

  public WorkflowManagerImpl(Session session, String uuid) {
    this.session = session;
    configuration = uuid;
  }
  public Session getSession() throws RepositoryException {
    return session;
  }

  public WorkflowDescriptor getWorkflowDescriptor(String category, Node item) throws RepositoryException {
    Node workflowNode = getWorkflowNode(category, item);
    if(workflowNode != null) {
      return new WorkflowDescriptor(this, category, item);
    } else {
      return null;
    }
  }
  public Workflow getWorkflow(WorkflowDescriptor descriptor) throws RepositoryException {
    try {
      return getWorkflow(descriptor.category, session.getRootNode().getNode(descriptor.nodeAbsPath));
    } catch(PathNotFoundException ex) {
      // FIXME
      return null;
    }
  }
  public Workflow getWorkflow(String category, Node item) throws RepositoryException {
    Node workflowNode = getWorkflowNode(category, item);
    if(workflowNode != null) {
      try {
        String serviceName = workflowNode.getProperty("service").getString();
        Workflow workflow = (Workflow) ((ServicingNode)item).getService(/*serviceName*/); // FIXME
        if(workflow instanceof WorkflowImpl) {
          ((WorkflowImpl)workflow).setWorkflowContext(new WorkflowContext(session));
        }
        return workflow;
      } catch(PathNotFoundException ex) {
        // FIXME
        return null;
      } catch(ValueFormatException ex) {
        // FIXME
        return null;
      }
    } else {
      return null;
    }
  }
}
