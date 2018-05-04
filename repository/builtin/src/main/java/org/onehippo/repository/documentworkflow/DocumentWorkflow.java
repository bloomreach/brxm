/*
 *  Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.repository.api.DocumentWorkflowAction;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowContextAware;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowAction;
import org.hippoecm.repository.standardworkflow.CopyWorkflow;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;

/**
 * Aggregate DocumentWorkflow, combining all Document handle based workflow operations into one generic interface.
 * <p>
 * This workflow replaces all deprecated org,hippoecm.repository.reviewedactions.* workflows, whose operations
 * have been inlined.
 * </p>
 * <p> The -Request- and VersionWorkflows operations have been re-defined to be able to use different parameters
 * (and methodNames) as needed to be functional on Document handle level </p>
 */
public interface DocumentWorkflow extends Workflow, EditableWorkflow, CopyWorkflow, WorkflowContextAware {

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
    @org.onehippo.repository.api.annotation.WorkflowAction(loggable = false, mutates = false)
    @Override
    Map<String, Serializable> hints() throws WorkflowException, RemoteException, RepositoryException;

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
    void requestDeletion()
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
    void requestDepublication()
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
    void requestDepublication(Date depublicationDate)
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
    void requestPublication()
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
    void requestPublication(Date publicationDate)
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
     * @deprecated Deprecated since 5.0.0, use {@link DocumentWorkflow#requestPublicationDepublication(Date, Date)}
     */
    @Deprecated
    void requestPublication(Date publicationDate, Date depublicationDate)
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
    void requestPublicationDepublication(Date publicationDate, Date depublicationDate)
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
    void delete()
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
    void rename(String newName)
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
    void copy(Document destination, String newName)
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
    void move(Document destination, String newName)
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
    void depublish()
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
    void depublish(Date depublicationDate)
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
    void publish()
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
    void publish(Date publicationDate)
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
    void publish(Date publicationDate, Date unpublicationDate)
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
     * Delete a specific outstanding, rejected or scheduled request with an optional reason.
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
    void cancelRequest(String requestIdentifier, String reason)
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
     * Approve and execute or schedule a specific request with an optional reason.
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
    void acceptRequest(String requestIdentifier, String reason)
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
    Document versionRestoreTo(Calendar historic, Document target)
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
    @org.onehippo.repository.api.annotation.WorkflowAction(loggable = false)
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
    @org.onehippo.repository.api.annotation.WorkflowAction(loggable = false)
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

    /**
     *
     * @return The {@link Set} of unique branches in version history
     *
     * @throws WorkflowException
     * @throws RepositoryException
     * @throws RepositoryException
     */
    Set<String> listBranches() throws WorkflowException, RepositoryException, RepositoryException;

    /**
     * <p>
     *     Branches this document. During branching the following will happen
     *     <ol>
     *         <li>
     *             The current preview will be version into version history. If the current preview is for a branchId x, the
     *             x-preview version label will be moved to the newly version. If the current preview is for core,
     *             the core-preview version label will be moved to the newly created version.
     *         </li>
     *         <li>
     *             The preview will be marked to be for branch {@code branchId}. If the {@code branchId} is equal to 'core',
     *             the result will be that from the preview the branch information is removed.
     *         </li>
     *     </ol>
     * </p>
     * @param brandId the id of the branch that will be stored on the document variant
     * @param branchName the name that will be stored on the document variant for the branch
     * @return {@link Document} wrapping the workspace preview node variant
     * @throws WorkflowException In case the {@code branchId} already exists or branching is not allowed in the current
     *                           document state
     * @throws RepositoryException
     * @throws RemoteException
     */
    Document branch(String brandId, String branchName) throws WorkflowException, RepositoryException, RemoteException;

    /**
     *  <p>
     *      Tries to restore from version history the version with label '${branchId}-preview' and throws a {@link WorkflowException}
     *      if no such version exists. Before a version from version history is restored, the preview gets versioned
     *  </p>
     *
     *  @return {@link Document} wrapping the workspace preview node variant
     *  @throws WorkflowException In case the {@code branchId} does not exist in version history or when checkoutBranch
     *                            is not allowed in the current document state
     * @throws RepositoryException
     * @throws RemoteException
     */
    Document checkoutBranch(String brandId) throws WorkflowException, RepositoryException, RemoteException;

    /**
     * Triggers workflow based on {@link org.hippoecm.repository.api.WorkflowAction}
     *
     * @param action {@link org.hippoecm.repository.api.WorkflowAction} instance
     * @return The object result of this action. The type of object depends on the type of action. It can be {@code null}, a {@link Document} instance,
     * but for example in case of the action {@link DocumentWorkflowAction#listVersions} the returned object is a SortedMap
     * @throws WorkflowException
     */
    // Note we use WorkflowAction instead of DocumentWorkflowAction here because in the future we might want to move this method to Workflow
    // (so all workflow impls need to implement #triggerAction but of course not with argument DocumentWorkflowAction
    default Object triggerAction(final WorkflowAction action) throws WorkflowException {
        throw new UnsupportedOperationException("Action has not been defined for this instance.");
    }
}
