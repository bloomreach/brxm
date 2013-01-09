/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;

/**
 * The interface available container documents (typically folders or directories)
 * A class implementing CopyWorkflow is returned by one of the calls in #WorkflowManager returning a #Workflow.
 */
public interface CopyWorkflow extends Workflow {

    /**
     * Creates a new document below the container document that this work-flow refers to, based upon another document.
     * When appropriate, the contents will be copied; this is up to the implementation.
     * @param target the source document which should be copied (or otherwise to be consulted to be the source of the copy)
     * @param newName the new name of the document.
     * @throws WorkflowException in case the work-flow did not succeed because the operation was refused, like insufficient rights
     * @throws RepositoryException upon an generic internal error while communicating with the repository
     * @throws MappingException in case the work-flow could not properly locate the source
     * @throws RemoteException upon a connection problem with the repository
     */
    public void copy(Document target, String newName)
        throws WorkflowException, MappingException, RepositoryException, RemoteException;
}
