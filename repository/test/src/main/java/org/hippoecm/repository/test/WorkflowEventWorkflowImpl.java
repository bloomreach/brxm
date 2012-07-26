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
package org.hippoecm.repository.test;

import java.rmi.RemoteException;
import java.util.Iterator;
import javax.jcr.RepositoryException;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.hippoecm.repository.standardworkflow.WorkflowEventWorkflow;

public class WorkflowEventWorkflowImpl extends WorkflowImpl implements WorkflowEventWorkflow {

    private long counter;

    public WorkflowEventWorkflowImpl() throws RemoteException {
    }

    public void fire() throws WorkflowException, MappingException {
        throw new WorkflowException("unexpected fire method called");
    }

    public void fire(Document document) throws WorkflowException, MappingException {
        throw new WorkflowException("unexpected fire method called");
    }

    public void fire(Iterator<Document> documentIterator) throws WorkflowException, MappingException {
        while(documentIterator.hasNext()) {
            Document document = documentIterator.next();
            try {
                Workflow documentWorkflow = getWorkflowContext().getWorkflow("postprocess", document);
                PostProcessWorkflow postprocessWorkflow = (PostProcessWorkflow) documentWorkflow;
                postprocessWorkflow.setIdentifier(counter);
                ++counter;
            } catch(RepositoryException ex) {
                System.err.println(ex.getClass().getName()+": "+ex.getMessage());
                ex.printStackTrace(System.err);
            }
        }
    }
}
