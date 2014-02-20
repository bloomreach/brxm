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
package org.hippoecm.repository.api;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * A workflow context class is made available to a workflow implementation (see WorkflowImpl@getWorkflowContext) to obtain additional
 * information during the execution of a workflow step.
 */
public interface WorkflowContext {

    /**
     * Obtains an alternative work-flow context, which has special behavior depending on the specification parameter passed.
     * Any work-flow obtained though an alternative work-flow context (see #getWorkflow) will be subject to the alternate rules
     * as indicated by the specification.  For example, the specification may indicate that any work-flow step invocation must
     * not be performed immediately, but on a specific date.
     * @param specification implementation dependent specification, alternate work-flow context implementations are passed
     * this object in order to pass parameters.  The type of the object also determines which alternative implementation is used.
     * @return a work-flow context with alternate behavior
     * @throws org.hippoecm.repository.api.MappingException when no implementation is available for the specification passed
     * @throws javax.jcr.RepositoryException when a generic error happens
     * @deprecated
     */
    @Deprecated
    public WorkflowContext getWorkflowContext(Object specification) throws RepositoryException;

    /**
     * Obtains a workflow instance for the same document this workflow context 
     * @param category 
     * @return 
     * @throws MappingException
     * @throws WorkflowException 
     * @throws RepositoryException
     */
    public Workflow getWorkflow(String category) throws WorkflowException, RepositoryException;

    /**
     * 
     * @param category
     * @param document
     * @return
     * @throws org.hippoecm.repository.api.MappingException
     * @throws org.hippoecm.repository.api.WorkflowException
     * @throws javax.jcr.RepositoryException
     */
    public Workflow getWorkflow(String category, Document document) throws WorkflowException, RepositoryException;

    /**
     * 
     * @return the invocation user identity
     */
    public String getUserIdentity();
    
    /**
     * Obtains the current workflow invocation (user) session.
     * Note that this doesn't have to be the first invocation session as workflows can be chained.
     * <p>
     *   <b>Do not use this session to make any changes as it might interfere with the workflow invocation state itself.</b>
     * </p>
     * @return the invocation user session
     */
    public Session getUserSession();

    /**
     * Obtain the internal workflow session which has 'root' privileges. This session is internally used by the
     * workflow and used to persist workflow modifications.
     * <p>
     *  <b>Be very careful making changes through the internal workflow session: state consistency can easily broken that way.</b>
     * </p>
     * <p>
     *  <b>Do not try to modify workflow managed Documents directly through the JCR API: that most certainly will lead to inconsistent state or even data corruption!</b>
     * </p>
     * <p>
     *   <b>NEVER invoke session.save() yourself through the internal workflow session!</b><br/>
     *   The workflow process hasn't been completed yet and doing intermediate saves easily can result in inconsistent (persisted) state.<br/>
     *   The workflow process will do a session.save() (or revert) automatically at the end of the processing.
     * </p> 
     * @return the internal workflow session with higer(st) privileges: be aware of possible dangerous side-effects when used for modifications
     */
    public Session getInternalWorkflowSession();

    /**
     * 
     * @return
     */
    public RepositoryMap getWorkflowConfiguration();
}
