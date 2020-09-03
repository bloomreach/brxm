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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.cms.channelmanager.content.UserContext;
import org.onehippo.cms.channelmanager.content.document.model.DocumentVersionInfo;
import org.onehippo.cms.channelmanager.content.document.model.Version;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.cms.channelmanager.content.error.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Boolean.TRUE;
import static org.apache.jackrabbit.JcrConstants.JCR_ROOTVERSION;
import static org.apache.jackrabbit.JcrConstants.MIX_VERSIONABLE;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_BY;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;
import static org.hippoecm.repository.util.JcrUtils.getDateProperty;
import static org.hippoecm.repository.util.JcrUtils.getStringProperty;
import static org.onehippo.repository.branch.BranchConstants.MASTER_BRANCH_ID;

public class DocumentVersionServiceImpl implements DocumentVersionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentVersionServiceImpl.class);

    private final BiFunction<Node, String, Map<String, ?>> documentHintsGetter;

    public DocumentVersionServiceImpl(final BiFunction<Node, String, Map<String, ?>> documentHintsGetter) {
        this.documentHintsGetter = documentHintsGetter;
    }

    @Override
    public DocumentVersionInfo getVersionInfo(
            String handleId,
            String branchId,
            UserContext userContext
    ) {
        final Session userSession = userContext.getSession();
        try {

            final Node handleNode = userSession.getNodeByIdentifier(handleId);
            if (!handleNode.isNodeType(NT_HANDLE)) {
                throw new BadRequestException(new ErrorInfo(ErrorInfo.Reason.NOT_A_DOCUMENT));
            }

            // note in case there is only a published variant, the 'preview' is equal to the live
            final Node preview = getUnpublished(handleNode);

            final List<Version> versionInfos = new ArrayList<>();

            if (preview.isNodeType(MIX_VERSIONABLE)) {
                final VersionHistory versionHistory = userSession.getWorkspace()
                        .getVersionManager().getVersionHistory(preview.getPath());

                // returns the oldest version first
                final VersionIterator allVersions = versionHistory.getAllVersions();
                while (allVersions.hasNext()) {
                    final javax.jcr.version.Version version = allVersions.nextVersion();
                    if (JCR_ROOTVERSION.equals(version.getName())) {
                        // skip root version, is just a placeholder without the actual contents
                        continue;
                    }
                    final Node frozenNode = version.getFrozenNode();
                    final String versionBranchId = getStringProperty(frozenNode, HIPPO_PROPERTY_BRANCH_ID, MASTER_BRANCH_ID);
                    if (versionBranchId.equals(branchId)) {
                        versionInfos.add(create(version.getCreated(), frozenNode, branchId));
                    }
                }

                // sort versions to have the newest one on top
                versionInfos.sort((o1, o2) -> new Long(o2.getTimestamp().getTimeInMillis()).compareTo(new Long(o1.getTimestamp().getTimeInMillis())));
            } else {
                log.debug("Document variant '{}' is not versionable, return only workspace version", preview.getPath());
            }

            // add workspace as the first version
            final Calendar lastModified = getDateProperty(preview, HIPPOSTDPUBWF_LAST_MODIFIED_DATE, Calendar.getInstance());
            final String workspaceBranchId = getStringProperty(preview, HIPPO_PROPERTY_BRANCH_ID, MASTER_BRANCH_ID);

            if (workspaceBranchId.equals(branchId)) {
                // current workspace is for branchId, insert as the first version
                versionInfos.add(0, create(lastModified, preview, branchId));
            }

            final Map<String, ?> hints = documentHintsGetter.apply(handleNode, branchId);
            final boolean restoreEnabled =
                    !versionInfos.isEmpty()
                            && (TRUE.equals(hints.get("restoreVersionToBranch")) || TRUE.equals(hints.get("restoreVersion")));

            final boolean createEnabled = TRUE.equals(hints.get("version"));

            return new DocumentVersionInfo(versionInfos, restoreEnabled, createEnabled);

        } catch (ItemNotFoundException e) {
            log.info("Document for id '{}' does not exist or user '{}' is not allowed to read it",
                    handleId, userSession.getUserID());
            throw new NotFoundException(new ErrorInfo(ErrorInfo.Reason.DOES_NOT_EXIST));

        } catch (RepositoryException e) {
            throw new InternalServerErrorException(new ErrorInfo(ErrorInfo.Reason.SERVER_ERROR));
        }
    }

    private Node getUnpublished(final Node handleNode) throws RepositoryException {
        for (Node variant : new NodeIterable(handleNode.getNodes(handleNode.getName()))) {
            final List<String> availability = JcrUtils.getStringListProperty(variant, HippoNodeType.HIPPO_AVAILABILITY, Collections.emptyList());
            if (availability.contains("preview")) {
                return variant;
            }
        }
        throw new BadRequestException(new ErrorInfo(ErrorInfo.Reason.NOT_A_DOCUMENT));
    }

    private Version create(Calendar historic, Node node, String branchId) throws RepositoryException {
        final String userName = getStringProperty(node, HIPPOSTDPUBWF_LAST_MODIFIED_BY, null);
        return new Version(historic, userName, node.getIdentifier(), branchId);
    }

}
