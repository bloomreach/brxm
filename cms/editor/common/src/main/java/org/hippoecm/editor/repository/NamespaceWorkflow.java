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
package org.hippoecm.editor.repository;

import java.rmi.RemoteException;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;

public interface NamespaceWorkflow extends Workflow {

    /**
     * Add a new document type descriptor node with 'basedocument' as the super type
     */
    void addDocumentType(String name) throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Add a new document type descriptor node with super type.
     */
    void addDocumentType(String name, String superType) throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Add a new compound type descriptor node.
     */
    void addCompoundType(String name) throws WorkflowException, MappingException, RepositoryException, RemoteException;

}
