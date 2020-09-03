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
package org.hippoecm.repository.standardworkflow;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.api.annotation.WorkflowAction;
import org.onehippo.repository.branch.BranchConstants;

/**
 * This interface is available as work-flow interface on documents which implement a default flow how to edit the documents.
 * In the model, some documents cannot be edited directly, as they may be used directly on a site, multiple people may not
 * simultaneously edit a document, or for another reason this may a work-flow is desirable.  This is a default interface to
 * be used in those cases.  To edit a document, the #obtainEditableInstance() should be used, providing the proper document
 * variant suitable for editing.  That document variant should always allow a document to be committed, or changes to be disposed,
 * using the EditableWorkflow interface returned by #WorflowManager.getWorkflow in the "edit" category.
 */
public interface EditableWorkflow extends Workflow {

    /**
     * Request the editable copy of the {@link BranchConstants#MASTER_BRANCH_ID} document.
     * @return A reference to the document that may actually be modified, and should either be committed or disposed.
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public Document obtainEditableInstance()
            throws WorkflowException, MappingException, RepositoryException, RemoteException;


    /**
     *
     * @param branchId
     * @return
     * @throws WorkflowException
     * @throws MappingException
     * @throws RepositoryException
     * @throws RemoteException
     */
    Document obtainEditableInstance(String branchId)
            throws WorkflowException, MappingException, RepositoryException, RemoteException;


    /**
     * Persists editable copy of the document, the editable variant of the document is no longer available after this call.
     * @return the document which has been persisted back.  This may be a different document variant than the one used to do this
     * call and may be different from the original used to obtain a editable copy.  To further continue working on the document,
     * the returned document should be used, which is no longer an editable copy.
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public Document commitEditableInstance()
            throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Do away with the editable copy of the document which was previously
     * obtained.
     * @return the document variant that is suitable to be used for further work-flow calls
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public Document disposeEditableInstance()
            throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Marks the draft as transferable. Other users are allowed to become the holder of the document.
     * @return the document which has been marked as draft.
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    Document saveDraft()
            throws WorkflowException, MappingException, RepositoryException, RemoteException;


    /**
     * Requests the current draft variant of the document
     * @return The draft variant of the document as is
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    Document editDraft()
            throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Compare the editable copy of the document with the last persisted version.
     * The hint for availability of this method is 'checkModified'.
     *
     * @return true if the content is different, false otherwise
     *
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    @WorkflowAction(loggable = false)
    public boolean isModified()
            throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * <p>
     * Same as for {@link #hints()} (which returns hints for master) only now the hints for a specific {@code branchId}
     * </p>
     * <p>
     *     The {@code branchId} is allowed to be a non-existing branch: then just the hints for a non existing branch
     *     are returned, which in general results in not many actions being allowed
     * </p>
     *
     * @param branchId the branch to request the hints for.
     * @see #hints()
     */
    @WorkflowAction(loggable = false, mutates = false)
    Map<String, Serializable> hints(String branchId) throws WorkflowException, RemoteException, RepositoryException;
}
