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
package org.hippoecm.hst.campaign;

import java.util.Optional;

import javax.jcr.Node;

import org.onehippo.repository.campaign.Campaign;

public interface DocumentCampaignService {

    /**
     * <p>
     *     Given the {@code handle} and the {@code branchId} returns a {@link Campaign} if the version for the
     *     {@link Campaign} is active. The implementation of this service decides based on which criteria there is an
     *     active campaign or not for the {@code handle} document. A typical heuristic would be to find a campaign
     *     version which has a from-to date which matches the current server time. If multiple campaign version match,
     *     a heuristic can be to return the campaign with the most recent start date. In general an
     *     implementation also requires that the possibly used version matches the {@code branchId}
     * </p>
     * @param handle the document for which an attempt is made to find the active campaign
     * @param branchId the branch for which an active campaign is sought
     * @return an optional containing the active {@link Campaign} or an empty optional in case it is absent
     */
    Optional<Campaign> findActiveCampaign(Node handle, String branchId);

}
