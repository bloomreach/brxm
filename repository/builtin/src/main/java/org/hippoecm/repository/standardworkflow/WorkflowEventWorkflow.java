/*
 *  Copyright 2011 Hippo.
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
 * Workflows that are executed as part of workflow events must implement this interface.
 * This ensures that when a workflow event is fired, the repository knows which interface to
 * call automatically.  Only a single fire method is applicable at a specific workflow event
 * that is triggered.  Which one depends on the actual workflow call (which caused the trigger
 * to fire) and condition that is being met.
 * @deprecated use interface WorkflowEventsWorkflow (plural) instead.
 */
public interface WorkflowEventWorkflow extends Workflow {
    /**
     * @exclude
     */
    static final String SVN_ID = "$Id$";

    /**
     * This method is called when a workflow event is fired in case the triggering workflow method
     * does not return a Document, nor a result-set was returned by the conditional event.
     * @throws WorkflowException may be thrown when the workflow finds a custom erroneous exception or forbids the action
     * @throws MappingException is thrown when the workflow is not applicable to the document
     * @throws RepositoryException is thrown in case an internal erroneous condition in the repository occurs
     * @throws RemoteException part of the Remote interface, should not be thrown or caught directly
     */
    public void fire() throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * This method is called in case the workflow that triggered the workflow event returned a return object
     * of type Document.
     * @param document the document object returned by the original workflow
     * @throws WorkflowException may be thrown when the workflow finds a custom erroneous exception or forbids the action
     * @throws MappingException is thrown when the workflow is not applicable to the document
     * @throws RepositoryException is thrown in case an internal erroneous condition in the repository occurs
     * @throws RemoteException part of the Remote interface, should not be thrown or caught directly
     */
    public void fire(Document document) throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * This method is called in case of a hipposys:workflowsimplequeryevent specified workflow configuration where the
     * final resultset (as specifield by the operator) yields a positive number of documents.
     * @param documents an iteration over the documents that where involved in the action, in case the workflow action is
     * specified as a set operation the actual result set of the modified documents.
     * @throws WorkflowException may be thrown when the workflow finds a custom erroneous exception or forbids the action
     * @throws MappingException is thrown when the workflow is not applicable to the document
     * @throws RepositoryException is thrown in case an internal erroneous condition in the repository occurs
     * @throws RemoteException part of the Remote interface, should not be thrown or caught directly
     */
    public void fire(Iterator<Document> documents) throws WorkflowException, MappingException, RepositoryException, RemoteException;
}
