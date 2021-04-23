/*
 *  Copyright 2014-2021 Hippo B.V. (http://www.onehippo.com)
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
import javax.jcr.version.Version;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.DocumentWorkflowAction;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowContextAware;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowAction;
import org.hippoecm.repository.standardworkflow.CopyWorkflow;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.hippoecm.repository.util.WorkflowUtils;

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
     * @see #copy(Document, String, String) copy(destination, newName, branchId) where branchId will be 'master'
     */
    void copy(Document destination, String newName)
            throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Copy this document to a specific target document folder with a new name
     *
     * @param destination the target document folder
     * @param newName the name for the copied document
     * @param branchId the branch to copy
     *
     * @throws WorkflowException   indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException    indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException indicates that the work-flow call failed because of storage problems internal to the
     *                             repository
     * @throws RemoteException     indicates that the work-flow call failed because of a connection problem with the
     *                             repository
     */
    void copy(Document destination, String newName, String branchId)
            throws WorkflowException, RepositoryException, RemoteException;

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
     * @return the updated target document (variant)
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
     * <p>
     *     Restores the {@code version} to the unpublished variant for branch with id {@code branchId}. If the current
     *     unpublished variant is not for {@code branchId}, first the branch for {@code branchId} will be checked out
     * </p>
     * @param version the {@link Version} to restore
     * @param branchId the id of the branch to restore to
     * @return the restored document (unpublished variant)
     * @throws WorkflowException if there does not exist a branch for {@code branchId} or some other workflow exception happens
     * @throws RepositoryException indicates that the work-flow call failed because of storage problems internal to the
     *                             repository
     */
    Document restoreVersionToBranch(Version version, final String branchId) throws WorkflowException, RepositoryException;

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
    @org.onehippo.repository.api.annotation.WorkflowAction(loggable = false, mutates = false)
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
    @org.onehippo.repository.api.annotation.WorkflowAction(loggable = false, mutates = false)
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
     * @return The {@link Set} of branches in version history
     *
     * @throws WorkflowException indicates that the work-flow call failed due work-flow specific conditions
     */
    @org.onehippo.repository.api.annotation.WorkflowAction(loggable = false, mutates = false)
    Set<String> listBranches() throws WorkflowException;

    /**
     * <p>
     *     Branches this document. During branching the following will happen
     *     <ol>
     *         <li>
     *             The current preview will be versioned into version history. If the current preview is for a branchId
     *             x, the x-preview version label will be moved to the newly created version. If the current preview is
     *             for core, the core-preview version label will be moved to the newly created version.
     *         </li>
     *         <li>
     *             The preview will be marked to be for branch {@code branchId}. If the {@code branchId} is equal to
     *             'core', the result will be that the branch information is removed from the preview.
     *         </li>
     *     </ol>
     * </p>
     * @param branchId the id of the branch that will be stored on the document variant
     * @param branchName the name that will be stored on the document variant for the branch
     * @return {@link Document} wrapping the workspace preview node variant
     * @throws WorkflowException In case the {@code branchId} already exists or branching is not allowed in the current
     *                           document state
     */
    Document branch(String branchId, String branchName) throws WorkflowException;

    /**
     * <p>
     *     Returns a {@link Document} for the combination {@code branchId} and {@code variant} or {@code null} if the
     *     {@code state} does not exist for {@code branchId}. The {@code branchId} is allowed to be 'master' in which
     *     the non-branch {@link Document} is returned if present.
     * </p>
     * @param branchId the id of the branch to get the {@link Document} for
     * @param state the {@link WorkflowUtils.Variant} that is requested
     * @return a {@link Document} where the backing {@link javax.jcr.Node} is for the {@code branchId} and {@code state}
     * or {@code null} in case the {@code state} is not available for {@code branchId}. The {@link javax.jcr.Node} can
     * be a state below the handle <em>or</em> a frozenNode from version history.
     * @throws WorkflowException If there is no branch for {@code branchId}
     */
    @org.onehippo.repository.api.annotation.WorkflowAction(loggable = false, mutates = false)
    Document getBranch(String branchId, WorkflowUtils.Variant state) throws WorkflowException;

    /**
     * <p>
     *     Removes the branch for {@code branchId} if it exists and throws a {@link WorkflowException} if it doesn't exist.
     * </p>
     * <p>
     *     Removing a branch is possible even when another branch is being edited, unless the branch for {@code branchId}
     *     is being edited.
     * </p>
     * <p>
     *     If the current <em>unpublished</em> variant is for the branch to be removed, then first the unpublished will
     *     be versioned, then from the unpublished the branch info will be removed and version history the labels for
     *     the branch will be removed, and then the core branch or any other branch if core does not exist is checked
     *     out, see {@link #checkoutBranch(String)}.
     * </p>
     * <p>
     *     If {@code branchId} is published (either as variant or in version history), this method will throw a
     *     WorkflowException.
     * </p>
     * @param branchId the {@code branchId} to remove
     * @return {@link Document} wrapping the workspace preview node variant
     * @throws WorkflowException in case {@code branchId} does not exist or when removeBranch
     *                            is not allowed in the current document state
     */
    void removeBranch(String branchId) throws WorkflowException;

    /**
     * <p>
     *     Tries to restore from version history the version with label '${branchId}-preview' and throws a {@link WorkflowException}
     *     if no such version exists. Before a version from version history is restored, the preview gets versioned.
     * </p>
     * <p>
     *     In case the current JCR workspace document variant is already for {@code branchId} this operation is allowed
     *     and will be a NOOP returning just the current JCR workspace document variant
     * </p>
     *  @param branchId the {@code branchId} of the branch to checkout
     *  @return {@link Document} wrapping the workspace preview node variant
     *  @throws WorkflowException In case the {@code branchId} does not exist in version history or when checkoutBranch
     *                            is not allowed in the current document state
     */
    Document checkoutBranch(String branchId) throws WorkflowException;

    /**
     * <p>
     *     Reintegrates the branch for {@code branchId}. Assume someone invokes {@code #reintegrateBranch('foo', true)}.
     *     As a result, the following happens
     *     <ol>
     *         <li>
     *             the 'foo-preview' version of the document gets published. Note that if the preview variant below the
     *             handle does not belong to project 'foo', first a checkout of 'foo' will be done (replacing the
     *             current preview) and then the preview will be published. This scenario is allowed EVEN when someone
     *             is editing the document:Assume an author is editing the branch 'bar'. In that case, the reintegrate
     *             won't touch the draft, and as a result, the author can continue working on the draft and won't notice
     *             that the preview might have changed. After saving the draft, the preview becomes again for branch
     *             'bar'.
     *         </li>
     *         <li>
     *             the new labels 'pre-reintegrate-core-live-x' and 'pre-reintegrate-core-preview-x' in version history
     *             will point to the core-live and core-preview labels before the reintegrate
     *             (x is a incremental counter for every reintegrate on this document). If there is no core-live present,
     *             the value of x in pre-reintegrate-core-live-x can contain gaps, for example jumping from 2 to 4.
     *             We add these extra labels to make sure that IF core-preview or live had changes which were not part of
     *             the reintegrate, these changes can always be found back in version history
     *          </li>
     *         <li>
     *             after the 'foo' version has been put live, the core-preview and core-live labels will be moved to
     *            'foo-preview' version.
     *         </li>
     *         <li>
     *             at the end, the branch 'foo' will be removed via {@link #removeBranch(String)}
     *         </li>
     *     </ol>
     * </p>
     * @param branchId the {@code branchId} to branch to reintegrate
     * @param publish {@code true} if the document also needs to be (re)published as part of the reintegrate
     * @throws WorkflowException if there is no branch for {@code branchId}
     */
    void reintegrateBranch(String branchId, boolean publish) throws WorkflowException;

    /**
     * <p>
     *     Publishes the branch for {@code branchId}. Note that publishing a branch can have as result that a published
     *     variant is created, but, if it already exists, can also result in only a marker in version history that the
     *     specific branch is live.
     * </p>
     * @param branchId the id of the branch to publish
     * @throws WorkflowException in case there does not exist a branch for {@code branchId} or when {@code branchId} is
     * equal to 'master' which is not allowed to be published as branch or in case the right unpublished version does not
     * exist
     */
    void publishBranch(String branchId) throws WorkflowException;

    /**
     * <p>
     *     Depublishes the branch for {@code branchId}. If there is no published version for {@code branchId}, a
     *     workflow exception is thrown. If you want to avoid a workflow exception, first check via
     *     {@link #getBranch(String, WorkflowUtils.Variant)} whether the published version for {@code branchId} exists
     *
     * </p>
     * @param branchId the id of the branch to publish
     * @throws WorkflowException in case there does not exist a branch for {@code branchId} or when {@code branchId} is
     * equal to 'master' which is not allowed to be depublished as branch or in case the right published version does not
     * exist
     */
    void depublishBranch(String branchId) throws WorkflowException;




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

    /**
     * If the unpublished variant or any of it's descendants has pending changes on the current workflow (i.e. there
     * are pending changes on the internal workflow session) then
     * - the lastModifiedBy of the unpublished variant is set to the id of the userSession
     * - the lastModificationDate of the unpublished variant  is set to now
     * - the internal workflow session is saved to persist the changes.
     * If there are no changes then calling this method will throw a {@link WorkflowException}.
     *
     * @throws WorkflowException if this action is not allowed based on the hints or if saving the changes fails
     */
    void saveUnpublished() throws WorkflowException;

    /**
     * <p>
     *     Marks a specific version from version history to be the live version between the dates {@code from} and
     *     {@code to}. If there is no frozen node for {@code uuid}, {@link WorkflowException} will be thrown.
     *     If the version to set as live campaign does not match the {@code branchId}, a WorkflowException will be thrown
     * </p>
     * <p>
     *     This method won't publish an unpublished document: for that, normal {@link #publishBranch(String)} must be
     *     invoked
     * </p>
     * @param frozenNodeId the uuid of the versioned JCR Node
     * @param branchId the branchId the versioned JCR Node must be for
     * @param from mandatory parameter for the date from which on this version must be served instead of published
     *             workspace version
     * @param to optional parameter for the date until which on this version must be served instead of published
     *           workspace version. If the parameter is missing, it is typically for an open-ended campaign document
     * @return the Document for {@code frozenNodeUUID}
     */
    Document campaign(String frozenNodeId, String branchId, Calendar from, Calendar to) throws WorkflowException;

    /**
     * <p>
     *     Removes the campaign if present for {@code frozeNodeId}
     * </p>
     * @param frozenNodeId
     * @return
     * @throws WorkflowException
     */
    Document removeCampaign(String frozenNodeId) throws WorkflowException;

}
