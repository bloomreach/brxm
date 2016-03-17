/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.RepositoryMap;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.InternalWorkflow;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;
import static org.hippoecm.repository.util.WorkflowUtils.getContainingFolder;

public class DefaultWorkflowImpl implements DefaultWorkflow, EditableWorkflow, InternalWorkflow {

    private static final Logger log = LoggerFactory.getLogger(DefaultWorkflowImpl.class);
    private static final long serialVersionUID = 1L;

    private WorkflowContext context;
    private Document document;
    private Node subject;
    
    public DefaultWorkflowImpl(WorkflowContext context, Session userSession, Session rootSession, Node subject) throws RepositoryException {
        this.context = context;
        this.document = new Document(subject);
        this.subject = rootSession.getNodeByIdentifier(subject.getIdentifier());
    }

    private WorkflowContext getWorkflowContext() {
        return context;
    }

    public Map<String,Serializable> hints() {
        Map<String, Serializable> map = new TreeMap<>();
        map.put("checkModified", true);
        return map;
    }

    public Document obtainEditableInstance()
            throws WorkflowException, MappingException, RepositoryException, RemoteException {
        return document;
    }

    public Document commitEditableInstance()
            throws WorkflowException, MappingException, RepositoryException, RemoteException {
        return document;
    }

    public Document disposeEditableInstance()
            throws WorkflowException, MappingException, RepositoryException, RemoteException {
        throw new WorkflowException("Document type does not allow for reverting changes");
    }

    @Override
    public boolean isModified() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        final HippoSession session = (HippoSession) context.getUserSession();
        final Node node = document.getNode(session);
        return session.pendingChanges(node, "nt:base", true).hasNext();
    }

    public void delete() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Workflow workflow = getWorkflowContext().getWorkflow(getFolderWorkflowCategory(), new Document(getContainingFolder(subject)));
        if(workflow instanceof FolderWorkflow) {
            ((FolderWorkflow) workflow).delete(document);
        } else {
            throw new WorkflowException("Cannot delete document that is not contained in a folder");
        }
    }

    private String getFolderWorkflowCategory() {
        String folderWorkflowCategory = "internal";
        RepositoryMap config = getWorkflowContext().getWorkflowConfiguration();
        if (config != null && config.exists() && config.get("folder-workflow-category") instanceof String) {
            folderWorkflowCategory = (String) config.get("folder-workflow-category");
        }
        return folderWorkflowCategory;
    }

    public void archive() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Document folder = new Document(getContainingFolder(subject));
        Workflow workflow = getWorkflowContext().getWorkflow(getFolderWorkflowCategory(), folder);
        if (workflow instanceof FolderWorkflow) {
            ((FolderWorkflow)workflow).archive(document);
        } else {
            throw new WorkflowException("cannot archive document which is not contained in a folder");
        }
    }

    public void rename(String newName) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Document folder = new Document(getContainingFolder(subject));
        Workflow workflow = getWorkflowContext().getWorkflow(getFolderWorkflowCategory(), folder);
        if (workflow instanceof FolderWorkflow) {
            ((FolderWorkflow)workflow).rename(document, newName);
        } else {
            throw new WorkflowException("cannot rename document which is not contained in a folder");
        }
    }

    @Override
    public void setDisplayName(final String hippoName) throws WorkflowException, RepositoryException, RemoteException {
        Node node;
        if (subject.isNodeType(NT_HANDLE)) {
            node = subject;
        } else {
            final Node parent = subject.getParent();
            if (parent.isNodeType(NT_HANDLE)) {
                node = parent;
            } else {
                node = subject;
            }
        }
        JcrUtils.ensureIsCheckedOut(node);
        if (!node.isNodeType(HippoNodeType.NT_NAMED)) {
            node.addMixin(HippoNodeType.NT_NAMED);
        }
        node.setProperty(HippoNodeType.HIPPO_NAME, hippoName);
        node.getSession().save();
    }

    public void copy(Document destination, String newName) throws MappingException, RemoteException, WorkflowException, RepositoryException {
        Document folder = new Document(getContainingFolder(subject));
        Workflow workflow = getWorkflowContext().getWorkflow(getFolderWorkflowCategory(), destination);
        if(workflow instanceof EmbedWorkflow)
            ((EmbedWorkflow)workflow).copyTo(folder, document, newName, null);
        else
            throw new WorkflowException("cannot copy document which is not contained in a folder");
    }

    public void move(Document destination, String newName) throws MappingException, RemoteException, WorkflowException, RepositoryException {
        Document folder = new Document(getContainingFolder(subject));
        Workflow workflow = getWorkflowContext().getWorkflow(getFolderWorkflowCategory(), folder);
        if(workflow instanceof FolderWorkflow)
            ((FolderWorkflow)workflow).move(document, destination, newName);
        else
            throw new WorkflowException("cannot move document which is not contained in a folder");
    }

}
