/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.DocumentVariant;

import static org.hippoecm.repository.standardworkflow.DocumentVariant.MASTER_BRANCH_LABEL_PUBLISHED;
import static org.hippoecm.repository.standardworkflow.DocumentVariant.MASTER_BRANCH_LABEL_UNPUBLISHED;

public class SetPreReintegrationLabelsTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;

    private static final String PRE_REINTEGRATE_LABEL_PREFIX = "pre-reintegrate-";

    private DocumentVariant unpublished;

    public void setUnpublished(DocumentVariant unpublished) {
        this.unpublished = unpublished;
    }

    @Override
    protected Object doExecute() throws WorkflowException, RepositoryException, RemoteException {

        final Session workflowSession = getWorkflowContext().getInternalWorkflowSession();

        final Node preview = unpublished.getNode(workflowSession);

        final VersionManager versionManager = workflowSession.getWorkspace().getVersionManager();
        final VersionHistory versionHistory = versionManager.getVersionHistory(preview.getPath());

        int counter = findNextCounter(versionHistory);

        for (final String masterLabel : new String[]{MASTER_BRANCH_LABEL_UNPUBLISHED, MASTER_BRANCH_LABEL_PUBLISHED}) {
            if (versionHistory.hasVersionLabel(masterLabel)) {
                final Version versionByLabel = versionHistory.getVersionByLabel(masterLabel);
                versionHistory.addVersionLabel(versionByLabel.getName(), PRE_REINTEGRATE_LABEL_PREFIX + masterLabel + "-" + counter, false);
            }
        }
        return null;
    }

    /**
     * the value X for 'pre-reintegrate-master-unpublished-X' and 'pre-reintegrate-master-published-X' is computed wrt the
     * 'unpublished' since the master can have 'gaps' in the incrementer : For example when there was no live version but only a preview
     * before a reintegrate took place
     */
    private int findNextCounter(final VersionHistory versionHistory) throws RepositoryException {
        int i = 1;
        while (versionHistory.hasVersionLabel(PRE_REINTEGRATE_LABEL_PREFIX + MASTER_BRANCH_LABEL_UNPUBLISHED + "-" + i)) {
            i++;
        }
        return i;
    }

}
