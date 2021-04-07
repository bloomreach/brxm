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
package org.onehippo.repository.documentworkflow.version;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class VersionsMeta {

    private Set<Campaign> campaigns = new HashSet<>();

    public Set<Campaign> getCampaigns() {
        return campaigns;
    }

    public void setCampaigns(final Set<Campaign> campaigns) {
        this.campaigns = campaigns;
    }

    public Optional<Campaign> getCampaign(final String campaignId) {
        return campaigns.stream().filter(campaign -> campaignId.equals(campaign.getUuid())).findFirst();
    }
    /**
     * <p>
     *     Adds {@code camppaign} and if the campaign is already present in {@code campaigns} replaces that campaign
     * </p>
     * @param campaign
     */
    public void setCampaign(final Campaign campaign) {
        campaigns.remove(campaign);
        campaigns.add(campaign);
    }

    public void removeCampaign(final String campaignId) {
        campaigns.removeIf(campaign -> campaign.getUuid().equals(campaignId));
    }

}
