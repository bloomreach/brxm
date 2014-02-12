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

import static org.hippoecm.repository.util.WorkflowUtils.getContainingFolder;

/**
 * Deprecated JCR-based implementation.  Kept for reference for new SCXML based implementation.
 */
@Deprecated
public class JCRFullReviewedActionsWorkflowImpl extends JCRBasicReviewedActionsWorkflowImpl implements FullReviewedActionsWorkflow {

    private static final Logger log = LoggerFactory.getLogger(JCRFullReviewedActionsWorkflowImpl.class);

    private static final long serialVersionUID = 1L;

    public JCRFullReviewedActionsWorkflowImpl() throws RemoteException {
    }

    @Override
    public Map<String, Serializable> hints() throws WorkflowException {
        Map<String, Serializable> info = super.hints();
        if (info.containsKey("delete")) {
            info.put("rename", info.get("delete"));
            info.put("move", info.get("delete"));
        }
        try {
            String state = JcrUtils.getStringProperty(getNode(), "hippostd:state", "");
            info.put("copy", (unpublishedDocument != null && PublishableDocument.UNPUBLISHED.equals(state))
                    || (unpublishedDocument == null && publishedDocument != null && PublishableDocument.PUBLISHED.equals(state)));
        } catch (RepositoryException ex) {
            // TODO DEDJO: ignore?
        }
        return info;
    }

    public void delete() throws WorkflowException {
        log.info("deletion on document ");
        if (current != null) {
            throw new WorkflowException("cannot delete document with pending request");
        }
        try {
            if (publishedDocument != null && publishedDocument.isAvailable("live")) {
                throw new WorkflowException("cannot delete published document");
            }
            if (draftDocument != null && draftDocument.getOwner() != null) {
                throw new WorkflowException("cannot delete document being edited");
            }
        } catch (RepositoryException ex) {
            throw new WorkflowException("Unable to determine legitimacy of delete operation", ex);
        }
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
        try {
            if (draftDocument != null) {
                deleteDocument(draftDocument);
            }
            if (publishedDocument != null) {
                deleteDocument(publishedDocument);
            }
            publishedDocument = draftDocument = null;
        } catch (RepositoryException ex) {
            throw new WorkflowException("exception trying to delete document", ex);
        }
        try {
            DefaultWorkflow defaultWorkflow = (DefaultWorkflow) getWorkflowContext().getWorkflow("core", unpublishedDocument);
            defaultWorkflow.archive();
        } catch (MappingException ex) {
            log.warn("invalid default workflow, falling back in behaviour", ex);
        } catch (WorkflowException ex) {
            log.warn("no default workflow for published documents, falling back in behaviour", ex);
        } catch (RepositoryException ex) {
            log.warn("exception trying to archive document, falling back in behaviour", ex);
        } catch (RemoteException ex) {
            log.warn("exception trying to archive document, falling back in behaviour", ex);
        }
    }

    public void copy(Document destination, String newName) throws MappingException, RemoteException, WorkflowException, RepositoryException {
        log.info("copy document");
        if (newName == null || newName.equals("")) {
            throw new WorkflowException("missing required name to copy document");
        }
        if (publishedDocument == null && unpublishedDocument == null) {
            throw new WorkflowException("cannot copy unsaved document");
        }

        String folderWorkflowCategory = "embedded";
        RepositoryMap config = getWorkflowContext().getWorkflowConfiguration();
        if (config != null && config.exists() && config.get("folder-workflow-category") instanceof String) {
            folderWorkflowCategory = (String) config.get("folder-workflow-category");
        }
        if (unpublishedDocument == null) {
            Document folder = getContainingFolder(publishedDocument);
            Workflow workflow = getWorkflowContext(null).getWorkflow(folderWorkflowCategory, destination);
            if (workflow instanceof EmbedWorkflow) {
                Document copy = ((EmbedWorkflow) workflow).copyTo(folder, publishedDocument, newName, null);
                FullReviewedActionsWorkflow copiedDocumentWorkflow = (FullReviewedActionsWorkflow) getWorkflowContext(null).getWorkflow("default", copy);
                copiedDocumentWorkflow.depublish();
            } else {
                throw new WorkflowException("cannot copy document which is not contained in a folder");
            }
        } else {
            Document folder = getContainingFolder(unpublishedDocument);
            Workflow workflow = getWorkflowContext().getWorkflow(folderWorkflowCategory, destination);
            if (workflow instanceof EmbedWorkflow) {
                ((EmbedWorkflow) workflow).copyTo(folder, unpublishedDocument, newName, null);
            } else {
                throw new WorkflowException("cannot copy document which is not contained in a folder");
            }
        }
    }

    public void move(Document destination, String newName) throws MappingException, RemoteException, WorkflowException, RepositoryException {
        log.info("move document");

        if (current != null) {
            throw new WorkflowException("cannot move document with pending request");
        }
        if (publishedDocument != null && publishedDocument.isAvailable("live")) {
            throw new WorkflowException("cannot move published document");
        }
        if (draftDocument != null && draftDocument.getOwner() != null) {
            throw new WorkflowException("cannot move document being edited");
        }

        PublishableDocument document = unpublishedDocument;
        if (document == null) {
            document = publishedDocument;
            if (document == null) {
                document = draftDocument;
            }
        }

        Document folder = getContainingFolder(document);
        String folderWorkflowCategory = "internal";
        RepositoryMap config = getWorkflowContext().getWorkflowConfiguration();
        if (config != null && config.exists() && config.get("folder-workflow-category") instanceof String) {
            folderWorkflowCategory = (String) config.get("folder-workflow-category");
        }
        Workflow workflow = getWorkflowContext().getWorkflow(folderWorkflowCategory, folder);
        if (workflow instanceof FolderWorkflow) {
            ((FolderWorkflow) workflow).move(document, destination, newName);
        } else {
            throw new WorkflowException("cannot move document which is not contained in a folder");
        }
    }

    public void rename(String newName) throws MappingException, RemoteException, WorkflowException, RepositoryException {
        log.info("rename on document ");
        if (current != null) {
            throw new WorkflowException("cannot rename document with pending request");
        }
        if (publishedDocument != null && publishedDocument.isAvailable("live")) {
            throw new WorkflowException("cannot rename published document");
        }
        if (draftDocument != null && draftDocument.getOwner() != null) {
            throw new WorkflowException("cannot rename document being edited");
        }
        PublishableDocument document = unpublishedDocument;
        if (document == null) {
            document = publishedDocument;
            if (document == null) {
                document = draftDocument;
            }
        }
        // doDepublish();
        DefaultWorkflow defaultWorkflow = (DefaultWorkflow) getWorkflowContext().getWorkflow("core", document);
        defaultWorkflow.rename(newName);
    }

    public void publish() throws WorkflowException, MappingException {
        log.info("publication on document ");

        try {
            if (unpublishedDocument == null) {
                if (publishedDocument == null) {
                    throw new WorkflowException("No unpublished version of document available for publication");
                } else if (publishedDocument.isAvailable("live")) {
                    throw new WorkflowException("Document has already been published");
                }
            }

            if (draftDocument != null && draftDocument.getOwner() != null) {
                throw new WorkflowException("cannot publish document being edited");
            }
        } catch (RepositoryException e) {
            throw new WorkflowException("Cannot determine whether document can be published", e);
        }
        doPublish();
    }

    public void doPublish() throws WorkflowException, MappingException {
        try {
            if (publishedDocument != null) {
                copyDocumentTo(unpublishedDocument, publishedDocument);
            } else {
                createPublished();
            }
            publishedDocument.setAvailability(new String[]{"live"});
            publishedDocument.setPublicationDate(new Date());
            VersionWorkflow versionWorkflow = (VersionWorkflow) getWorkflowContext().getWorkflow("versioning", unpublishedDocument);
            versionWorkflow.version();
        } catch (MappingException ex) {
            log.warn(ex.getClass().getName() + ": " + ex.getMessage(), ex);
        } catch (RemoteException ex) {
            log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
            throw new WorkflowException("Versioning of published document failed");
        } catch (RepositoryException ex) {
            log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
            throw new WorkflowException("Versioning of published document failed");
        }
    }

    public void depublish() throws WorkflowException {
        log.info("depublication on document ");
        try {
            if (draftDocument != null && draftDocument.getOwner() != null) {
                throw new WorkflowException("cannot publish document being edited");
            }
        } catch (RepositoryException e) {
            throw new WorkflowException("cannot determine draft ownership for depublication");
        }
        doDepublish();
    }

    void doDepublish() throws WorkflowException {
        try {
            if (unpublishedDocument == null) {
                createUnpublished(publishedDocument);
            }
            publishedDocument.setAvailability(new String[]{});

            VersionWorkflow versionWorkflow = (VersionWorkflow) getWorkflowContext().getWorkflow("versioning", unpublishedDocument);
            try {
                versionWorkflow.version();
            } catch (MappingException ex) {
                log.warn(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                throw new WorkflowException("Versioning of published document failed");
            } catch (RemoteException ex) {
                log.warn(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                throw new WorkflowException("Versioning of published document failed");
            } catch (RepositoryException ex) {
                log.warn(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                throw new WorkflowException("Versioning of published document failed");
            }
        } catch (MappingException ex) {
            log.warn(ex.getClass().getName() + ": " + ex.getMessage(), ex);
            throw new WorkflowException("Versioning of published document failed");
        } catch (RepositoryException ex) {
            log.warn(ex.getClass().getName() + ": " + ex.getMessage(), ex);
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
