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

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.reviewedactions.FullReviewedActionsWorkflow;
import org.hippoecm.repository.reviewedactions.UnlockWorkflow;
import org.onehippo.repository.api.annotation.WorkflowAction;

/**
 * Aggregate HandleDocumentWorkflow, combining all Document handle based workflow operations into one generic interface.
 * <p>
 * The -Request- and VersionWorkflows operations have been re-defined inline to be able to use different parameters (and methodNames)
 * as needed to be functional on Document handle level
 * </p>
 */
public interface HandleDocumentWorkflow extends Workflow, FullReviewedActionsWorkflow, UnlockWorkflow {

    Map<String, Serializable> getInfo();

    Map<String, Boolean> getActions();

    // Request Workflow on Document handle level

    /**
     * Cancels and/or disposes (!) a specific request.
     */
    public void cancelRequest(Node request)
            throws WorkflowException, RepositoryException, RemoteException;

    /**
     * Approve and execute or schedule a specific request
     */
    public void acceptRequest(Node request)
            throws WorkflowException, RepositoryException, RemoteException;

    /**
     * Rejects a specific request with given reason
     */
    public void rejectRequest(Node request, String reason)
            throws WorkflowException, RepositoryException, RemoteException;



    // Version Workflow on Document handle level

    /**
     * Creates a version of the current document state, such that the current state of the document may be re-retrieved or restored
     * later.  Even if the document has been changed, republished or archived later on.
     * @return the same document on which version call has been made
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public Document version()
            throws WorkflowException, RepositoryException, RemoteException;

    /**
     * Restore a specific version
     * @param version the version to restore
     * @return the updated target node
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public Document restoreFromVersion(Version version)
            throws WorkflowException, RepositoryException, RemoteException;

    /**
     * Restore a version from a specific historic date
     * @param historic
     * @return
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public Document restoreFromVersion(Calendar historic)
            throws WorkflowException, RepositoryException, RemoteException;

    /**
     * Lists the historic versions of a documents that are available.  A historic version is created using the {@link #version()}
     * call.  The time at which such a call is made is listed at key item in the returned map.  This time may be used in a call
     * to {@link #restoreFromVersion}
     * @return A time-ordered map from earliest to latest of historic version (the timestamps at which {@link #version} was called)
     * mapped to a list of symbolic names that were given to the versions.  The symbolic names currently cannot be set using
     * this work-flow interface, but can be set using the regular JCR API.
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    @WorkflowAction(loggable = false)
    public SortedMap<Calendar,Set<String>> listVersions()
            throws WorkflowException, RepositoryException, RemoteException;

    /**
     * Returns a Version from a specific historic date as Document
     * @param historic
     * @return
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    @WorkflowAction(loggable = false)
    public Document retrieveVersion(Calendar historic)
            throws WorkflowException, RepositoryException, RemoteException;
}
