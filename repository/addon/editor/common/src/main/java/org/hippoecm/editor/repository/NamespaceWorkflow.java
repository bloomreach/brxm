/*
 *  Copyright 2008 Hippo.
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
    final static String SVN_ID = "$Id$";

    /**
     * Add a new type descriptor node.
     * <p>
     * The node is created by using a type template.  The list of available templates can be
     * obtained by using hints().get("templates").  This is a Set of Strings.
     * 
     * @param template the template to use.  One of the entries in 
     * @param name
     * @throws WorkflowException
     * @throws MappingException
     * @throws RepositoryException
     * @throws RemoteException
     */
    void addType(String template, String name) throws WorkflowException, MappingException, RepositoryException, RemoteException;

    void updateModel(String cnd, Object updates) throws WorkflowException, MappingException, RepositoryException, RemoteException;
}
