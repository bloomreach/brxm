/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.documentworkflow;

import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.SortedMap;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.CopyWorkflow;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.onehippo.repository.api.annotation.WorkflowAction;

/**
 * Aggregate DocumentWorkflow, combining all Document handle based workflow operations into one generic interface.
 * <p>
 * This workflow replaces now all deprecated org,hippoecm.repository.reviewedactions.* workflow, which operations
 * have been inlined.
 * </p>
 * <p> The -Request- and VersionWorkflows operations have been re-defined to be able to use different parameters
 * (and methodNames) as needed to be functional on Document handle level </p>
 */
public interface DocumentWorkflow extends Workflow, EditableWorkflow, CopyWorkflow {

    // Operations previously provided through BasicReviewedActionsWorkflow, now provided on Document handle level

    /**
     * Request unpublication and deletion of document.
     */
    public void requestDeletion()
            throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Request unpublication.
     */
    public void requestDepublication()
            throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Request unpublication at given date.
     */
    public void requestDepublication(Date publicationDate)
            throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Request for this instance of the document to be published.
     */
    public void requestPublication()
            throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Request for this instance of the document to be published at the given
     * date.
     */
    public void requestPublication(Date publicationDate)
            throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Request for this instance of the document to be published at the given
     * date and to be scheduled for unpublication.
     */
    public void requestPublication(Date publicationDate, Date unpublicationDate)
            throws WorkflowException, MappingException, RepositoryException, RemoteException;


    // Operations previously provided through FullReviewedActionsWorkflow, now provided on Document handle level

    /**
     * Immediate unpublication and deletion of document.
     * The current user must have authorization for this.
     */
    public void delete()
            throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Rename document.
     * The current user must have authorization for this.
     */
    public void rename(String newName)
            throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Immediate unpublication and rename document.
     * The current user must have authorization for this.
     */
    public void copy(Document target, String newName)
            throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Immediate unpublication and rename document.
     * The current user must have authorization for this.
     */
    public void move(Document target, String newName)
            throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Immediate unpublication.
     * The current user must have authorization for this.
     */
    public void depublish()
            throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Immediate unpublication.
     * The current user must have authorization for this.
     */
    public void depublish(Date depublicationDate)
            throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Immediate publication.
     * The current user must have authorization for this.
     */
    public void publish()
            throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Publish at the requested date.
     * The current user must have authorization for this.
     */
    public void publish(Date publicationDate)
            throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Publish at the requested date, and depublish at the requested second date
     * The current user must have authorization for this.
     */
    public void publish(Date publicationDate, Date unpublicationDate)
            throws WorkflowException, MappingException, RepositoryException, RemoteException;


    // Operations previously provided through *RequestWorkflow, now provided on Document handle level

    /**
     * Cancels and/or disposes (!) a specific request.
     */
    void cancelRequest(String requestIdentifier)
            throws WorkflowException, RepositoryException, RemoteException;

    /**
     * Approve and execute or schedule a specific request
     */
    void acceptRequest(String requestIdentifier)
            throws WorkflowException, RepositoryException, RemoteException;

    /**
     * Rejects a specific request with given reason
     */
    void rejectRequest(String requestIdentifier, String reason)
            throws WorkflowException, RepositoryException, RemoteException;


    // Operations previously provided through VersionWorkflow, on provided on Document handle level

    /**
     * Creates a version of the current document state, such that the current state of the document may be re-retrieved
     * or restored later.  Even if the document has been changed, republished or archived later on.
     *
     * @return the same document on which version call has been made
     * @throws WorkflowException   indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException    indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException indicates that the work-flow call failed because of storage problems internal to the
     *                             repository
     * @throws RemoteException     indicates that the work-flow call failed because of a connection problem with the
     *                             repository
     */
    Document version()
            throws WorkflowException, RepositoryException, RemoteException;

    /**
     * Restore a version from a specific historic date
     *
     * @param historic
     * @return
     * @throws WorkflowException   indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException    indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException indicates that the work-flow call failed because of storage problems internal to the
     *                             repository
     * @throws RemoteException     indicates that the work-flow call failed because of a connection problem with the
     *                             repository
     */
    Document restoreVersion(Calendar historic)
            throws WorkflowException, RepositoryException, RemoteException;

    /**
     * Restore a historic version by putting its contents in the target document.
     * @param historic
     * @param target the Document representation of the target node
     * @return the updated target node
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public Document versionRestoreTo(Calendar historic, Document target)
            throws WorkflowException, RepositoryException, RemoteException;

    /**
     * Lists the historic versions of a documents that are available.  A historic version is created using the {@link
     * #version()} call.  The time at which such a call is made is listed at key item in the returned map.  This time
     * may be used in a call to {@link #restoreVersion} or {@link #versionRestoreTo}
     *
     * @return A time-ordered map from earliest to latest of historic version (the timestamps at which {@link #version}
     * was called) mapped to a list of symbolic names that were given to the versions.  The symbolic names currently
     * cannot be set using this work-flow interface, but can be set using the regular JCR API.
     * @throws WorkflowException   indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException    indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException indicates that the work-flow call failed because of storage problems internal to the
     *                             repository
     * @throws RemoteException     indicates that the work-flow call failed because of a connection problem with the
     *                             repository
     */
    @WorkflowAction(loggable = false)
    SortedMap<Calendar, Set<String>> listVersions()
            throws WorkflowException, RepositoryException, RemoteException;

    /**
     * Returns a Version from a specific historic date as Document
     *
     * @param historic
     * @return
     * @throws WorkflowException   indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException    indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException indicates that the work-flow call failed because of storage problems internal to the
     *                             repository
     * @throws RemoteException     indicates that the work-flow call failed because of a connection problem with the
     *                             repository
     */
    @WorkflowAction(loggable = false)
    Document retrieveVersion(Calendar historic)
            throws WorkflowException, RepositoryException, RemoteException;


    // Operations previously provided through UnlockWorkflow, now provided on Document handle level

    /**
     * Unlock document, i.e. take ownership of draft
     */
    void unlock()
            throws WorkflowException, MappingException, RepositoryException, RemoteException;

}
