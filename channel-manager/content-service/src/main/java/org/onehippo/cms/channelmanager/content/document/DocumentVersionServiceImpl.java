/*
 * Copyright 2020-2021 Bloomreach
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.hippoecm.hst.campaign.DocumentCampaignService;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.cms.channelmanager.content.UserContext;
import org.onehippo.cms.channelmanager.content.document.model.DocumentVersionInfo;
import org.onehippo.cms.channelmanager.content.document.model.Version;
import org.onehippo.cms.channelmanager.content.document.util.ContentWorkflowUtils;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ConflictException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.cms.channelmanager.content.error.NotFoundException;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.campaign.Campaign;
import org.onehippo.repository.campaign.VersionLabel;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.repository.documentworkflow.campaign.JcrVersionsMetaUtils;
import org.onehippo.repository.campaign.VersionsMeta;
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
    private boolean pageCampaignSupported;

    public DocumentVersionServiceImpl(final BiFunction<Node, String, Map<String, ?>> documentHintsGetter) {
        this.documentHintsGetter = documentHintsGetter;
    }

    public void setPageCampaignSupported(final boolean pageCampaignSupported) {
        this.pageCampaignSupported = pageCampaignSupported;
    }

    @Override
    public DocumentVersionInfo getVersionInfo(final String handleId, final String branchId,
                                              final UserContext userContext, final boolean campaignVersionOnly) {
        final Session userSession = userContext.getSession();
        try {

            final Node handleNode = userSession.getNodeByIdentifier(handleId);
            if (!handleNode.isNodeType(NT_HANDLE)) {
                throw new BadRequestException(new ErrorInfo(ErrorInfo.Reason.NOT_A_DOCUMENT));
            }

            final VersionsMeta versionsMeta = JcrVersionsMetaUtils.getVersionsMeta(handleNode);

            // note in case there is only a published variant, the 'preview' is equal to the live
            final Node preview = getUnpublished(handleNode);

            final List<Version> versionInfos = new ArrayList<>();

            Node publishedFrozenNode = null;
            Version publishedVersion = null;

            final Map<String, ?> hints = documentHintsGetter.apply(handleNode, branchId);
            final boolean isLive = TRUE.equals(hints.get("isLive"));

            if (preview.isNodeType(MIX_VERSIONABLE)) {
                final VersionHistory versionHistory = userSession.getWorkspace()
                        .getVersionManager().getVersionHistory(preview.getPath());

                try {
                    final javax.jcr.version.Version jcrPublishedVersion = versionHistory.getVersionByLabel(branchId + "-published");
                    publishedFrozenNode = jcrPublishedVersion.getFrozenNode();
                } catch (VersionException e ){
                    log.debug("There is no version in history representing the published", e);
                }

                // returns the oldest version first
                final VersionIterator allVersions = versionHistory.getAllVersions();
                Version lastVersion = null;
                while (allVersions.hasNext()) {
                    final javax.jcr.version.Version jcrVersion = allVersions.nextVersion();
                    if (JCR_ROOTVERSION.equals(jcrVersion.getName())) {
                        // skip root version, is just a placeholder without the actual contents
                        continue;
                    }
                    final Node frozenNode = jcrVersion.getFrozenNode();
                    final String versionBranchId = getStringProperty(frozenNode, HIPPO_PROPERTY_BRANCH_ID, MASTER_BRANCH_ID);
                    if (versionBranchId.equals(branchId)) {
                        final boolean isPublishedVersion = publishedFrozenNode != null && frozenNode.isSame(publishedFrozenNode);
                        final Version version = create(jcrVersion.getCreated(), frozenNode, isPublishedVersion, branchId, versionsMeta);
                        if (isPublishedVersion) {
                            publishedVersion = version;
                        }
                        if (campaignVersionOnly) {
                            if (version.getCampaign() != null) {
                                versionInfos.add(version);
                            }
                        } else {
                            versionInfos.add(version);
                        }
                        lastVersion = version;
                    }
                }

                // sort versions to have the newest one on top in case all versions are shown, but in case of
                // campaigns only, sort them by 'start date' of the campaign
                        if (campaignVersionOnly) {
                    // campaign never null in case of campaignVersionOnly
                    versionInfos.sort(Comparator.comparing(Version::getCampaign, Comparator.comparing(Campaign::getFrom)).reversed());
                        } else {
                    versionInfos.sort(Comparator.comparing(Version::getTimestamp).reversed());
                        }

                if (publishedFrozenNode == null && isLive && lastVersion != null) {
                    // there is no explicit version for the published: this can only happen for bootstrapped live
                    // documents which got edited but have not yet been re-published.
                    // this means that the oldest version from version history reflects the published. If the
                    // last jcr version is not present in 'versionInfos' it must be added, otherwise the last
                    // item must be set to published = true
                    if (versionInfos.contains(lastVersion)) {
                        versionInfos.get(versionInfos.size() - 1).setPublished(true);
                    } else {
                        lastVersion.setPublished(true);
                        if (campaignVersionOnly) {
                            versionInfos.add(0, lastVersion);
                        } else {
                            versionInfos.add(lastVersion);
                        }
                    }
                }

            } else {
                log.debug("Document variant '{}' is not versionable, return only workspace version", preview.getPath());
            }


            final boolean restoreEnabled =
                    !versionInfos.isEmpty()
                            && (TRUE.equals(hints.get("restoreVersionToBranch")) || TRUE.equals(hints.get("restoreVersion")));

            final boolean createEnabled = TRUE.equals(hints.get("version"));

            // now find the 'active' version
            final DocumentCampaignService documentCampaignService = HippoServiceRegistry.getService(DocumentCampaignService.class);
            final Optional<Campaign> activeCampaign = documentCampaignService == null ? Optional.empty() : documentCampaignService.findActiveCampaign(handleNode, branchId);

            // truncate the number of versions to 100 but if there is a published version which is not part of the first
            // 100, set it as the 100th version to always at least have that one in the response
            final List<Version> visibleVersions = versionInfos.subList(0, versionInfos.size() > 100 ? 100 : versionInfos.size());
            if (publishedVersion != null && !visibleVersions.contains(publishedVersion)) {
                if (campaignVersionOnly) {
                    // add the published as first version in case filtering on campaigns only since the 'published' should
                    // always be shown: since unclear 'where' to show it (versions are ordered by campaign date, just
                    // show it as the first after the working version
                    visibleVersions.add(0, publishedVersion);
                } else {
                    // apparently the published version is not in the first 100 versions, set it as the last version of the
                    // visible versions
                    visibleVersions.set(visibleVersions.size() - 1 , publishedVersion);
                }
            }

            // in case no activeCampaign, then the Version which has 'published = true' is also the active one
            versionInfos.stream()
                    .filter(version -> activeCampaign.isPresent() ? activeCampaign.get().getUuid().equals(version.getJcrUUID()) : version.isPublished())
                    .findFirst().ifPresent(v -> v.setActive(true));


            // add workspace as the first version
            final Calendar lastModified = getDateProperty(preview, HIPPOSTDPUBWF_LAST_MODIFIED_DATE, Calendar.getInstance());
            final String workspaceBranchId = getStringProperty(preview, HIPPO_PROPERTY_BRANCH_ID, MASTER_BRANCH_ID);

            if (workspaceBranchId.equals(branchId)) {
                // current workspace is for branchId, insert as the first version

                // Note only for bootstrapped documents, it can happen that 'publishedFrozenNode' is null : in that
                // case, when there are no versions and the document is live, it means that the current unpublished
                // is the same as the live (most likely there is not even an unpublished)
                visibleVersions.add(0, create(lastModified, preview, isLive && versionInfos.size() == 0, branchId, versionsMeta));
            }

            // only on master branch we allow setting a campaign
            final boolean campaignEnabled = pageCampaignSupported && MASTER_BRANCH_ID.equals(branchId) &&
                    (TRUE.equals(hints.get("campaign")) || TRUE.equals(hints.get("removeCampaign")));
            final boolean labelEnabled = TRUE.equals(hints.get("labelVersion")) || TRUE.equals(hints.get("removeLabelVersion"));

            return new DocumentVersionInfo(visibleVersions, restoreEnabled, createEnabled, pageCampaignSupported, labelEnabled, campaignEnabled, isLive);

        } catch (ItemNotFoundException e) {
            log.info("Document for id '{}' does not exist or user '{}' is not allowed to read it",
                    handleId, userSession.getUserID());
            throw new NotFoundException(new ErrorInfo(ErrorInfo.Reason.DOES_NOT_EXIST));

        } catch (RepositoryException e) {
            throw new InternalServerErrorException(new ErrorInfo(ErrorInfo.Reason.SERVER_ERROR));
        }
    }

    @Override
    public Version updateVersion(final String handleId, final String branchId, final String frozenNodeId,
                                 final Version version, final UserContext userContext) {
        final Session userSession = userContext.getSession();
        if (version.getJcrUUID() != null && !version.getJcrUUID().equals(frozenNodeId)) {
            log.warn("Provided uuid in 'version' does not match the 'frozenNodeId' in the pathInfo");
            throw new ConflictException(new ErrorInfo(ErrorInfo.Reason.INVALID_DATA));
        }
        try {

            final Node handleNode = userSession.getNodeByIdentifier(handleId);
            if (!handleNode.isNodeType(NT_HANDLE)) {
                throw new BadRequestException(new ErrorInfo(ErrorInfo.Reason.NOT_A_DOCUMENT));
            }

            final VersionsMeta versionsMeta = JcrVersionsMetaUtils.getVersionsMeta(handleNode);

            if (requiresUpdate(versionsMeta.getCampaign(frozenNodeId), version.getCampaign())) {
                final DocumentWorkflow documentWorkflow = ContentWorkflowUtils.getDocumentWorkflow(handleNode);
                if (version.getCampaign() == null) {
                    documentWorkflow.removeCampaign(frozenNodeId);
                } else {
                    documentWorkflow.campaign(frozenNodeId, branchId, version.getCampaign().getFrom(), version.getCampaign().getTo());
                }
            }

            if (requiresUpdate(versionsMeta.getVersionLabel(frozenNodeId), version.getLabel())) {
                // update label
                final DocumentWorkflow documentWorkflow = ContentWorkflowUtils.getDocumentWorkflow(handleNode);
                if (version.getLabel() == null) {
                    documentWorkflow.removeLabelVersion(frozenNodeId);
                } else {
                    documentWorkflow.labelVersion(frozenNodeId, version.getLabel());
                }
            }

        } catch (ItemNotFoundException e) {
            log.info("Document for id '{}' does not exist or user '{}' is not allowed to read it",
                    handleId, userSession.getUserID());
            throw new NotFoundException(new ErrorInfo(ErrorInfo.Reason.DOES_NOT_EXIST));

        } catch (RepositoryException | WorkflowException| RemoteException e) {
            throw new InternalServerErrorException(new ErrorInfo(ErrorInfo.Reason.SERVER_ERROR));
        }
        return version;
    }

    private boolean requiresUpdate(final Optional<Campaign> stored, final Campaign update) {
        if (stored.isPresent()) {
            if (update == null) {
                // campaign must be removed
                return true;
            }

            // only when different it requires an update
            final Campaign campaign = stored.get();
            if (!campaign.getFrom().equals(update.getFrom())) {
                return true;
            }

            final long storedTo = campaign.getTo() == null ? 0 : campaign.getTo().getTimeInMillis();
            final long updateTo = update.getTo() == null ? 0 : update.getTo().getTimeInMillis();

            return storedTo != updateTo;
        } else {
            // stored is absent, if inpout not null, update required
            return update != null;
        }
    }

    private boolean requiresUpdate(final Optional<VersionLabel> stored, final String update) {
        if (stored.isPresent()) {
            if (update == null) {
                // label must be removed
                return true;
            }
            // only when different it requires an update
            return !stored.get().equals(update);
        } else {
            // stored is absent, if inpout not null, update required
            return update != null;
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

    private Version create(Calendar historic, Node node, boolean published, String branchId, final VersionsMeta versionsMeta) throws RepositoryException {
        final String userName = getStringProperty(node, HIPPOSTDPUBWF_LAST_MODIFIED_BY, null);

        final String identifier = node.getIdentifier();
        return new Version(historic, userName, identifier, published, branchId,
                versionsMeta.getVersionLabel(identifier).map(vl -> vl.getVersionLabel()).orElse(null),
                versionsMeta.getCampaign(identifier).orElse(null));
    }

}
