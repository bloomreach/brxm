/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.api;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.onehippo.repository.api.annotation.WorkflowAction;

/**
 * A workflow is a set of procedures that can be performed on a document in the repository.  These procedures can be
 * accessed by obtaining a Workflow implementation from the {@link WorkflowManager}.  Calling a method that is defined
 * by an interface extending the {@link Workflow} interface causes such a workflow step (procedure) to be executed.
 * Which workflow is active on a document depends amongst others on the type of document.  Therefor the {@link Workflow}
 * interface itself defines no workflow actions, but any {@link Workflow} instance should be cast to a document-specific
 * interface. The implementation of these document-specific workflows can be provided at run-time to the repository.
 * Therefor there is no standard set of workflows available.  There are a number of  commonly available workflows, but
 * these are not mandatory.  See all known sub-interfaces of the {@link Workflow} interface, or
 * org.hippoecm.repository.standardworkflow.FolderWorkflow for an example.
 * <p/>
 * Implementors of this interface should never return subclasses of the {@link Document} class in their interface.  It
 * is allowed to return an instance of a subclass of a {@link Document}, but the repository will force recreating the
 * object returned as a direct instance of an {@link Document}.
 */
public interface Workflow extends Remote, Serializable {

    /**
     * The hints method is not an actual workflow call, but a method by which information can be retrieved from the
     * workflow.  All implementations must implement this call as a pure function, no modification may be made, nor no
     * state may be maintained and and in principle no additional lookups of data is allowed.  This allows for caching
     * the result as long as the document on which the workflow operates isn't modified. By convention, keys that are
     * names or signatures of methods implemented by the workflow provide information to the application program whether
     * the workflow method is available this time, or will result in a WorkflowException.  The value for these keys will
     * often be a {@link java.lang.Boolean} to indicate the enabled status of the method.<p/> Non-standard keys in this
     * map should be prefixed with the implementation package name using dot seperations.
     *
     * @return a map containing hints given by the workflow, the data in this map may be considered valid until the
     * document itself changes
     * @throws org.hippoecm.repository.api.WorkflowException thrown in case the implementing workflow encounters an
     *                                                       error, this exception should normally never be thrown by
     *                                                       implementations for the hints method.
     * @throws java.rmi.RemoteException                      a connection error with the repository
     * @throws javax.jcr.RepositoryException                 a generic error communicating with the repository
     */
    @WorkflowAction(loggable = false, mutates = false)
    public Map<String, Serializable> hints() throws WorkflowException, RemoteException, RepositoryException;
}
