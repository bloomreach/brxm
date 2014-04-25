/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.repository.scxml;

import java.io.Serializable;
import java.security.AccessControlException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;

/**
 * A SCXMLWorkflowContext serves as the main bridge between the calling {@link org.hippoecm.repository.api.Workflow}
 * implementation and the SCXML state machine.
 * <p>
 * A SCXMLWorkflowContext is injected in the external context of an SCXML state machine by the
 * {@link SCXMLWorkflowExecutor} under the predefined and reserved {@link #SCXML_CONTEXT_KEY} ("workflowContext") key
 * to provide access to the {@link WorkflowContext} and its {@link #getActions()},
 * {@link #getFeedback()} and {@link #getResult()} state for the SCXML state machine.
 * </p>
 * <p>
 * The SCXML state machine uses the SCXMLWorkflowContext to communicate back the allowable actions, additional
 * feedback and possible state machine execution results to the the invoking workflow implementation.
 * </p>
 * <p>
 * In addition, the SCXMLWorkflowContext also provides a content authorization service through several isGranted(..)
 * methods which can be used from within the SCXML state machine to check if the current invoking user is granted
 * specific privileges to relevant JCR content.
 * </p>
 * <p>
 * The internal state of the SCXMLWorkflowContext is controlled and managed by the {@link SCXMLWorkflowExecutor} through
 * the {@link #initialize()} and {@link #reset()} methods. Extended versions of the SCXMLWorkflowContext must honor
 * these methods and not retain any internal state outside the scope of these method calls.
 * </p>
 */
public class SCXMLWorkflowContext {

    public static final String SCXML_CONTEXT_KEY = "workflowContext";

    private final String scxmlId;
    private final WorkflowContext workflowContext;
    private final Map<String, Boolean> actions = new HashMap<>();
    private final Map<String, Serializable> feedback = new HashMap<>();
    private final Map<String, Map<String, Boolean>> identifierPrivilegesMap = new HashMap<>();
    private Object result;
    private boolean initialized;

    public SCXMLWorkflowContext(String scxmlId, WorkflowContext workflowContext) {
        this.scxmlId = scxmlId;
        this.workflowContext = workflowContext;
    }

    /**
     * Checks if specific privileges (e.g. hippo:editor) are granted to the current workflow context its
     * {@link WorkflowContext#getSubjectSession()} for a specific {@link org.hippoecm.repository.api.Document}.
     * <p> Implementation note: previously evaluated privileges are cached against the Document node's identifier
     * within the SCXMLWorkflowContext instance </p>
     *
     * @param document the document to check permission for
     * @param privileges the privileges (, separated) to check permission for
     * @return true if the current subject session has been granted all the specified privileges for the document node
     */
    public final boolean isGranted(Document document, String privileges) {
        if (document == null) {
            return false;
        }

        return isGranted(document.getIdentity(), privileges);

    }

    /**
     * Checks if specific privileges (e.g. hippo:editor) are granted to the current workflow context its
     * {@link WorkflowContext#getSubjectSession()} for a specific {@link javax.jcr.Node}.
     * <p> Implementation note: previously evaluated privileges are cached against the node's identifier
     * within the SCXMLWorkflowContext instance </p>
     *
     * @param node the JCR node to check permission for
     * @param privileges the privileges (, separated) to check permission for
     * @return true if the current subject session has been granted all of the specified privileges for the JCR node
     */
    public final boolean isGranted(Node node, String privileges) {
        if (node == null) {
            return false;
        }

        try {
            return isGranted(node.getIdentifier(), privileges);
        } catch (RepositoryException e) {
            return false;
        }
    }

    private boolean isGranted(String identifier, String privileges) {
        if (identifier == null || privileges == null) {
            return false;
        }

        final Collection<String> privs = Arrays.asList(privileges.split(","));
        if (privs.isEmpty()) {
            return false;
        }

        try {
            final Session subjectSession = workflowContext.getSubjectSession();
            Node userDocumentNode = subjectSession.getNodeByIdentifier(identifier);
            String userDocumentPath = userDocumentNode.getPath();
            for (String priv : privs) {
                Map<String, Boolean> privilegesMap = identifierPrivilegesMap.get(identifier);
                if (privilegesMap == null) {
                    privilegesMap = new HashMap<>();
                    identifierPrivilegesMap.put(identifier, privilegesMap);
                }
                Boolean hasPrivilege = privilegesMap.get(priv);
                if (hasPrivilege == null) {
                    hasPrivilege = Boolean.FALSE;
                    try {
                        hasPrivilege = subjectSession.hasPermission(userDocumentPath, priv);
                    } catch (AccessControlException | IllegalArgumentException | RepositoryException ignore) {
                    }
                    privilegesMap.put(priv, hasPrivilege);
                }
                if (!hasPrivilege) {
                    return false;
                }
            }
            return true;
        }
        catch (RepositoryException e) {
            return false;
        }

    }

    /**
     * @return the unique SCXML state machine (repository) id used for retrieving through the {@link SCXMLRegistry}
     */
    public final String getScxmlId() {
        return scxmlId;
    }

    /**
     * @return the invoking workflow its context
     */
    public final WorkflowContext getWorkflowContext() {
        return workflowContext;
    }

    /**
     * @return the current invoking workflow user
     */
    public final String getUser() {
        return workflowContext.getUserIdentity();
    }

    /**
     * The actions map is used by the SCXML state machine to communicate the currently (e.g. based upon the current
     * state machine state) available events which it can handle transitions for.
     * <p>
     * Typically these actions should be logically named after the supported invoking workflow operations (method names)
     * and either not be set at all (meaning: current user doesn't have the appropriate privileges), or be configured
     * with value Boolean.FALSE if the action is not possible within the current state configuration.
     * </p>
     * <p>
     * Only actions which are configured with value Boolean.TRUE through this map will be allowed to be executed against
     * the SCXML state machine, which is checked first by the {@link SCXMLWorkflowExecutor} before they are 'triggered'
     * as an event against the SCXML state machine.
     * </p>
     *
     * @return the map of actions available to the invoking workflow as allowable operations / events to be
     * fired.
     */
    public final Map<String, Boolean> getActions() {
        return actions;
    }

    /**
     * @return an optional map of additional information (like messages, info, data, etc.) to be communicated back to
     * the invoking workflow implementation, besides the {@link #getActions()} and possible (single) {@link #getResult()}
     * object.
     */
    public final Map<String, Serializable> getFeedback() {
        return feedback;
    }

    /**
     * @return an optional result object set by the SCXML state machine after processing one SCXML state machine
     * evaluation (start or triggered event). Note: The result object is always reset to again before invoking the state
     * machine.
     */
    public final Object getResult() {
        return result;
    }

    /**
     * Method reserved for the SCXML state machine to set a possible result object, NOT to be invoked outside
     * the scope of the SCXML state machine execution.
     * @param result
     */
    public final void setResult(Object result) {
        this.result = result;
    }

    protected boolean isInitialized() {
        return initialized;
    }

    /**
     * Invoked by the {@link SCXMLWorkflowExecutor} when starting the SCXML state machine
     * @throws WorkflowException
     */
    protected void initialize() throws WorkflowException {
        if (initialized) {
            reset();
        }
        initialized = true;
    }

    /**
     * Invoked by the {@link SCXMLWorkflowExecutor} when resetting the SCXML state machine
     * @throws WorkflowException
     */
    protected void reset() {
        if (initialized) {
            actions.clear();
            feedback.clear();
            identifierPrivilegesMap.clear();
            result = null;
            initialized = false;
        }
    }
}
