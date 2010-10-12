/*
 *  Copyright 2008-2010 Hippo.
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

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.RepositoryMap;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.hippoecm.repository.standardworkflow.VersionWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;

public class FullReviewedActionsWorkflowImpl extends BasicReviewedActionsWorkflowImpl implements FullReviewedActionsWorkflow {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public FullReviewedActionsWorkflowImpl() throws RemoteException {
    }

    @Override
    public Map<String,Serializable> hints()  {
        Map<String,Serializable> info = super.hints();
        info.put("rename", info.get("delete"));
        info.put("move", info.get("delete"));
        info.put("copy", (unpublishedDocument != null && PublishableDocument.UNPUBLISHED.equals(state))
                || (unpublishedDocument == null && publishedDocument != null && PublishableDocument.PUBLISHED.equals(state)));
        return info;
    }

    public void delete() throws WorkflowException {
        ReviewedActionsWorkflowImpl.log.info("deletion on document ");
        if(current != null)
            throw new WorkflowException("cannot delete document with pending request");
        if(publishedDocument != null)
            throw new WorkflowException("cannot delete published document");
        if(draftDocument != null)
            throw new WorkflowException("cannot delete document being edited");
        doDelete();
    }

    public void publish(Date publicationDate, Date depublicationDate) throws WorkflowException {
        ReviewedActionsWorkflowImpl.log.info("publication on document ");
        throw new WorkflowException("unsupported");
    }

   public void publish(Date publicationDate) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        doSchedPublish(publicationDate);
    }

    public void depublish(Date depublicationDate) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        doSchedDepublish(depublicationDate);
    }

    public void doDelete() throws WorkflowException {
        /* Previous behaviour was to let the handle exists, and only delete all variants.  This is still the best option
         * especially when there are multiple language variants.  Then the document should remain existing.  For now,
         * that behaviour which was implemented with just:
         *    unpublished = draft = null;
         * is removed and we will archive the document.
         */
        try {
            DefaultWorkflow defaultWorkflow = (DefaultWorkflow) getWorkflowContext().getWorkflow("core", unpublishedDocument);
            defaultWorkflow.archive();
        } catch(MappingException ex) {
            ReviewedActionsWorkflowImpl.log.warn("invalid default workflow, falling back in behaviour", ex);
            unpublishedDocument = draftDocument = null;
        } catch(WorkflowException ex) {
            ReviewedActionsWorkflowImpl.log.warn("no default workflow for published documents, falling back in behaviour", ex);
            unpublishedDocument = draftDocument = null;
        } catch(RepositoryException ex) {
            ReviewedActionsWorkflowImpl.log.warn("exception trying to archive document, falling back in behaviour", ex);
            unpublishedDocument = draftDocument = null;
        } catch(RemoteException ex) {
            ReviewedActionsWorkflowImpl.log.warn("exception trying to archive document, falling back in behaviour", ex);
            unpublishedDocument = draftDocument = null;
        }
    }

    public void copy(Document destination, String newName) throws MappingException, RemoteException, WorkflowException, RepositoryException {
        ReviewedActionsWorkflowImpl.log.info("copy document");

        if(publishedDocument == null && unpublishedDocument == null)
            throw new WorkflowException("cannot copy unsaved document");

        String folderWorkflowCategory = getFolderWorkflowCategory();
        if (unpublishedDocument == null) {
            Document folder = getWorkflowContext().getDocument("embedded", publishedDocument.getIdentity());
            Workflow workflow = getWorkflowContext(null).getWorkflow(folderWorkflowCategory, folder);
            if (workflow instanceof FolderWorkflow) {
                Document copy = ((FolderWorkflow)workflow).copy(publishedDocument, destination, newName);
                FullReviewedActionsWorkflow copiedDocumentWorkflow = (FullReviewedActionsWorkflow) getWorkflowContext(null).getWorkflow("default", copy);
                copiedDocumentWorkflow.depublish();
            } else
                throw new WorkflowException("cannot copy document which is not contained in a folder");
        } else {
            Document folder = getWorkflowContext().getDocument("embedded", unpublishedDocument.getIdentity());
            Workflow workflow = getWorkflowContext().getWorkflow(folderWorkflowCategory, folder);
            if(workflow instanceof FolderWorkflow)
                ((FolderWorkflow)workflow).copy(unpublishedDocument, destination, newName);
            else
                throw new WorkflowException("cannot copy document which is not contained in a folder");
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

    public void move(Document destination, String newName) throws MappingException, RemoteException, WorkflowException, RepositoryException {
        ReviewedActionsWorkflowImpl.log.info("move document");
        if(current != null)
            throw new WorkflowException("cannot move document with pending request");
        if(publishedDocument != null)
            throw new WorkflowException("cannot move published document");
        if(draftDocument != null)
            throw new WorkflowException("cannot move document being edited");

        Document folder = getWorkflowContext().getDocument("embedded", unpublishedDocument.getIdentity());
        Workflow workflow = getWorkflowContext().getWorkflow(getFolderWorkflowCategory(), folder);
        if(workflow instanceof FolderWorkflow)
            ((FolderWorkflow)workflow).move(unpublishedDocument, destination, newName);
        else
            throw new WorkflowException("cannot move document which is not contained in a folder");
    }

    public void rename(String newName) throws MappingException, RemoteException, WorkflowException, RepositoryException {
        ReviewedActionsWorkflowImpl.log.info("rename on document ");
        if(current != null)
            throw new WorkflowException("cannot rename document with pending request");
        if(publishedDocument != null || unpublishedDocument == null)
            throw new WorkflowException("cannot rename published document");
        if(draftDocument != null)
            throw new WorkflowException("cannot rename document being edited");
        // doDepublish();
        DefaultWorkflow defaultWorkflow = (DefaultWorkflow) getWorkflowContext().getWorkflow("core", unpublishedDocument);
        defaultWorkflow.rename(newName);
    }

    public void publish() throws WorkflowException, MappingException {
        ReviewedActionsWorkflowImpl.log.info("publication on document ");
        if(unpublishedDocument == null) {
            if(publishedDocument == null) {
                throw new WorkflowException("No unpublished version of document available for publication");
            } else {
                throw new WorkflowException("Document has already been published");
            }
        }
        if(draftDocument != null)
            throw new WorkflowException("cannot publish document being edited");
        doPublish();
    }

    public void doPublish() throws WorkflowException, MappingException {
        publishedDocument = null;
        unpublishedDocument.setState(PublishableDocument.PUBLISHED);
        unpublishedDocument.setPublicationDate(new Date());
        unpublishedDocument.availability = new String[] { "live", "preview" };
        try {
            VersionWorkflow versionWorkflow = (VersionWorkflow) getWorkflowContext().getWorkflow("versioning", unpublishedDocument);
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
        if(draftDocument != null)
            throw new WorkflowException("cannot publish document being edited");
        doDepublish();
    }

    void doDepublish() throws WorkflowException {
        try {
            VersionWorkflow versionWorkflow;
            if(unpublishedDocument == null) {
                publishedDocument.state = PublishableDocument.UNPUBLISHED;
                publishedDocument.availability = new String[] { "preview" };
                versionWorkflow = (VersionWorkflow) getWorkflowContext().getWorkflow("versioning", publishedDocument);
            } else {
                publishedDocument = null;
                versionWorkflow = (VersionWorkflow) getWorkflowContext().getWorkflow("versioning", unpublishedDocument);
            }
            try {
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
        } catch(MappingException ex) {
            ReviewedActionsWorkflowImpl.log.warn(ex.getClass().getName()+": "+ex.getMessage(), ex);
            throw new WorkflowException("Versioning of published document failed");
        } catch(RepositoryException ex) {
            ReviewedActionsWorkflowImpl.log.warn(ex.getClass().getName()+": "+ex.getMessage(), ex);
            throw new WorkflowException("Versioning of published document failed");
        }
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
