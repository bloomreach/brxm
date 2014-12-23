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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * A workflow context class is made available to a workflow implementation (see WorkflowImpl@getWorkflowContext) to obtain additional
 * information during the execution of a workflow step.
 */
public interface WorkflowContext {

    /**
     * Obtains a workflow instance for this workflow context its subject.
     * <p>
     *   Be aware that the returned workflow is checked against the {@link #getInternalWorkflowSession()} privileges,
     *   so <b>NOT</b> against the initial workflow invocation {@link #getUserSession()}.
     * </p>
     * <p>
     *   For the returned workflow it is assumed there are no pending changes on its {@link #getInternalWorkflowSession()}.
     *   So before using the returned workflow it might be required to first do a {@link Session#save()}
     *   or {@link Session#refresh(boolean) Session.refresh(false)}.
     * </p>
     * @param category Name of the workflow category
     * @return a workflow instance
     * @throws MappingException
     * @throws WorkflowException
     * @throws RepositoryException
     */
    public Workflow getWorkflow(String category) throws WorkflowException, RepositoryException;

    /**
     * Obtains a workflow instance for a document subject
     * <p>
     *   Be aware that the returned workflow is checked against the {@link #getInternalWorkflowSession()} privileges,
     *   so <b>NOT</b> against the initial workflow invocation {@link #getUserSession()}.
     * </p>
     * <p>
     *   For the returned workflow it is assumed there are no pending changes on its {@link #getInternalWorkflowSession()}.
     *   So before using the returned workflow it might be required to first do a {@link Session#save()}
     *   or {@link Session#refresh(boolean) Session.refresh(false)}.
     * </p>
     * @param category Name of the workflow category
     * @param document Document subject for which the new workflow instance will be returned
     * @return a workflow instance
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
     * Obtains the initial workflow invocation (user) session.
     * @return the invocation user session
     */
    public Session getUserSession();

    /**
     * Obtains the subject of this WorkflowContext
     * @return the subject of this WorkflowContext
     */
    public Node getSubject();

    /**
     * Obtain the subject session used to check and load the current workflow.
     * <p>
     *   For an initial workflow invocation this will be, in most cases, <em>but not all, see below</em>, the {@link #getUserSession()}.<br/>
     *   For any subsequent (nested) workflow invocation this will be the {@link #getInternalWorkflowSession()}.
     * </p>
     * <p>
     *   If the initial workflow was invoked through {@link WorkflowManager#getWorkflow(String, Node)}, the provided node parameter
     *   its own session will be used as subject session, so potentially <em>different</em> from the user session invoking the workflow!
     * </p>
     * <p>
     *   This separate session reference is useful to check {@link Session#hasPermission(String, String) JCR permissions} within the workflow as was used to
     *   validate access the workflow itself.
     * </p>
     * @return the session to check permissions on and within the current workflow itself
     */
    public Session getSubjectSession();

    /**
     * Obain the internal workflow session which has 'root' privileges. This session is internally used by the
     * workflow and used to persist workflow modifications. Also, the workflow subject (Node) itself will be loaded through
     * this internal workflow session.
     * <p>
     *   Do <b>NOT</b> return JCR Items loaded through this internal workflow session back to the invoking user,
     *   as it will expose access to this session.
     * </p>
     * <p>
     *  <b>Be very careful making changes through the internal workflow session: state consistency can easily broken that way.</b>
     * </p>
     * @return the internal workflow session with higer(st) privileges: be aware of possible dangerous side-effects when used for modifications
     */
    public Session getInternalWorkflowSession();

    public RepositoryMap getWorkflowConfiguration();

    public String getInteraction();

    public String getInteractionId();
}