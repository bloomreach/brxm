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
    private Set<VersionLabel> versionLabels = new HashSet<>();

    public Set<Campaign> getCampaigns() {
        return campaigns;
    }

    public void setCampaigns(final Set<Campaign> campaigns) {
        this.campaigns = campaigns;
    }

    public Set<VersionLabel> getVersionLabels() {
        return versionLabels;
    }

    public void setVersionLabels(final Set<VersionLabel> versionLabels) {
        this.versionLabels = versionLabels;
    }

    public Optional<Campaign> getCampaign(final String uuid) {
        return campaigns.stream().filter(campaign -> uuid.equals(campaign.getUuid())).findFirst();
    }

    public Optional<VersionLabel> getVersionLabel(final String uuid) {
        return versionLabels.stream().filter(label -> uuid.equals(label.getUuid())).findFirst();
    }

    /**
     * <p>
     *     Adds {@code campaign} and if the campaign is already present in {@code campaigns} replaces that campaign
     * </p>
     * @param campaign
     */
    public void setCampaign(final Campaign campaign) {
        // first remove campaign with the same UUID since that one will be replaced
        campaigns.removeIf(c -> c.getUuid().equals(campaign.getUuid()));
        campaigns.add(campaign);
    }

    public void removeCampaign(final String uuid) {
        campaigns.removeIf(campaign -> campaign.getUuid().equals(uuid));
    }

    /**
     * <p>
     *     Adds {@code versionLabel} and if the versionLabel is already present in {@code versionLabels}
     *     replaces that versionLabel
     * </p>
     * @param versionLabel
     */
    public void setVersionLabel(final VersionLabel versionLabel) {
        versionLabels.removeIf(vl -> vl.getUuid().equals(versionLabel.getUuid()));
        versionLabels.add(versionLabel);
    }

    public void removeVersionLabel(final String uuid) {
        versionLabels.removeIf(versionLabel -> versionLabel.getUuid().equals(uuid));
    }

}
