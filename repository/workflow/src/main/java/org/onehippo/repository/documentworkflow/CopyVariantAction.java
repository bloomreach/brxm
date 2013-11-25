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
package org.onehippo.repository.documentworkflow;

import java.util.Collection;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.scxml2.ErrorReporter;
import org.apache.commons.scxml2.EventDispatcher;
import org.apache.commons.scxml2.SCInstance;
import org.apache.commons.scxml2.SCXMLExpressionException;
import org.apache.commons.scxml2.TriggerEvent;
import org.apache.commons.scxml2.model.ModelException;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom action for copying (creating if necessary) from one varaint node to another variant node
 * with workflow properties setting options. 
 */
public class CopyVariantAction extends AbstractDocumentAction {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(CopyVariantAction.class);

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
    protected void doExecute(EventDispatcher evtDispatcher, ErrorReporter errRep, SCInstance scInstance, Log appLog,
            Collection<TriggerEvent> derivedEvents) throws ModelException, SCXMLExpressionException,
            RepositoryException {

        DocumentHandle handle = getDocumentHandle(scInstance);

        DocumentVariant sourceDoc = handle.getDocumentVariantByState(getSourceState());
        DocumentVariant targetDoc = handle.getDocumentVariantByState(getTargetState());

        if (sourceDoc == null || sourceDoc.getNode() == null) {
            throw new ModelException("Source document variant (node) is not available.");
        }

        final Node sourceNode = sourceDoc.getNode();
        final Node parent = sourceNode.getParent();
        Node targetNode = null;

        JcrUtils.ensureIsCheckedOut(parent, true);

        boolean saveNeeded = false;

        if (targetDoc == null) {
            saveNeeded = true;
            targetNode = cloneDocumentNode(sourceNode);

            if (PublishableDocument.DRAFT.equals(getTargetState())) {
                if (targetNode.isNodeType(HippoNodeType.NT_HARDDOCUMENT)) {
                    targetNode.removeMixin(HippoNodeType.NT_HARDDOCUMENT);
                }
            }

            PublishableDocument variant = new PublishableDocument(targetNode);
            variant.setState(getTargetState());
            targetDoc = new DocumentVariant(variant);
        }
        else {
            targetNode = targetDoc.getNode();

            if (sourceNode.isSame(targetNode)) {
                throw new ModelException(
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
            targetDoc.setModified(handle.getUser());
        }

        if (saveNeeded) {
            targetDoc.getNode().getSession().save();
        }

        handle.putDocumentVariant(targetDoc);
    }
}
