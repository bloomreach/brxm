/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.RepositoryMap;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentHandle {

    private static final Logger log = LoggerFactory.getLogger(DocumentHandle.class);

    private WorkflowContext context;
    private Node subject;
    private Version version;

    private Node handle;
    private String user;
    private DocumentWorkflow.SupportedFeatures supportedFeatures = DocumentWorkflow.SupportedFeatures.all;

    private Map<String, PublishableDocument> documents = null;
    private PublicationRequest rejectedRequest = null;
    private PublicationRequest request = null;

    private String subjectState;
    private String states;

    private Map<String, Serializable> hints = new HashMap<>();

    private Map<String, Boolean> privilegesMap;

    public DocumentHandle(WorkflowContext context, Node workflowSubject) throws RepositoryException {

        boolean supportedFeaturesPreset = false;

        this.context = context;
        this.subject = workflowSubject;
        this.user = context.getUserIdentity();

        if (workflowSubject.isNodeType(JcrConstants.NT_FROZEN_NODE)) {
            // only can support VersionWorkflow operations
            supportedFeatures = DocumentWorkflow.SupportedFeatures.version;
            supportedFeaturesPreset = true;
            this.version = (Version) workflowSubject.getParent();
            try {
            this.subject = workflowSubject.getSession().getNodeByIdentifier(workflowSubject.getProperty(JcrConstants.JCR_FROZEN_UUID).getString());
            }
            catch (ItemNotFoundException e) {
                // no version subject (anymore)
                subject = null;
                return;
            }
        }
        else {
            if (workflowSubject.isNodeType(HippoNodeType.NT_REQUEST)) {
                // only can support RequestWorkflow operations
                supportedFeatures = DocumentWorkflow.SupportedFeatures.request;
                supportedFeaturesPreset = true;
            }
        }
        handle = subject.getParent();

        if (!supportedFeaturesPreset) {
            RepositoryMap workflowConfiguration = context.getWorkflowConfiguration();
            if (workflowConfiguration.exists()) {
                String supportedFeaturesConfiguration = (String)workflowConfiguration.get("workflow.supportedFeatures");
                if (supportedFeaturesConfiguration != null) {
                    try {
                        supportedFeatures = DocumentWorkflow.SupportedFeatures.valueOf(supportedFeaturesConfiguration);
                    }
                    catch (IllegalArgumentException e) {
                        String configurationPath = (String)workflowConfiguration.get("_path");
                        if (configurationPath == null) {
                            configurationPath = "<unknown>";
                        }
                        log.warn("Unknown DocumentWorkflow.SupportedFeatures [{}] configured for property workflow.supportedFeatures at: {}", supportedFeaturesConfiguration, configurationPath);
                    }
                }
            }
        }
        boolean subjectFound = false;

        for (Node variant : new NodeIterable(handle.getNodes(handle.getName()))) {
            PublishableDocument doc = new PublishableDocument(variant);
            if (documents != null && documents.containsKey(doc.getState())) {
                log.warn("Document at path {} has multiple variants with state {}. Variant with identifier {} ignored.",
                        new String[]{handle.getPath(), doc.getState(), variant.getIdentifier()});
            }
            getDocuments(true).put(doc.getState(), doc);
            if (!subjectFound && variant.isSame(subject)) {
                subjectState = doc.getState();
                subjectFound = true;
            }
        }
        for (Node requestNode : new NodeIterable(handle.getNodes(PublicationRequest.HIPPO_REQUEST))) {
            PublicationRequest req = new PublicationRequest(requestNode);
            String requestType = JcrUtils.getStringProperty(requestNode, PublicationRequest.HIPPOSTDPUBWF_TYPE, "");
            if ("rejected".equals(requestType)) {
                if (!subjectFound && requestNode.isSame(subject)) {
                    rejectedRequest = req;
                    subjectFound = true;
                }
                else {
                    // ignore all other rejected requests
                }
                continue;
            }
            if (request != null) {
                log.warn("Document at path {} has multiple outstanding requests. Request with identifier {} ignored.",
                        new String[]{handle.getPath(), requestNode.getIdentifier()});
                continue;
            }
            request = req;
            if (!subjectFound && requestNode.isSame(subject)) {
                subjectFound = true;
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
     * Checks if specific privileges (e.g. hippo:editor) are allowed for the current user session against the workflow subject node.
     * <p>
     *     Implementation note: previously evaluated privileges are cached within the DocumentHandle instance
     * </p>
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
                    hasPrivilege = Boolean.valueOf(context.getUserSession().hasPermission(subject.getPath(), priv));
                } catch (AccessControlException e) {
                } catch (AccessDeniedException e) {
                } catch (IllegalArgumentException e) { // the underlying repository does not recognized the privileges requested.
                } catch (RepositoryException e) {
                }
                this.privilegesMap.put(priv, hasPrivilege);
            }
            if (!hasPrivilege.booleanValue()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Script supporting shortened version of {@link #hasPermission(String)}
     * @param privileges the privileges (, separated) to check permission for
     * @return true if for the current user session the current workflow subject node allows the specified privileges
     */
    public boolean pm(String privileges) {
        return hasPermission(privileges);
    }

    /**
     * @return configured Features or {@link org.onehippo.repository.documentworkflow.DocumentWorkflow.SupportedFeatures#all} by default
     */
    public DocumentWorkflow.SupportedFeatures getSupportedFeatures() {
        return supportedFeatures;
    }

    /**
     * Script supporting shortened version of {@link #getSupportedFeatures()}
     * @return configured SupportedFeatures or {@link org.onehippo.repository.documentworkflow.DocumentWorkflow.SupportedFeatures#all} by default
     */
    public DocumentWorkflow.SupportedFeatures getSf() {
        return getSupportedFeatures();
    }

    /**
     * Get short notation representing the states of all existing Variants
     * @return concatenation of: "d" if Draft variant exists, "u" if Unpublished variant exists, "p" if Published variant exists. Never returns null
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
     * @return concatenation of: "d" if Draft variant exists, "u" if Unpublished variant exists, "p" if Published variant exists. Never returns null
     */
    public String getS() {
        return getStates();
    }

    public String getUser() {
        return user;
    }

    /**
     * @return the state of the Document variant which is subject of this workflow or empty String if none. Never returns null;
     */
    public String getSubjectState() {
        return subjectState == null ? "" : subjectState;
    }

    /**
     * Script supporting shortened version of {@link #getSubjectState}
     * @return the state of the Document variant which is subject of this workflow or empty String if none. Never returns null;
     */
    public String getSs() {
        return getSubjectState();
    }

    /**
     * @return the Version of the Workflow invocation subject of type {@link JcrConstants#JCR_FROZEN_NODE} or null if not
     */
    public Version getVersion() {
        return version;
    }

    /**
     * Script supporting shortened version of {@link #getVersion}
     * @return the Version of the Workflow invocation subject of type {@link JcrConstants#JCR_FROZEN_NODE} or null if not
     */
    public Version getV() {
        return version;
    }

    /**
     * @return the active Request or null if none
     */
    public PublicationRequest getRequest() {
        return request;
    }

    /**
     * Script supporting shortened version of {@link #getRequest}
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
     * @return the rejected Request which is subject of this workflow or null if none
     */
    public PublicationRequest getRr() {
        return getRejectedRequest();
    }

    public void setRequest(final PublicationRequest request) throws RepositoryException {
        if (this.request != null) {
            // TODO: probably an error situation?
        }
        this.request = request;
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

    public Map<String, Serializable> getHints() {
            return hints;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
