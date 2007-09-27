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
package org.hippoecm.repository.servicing;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowManagerImpl implements WorkflowManager {
    final Logger log = LoggerFactory.getLogger(Workflow.class);

    class Entry {
        Workflow workflow;
        String uuid;
        Node types;
        Entry(Workflow workflow, String uuid, Node types) {
            this.workflow = workflow;
            this.uuid     = uuid;
            this.types    = types;
        }
    }

    Session session;
    private List<Entry> usedWorkflows;
    String configuration;

    public WorkflowManagerImpl(Session session) {
        this.session = session;
        usedWorkflows = new LinkedList<Entry>();
        try {
            configuration = session.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH + "/" +
                                                          HippoNodeType.WORKFLOWS_PATH).getUUID();
        } catch(RepositoryException ex) {
            log.error("workflow manager configuration failed: "+ex.getMessage());
        }
    }
    
    private Node getWorkflowNode(String category, Node item) {
    	if(configuration == null) {
    	    return null;
    	}
        try {
            log.debug("looking for workflow in category " + category + " for node " + (item == null ? "<none>" : item.getPath()));
            Node node = session.getNodeByUUID(configuration);
            if (node.hasNode(category)) {
                node = node.getNode(category);
                Node workflowNode = null;
                for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                    workflowNode = iter.nextNode();
                    log.debug("matching item type against " + workflowNode.getProperty(HippoNodeType.HIPPO_NODETYPE).getString());
                    if (item.isNodeType(workflowNode.getProperty(HippoNodeType.HIPPO_NODETYPE).getString())) {
                        log.debug("found workflow in category " + category + " for node " + (item == null ? "<none>" : item.getPath()));
                        return workflowNode;
                    }
                }
            } else {
                log.debug("workflow in category " + category + " for node " + (item == null ? "<none>" : item.getPath()) + " not found");                
            }
        } catch (ItemNotFoundException ex) {
            log.error("workflow category does not exist or workflows definition missing " + ex.getMessage());
        } catch (PathNotFoundException ex) {
            log.error("workflow category does not exist or workflows definition missing " + ex.getMessage());
        } catch (ValueFormatException ex) {
            log.error("misconfiguration of workflow definition");
        } catch (RepositoryException ex) {
            log.error("generic error accessing workflow definitions " + ex.getMessage());
        }
        return null;
    }

    public WorkflowManagerImpl(Session session, String uuid) {
        this.session = session;
        configuration = uuid;
    }

    void save(Workflow workflow, String uuid, Node types) throws RepositoryException {
        HippoWorkspace workspace = (HippoWorkspace) session.getWorkspace();
        DocumentManagerImpl documentManager = (DocumentManagerImpl) workspace.getDocumentManager();
        ((org.hippoecm.repository.servicing.WorkflowImpl)workflow).post(); // FIXME: workaround for current mapping issues
        documentManager.putObject(uuid, types, workflow);
    }

    void save() throws RepositoryException {
        for(Iterator<Entry> iter = usedWorkflows.iterator(); iter.hasNext(); ) {
            Entry entry = iter.next();
            save(entry.workflow, entry.uuid, entry.types);
        }
        // FIXME: this assumes that Workflows are no longer used after a session.save()
        usedWorkflows.clear();
    }

    public Session getSession() throws RepositoryException {
        return session;
    }

    public WorkflowDescriptor getWorkflowDescriptor(String category, Node item) throws RepositoryException {
        Node workflowNode = getWorkflowNode(category, item);
        if(workflowNode != null) {
            return new WorkflowDescriptorImpl(this, category, workflowNode);
        } else {
            log.debug("Workflow for category "+category+" on "+item.getPath()+" is not available");
            return null;
        }
    }

    public Workflow getWorkflow(WorkflowDescriptor descriptor) throws RepositoryException {
        WorkflowDescriptorImpl descriptorImpl = (WorkflowDescriptorImpl) descriptor;
        try {
            String path = descriptorImpl.nodeAbsPath.substring(1);
            if(path.startsWith("/"))
                path = path.substring(1);
            return getWorkflow(descriptorImpl.category, session.getRootNode().getNode(path));
        } catch(PathNotFoundException ex) {
            log.debug("Workflow no longer available "+descriptorImpl.nodeAbsPath);
            return null;
        }
    }

    public Workflow getWorkflow(String category, Node item) throws RepositoryException {
        Node workflowNode = getWorkflowNode(category, item);
        if(workflowNode != null) {
            try {
                String classname = workflowNode.getProperty(HippoNodeType.HIPPO_SERVICE).getString();
                Node types = workflowNode.getNode(HippoNodeType.HIPPO_TYPES);
                DocumentManagerImpl manager = (DocumentManagerImpl) ((HippoWorkspace)session.getWorkspace())
                    .getDocumentManager();
                String uuid = item.getUUID();
                Object object = manager.getObject(uuid, classname, types);
                Workflow workflow = (Workflow) object;
                usedWorkflows.add(new Entry(workflow, uuid, types));
                if(workflow instanceof WorkflowImpl) {
                    ((WorkflowImpl)workflow).setWorkflowContext(new WorkflowContext(session));
                    ((WorkflowImpl)workflow).pre();
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
