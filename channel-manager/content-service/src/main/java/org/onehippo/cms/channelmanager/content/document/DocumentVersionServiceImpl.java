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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.onehippo.cms.channelmanager.content.UserContext;
import org.onehippo.cms.channelmanager.content.document.model.DocumentVersionInfo;
import org.onehippo.cms.channelmanager.content.document.model.Version;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.cms.channelmanager.content.error.NotFoundException;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.jackrabbit.JcrConstants.JCR_FROZENUUID;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_BY;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.util.JcrUtils.getDateProperty;
import static org.hippoecm.repository.util.JcrUtils.getStringProperty;
import static org.onehippo.repository.branch.BranchConstants.MASTER_BRANCH_ID;
import static org.onehippo.repository.util.JcrConstants.NT_FROZEN_NODE;

public class DocumentVersionServiceImpl implements DocumentVersionService {

    private final static Logger log = LoggerFactory.getLogger(DocumentVersionServiceImpl.class);

    @Override
    public DocumentVersionInfo getVersionInfo(
            String uuid,
            String branchId,
            UserContext userContext
    ) {
        final Session userSession = userContext.getSession();
        try {

            final Node requestNode = userSession.getNodeByIdentifier(uuid);
            final Node workspaceUnpublished;

            final String workspaceUUID;
            if (requestNode.isNodeType(JcrConstants.NT_FROZEN_NODE)) {
                Node current = requestNode;
                while (current.getParent().isNodeType(NT_FROZEN_NODE)) {
                    current = current.getParent();
                }
                // expected that current is now the frozen node of the unpublished variant
                workspaceUUID = current.getProperty(JCR_FROZENUUID).getString();
                workspaceUnpublished = userSession.getNodeByIdentifier(workspaceUUID);
            } else {
                workspaceUnpublished = requestNode;
            }

            final List<Version> versionInfos = new ArrayList<>();


            // add workspace as the first version
            final Calendar lastModified = getDateProperty(workspaceUnpublished, HIPPOSTDPUBWF_LAST_MODIFIED_DATE, Calendar.getInstance());
            final String workspaceBranchId = getStringProperty(workspaceUnpublished, HIPPO_PROPERTY_BRANCH_ID, MASTER_BRANCH_ID);


            final VersionHistory versionHistory = userSession.getWorkspace()
                    .getVersionManager().getVersionHistory(workspaceUnpublished.getPath());

            // returns the oldest version first
            final VersionIterator allVersions = versionHistory.getAllVersions();
            while (allVersions.hasNext()) {
                final javax.jcr.version.Version version = allVersions.nextVersion();
                final Node frozenNode = version.getFrozenNode();
                final String versionBranchId = getStringProperty(frozenNode, HIPPO_PROPERTY_BRANCH_ID, MASTER_BRANCH_ID);
                if (versionBranchId.equals(branchId)) {
                    versionInfos.add(create(version.getCreated(), frozenNode, branchId));
                }
            }

            // sort versions to have the newest one on top
            versionInfos.sort((o1, o2) -> new Long(o2.getTimestamp().getTimeInMillis()).compareTo(new Long(o1.getTimestamp().getTimeInMillis())));

            if (workspaceBranchId.equals(branchId)) {
                // current workspace is for branchId, insert as the first version
                versionInfos.add(0, create(lastModified, workspaceUnpublished, branchId));
            }

            return new DocumentVersionInfo(versionInfos, uuid);
        } catch (ItemNotFoundException e) {
            log.info("Document for id '{}' does not exist or user '{}' is not allowed to read it",
                    uuid, userSession.getUserID());
            throw new NotFoundException(new ErrorInfo(ErrorInfo.Reason.DOES_NOT_EXIST));

        } catch (RepositoryException e) {
            throw new InternalServerErrorException(new ErrorInfo(ErrorInfo.Reason.SERVER_ERROR));
        }
    }

    private Version create(Calendar historic, Node node, String branchId) throws RepositoryException {
        final String userName = getStringProperty(node, HIPPOSTDPUBWF_LAST_MODIFIED_BY, null);
        return new Version(historic, userName, node.getIdentifier(), branchId);
    }

}
