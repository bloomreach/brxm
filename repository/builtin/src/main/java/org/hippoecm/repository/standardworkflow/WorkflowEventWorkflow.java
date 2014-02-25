/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.rmi.RemoteException;
import java.util.Iterator;
import javax.jcr.RepositoryException;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;

/**
 * Work-flows that are executed as part of work-flow events must implement this interface.
 * This ensures that when a work-flow event is fired, the repository knows which interface to
 * call automatically.  Only a single fire method is applicable at a specific work-flow event
 * that is triggered.  Which one depends on the actual work-flow call (which caused the trigger
 * to fire) and condition that is being met.
 * @deprecated to do workflow post processing, use the event bus mechanism instead
 */
@Deprecated
public interface WorkflowEventWorkflow extends Workflow {

    /**
     * This method is called when a work-flow event is fired in case the triggering workflow method
     * does not return a Document, nor a result-set was returned by the conditional event.
     * @throws WorkflowException may be thrown when the work-flow finds a custom erroneous exception or forbids the action
     * @throws MappingException is thrown when the work-flow is not applicable to the document
     * @throws RepositoryException is thrown in case an internal erroneous condition in the repository occurs
     * @throws RemoteException part of the Remote interface, should not be thrown or caught directly
     */
    public void fire() throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * This method is called in case the work-flow that triggered the work-flow event returned a return object
     * of type Document.
     * @param document the document object returned by the original work-flow
     * @throws WorkflowException may be thrown when the work-flow finds a custom erroneous exception or forbids the action
     * @throws MappingException is thrown when the work-flow is not applicable to the document
     * @throws RepositoryException is thrown in case an internal erroneous condition in the repository occurs
     * @throws RemoteException part of the Remote interface, should not be thrown or caught directly
     */
    public void fire(Document document) throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * This method is called in case of a hipposys:workflowsimplequeryevent specified work-flow configuration where the
     * final result set (as specified by the operator) yields a positive number of documents.
     * @param documents an iteration over the documents that where involved in the action, in case the work-flow action is
     * specified as a set operation the actual result set of the modified documents.
     * @throws WorkflowException may be thrown when the work-flow finds a custom erroneous exception or forbids the action
     * @throws MappingException is thrown when the work-flow is not applicable to the document
     * @throws RepositoryException is thrown in case an internal erroneous condition in the repository occurs
     * @throws RemoteException part of the Remote interface, should not be thrown or caught directly
     */
    public void fire(Iterator<Document> documents) throws WorkflowException, MappingException, RepositoryException, RemoteException;
}
