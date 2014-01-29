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

import java.util.Calendar;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import java.rmi.RemoteException;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.api.annotation.WorkflowAction;

/**
 * Work-flow interface that is always available on all documents within the work-flow category "version" passed to methods like
 * #WorkflowManager.getWorkflow(String,Node).  Although any document is in principle always versionable, not all document work-flow
 * actually use it.  The VersionWorkflow is normally not directly invoked, but from other work-flows that implement business logic
 * to determine when to version documents.  The default publication work-flow versions documents whenever a document is published,
 * for instance.  So it invokes the VersionWorkflow.version() whenever its publish action is invoked.
 *
 * @deprecated since CMS 7.9, use/configure {@link org.onehippo.repository.documentworkflow.HandleDocumentWorkflow} instead.
 */
@Deprecated
public interface VersionWorkflow extends Workflow {

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
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Restores a document to one of its previous states, at which point the #version() call was made.
     * @param historic the exact date on which the version was created using the #version() call.  The actual dates available can
     * be retrieved using the #list() method.
     * @return the restored document variant
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public Document revert(Calendar historic)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Restore a historic version by putting its contents in the target document.
     * Can only be used when the work-flow is used on an nt:version Node (a {@link javax.jcr.Version}).
     * @param target the Document representation of the target node
     * @return the updated target node
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public Document restoreTo(Document target)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Restore a
     * @param historic
     * @return
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public Document restore(Calendar historic)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * 
     * @param historic
     * @param replacements
     * @return
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     *
     * @deprecated use the {@link #restore(java.util.Calendar)} method and implement any further operations on the returned document.
     */
    @Deprecated
    public Document restore(Calendar historic, Map<String, String[]> replacements)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Lists the historic versions of a documents that are available.  A historic version is created using the {@link version()}
     * call.  The time at which such a call is made is listed at key item in the returned map.  This time may be used in a call
     * to {@link revert} or {@link restore}
     * @return A time-ordered map from earliest to latest of historic version (the timestamps at which #version() was called)
     * mapped to a list of symbolic names that were given to the versions.  The symbolic names currently cannot be set using
     * this work-flow interface, but can be set using the regular JCR API.
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    @WorkflowAction(loggable = false)
    public SortedMap<Calendar,Set<String>> list()
      throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * 
     * @param historic
     * @return
     * @throws WorkflowException  indicates that the work-flow call failed due work-flow specific conditions
     * @throws MappingException indicates that the work-flow call failed because of configuration problems
     * @throws RepositoryException  indicates that the work-flow call failed because of storage problems internal to the repository
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    @WorkflowAction(loggable = false)
    public Document retrieve(Calendar historic)
      throws WorkflowException, MappingException, RepositoryException, RemoteException;
}
