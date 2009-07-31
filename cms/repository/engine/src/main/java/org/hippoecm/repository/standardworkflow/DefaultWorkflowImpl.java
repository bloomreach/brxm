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

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.RepositoryMap;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.DocumentManager;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.ext.InternalWorkflow;

public class DefaultWorkflowImpl implements DefaultWorkflow, EditableWorkflow, InternalWorkflow {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    Document document;
    Session session;
    Node subject;
    Session rootSession; // FIXME; having the need for a rootSession is PLAIN WRONG, however because the document manager cannot read its own confiugration because of authorization rules, editors and authors cannot access documents

    public DefaultWorkflowImpl(Session userSession, Session rootSession, Node subject) throws RepositoryException {
        document = new Document(subject.getUUID());
        this.subject = subject;
        this.session = rootSession; // FIXME SHOULD BE THE USERSESSION!
        this.rootSession = rootSession;
    }

    private WorkflowContext getWorkflowContext() {
        try {
            final DocumentManager documentManager = ((HippoWorkspace)rootSession.getWorkspace()).getDocumentManager();
            final WorkflowManager workflowManager = ((HippoWorkspace)session.getWorkspace()).getWorkflowManager();
            return new WorkflowContext() {
                public WorkflowContext getWorkflowContext(Object jobSpecification) throws MappingException, RepositoryException {
                    throw new MappingException("No workflow context defined for class "+jobSpecification.getClass().getName());
                }
                public RepositoryMap getWorkflowConfiguration() {
                    return null;
                }
                public Document getDocument(String category, String identifier) throws RepositoryException {
                    return documentManager.getDocument(category, identifier);
                }

                public Workflow getWorkflow(String category, Document document) throws MappingException, WorkflowException, RepositoryException {
                    return workflowManager.getWorkflow(category, session.getNodeByUUID(document.getIdentity()));
                }

                public Workflow getWorkflow(String category) throws MappingException, WorkflowException, RepositoryException {
                    return workflowManager.getWorkflow(category, subject);
                }

                public String getUserIdentity() {
                    return session.getUserID();
                }
            };
        } catch (RepositoryException ex) {
            return null;
        }
    }

    public Map<String,Serializable> hints() {
        return null;
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

    public void delete() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Document folder = getWorkflowContext().getDocument("embedded", document.getIdentity());
        Workflow workflow = getWorkflowContext().getWorkflow("internal", folder);
        if(workflow instanceof FolderWorkflow)
            ((FolderWorkflow)workflow).delete(document);
        else
            throw new WorkflowException("cannot delete document which is not contained in a folder");
    }

    public void archive() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Document folder = getWorkflowContext().getDocument("embedded", document.getIdentity());
        Workflow workflow = getWorkflowContext().getWorkflow("internal", folder);
        if(workflow instanceof FolderWorkflow)
            ((FolderWorkflow)workflow).archive(document);
        else
            throw new WorkflowException("cannot delete document which is not contained in a folder");
    }

    public void rename(String newName) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Document folder = getWorkflowContext().getDocument("embedded", document.getIdentity());
        Workflow workflow = getWorkflowContext().getWorkflow("internal", folder);
        if(workflow instanceof FolderWorkflow)
            ((FolderWorkflow)workflow).rename(document, newName);
        else
            throw new WorkflowException("cannot delete document which is not contained in a folder");
    }

    public void copy(Document destination, String newName) throws MappingException, RemoteException, WorkflowException, RepositoryException {
        Document folder = getWorkflowContext().getDocument("embedded", document.getIdentity());
        Workflow workflow = getWorkflowContext().getWorkflow("internal", folder);
        if(workflow instanceof FolderWorkflow)
            ((FolderWorkflow)workflow).copy(document, destination, newName);
        else
            throw new WorkflowException("cannot copy document which is not contained in a folder");
    }

    public void move(Document destination, String newName) throws MappingException, RemoteException, WorkflowException, RepositoryException {
        Document folder = getWorkflowContext().getDocument("embedded", document.getIdentity());
        Workflow workflow = getWorkflowContext().getWorkflow("internal", folder);
        if(workflow instanceof FolderWorkflow)
            ((FolderWorkflow)workflow).move(document, destination, newName);
        else
            throw new WorkflowException("cannot move document which is not contained in a folder");
    }

}
