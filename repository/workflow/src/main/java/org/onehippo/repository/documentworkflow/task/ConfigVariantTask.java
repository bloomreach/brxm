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
package org.onehippo.repository.documentworkflow.task;

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.documentworkflow.PublishableDocument;
import org.onehippo.repository.util.JcrConstants;

/**
 * Custom workflow task for configuring a variant node
 * with workflow properties setting options. 
 */
public class ConfigVariantTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;

    private PublishableDocument variant;
    private String availabilities;
    private boolean applyModified;
    private boolean versionable;
    private boolean skipIndex;
    private boolean setHolder;

    public PublishableDocument getVariant() {
        return variant;
    }

    public void setVariant(PublishableDocument variant) {
        this.variant = variant;
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

    public void setApplyModified(boolean applyModified) {
        this.applyModified = applyModified;
    }

    public boolean isSkipIndex() {
        return skipIndex;
    }

    public void setSkipIndex(boolean skipIndex) {
        this.skipIndex = skipIndex;
    }

    public boolean isVersionable() {
        return versionable;
    }

    public void setVersionable(boolean versionable) {
        this.versionable = versionable;
    }

    public boolean isSetHolder() {
        return setHolder;
    }

    public void setSetHolder(final boolean setHolder) {
        this.setHolder = setHolder;
    }

    @Override
    public Object doExecute() throws WorkflowException, RepositoryException, RemoteException {

        DocumentHandle dm = getDocumentHandle();

        if (getVariant() == null || getVariant().getNode() == null) {
            throw new WorkflowException("No variant provided");
        }
        Node targetNode = getVariant().getNode();

        if (targetNode.isNodeType(HippoNodeType.NT_HARDDOCUMENT)) {
            targetNode.removeMixin(HippoNodeType.NT_HARDDOCUMENT);
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

        if (isApplyModified()) {
            variant.setModified(dm.getUser());
        }

        variant.setAvailability(getAvailabilities() != null ? StringUtils.split(getAvailabilities(), "\t\r\n, ") : null);

        return null;
    }
}
