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
package org.hippoecm.repository.api;

import javax.jcr.RepositoryException;

/**
 * A workflow context class is make available to a workflow implementation (see WorkflowImpl@getWorkflowContext) to obtain additional
 * information during the execution of a workflow step.
 */
public interface WorkflowContext {
    /**
     * 
     */
    static final String SVN_ID = "$Id$";

    /**
     * Obtains an alternative workflow context, which has special behaviour depending on the specification parameter passed.
     * Any workflow obtained though an alternative workflow context (see #getWorkflow) will be subject to the alternate rules
     * as indicated by the specification.  For example, the specification may indicate that any workflow step invocation must
     * not be performed immediately, but on a specific date.
     * @param specification implementation dependent specification, alternate workflow context implementations are passed
     * this object in order to pass parameters.  The type of the object also determins which alternative implementation is used.
     * @return a workflow context with alternate behaviour
     * @throws org.hippoecm.repository.api.MappingException when no implementation is available for the specificaiton passed
     * @throws javax.jcr.RepositoryException when a generic error happens
     */
    public WorkflowContext getWorkflowContext(Object specification) throws MappingException, RepositoryException;

    /**
     * Obtains a Document implementation --a class which extends Document-- represeting the document stored as JCR nodes as
     * indicated by the identifier (implementation dependent on category, mostly a UUID or path) as specified by a configuration
     * indicated by the category
     * @param category defines the category to use as search query to find document definitions
     * @param identifier an indicator which document stored as JCR nodes to map to a Java object
     * @return an instance of a specific Document type
     * @throws javax.jcr.RepositoryException a generic repostory error occurs
     */
    public Document getDocument(String category, String identifier) throws RepositoryException;

    /**
     * Obtains a workflow instance for the same document this workflow context 
     * @param category 
     * @return 
     * @throws MappingException
     * @throws WorkflowException 
     * @throws RepositoryException
     */
    public Workflow getWorkflow(String category) throws MappingException, WorkflowException, RepositoryException;

    /**
     * 
     * @param category
     * @param document
     * @return
     * @throws org.hippoecm.repository.api.MappingException
     * @throws org.hippoecm.repository.api.WorkflowException
     * @throws javax.jcr.RepositoryException
     */
    public Workflow getWorkflow(String category, Document document) throws MappingException, WorkflowException,
                                                                           RepositoryException;

    /**
     * 
     * @return
     */
    public String getUserIdentity();

    /**
     * 
     * @return
     */
    public RepositoryMap getWorkflowConfiguration();
}
