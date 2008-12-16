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
package org.hippoecm.repository.reviewedactions;

import java.rmi.RemoteException;
import java.util.Date;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.hippoecm.repository.standardworkflow.VersionWorkflow;

public class FullReviewedActionsWorkflowImpl extends BasicReviewedActionsWorkflowImpl implements FullReviewedActionsWorkflow {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public FullReviewedActionsWorkflowImpl() throws RemoteException {
    }

    public void delete() throws WorkflowException {
        ReviewedActionsWorkflowImpl.log.info("deletion on document ");
        if(current != null)
            throw new WorkflowException("cannot delete document with pending publication request");
        if(current2 != null)
            throw new WorkflowException("cannot delete document with pending depublication request");
        if(published != null)
            throw new WorkflowException("cannot delete published document");
        if(draft != null)
            throw new WorkflowException("cannot delete document being edited");
        doDelete();
    }

    public void doDelete() throws WorkflowException {
        unpublished = draft = null;
    }

    public void rename(String newName) throws MappingException, RemoteException, WorkflowException, RepositoryException {
        ReviewedActionsWorkflowImpl.log.info("rename on document ");
        if(current != null)
            throw new WorkflowException("cannot rename document with pending publication request");
        if(current2 != null)
            throw new WorkflowException("cannot rename document with pending depublication request");
        doDepublish();
        DefaultWorkflow defaultWorkflow = (DefaultWorkflow) getWorkflowContext().getWorkflow("core", unpublished);
        defaultWorkflow.rename(newName);
    }

    public void publish() throws WorkflowException, MappingException {
        ReviewedActionsWorkflowImpl.log.info("publication on document ");
        if(unpublished == null) {
            if(published == null) {
                throw new WorkflowException("No unpublished version of document available for publication");
            } else {
                throw new WorkflowException("Document has already been published");
            }
        }
        if(current != null)
            throw new WorkflowException("cannot publish document with pending publication request");
        if(current2 != null)
            throw new WorkflowException("cannot publish document with pending depublication request");
        doPublish();
    }

    public void doPublish() throws WorkflowException, MappingException {
        published = null;
        unpublished.setState(PublishableDocument.PUBLISHED);
        try {
            VersionWorkflow versionWorkflow = (VersionWorkflow) getWorkflowContext().getWorkflow("versioning", unpublished);
            versionWorkflow.version();
        } catch(MappingException ex) {
            ReviewedActionsWorkflowImpl.log.warn(ex.getClass().getName()+": "+ex.getMessage(), ex);
        } catch(RemoteException ex) {
            ReviewedActionsWorkflowImpl.log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
            throw new WorkflowException("Versioning of published document failed");
        } catch(RepositoryException ex) {
            ReviewedActionsWorkflowImpl.log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
            throw new WorkflowException("Versioning of published document failed");
        }
    }

    public void depublish() throws WorkflowException {
        ReviewedActionsWorkflowImpl.log.info("depublication on document ");
        if(current != null)
            throw new WorkflowException("cannot depublish document with pending publication request");
        if(current2 != null)
            throw new WorkflowException("cannot depublish document with pending depublication request");
        doDepublish();
    }

    void doDepublish() throws WorkflowException {
        try {
            if(unpublished == null) {
                unpublished = (PublishableDocument) published.clone();
                unpublished.state = PublishableDocument.UNPUBLISHED;
            }
            published = null;
            try {
                VersionWorkflow versionWorkflow = (VersionWorkflow) getWorkflowContext().getWorkflow("versioning", unpublished);
                versionWorkflow.version();
            } catch(MappingException ex) {
                ReviewedActionsWorkflowImpl.log.warn(ex.getClass().getName()+": "+ex.getMessage(), ex);
                throw new WorkflowException("Versioning of published document failed");
            } catch(RemoteException ex) {
                ReviewedActionsWorkflowImpl.log.warn(ex.getClass().getName()+": "+ex.getMessage(), ex);
                throw new WorkflowException("Versioning of published document failed");
            } catch(RepositoryException ex) {
                ReviewedActionsWorkflowImpl.log.warn(ex.getClass().getName()+": "+ex.getMessage(), ex);
                throw new WorkflowException("Versioning of published document failed");
            }
        } catch(CloneNotSupportedException ex) {
            ReviewedActionsWorkflowImpl.log.warn(ex.getClass().getName()+": "+ex.getMessage(), ex);
            throw new WorkflowException("document is not a publishable document");
        }
    }

    public void publish(Date publicationDate) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        doSchedPublish(publicationDate);
    }

    public void depublish(Date depublicationDate) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        doSchedDepublish(depublicationDate);
    }

    void doSchedPublish(Date publicationDate) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        WorkflowContext wfCtx = getWorkflowContext();
        wfCtx = wfCtx.getWorkflowContext(publicationDate);

        FullReviewedActionsWorkflow wf = (FullReviewedActionsWorkflow) wfCtx.getWorkflow("default");
        wf.publish();
    }

    void doSchedDepublish(Date depublicationDate) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        WorkflowContext wfCtx = getWorkflowContext();
        wfCtx = wfCtx.getWorkflowContext(depublicationDate);
        FullReviewedActionsWorkflow wf = (FullReviewedActionsWorkflow) wfCtx.getWorkflow("default");
        wf.depublish();
    }
}
