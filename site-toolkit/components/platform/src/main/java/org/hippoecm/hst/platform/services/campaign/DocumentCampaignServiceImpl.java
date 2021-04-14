/*
 *  Copyright 2021 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.platform.services.campaign;

import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.campaign.DocumentCampaignService;
import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.campaign.Campaign;
import org.onehippo.repository.campaign.VersionsMeta;
import org.onehippo.repository.documentworkflow.campaign.JcrVersionsMetaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.util.JcrUtils.getStringProperty;
import static org.onehippo.repository.branch.BranchConstants.MASTER_BRANCH_ID;
import static org.onehippo.repository.util.JcrConstants.NT_FROZEN_NODE;

public class DocumentCampaignServiceImpl implements DocumentCampaignService {

    private final static Logger log = LoggerFactory.getLogger(DocumentCampaignServiceImpl.class);

    public void init() {
        HippoServiceRegistry.register(this, DocumentCampaignService.class);
    }

    public void destroy() {
        HippoServiceRegistry.unregister(this, DocumentCampaignService.class);
    }


    /**
     * <p>
     *     If the {@code handle} contains versionsMeta and the versionsMeta has active campaigns which match
     *     the branchId and the current server time, return the {@link Campaign} with the shortest duration
     * </p>
     * @param handle the document for which an attempt is made to find the active campaign
     * @param branchId the branch for which an active campaign is sought
     * @return an optional containing the active {@link Campaign} or an empty optional in case it is absent
     */
    @Override
    public Optional<Campaign> findActiveCampaign(final Node handle, final String branchId) {
        try {
            final VersionsMeta versionsMeta = JcrVersionsMetaUtils.getVersionsMeta(handle);
            if (versionsMeta.getCampaigns().isEmpty()) {
                return Optional.empty();
            }
            long currentEpochMillis = System.currentTimeMillis();
            // find whether there is an active campaign for the current branch, and if so, serve that version
            return versionsMeta.getCampaigns().stream()
                    .filter(campaign ->
                            // only campaigns which date range matches current time
                            campaign.getFrom().getTimeInMillis() < currentEpochMillis
                                    && campaign.getTo().getTimeInMillis() > currentEpochMillis
                    ).filter(campaign ->
                            // only those versions which match the branchId
                            {
                                try {
                                    final Node frozenNode = handle.getSession().getNodeByIdentifier(campaign.getUuid());
                                    if (!frozenNode.isNodeType(NT_FROZEN_NODE)) {
                                        log.warn("Campaign unexpectedly not pointing to a frozenNode '{}'. Skip campaign", campaign.getUuid());
                                        return false;
                                    }
                                    // filter for campaigns for current branch
                                    return getStringProperty(frozenNode, HippoNodeType.HIPPO_PROPERTY_BRANCH_ID, MASTER_BRANCH_ID)
                                            .equals(branchId);
                                } catch (RepositoryException e) {
                                    log.warn("Could not fetch campaign for uuid '{}' : {}", campaign.getUuid(), e.getMessage());
                                    return false;
                                }
                            }
                    ).reduce((campaign, campaign2) -> {
                        // return shortest campaign if multiple match
                        final long duration1 = campaign.getTo().getTimeInMillis() - campaign.getFrom().getTimeInMillis();
                        final long duration2 = campaign2.getTo().getTimeInMillis() - campaign2.getFrom().getTimeInMillis();
                        return duration1 < duration2 ? campaign : campaign2;
                    });
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.error("Repository Exception happended, return empty optional", e);
            } else {
                log.error("Repository Exception happended, return empty optional : {}", e.getMessage());
            }
            return Optional.empty();
        }
    }
}
