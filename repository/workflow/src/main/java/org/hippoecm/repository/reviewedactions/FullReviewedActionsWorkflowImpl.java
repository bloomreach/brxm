/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.repository.standardworkflow.EmbedWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.hippoecm.repository.standardworkflow.VersionWorkflow;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FullReviewedActionsWorkflowImpl extends BasicReviewedActionsWorkflowImpl implements FullReviewedActionsWorkflow {

    private static final Logger log = LoggerFactory.getLogger(FullReviewedActionsWorkflowImpl.class);

    private static final long serialVersionUID = 1L;

    public FullReviewedActionsWorkflowImpl() throws RemoteException {
    }

    @Override
    public Map<String,Serializable> hints()  {
        Map<String,Serializable> info = super.hints();
        if (info.containsKey("delete")) {
            info.put("rename", info.get("delete"));
            info.put("move", info.get("delete"));
        }
        try {
            String state = JcrUtils.getStringProperty(getNode(), "hippostd:state", "");
            info.put("copy", (unpublishedDocument != null && PublishableDocument.UNPUBLISHED.equals(state))
                    || (unpublishedDocument == null && publishedDocument != null && PublishableDocument.PUBLISHED.equals(state)));
        }
        catch (RepositoryException ex) {
            // TODO DEDJO: ignore?
        }
        return info;
    }

    public void delete() throws WorkflowException {
        log.info("deletion on document ");
        if(current != null)
            throw new WorkflowException("cannot delete document with pending request");
        if(publishedDocument != null)
            throw new WorkflowException("cannot delete published document");
        if(draftDocument != null)
            throw new WorkflowException("cannot delete document being edited");
        doDelete();
    }

    public void publish(Date publicationDate, Date depublicationDate) throws WorkflowException {
        log.info("publication on document ");
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
        boolean fallbackDelete = false;
        try {
            DefaultWorkflow defaultWorkflow = (DefaultWorkflow) getWorkflowContext().getWorkflow("core", unpublishedDocument);
            defaultWorkflow.archive();
        } catch(MappingException ex) {
            log.warn("invalid default workflow, falling back in behaviour", ex);
            fallbackDelete = true;
        } catch(WorkflowException ex) {
            log.warn("no default workflow for published documents, falling back in behaviour", ex);
            fallbackDelete = true;
        } catch(RepositoryException ex) {
            log.warn("exception trying to archive document, falling back in behaviour", ex);
            fallbackDelete = true;
        } catch(RemoteException ex) {
            log.warn("exception trying to archive document, falling back in behaviour", ex);
            fallbackDelete = true;
        }
        if (fallbackDelete) {
            try {
                if (draftDocument != null) {
                    deleteDocument(draftDocument);
                }
                if (unpublishedDocument != null) {
                    deleteDocument(unpublishedDocument);
                }
                unpublishedDocument = draftDocument = null;
            }
            catch (RepositoryException ex) {
                log.error("exception trying to delete document", ex);
                // TODO DEDJO: ignore?
            }
        }
    }

    public void copy(Document destination, String newName) throws MappingException, RemoteException, WorkflowException, RepositoryException {
        log.info("copy document");
        if (newName == null || newName.equals("")) {
            throw new WorkflowException("missing required name to copy document");
        }
        if(publishedDocument == null && unpublishedDocument == null) {
            throw new WorkflowException("cannot copy unsaved document");
        }

        String folderWorkflowCategory = "embedded";
        RepositoryMap config = getWorkflowContext().getWorkflowConfiguration();
        if (config != null && config.exists() && config.get("folder-workflow-category") instanceof String) {
            folderWorkflowCategory = (String) config.get("folder-workflow-category");
        }
        if (unpublishedDocument == null) {
            Document folder = getWorkflowContext().getDocument("embedded", publishedDocument.getIdentity());
            Workflow workflow = getWorkflowContext(null).getWorkflow(folderWorkflowCategory, destination);
            if (workflow instanceof EmbedWorkflow) {
                Document copy = ((EmbedWorkflow)workflow).copyTo(folder, publishedDocument, newName, null);
                FullReviewedActionsWorkflow copiedDocumentWorkflow = (FullReviewedActionsWorkflow) getWorkflowContext(null).getWorkflow("default", copy);
                copiedDocumentWorkflow.depublish();
            } else
                throw new WorkflowException("cannot copy document which is not contained in a folder");
        } else {
            Document folder = getWorkflowContext().getDocument("embedded", unpublishedDocument.getIdentity());
            Workflow workflow = getWorkflowContext().getWorkflow(folderWorkflowCategory, destination);
            if(workflow instanceof EmbedWorkflow) {
                ((EmbedWorkflow)workflow).copyTo(folder, unpublishedDocument, newName, null);
            }
            else {
                throw new WorkflowException("cannot copy document which is not contained in a folder");
            }
        }
    }

    public void move(Document destination, String newName) throws MappingException, RemoteException, WorkflowException, RepositoryException {
        log.info("move document");
        if(current != null)
            throw new WorkflowException("cannot move document with pending request");
        if(publishedDocument != null)
            throw new WorkflowException("cannot move published document");
        if(draftDocument != null)
            throw new WorkflowException("cannot move document being edited");

        Document folder = getWorkflowContext().getDocument("embedded", unpublishedDocument.getIdentity());
        String folderWorkflowCategory = "internal";
        RepositoryMap config = getWorkflowContext().getWorkflowConfiguration();
        if (config != null && config.exists() && config.get("folder-workflow-category") instanceof String) {
            folderWorkflowCategory = (String) config.get("folder-workflow-category");
        }
        Workflow workflow = getWorkflowContext().getWorkflow(folderWorkflowCategory, folder);
        if(workflow instanceof FolderWorkflow) {
            ((FolderWorkflow)workflow).move(unpublishedDocument, destination, newName);
        }
        else {
            throw new WorkflowException("cannot move document which is not contained in a folder");
        }
    }

    public void rename(String newName) throws MappingException, RemoteException, WorkflowException, RepositoryException {
        log.info("rename on document ");
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
        log.info("publication on document ");
        if(unpublishedDocument == null) {
            if(publishedDocument == null) {
                throw new WorkflowException("No unpublished version of document available for publication");
            } else {
                throw new WorkflowException("Document has already been published");
            }
        }
        if(draftDocument != null) {
            throw new WorkflowException("cannot publish document being edited");
        }
        doPublish();
    }

    public void doPublish() throws WorkflowException, MappingException {
        try {
            if (publishedDocument != null) {
                deleteDocument(publishedDocument);
            }
            publishedDocument = unpublishedDocument;
            unpublishedDocument = null;
            publishedDocument.setState(PublishableDocument.PUBLISHED);
            publishedDocument.setPublicationDate(new Date());
            publishedDocument.setAvailability(new String[] { "live", "preview" });
            VersionWorkflow versionWorkflow = (VersionWorkflow) getWorkflowContext().getWorkflow("versioning", publishedDocument);
            versionWorkflow.version();
        } catch(MappingException ex) {
            log.warn(ex.getClass().getName()+": "+ex.getMessage(), ex);
        } catch(RemoteException ex) {
            log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
            throw new WorkflowException("Versioning of published document failed");
        } catch(RepositoryException ex) {
            log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
            throw new WorkflowException("Versioning of published document failed");
        }
    }

    public void depublish() throws WorkflowException {
        log.info("depublication on document ");
        if(draftDocument != null)
            throw new WorkflowException("cannot publish document being edited");
        doDepublish();
    }

    void doDepublish() throws WorkflowException {
        try {
            VersionWorkflow versionWorkflow;
            if(unpublishedDocument == null) {
                unpublishedDocument = publishedDocument;
                unpublishedDocument.setState(PublishableDocument.UNPUBLISHED);
                unpublishedDocument.setAvailability(new String[] { "preview" });
            }
            else {
                deleteDocument(publishedDocument);
            }
            publishedDocument = null;
            versionWorkflow = (VersionWorkflow) getWorkflowContext().getWorkflow("versioning", unpublishedDocument);
            try {
                versionWorkflow.version();
            } catch(MappingException ex) {
                log.warn(ex.getClass().getName()+": "+ex.getMessage(), ex);
                throw new WorkflowException("Versioning of published document failed");
            } catch(RemoteException ex) {
                log.warn(ex.getClass().getName()+": "+ex.getMessage(), ex);
                throw new WorkflowException("Versioning of published document failed");
            } catch(RepositoryException ex) {
                log.warn(ex.getClass().getName()+": "+ex.getMessage(), ex);
                throw new WorkflowException("Versioning of published document failed");
            }
        } catch(MappingException ex) {
            log.warn(ex.getClass().getName()+": "+ex.getMessage(), ex);
            throw new WorkflowException("Versioning of published document failed");
        } catch(RepositoryException ex) {
            log.warn(ex.getClass().getName()+": "+ex.getMessage(), ex);
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
