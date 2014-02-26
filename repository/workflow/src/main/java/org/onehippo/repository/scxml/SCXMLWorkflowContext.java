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

public class SCXMLWorkflowContext {

    public static final String SCXML_CONTEXT_KEY = "workflowContext";

    private final String scxmlId;
    private final WorkflowContext workflowContext;
    private final Map<String, Boolean> actions = new HashMap<>();
    private final Map<String, Serializable> feedback = new HashMap<>();
    private final Map<String, Map<String, Boolean>> pathPrivilegesMap = new HashMap<>();
    private Object result;
    private boolean initialized;

    public SCXMLWorkflowContext(String scxmlId, WorkflowContext workflowContext) {
        this.scxmlId = scxmlId;
        this.workflowContext = workflowContext;
    }

    /**
     * Checks if specific privileges (e.g. hippo:editor) are granted to the current workflow subject (e.g. handle) its session for a specific
     * {@link org.hippoecm.repository.api.Document}.
     * <p> Implementation note: previously evaluated privileges are cached against the Document node its path
     * within the DocumentHandle instance </p>
     *
     * @param document the document to check permission for
     * @param privileges the privileges (, separated) to check permission for
     * @return true if the current subject session has been granted all of the specified privileges for the document node
     */
    public final boolean isGranted(Document document, String privileges) {
        if (privileges == null || document == null || document.getIdentity() == null) {
            return false;
        }

        Collection<String> privs = Arrays.asList(privileges.split(","));
        if (privs.isEmpty()) {
            return false;
        }

        try {
            final Session subjectSession = workflowContext.getSubjectSession();
            Node userDocumentNode = subjectSession.getNodeByIdentifier(document.getIdentity());
            String userDocumentPath = userDocumentNode.getPath();
            for (String priv : privs) {
                Map<String, Boolean> privilegesMap = pathPrivilegesMap.get(userDocumentPath);
                if (privilegesMap == null) {
                    privilegesMap = new HashMap<>();
                    pathPrivilegesMap.put(userDocumentPath, privilegesMap);
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

    public final String getScxmlId() {
        return scxmlId;
    }

    public final WorkflowContext getWorkflowContext() {
        return workflowContext;
    }

    public final String getUser() {
        return workflowContext.getUserIdentity();
    }

    public final Map<String, Boolean> getActions() {
        return actions;
    }

    public final Map<String, Serializable> getFeedback() {
        return feedback;
    }

    public final Object getResult() {
        return result;
    }

    public final void setResult(Object result) {
        this.result = result;
    }

    protected boolean isInitialized() {
        return initialized;
    }

    protected void initialize() throws WorkflowException {
        if (initialized) {
            reset();
        }
        initialized = true;
    }

    protected void reset() {
        if (initialized) {
            actions.clear();
            feedback.clear();
            pathPrivilegesMap.clear();
            result = null;
            initialized = false;
        }
    }
}
