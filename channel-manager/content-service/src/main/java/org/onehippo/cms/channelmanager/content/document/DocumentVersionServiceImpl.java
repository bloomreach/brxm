/*
 * Copyright 2020 Bloomreach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.onehippo.cms.channelmanager.content.document;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms.channelmanager.content.UserContext;
import org.onehippo.cms.channelmanager.content.document.model.DocumentVersionInfo;
import org.onehippo.cms.channelmanager.content.document.model.Version;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.onehippo.repository.branch.BranchConstants.MASTER_BRANCH_ID;

public class DocumentVersionServiceImpl implements DocumentVersionService {

    private final BiFunction<UserContext, String, DocumentWorkflow> workflowGetter;

    public DocumentVersionServiceImpl(
            BiFunction<UserContext, String, DocumentWorkflow> workflowGetter
    ) {
        this.workflowGetter = workflowGetter;
    }

    @Override
    public DocumentVersionInfo getVersionInfo(
            String uuid,
            String branchId,
            UserContext userContext
    ) {
        final DocumentWorkflow documentWorkflow = workflowGetter.apply(userContext, uuid);
        try {
            final SortedMap<Calendar, Set<String>> versions = documentWorkflow.listVersions();
            final List<Version> versionInfos = new ArrayList<>();
            for (Calendar historic : versions.keySet()) {
                final Node frozenNode = documentWorkflow.retrieveVersion(historic).getNode(userContext.getSession());
                final String frozenBranchId = JcrUtils.getStringProperty(frozenNode, HIPPO_PROPERTY_BRANCH_ID, MASTER_BRANCH_ID);
                if (frozenBranchId.equals(branchId)) {
                    versionInfos.add(create(historic, frozenNode));
                }
            }
            final String currentVersion = versionInfos.stream()
                    .map(Version::getVersionId)
                    .filter(Predicate.isEqual(uuid))
                    .findFirst()
                    .orElse(null);
            return new DocumentVersionInfo(versionInfos, currentVersion);
        } catch (WorkflowException | RemoteException | RepositoryException e) {
            throw new InternalServerErrorException(new ErrorInfo(ErrorInfo.Reason.SERVER_ERROR));
        }
    }

    private Version create(Calendar historic, Node frozenNode) throws RepositoryException {
        final String userName = JcrUtils.getStringProperty(frozenNode, HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_BY, null);
        return new Version(historic, userName, frozenNode.getIdentifier());
    }

}
