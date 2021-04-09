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
package org.onehippo.cms.channelmanager.content.document.model;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DocumentVersionInfo {

    private final List<Version> versions;
    private final boolean restoreEnabled;
    private boolean createEnabled;
    private boolean campaignEnabled;
    private boolean labelEnabled;

    @JsonCreator
    public DocumentVersionInfo(
            @JsonProperty("versions") List<Version> versions,
            @JsonProperty("restoreEnabled") boolean restoreEnabled,
            @JsonProperty("createEnabled") boolean createEnabled,
            @JsonProperty("labelEnabled") boolean labelEnabled,
            @JsonProperty("campaignEnabled") boolean campaignEnabled

    ) {
        Objects.requireNonNull(versions);
        this.versions = versions;
        this.restoreEnabled = restoreEnabled;
        this.createEnabled = createEnabled;
        this.labelEnabled = labelEnabled;
        this.campaignEnabled = campaignEnabled;
    }

    public List<Version> getVersions() {
        return versions;
    }

    public boolean isRestoreEnabled() {
        return restoreEnabled;
    }

    public boolean isCreateEnabled() {
        return createEnabled;
    }

    public boolean isCampaignEnabled() {
        return campaignEnabled;
    }

    public boolean isLabelEnabled() {
        return labelEnabled;
    }
}
