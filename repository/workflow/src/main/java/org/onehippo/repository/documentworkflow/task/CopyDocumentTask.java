/*
 * Copyright 2013-2020 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.RepositoryMap;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.EmbedWorkflow;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.WorkflowUtils;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.documentworkflow.DocumentVariant;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.HippoStdNodeType.DRAFT;
import static org.hippoecm.repository.HippoStdNodeType.PUBLISHED;
import static org.hippoecm.repository.HippoStdNodeType.UNPUBLISHED;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MIXIN_BRANCH_INFO;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_NAME;
import static org.hippoecm.repository.api.HippoNodeType.NT_HIPPO_VERSION_INFO;

/**
 * Custom workflow task for copying document.
 */
public class CopyDocumentTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(CopyDocumentTask.class);

    // TODO for now, we only support copy branch to master, we need to support this later on but for now can't because
    // results in other issues, see CMS-14276
    public static final boolean COPY_BRANCH_TO_BRANCH_SUPPORTED = false;

    private Document destination;
    private String newName;

    public Document getDestination() {
        return destination;
    }

    public void setDestination(Document destination) {
        this.destination = destination;
    }

    public String getNewName() {
        return newName;
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }

    @Override
    public Object doExecute() throws WorkflowException, RepositoryException, RemoteException {
        if (destination == null) {
            throw new WorkflowException("Destination is null.");
        }

        if (StringUtils.isBlank(newName)) {
            throw new WorkflowException("New document name is blank.");
        }

        DocumentHandle dm = getDocumentHandle();

        String folderWorkflowCategory = "embedded";
        RepositoryMap config = getWorkflowContext().getWorkflowConfiguration();

        if (config != null && config.exists() && config.get("folder-workflow-category") instanceof String) {
            folderWorkflowCategory = (String) config.get("folder-workflow-category");
        }

        final Document copy;
        String sourceState = getSourceState(dm.getDocuments());
        DocumentVariant sourceVariant = dm.getDocuments().get(sourceState);
        final Optional<Pair<String, String>> branchIdNamePairSource = getBranchIdNamePair(sourceVariant);

        Document folder = WorkflowUtils.getContainingFolder(sourceVariant, getWorkflowContext().getInternalWorkflowSession());
        Workflow workflow = getWorkflowContext().getWorkflow(folderWorkflowCategory, destination);

        if (workflow instanceof EmbedWorkflow) {
            log.debug("Copy node : { path : {}, sourceState: {} } to {}",
                    JcrUtils.getNodePathQuietly(sourceVariant.getNode())
                    , sourceState, destination.getIdentity());
            copy = ((EmbedWorkflow) workflow).copyTo(folder, sourceVariant, newName, null);
            if (PUBLISHED.equals(sourceState)){
                Node copyHandle = copy.getNode(getWorkflowContext().getInternalWorkflowSession()).getParent();
                DocumentWorkflow copiedDocumentWorkflow = (DocumentWorkflow) getWorkflowContext().getWorkflow("default", new Document(copyHandle));
                copiedDocumentWorkflow.depublish();
            }
        } else {
            throw new WorkflowException("cannot copy document which is not contained in a folder");
        }

        if (branchIdNamePairSource.isPresent()) {
            final Node copyHandle = copy.getNode(getWorkflowContext().getInternalWorkflowSession()).getParent();

            // do not branch via workflow since that would check-in 'master' unpublished first which should not be done
            // since we copy a branch there should not be a master directly
            copyHandle.addMixin(NT_HIPPO_VERSION_INFO);
            copyHandle.setProperty(HippoNodeType.HIPPO_BRANCHES_PROPERTY, new String[]{branchIdNamePairSource.get().getLeft()});
            final Node variant = copyHandle.getNode(copyHandle.getName());
            variant.addMixin(HIPPO_MIXIN_BRANCH_INFO);
            variant.setProperty(HIPPO_PROPERTY_BRANCH_ID, branchIdNamePairSource.get().getLeft());
            variant.setProperty(HIPPO_PROPERTY_BRANCH_NAME, branchIdNamePairSource.get().getRight());
        }

        return null;
    }

    private String getSourceState(Map<String, DocumentVariant> documents) {
        if (documents.containsKey(UNPUBLISHED)) {
            return UNPUBLISHED;
        }
        if (documents.containsKey(PUBLISHED)){
            return PUBLISHED;
        }
        return DRAFT;
    }

    private Optional<Pair<String, String>> getBranchIdNamePair(final DocumentVariant variant) throws RepositoryException {
        // see CMS-14276
        if (!COPY_BRANCH_TO_BRANCH_SUPPORTED) {
            return Optional.empty();
        }

        if (variant.getNode().isNodeType(HIPPO_MIXIN_BRANCH_INFO)) {
            final String branchIdSource = variant.getNode().getProperty(HIPPO_PROPERTY_BRANCH_ID).getString();
            final String branchNameSource = variant.getNode().getProperty(HIPPO_PROPERTY_BRANCH_NAME).getString();
            return Optional.of(new ImmutablePair<>(branchIdSource, branchNameSource));
        } else {
            return Optional.empty();
        }
    }

}
