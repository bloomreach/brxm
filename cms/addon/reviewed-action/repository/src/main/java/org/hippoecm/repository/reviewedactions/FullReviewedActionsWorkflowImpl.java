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
package org.hippoecm.repository.reviewedactions;

import java.util.Date;
import java.rmi.RemoteException;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.hippoecm.repository.standardworkflow.VersionWorkflow;

public class FullReviewedActionsWorkflowImpl extends BasicReviewedActionsWorkflowImpl implements FullReviewedActionsWorkflow {

    private static final long serialVersionUID = 1L;

    public FullReviewedActionsWorkflowImpl() throws RemoteException {
    }

    public void delete() throws WorkflowException {
        ReviewedActionsWorkflowImpl.log.info("deletion on document ");
        if(current != null)
            throw new WorkflowException("cannot delete document with pending publication request");
        if(published != null)
            throw new WorkflowException("cannot delete published document");
        unpublished = draft = null;
    }

    public void publish() throws WorkflowException, MappingException {
        ReviewedActionsWorkflowImpl.log.info("publication on document ");
        published = null;
        unpublished.setState(PublishableDocument.PUBLISHED);
        try {
            VersionWorkflow versionWorkflow = (VersionWorkflow) getWorkflowContext().getWorkflow("versioning", unpublished);
            versionWorkflow.version();
        } catch(MappingException ex) {
            System.err.println(ex.getClass().getName()+": "+ex.getMessage());
            ex.printStackTrace(System.err);
            // FIXME: should log a warning here
        } catch(RemoteException ex) {
            System.err.println(ex.getClass().getName()+": "+ex.getMessage());
            ex.printStackTrace(System.err);
            throw new WorkflowException("Versioning of published document failed");
        } catch(RepositoryException ex) {
            System.err.println(ex.getClass().getName()+": "+ex.getMessage());
            ex.printStackTrace(System.err);
            throw new WorkflowException("Versioning of published document failed");
        }
    }

    public void depublish() throws WorkflowException {
        ReviewedActionsWorkflowImpl.log.info("depublication on document ");
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
                System.err.println(ex.getClass().getName()+": "+ex.getMessage());
                ex.printStackTrace(System.err);
                // FIXME: should log a warning here
            } catch(RemoteException ex) {
                System.err.println(ex.getClass().getName()+": "+ex.getMessage());
                ex.printStackTrace(System.err);
                throw new WorkflowException("Versioning of published document failed");
            } catch(RepositoryException ex) {
                System.err.println(ex.getClass().getName()+": "+ex.getMessage());
                ex.printStackTrace(System.err);
                throw new WorkflowException("Versioning of published document failed");
            }
        } catch(CloneNotSupportedException ex) {
            System.err.println(ex.getClass().getName()+": "+ex.getMessage());
            ex.printStackTrace(System.err);
            throw new WorkflowException("document is not a publishable document");
        }
    }
}
