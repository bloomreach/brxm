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
import org.hippoecm.repository.api.RepositoryMap;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.repository.scxml.SCXMLDataModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentHandle implements SCXMLDataModel {

    private static final Logger log = LoggerFactory.getLogger(DocumentHandle.class);

    private Map<String, Boolean> actions = new HashMap<>();
    private Object result;

    private final WorkflowContext context;
    private final Node handle;
    private final String user;

    private DocumentWorkflow.SupportedFeatures supportedFeatures = DocumentWorkflow.SupportedFeatures.all;

    private Map<String, PublishableDocument> documents = null;
    private PublicationRequest rejectedRequest = null;
    private PublicationRequest request = null;

    private String states;

    private Map<String, Boolean> privilegesMap;

    private Map<String, Serializable> info = new HashMap<>();

    public DocumentHandle(WorkflowContext context, Node handle) throws RepositoryException {
        this.context = context;
        this.handle = handle;
        this.user = context.getUserIdentity();

        RepositoryMap workflowConfiguration = context.getWorkflowConfiguration();
        if (workflowConfiguration.exists()) {
            String supportedFeaturesConfiguration = (String) workflowConfiguration.get("workflow.supportedFeatures");
            if (supportedFeaturesConfiguration != null) {
                try {
                    supportedFeatures = DocumentWorkflow.SupportedFeatures.valueOf(supportedFeaturesConfiguration);
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
            PublishableDocument doc = new PublishableDocument(variant);
            if (documents != null && documents.containsKey(doc.getState())) {
                log.warn("Document at path {} has multiple variants with state {}. Variant with identifier {} ignored.",
                        new String[]{handle.getPath(), doc.getState(), variant.getIdentifier()});
            }
            getDocuments(true).put(doc.getState(), doc);
        }

        for (Node requestNode : new NodeIterable(handle.getNodes(PublicationRequest.HIPPO_REQUEST))) {
            PublicationRequest req = new PublicationRequest(requestNode);
            String requestType = JcrUtils.getStringProperty(requestNode, PublicationRequest.HIPPOSTDPUBWF_TYPE, "");
            if ("rejected".equals(requestType)) {
                if (req.getOwner() != null && !req.getOwner().equals(user)) {
                    continue;
                }
                if (rejectedRequest != null) {
                    if (rejectedRequest.getOwner() != null && req.getOwner() != null) {
                        log.warn("Document at path {} has multiple rejected requests. Request with identifier {} ignored.",
                                new String[]{handle.getPath(), requestNode.getIdentifier()});
                        continue;
                    }
                    if (rejectedRequest.getOwner() != null) {
                        continue;
                    }
                }
                // ignore all rejected requests
                rejectedRequest = req;
            } else {
                if (request != null) {
                    log.warn("Document at path {} has multiple outstanding requests. Request with identifier {} ignored.",
                            new String[]{handle.getPath(), requestNode.getIdentifier()});
                    continue;
                }
                request = req;
            }
        }
    }

    protected Map<String, PublishableDocument> getDocuments(boolean create) {
        if (create && documents == null) {
            documents = new HashMap<>();
        }
        if (documents != null) {
            return documents;
        }
        return Collections.emptyMap();
    }

    /**
     * Checks if specific privileges (e.g. hippo:editor) are allowed for the current user session against the workflow
     * subject node. <p> Implementation note: previously evaluated privileges are cached within the DocumentHandle
     * instance </p>
     *
     * @param privileges the privileges (, separated) to check permission for
     * @return true if for the current user session the workflow subject node allows the specified privileges
     */
    public boolean hasPermission(String privileges) {
        if (privileges == null) {
            return false;
        }

        HashSet<String> privs = new HashSet<>(Arrays.asList(privileges.split(",")));
        if (privs.isEmpty()) {
            return false;
        }

        if (privilegesMap == null) {
            privilegesMap = new HashMap<>();
        }
        for (String priv : privs) {
            Boolean hasPrivilege = privilegesMap.get(priv);
            if (hasPrivilege == null) {
                hasPrivilege = Boolean.FALSE;
                try {
                    final Session userSession = context.getUserSession();
                    Node userHandle = userSession.getNodeByIdentifier(handle.getIdentifier());
                    for (Node child : new NodeIterable(userHandle.getNodes())) {
                        if (userSession.hasPermission(child.getPath(), priv)) {
                            hasPrivilege = Boolean.TRUE;
                            break;
                        }
                    }
                } catch (AccessControlException e) {
                } catch (AccessDeniedException e) {
                } catch (IllegalArgumentException e) { // the underlying repository does not recognized the privileges requested.
                } catch (RepositoryException e) {
                }
                this.privilegesMap.put(priv, hasPrivilege);
            }
            if (!hasPrivilege) {
                return false;
            }
        }
        return true;
    }

    /**
     * Script supporting shortened version of {@link #hasPermission(String)}
     *
     * @param privileges the privileges (, separated) to check permission for
     * @return true if for the current user session the current workflow subject node allows the specified privileges
     */
    public boolean pm(String privileges) {
        return hasPermission(privileges);
    }

    /**
     * @return configured Features or {@link org.onehippo.repository.documentworkflow.DocumentWorkflow.SupportedFeatures#all}
     * by default
     */
    public DocumentWorkflow.SupportedFeatures getSupportedFeatures() {
        return supportedFeatures;
    }

    /**
     * Script supporting shortened version of {@link #getSupportedFeatures()}
     *
     * @return configured SupportedFeatures or {@link org.onehippo.repository.documentworkflow.DocumentWorkflow.SupportedFeatures#all}
     * by default
     */
    public DocumentWorkflow.SupportedFeatures getSf() {
        return getSupportedFeatures();
    }

    public WorkflowContext getWorkflowContext() {
        return context;
    }

    /**
     * Get short notation representing the states of all existing Variants
     *
     * @return concatenation of: "d" if Draft variant exists, "u" if Unpublished variant exists, "p" if Published
     * variant exists. Never returns null
     */
    public String getStates() {
        if (states == null) {
            states = "";
            if (getDraft() != null) {
                states += "d";
            }
            if (getUnpublished() != null) {
                states += "u";
            }
            if (getPublished() != null) {
                states += "p";
            }
        }
        return states;
    }

    /**
     * Script supporting shortened version of {@link #getStates}
     *
     * @return concatenation of: "d" if Draft variant exists, "u" if Unpublished variant exists, "p" if Published
     * variant exists. Never returns null
     */
    public String getS() {
        return getStates();
    }

    public String getUser() {
        return user;
    }

    /**
     * @return the active Request or null if none
     */
    public PublicationRequest getRequest() {
        return request;
    }

    /**
     * Script supporting shortened version of {@link #getRequest}
     *
     * @return the active Request or null if none
     */
    public PublicationRequest getR() {
        return getRequest();
    }

    /**
     * @return the rejected Request which is subject of this workflow or null if none
     */
    public PublicationRequest getRejectedRequest() {
        return rejectedRequest;
    }

    /**
     * Script supporting shortened version of {@link #getRejectedRequest}
     *
     * @return the rejected Request which is subject of this workflow or null if none
     */
    public PublicationRequest getRr() {
        return getRejectedRequest();
    }

    public void putDocumentVariant(PublishableDocument variant) throws RepositoryException {
        states = null;
        getDocuments(true).put(variant.getState(), variant);
    }

    public PublishableDocument getDocumentVariantByState(String state) {
        return getDocuments(false).get(state);
    }

    /**
     * @return Draft variant if it exists or null otherwise
     */
    public PublishableDocument getDraft() {
        return getDocuments(false).get(PublishableDocument.DRAFT);
    }

    /**
     * @return Draft variant if it exists or null otherwise
     */
    public PublishableDocument getD() {
        return getDraft();
    }

    /**
     * @return Unpublished variant if it exists or null otherwise
     */
    public PublishableDocument getUnpublished() {
        return getDocuments(false).get(PublishableDocument.UNPUBLISHED);
    }

    /**
     * @return Unpublished variant if it exists or null otherwise
     */
    public PublishableDocument getU() {
        return getUnpublished();
    }

    /**
     * @return Published variant if it exists or null otherwise
     */
    public PublishableDocument getPublished() {
        return getDocuments(false).get(PublishableDocument.PUBLISHED);
    }

    /**
     * @return Published variant if it exists or null otherwise
     */
    public PublishableDocument getP() {
        return getPublished();
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
        actions.clear();
        info.clear();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
