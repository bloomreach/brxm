/*
 * Copyright 2020-2023 Bloomreach
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
package org.onehippo.cms.channelmanager.content.document.model;

import java.util.Calendar;

import org.onehippo.repository.campaign.Campaign;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Version {

    private final Calendar timestamp;
    private final String userName;
    private final String jcrUUID;
    // if true, this version corresponds to the 'published variant' below the handle
    private boolean published;
    private final String branchId;
    private String label;
    private Campaign campaign;
    private boolean active;

    @JsonCreator
    public Version(
            @JsonProperty("timestamp") Calendar timestamp,
            @JsonProperty("userName") String userName,
            @JsonProperty("jcrUUID") String jcrUUID,
            @JsonProperty("published") boolean published,
            @JsonProperty("branchId") String branchId,
            @JsonProperty("label") String label,
            @JsonProperty("campaign") Campaign campaign) {
        this.timestamp = timestamp;
        this.userName = userName;
        this.jcrUUID = jcrUUID;
        this.published = published;
        this.branchId = branchId;
        this.label = label;
        this.campaign = campaign;
    }

    public Calendar getTimestamp() {
        return timestamp;
    }

    public String getUserName() {
        return userName;
    }

    public String getJcrUUID() {
        return jcrUUID;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(final boolean published) {
        this.published = published;
    }

    public String getBranchId() {
        return branchId;
    }

    public String getLabel() {
        return label;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }
}
