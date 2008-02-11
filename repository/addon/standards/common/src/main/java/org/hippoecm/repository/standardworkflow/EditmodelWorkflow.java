/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.standardworkflow;

import java.rmi.RemoteException;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.MappingException;

public interface EditmodelWorkflow extends Workflow {
    /**
     * Returns the path of the JCR node containing the template definition to
     * edit.  Because this function is invoked on the template definition
     * itself this is at this time equivalent to returning the UUID of the
     * same node.
     */
    public String edit() throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Return the path of the JCR node containing a new the template
     * definition to edit.  This new template definition is created by first
     * copying the node definition of the template definition indicated.
     * The created template definition resides within the same namespace.
     */
    public String copy(String name) throws WorkflowException, MappingException, RepositoryException, RemoteException;
}
