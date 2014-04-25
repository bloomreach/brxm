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
import java.util.Date;
import java.util.Map;
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
 * This workflow replaces all deprecated org,hippoecm.repository.reviewedactions.* workflows, whose operations
 * have been inlined.
 * </p>
 * <p> The -Request- and VersionWorkflows operations have been re-defined to be able to use different parameters
 * (and methodNames) as needed to be functional on Document handle level </p>
 */
public interface DocumentWorkflow extends Workflow, EditableWorkflow, CopyWorkflow {

    /**
     * <p>
     * The DocumentWorkflow hints method provides all the operational hints corresponding with the DocumentWorkflow
     * operational method names which are available to the current user to be invoked. However, only when the returned
     * value is Boolean.TRUE are they <em>allowed</em> to be invoked. They might have a value of Boolean.FALSE though
     * when the current document <em>state</em> does not allow executing the specific operation.
     * </p>
     * <p>
     * The returned hints map may also provide additional information, like the current editable "status", if the
     * document "isLive", or if currently there is a "previewAvailable".
     * </p>
     * <p>
     * Separately, the hints map also returns status information about existing workflow or scheduled requests.
     * This request status data is returned under key "requests" with as value a nested 'hints'
     * Map&lt;String,Map&lt;String, Boolean&gt;&gt; per request node identifier,
     * defining the allowable request operations ({@link #acceptRequest(String)}, {@link #cancelRequest(String)},
     * {@link #rejectRequest(String, String)}) for each request node.
     * </p>
     *
     * @return a map containing hints given by the workflow, the data in this map may be considered valid until the
     * document itself changes
     *
     * @throws WorkflowException   indicates that the work-flow call failed due work-flow specific conditions
     * @throws RepositoryException indicates that the work-flow call failed because of storage problems internal to the
     *                             repository
     * @throws RemoteException     indicates that the work-flow call failed because of a connection problem with the
     *                             repository
     */
    @WorkflowAction(loggable = false, mutates = false)
    @Override
    public Map<String, Serializable> hints() throws WorkflowException, RemoteException, RepositoryException;

    // Operations previously provided through BasicReviewedActionsWorkflow, now provided on Document handle level

    /**
     * Request deletion of this document.
     *
     * @throws WorkflowException   indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException    indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException indicates that the work-flow call failed because of storage problems internal to the
     *                             repository
     * @throws RemoteException     indicates that the work-flow call failed because of a connection problem with the
     *                             repository
     */
    public void requestDeletion()
            throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Request depublication of this document.
     *
     * @throws WorkflowException   indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException    indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException indicates that the work-flow call failed because of storage problems internal to the
     *                             repository
     * @throws RemoteException     indicates that the work-flow call failed because of a connection problem with the
     *                             repository
     */
    public void requestDepublication()
            throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Request scheduled depublication of this document at the given date.
     *
     * @param depublicationDate date at which the document is requested to be depublished
     *
     * @throws WorkflowException   indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException    indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException indicates that the work-flow call failed because of storage problems internal to the
     *                             repository
     * @throws RemoteException     indicates that the work-flow call failed because of a connection problem with the
     *                             repository
     */
    public void requestDepublication(Date depublicationDate)
            throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Request this document to be published.
     *
     * @throws WorkflowException   indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException    indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException indicates that the work-flow call failed because of storage problems internal to the
     *                             repository
     * @throws RemoteException     indicates that the work-flow call failed because of a connection problem with the
     *                             repository
     */
    public void requestPublication()
            throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Request scheduled publication of this document at the given date.
     *
     * @param publicationDate date at which the document is requested to be published
     *
     * @throws WorkflowException   indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException    indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException indicates that the work-flow call failed because of storage problems internal to the
     *                             repository
     * @throws RemoteException     indicates that the work-flow call failed because of a connection problem with the
     *                             repository
     */
    public void requestPublication(Date publicationDate)
            throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Request this document to be published at the given
     * publication date and to be depublished again at given depublication date.
     * <p>
     * Note: this is currently <em>NOT</em> implemented nor supported in the default implementation.
     * </p>
     *
     * @throws WorkflowException   indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException    indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException indicates that the work-flow call failed because of storage problems internal to the
     *                             repository
     * @throws RemoteException     indicates that the work-flow call failed because of a connection problem with the
     *                             repository
     */
    public void requestPublication(Date publicationDate, Date depublicationDate)
            throws WorkflowException, MappingException, RepositoryException, RemoteException;


    // Operations previously provided through FullReviewedActionsWorkflow, now provided on Document handle level

    /**
     * Delete this document.
     *
     * @throws WorkflowException   indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException    indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException indicates that the work-flow call failed because of storage problems internal to the
     *                             repository
     * @throws RemoteException     indicates that the work-flow call failed because of a connection problem with the
     *                             repository
     */
    public void delete()
            throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Rename this document to the provided new name
     *
     * @param newName the new name for this document
     *
     * @throws WorkflowException   indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException    indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException indicates that the work-flow call failed because of storage problems internal to the
     *                             repository
     * @throws RemoteException     indicates that the work-flow call failed because of a connection problem with the
     *                             repository
     */
    public void rename(String newName)
            throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Copy this document to a specific target document folder with a new name
     *
     * @param destination the target document folder
     * @param newName the name for the copied document
     *
     * @throws WorkflowException   indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException    indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException indicates that the work-flow call failed because of storage problems internal to the
     *                             repository
     * @throws RemoteException     indicates that the work-flow call failed because of a connection problem with the
     *                             repository
     */
    public void copy(Document destination, String newName)
            throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Move this document to a specific target document folder with a new name
     *
     * @param destination the target document folder
     * @param newName the new name for the moved document
     *
     * @throws WorkflowException   indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException    indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException indicates that the work-flow call failed because of storage problems internal to the
     *                             repository
     * @throws RemoteException     indicates that the work-flow call failed because of a connection problem with the
     *                             repository
     */
    public void move(Document destination, String newName)
            throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Depublish this document
     *
     * @throws WorkflowException   indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException    indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException indicates that the work-flow call failed because of storage problems internal to the
     *                             repository
     * @throws RemoteException     indicates that the work-flow call failed because of a connection problem with the
     *                             repository
     */
    public void depublish()
            throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Schedule depublication of this document at the given date
     *
     * @param depublicationDate the date at which to depublish this document
     *
     * @throws WorkflowException   indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException    indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException indicates that the work-flow call failed because of storage problems internal to the
     *                             repository
     * @throws RemoteException     indicates that the work-flow call failed because of a connection problem with the
     *                             repository
     */
    public void depublish(Date depublicationDate)
            throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Publish this document.
     *
     * @throws WorkflowException   indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException    indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException indicates that the work-flow call failed because of storage problems internal to the
     *                             repository
     * @throws RemoteException     indicates that the work-flow call failed because of a connection problem with the
     *                             repository
     */
    public void publish()
            throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Schedule publication of this document at the given date.
     *
     * @param publicationDate the date at which to publish this document
     *
     * @throws WorkflowException   indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException    indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException indicates that the work-flow call failed because of storage problems internal to the
     *                             repository
     * @throws RemoteException     indicates that the work-flow call failed because of a connection problem with the
     *                             repository
     */
    public void publish(Date publicationDate)
            throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Request scheduling this document to be published at the given
     * publication date and to be depublished again at given depublication date.
     * <p>
     * Note: this is currently <em>NOT</em> implemented nor supported in the default implementation.
     * </p>
     *
     * @throws WorkflowException   indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException    indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException indicates that the work-flow call failed because of storage problems internal to the
     *                             repository
     * @throws RemoteException     indicates that the work-flow call failed because of a connection problem with the
     *                             repository
     */
    public void publish(Date publicationDate, Date unpublicationDate)
            throws WorkflowException, MappingException, RepositoryException, RemoteException;


    // Operations previously provided through *RequestWorkflow, now provided on Document handle level

    /**
     * Delete a specific outstanding, rejected or scheduled request.
     *
     * @param requestIdentifier the request node identifier
     *
     * @throws WorkflowException   indicates that the work-flow call failed due work-flow specific conditions
     * @throws RepositoryException indicates that the work-flow call failed because of storage problems internal to the
     *                             repository
     * @throws RemoteException     indicates that the work-flow call failed because of a connection problem with the
     *                             repository
     */
    void cancelRequest(String requestIdentifier)
            throws WorkflowException, RepositoryException, RemoteException;

    /**
     * Approve and execute or schedule a specific request
     *
     * @param requestIdentifier the request node identifier
     *
     * @throws WorkflowException   indicates that the work-flow call failed due work-flow specific conditions
     * @throws RepositoryException indicates that the work-flow call failed because of storage problems internal to the
     *                             repository
     * @throws RemoteException     indicates that the work-flow call failed because of a connection problem with the
     *                             repository
     */
    void acceptRequest(String requestIdentifier)
            throws WorkflowException, RepositoryException, RemoteException;

    /**
     * Rejects a specific request with an optional reason
     *
     * @param requestIdentifier the request node identifier
     * @param reason the optional reason to be recorded
     *
     * @throws WorkflowException   indicates that the work-flow call failed due work-flow specific conditions
     * @throws RepositoryException indicates that the work-flow call failed because of storage problems internal to the
     *                             repository
     * @throws RemoteException     indicates that the work-flow call failed because of a connection problem with the
     *                             repository
     */
    void rejectRequest(String requestIdentifier, String reason)
            throws WorkflowException, RepositoryException, RemoteException;


    // Operations previously provided through VersionWorkflow, on provided on Document handle level

    /**
     * Creates a version of the current document
     *
     * @return the JCR Version document created
     *
     * @throws WorkflowException   indicates that the work-flow call failed due work-flow specific conditions
     * @throws RepositoryException indicates that the work-flow call failed because of storage problems internal to the
     *                             repository
     * @throws RemoteException     indicates that the work-flow call failed because of a connection problem with the
     *                             repository
     */
    Document version()
            throws WorkflowException, RepositoryException, RemoteException;

    /**
     * Restore a version of this document from a specific historic date
     *
     * @param historic the date of the version to restore this document from
     *
     * @return the restored document (variant)
     *
     * @throws WorkflowException   indicates that the work-flow call failed due work-flow specific conditions
     * @throws RepositoryException indicates that the work-flow call failed because of storage problems internal to the
     *                             repository
     * @throws RemoteException     indicates that the work-flow call failed because of a connection problem with the
     *                             repository
     */
    Document restoreVersion(Calendar historic)
            throws WorkflowException, RepositoryException, RemoteException;

    /**
     * Restore a historic version of this document onto a specified target document.
     *
     * @param historic the date of the version of this document to restore from
     * @param target the target document to restore to
     *
     * @return the updated target document
     *
     * @throws WorkflowException   indicates that the work-flow call failed due work-flow specific conditions
     * @throws RepositoryException indicates that the work-flow call failed because of storage problems internal to the
     *                             repository
     * @throws RemoteException     indicates that the work-flow call failed because of a connection problem with the
     *                             repository
     */
    public Document versionRestoreTo(Calendar historic, Document target)
            throws WorkflowException, RepositoryException, RemoteException;

    /**
     * Lists the available historic versions of this document.  A historic version is created using the {@link
     * #version()} call.  The time at which such a call is made is listed at key item in the returned map.  This time
     * may be used in a call to {@link #restoreVersion} or {@link #versionRestoreTo}
     *
     * @return A time-ordered map from earliest to latest of historic version (the timestamps at which {@link #version}
     * was called) mapped to a list of version labels that were given to (all) the versions, if any, and the version
     * name itself. The version labels and names currently cannot be set using this workflow interface, but can be set
     * using the regular JCR API.
     *
     * @throws WorkflowException   indicates that the work-flow call failed due work-flow specific conditions
     * @throws RepositoryException indicates that the work-flow call failed because of storage problems internal to the
     *                             repository
     * @throws RemoteException     indicates that the work-flow call failed because of a connection problem with the
     *                             repository
     */
    @WorkflowAction(loggable = false)
    SortedMap<Calendar, Set<String>> listVersions()
            throws WorkflowException, RepositoryException, RemoteException;

    /**
     * Returns the frozen node document from a specific historic version of this document.
     *
     * @param historic the version date of this document
     *
     * @return the frozen node document of the specific historic version of this document
     *
     * @throws WorkflowException   indicates that the work-flow call failed due work-flow specific conditions
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
     * Unlock this document (possibly) held by another user by making the current user holder of the document.
     *
     * @throws WorkflowException   indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException    indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException indicates that the work-flow call failed because of storage problems internal to the
     *                             repository
     * @throws RemoteException     indicates that the work-flow call failed because of a connection problem with the
     *                             repository
     */
    void unlock()
            throws WorkflowException, MappingException, RepositoryException, RemoteException;
}
