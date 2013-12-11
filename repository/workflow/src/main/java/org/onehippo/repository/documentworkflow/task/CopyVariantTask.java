/**
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
package org.onehippo.repository.documentworkflow.task;

import java.rmi.RemoteException;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.documentworkflow.PublishableDocument;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom workflow task for copying (creating if necessary) from one varaint node to another variant node
 * with workflow properties setting options. 
 */
public class CopyVariantTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(CopyVariantTask.class);

    private String sourceState;
    private String targetState;
    private String availabilities;
    private boolean applyModified;
    private boolean versionable;
    private boolean skipIndex;

    public String getSourceState() {
        return sourceState;
    }

    public void setSourceState(String sourceState) {
        this.sourceState = sourceState;
    }

    public String getTargetState() {
        return targetState;
    }

    public void setTargetState(String targetState) {
        this.targetState = targetState;
    }

    public String getAvailabilities() {
        return availabilities;
    }

    public void setAvailabilities(String availabilities) {
        this.availabilities = availabilities;
    }

    public boolean isApplyModified() {
        return applyModified;
    }

    public void setApplyModified(String applyModified) {
        this.applyModified = Boolean.parseBoolean(applyModified);
    }

    public boolean isSkipIndex() {
        return skipIndex;
    }

    public void setSkipIndex(String skipIndex) {
        this.skipIndex = Boolean.parseBoolean(skipIndex);
    }

    public boolean isVersionable() {
        return versionable;
    }

    public void setVersionable(String versionable) {
        this.versionable = Boolean.parseBoolean(versionable);
    }

    @Override
    public void doExecute(Map<String, Object> properties) throws WorkflowException, RepositoryException, RemoteException {

        DocumentHandle dm = getDataModel();

        PublishableDocument sourceDoc = dm.getDocumentVariantByState(getSourceState());
        PublishableDocument targetDoc = dm.getDocumentVariantByState(getTargetState());

        if (sourceDoc == null || sourceDoc.getNode() == null) {
            throw new WorkflowException("Source document variant (node) is not available.");
        }

        final Node sourceNode = sourceDoc.getNode();
        final Node parent = sourceNode.getParent();
        Node targetNode = null;

        JcrUtils.ensureIsCheckedOut(parent);

        boolean saveNeeded = false;

        if (targetDoc == null) {
            saveNeeded = true;
            targetNode = cloneDocumentNode(sourceNode);

            if (PublishableDocument.DRAFT.equals(getTargetState())) {
                if (targetNode.isNodeType(HippoNodeType.NT_HARDDOCUMENT)) {
                    targetNode.removeMixin(HippoNodeType.NT_HARDDOCUMENT);
                }
            }

            targetDoc = new PublishableDocument(targetNode);
            targetDoc.setState(getTargetState());
        }
        else {
            targetNode = targetDoc.getNode();

            if (sourceNode.isSame(targetNode)) {
                throw new WorkflowException(
                        "The target document variant (node) is the same as the source document variant (node).");
            }
            copyTo(sourceNode, targetNode);
        }

        if (!targetNode.isNodeType(JcrConstants.MIX_REFERENCEABLE)) {
            targetNode.addMixin(JcrConstants.MIX_REFERENCEABLE);
        }

        if (isVersionable() && !targetNode.isNodeType(JcrConstants.MIX_VERSIONABLE)) {
            targetNode.addMixin(JcrConstants.MIX_VERSIONABLE);
        }

        if (isSkipIndex() && !targetNode.isNodeType((HippoNodeType.NT_SKIPINDEX))) {
            targetNode.addMixin(HippoNodeType.NT_SKIPINDEX);
        }

        targetDoc.setAvailability(getAvailabilities() != null ? StringUtils.split(getAvailabilities(), "\t\r\n, ") : null);
        if (isApplyModified()) {
            targetDoc.setModified(dm.getUser());
        }

        if (saveNeeded) {
            targetDoc.getNode().getSession().save();
        }

        dm.putDocumentVariant(targetDoc);
    }
}
