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
package org.hippoecm.repository.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;

import org.hippoecm.repository.servicing.ServicingNode;
import org.hippoecm.repository.servicing.WorkflowManager;
import org.hippoecm.repository.servicing.ServicesManagerImpl;
import org.hippoecm.repository.servicing.Service;
import org.hippoecm.repository.servicing.ServicingWorkspace;

public class WorkflowManagerImpl
  implements WorkflowManager
{
  private final Logger log = LoggerFactory.getLogger(Workflow.class);

  Session session;
  String configuration;
  public WorkflowManagerImpl(Session session) {
    this.session = session;
    // FIXME
    try {
      configuration = session.getRootNode().getNode("configuration/workflows").getUUID();
    } catch(RepositoryException ex) {
      // FIXME
      System.err.println("RepositoryException: "+ex.getMessage());
      ex.printStackTrace(System.err);
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
      return new WorkflowDescriptor(this, category, workflowNode);
    } else {
      log.debug("Workflow for category "+category+" on "+item.getPath()+" is not available");
      return null;
    }
  }
  public Workflow getWorkflow(WorkflowDescriptor descriptor) throws RepositoryException {
    try {
      return getWorkflow(descriptor.category, session.getRootNode().getNode(descriptor.nodeAbsPath.substring(1)));
    } catch(PathNotFoundException ex) {
      log.debug("Workflow no longer available "+descriptor.nodeAbsPath);
      return null;
    }
  }
  public Workflow getWorkflow(String category, Node item) throws RepositoryException {
    Node workflowNode = getWorkflowNode(category, item);
    if(workflowNode != null) {
      try {
        String classname = workflowNode.getProperty("service").getString();
        Node types = workflowNode.getNode("types");
        ServicesManagerImpl manager = (ServicesManagerImpl)((ServicingWorkspace)session.getWorkspace()).getServicesManager();
        Service service = manager.getService(item.getUUID(), classname, types);
        Workflow workflow = (Workflow) service;
        if(workflow instanceof WorkflowImpl) {
          ((WorkflowImpl)workflow).setWorkflowContext(new WorkflowContext(session));
        }
        return workflow;
      } catch(PathNotFoundException ex) {
        log.error("Workflow specification corrupt on node " + workflowNode.getPath());
        throw new RepositoryException("workflow specification corrupt", ex);
      } catch(ValueFormatException ex) {
        log.error("Workflow specification corrupt on node " + workflowNode.getPath());
        throw new RepositoryException("workflow specification corrupt", ex);
      }
    } else {
      log.debug("Workflow for category "+category+" on "+item.getPath()+" is not available");
      return null;
    }
  }
}
