/*
 * Copyright 2013-2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.repository.documentworkflow;

import java.io.Serializable;
import java.security.AccessControlException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.RepositoryMap;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.repository.scxml.SCXMLDataModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentHandle implements SCXMLDataModel {

    private static final Logger log = LoggerFactory.getLogger(DocumentHandle.class);

    private final String scxmlId;

    private boolean initialized;

    private Map<String, Boolean> actions = new HashMap<>();
    private Object result;

    private final WorkflowContext context;
    private final Node handle;
    private final String user;

    private HandleDocumentWorkflow.SupportedFeatures supportedFeatures = HandleDocumentWorkflow.SupportedFeatures.all;

    private Map<String, DocumentVariant> documents;
    private Map<String, Request> requests;
    private boolean requestPending;

    private Map<String, Map<String, Boolean>> pathPrivilegesMap;

    private Map<String, Serializable> info = new HashMap<>();

    public DocumentHandle(String scxmlId, WorkflowContext context, Node handle) throws WorkflowException {
        this.scxmlId = scxmlId;
        this.context = context;
        this.handle = handle;
        this.user = context.getUserIdentity();
        initialize();
    }

    public void initialize() throws WorkflowException {
        result = null;
        if (!initialized) {
            try {
                RepositoryMap workflowConfiguration = context.getWorkflowConfiguration();
                if (workflowConfiguration.exists()) {
                    String supportedFeaturesConfiguration = (String) workflowConfiguration.get("workflow.supportedFeatures");
                    if (supportedFeaturesConfiguration != null) {
                        try {
                            supportedFeatures = HandleDocumentWorkflow.SupportedFeatures.valueOf(supportedFeaturesConfiguration);
                        } catch (IllegalArgumentException e) {
                            String configurationPath = (String) workflowConfiguration.get("_path");
                            if (configurationPath == null) {
                                configurationPath = "<unknown>";
                            }
                            log.warn("Unknown DocumentWorkflow.SupportedFeatures [{}] configured for property workflow.supportedFeatures at: {}",
                                    supportedFeaturesConfiguration, configurationPath);
                        }
                    }
                }

                for (Node variant : new NodeIterable(handle.getNodes(handle.getName()))) {
                    DocumentVariant doc = new DocumentVariant(variant);
                    if (documents != null && documents.containsKey(doc.getState())) {
                        log.warn("Document at path {} has multiple variants with state {}. Variant with identifier {} ignored.",
                                new String[]{handle.getPath(), doc.getState(), variant.getIdentifier()});
                    }
                    getDocuments(true).put(doc.getState(), doc);
                }

                for (Node requestNode : new NodeIterable(handle.getNodes(WorkflowRequest.HIPPO_REQUEST))) {
                    Request request = Request.createRequest(requestNode);
                    if (request != null) {
                        if (request.isWorkflowRequest()) {
                            getRequests(true).put(request.getIdentity(), request);
                            if (!WorkflowRequest.REJECTED.equals(((WorkflowRequest)request).getType())) {
                                requestPending = true;
                            }
                        }
                        else if (request.isScheduledRequest()) {
                            getRequests(true).put(request.getIdentity(), request);
                            requestPending = true;
                        }
                    }
                }
            }
            catch (RepositoryException e) {
                throw new WorkflowException("DocumentHandle initialization failed", e);
            }
            initialized = true;
        }
    }

    protected Map<String, DocumentVariant> getDocuments(boolean create) {
        if (create && documents == null) {
            documents = new HashMap<>();
        }
        if (documents != null) {
            return documents;
        }
        return Collections.emptyMap();
    }

    protected Map<String, Request> getRequests(boolean create) {
        if (create && requests == null) {
            requests = new HashMap<>();
        }
        if (requests != null) {
            return requests;
        }
        return Collections.emptyMap();
    }

    public String getScxmlId() {
        return scxmlId;
    }

    public Node getHandle() {
        return handle;
    }

    public Map<String, Request> getRequests() {
        return getRequests(false);
    }

    public boolean isRequestPending() {
        return requestPending;
    }

    /**
     * Checks if specific privileges (e.g. hippo:editor) are granted to the current user session against a specific
     * {@link Document}.
     * <p> Implementation note: previously evaluated privileges are cached against the Document node its path
     * within the DocumentHandle instance </p>
     *
     * @param document the document to check permission against
     * @param privileges the privileges (, separated) to check permission for
     * @return true if the current user session has been granted all of the specified privileges for the document node
     */
    public boolean isGranted(Document document, String privileges) {
        if (privileges == null || document == null || document.getIdentity() == null) {
            return false;
        }

        HashSet<String> privs = new HashSet<>(Arrays.asList(privileges.split(",")));
        if (privs.isEmpty()) {
            return false;
        }

        if (pathPrivilegesMap == null) {
            pathPrivilegesMap = new HashMap<>();
        }
        final Session userSession = context.getUserSession();
        try {
            Node userDocumentNode = userSession.getNodeByIdentifier(document.getIdentity());
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
                        hasPrivilege = Boolean.valueOf(userSession.hasPermission(userDocumentPath, priv));
                    } catch (AccessControlException e) {
                    } catch (AccessDeniedException e) {
                    } catch (IllegalArgumentException e) { // the underlying repository does not recognized the privileges requested.
                    } catch (RepositoryException e) {
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
     * @return configured Features or {@link org.onehippo.repository.documentworkflow.HandleDocumentWorkflow.SupportedFeatures#all}
     * by default
     */
    public HandleDocumentWorkflow.SupportedFeatures getSupportedFeatures() {
        return supportedFeatures;
    }

    public WorkflowContext getWorkflowContext() {
        return context;
    }

    public String getUser() {
        return user;
    }

    public void putDocumentVariant(DocumentVariant variant) throws RepositoryException {
        getDocuments(true).put(variant.getState(), variant);
    }

    public DocumentVariant getDocumentVariantByState(String state) {
        return getDocuments(false).get(state);
    }

    public Map<String, DocumentVariant> getDocuments() {
        return getDocuments(false);
    }

    public Map<String, Serializable> getInfo() {
        return info;
    }

    @Override
    public Map<String, Boolean> getActions() {
        return actions;
    }

    @Override
    public Object getResult() {
        return result;
    }

    @Override
    public void setResult(Object result) {
        this.result = result;
    }

    @Override
    public void reset() {
        initialized = false;
        actions.clear();
        info.clear();
        if (documents != null) {
            documents.clear();
        }
        if (requests != null) {
            requests.clear();
        }
        requestPending = false;
        if (pathPrivilegesMap != null) {
            pathPrivilegesMap.clear();
        }
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
