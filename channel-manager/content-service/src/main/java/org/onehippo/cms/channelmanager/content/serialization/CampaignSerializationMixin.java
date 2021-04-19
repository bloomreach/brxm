/*
 * Copyright 2021 Hippo B.V. (http://www.onehippo.com)
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
 */
package org.onehippo.cms.channelmanager.content.serialization;

import org.onehippo.repository.campaign.Campaign;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * The uuid from the campaign does not require to be serialized for the 'Versions' rest endpoint
 */
@JsonInclude(NON_NULL)
public class CampaignSerializationMixin extends Campaign {

    @JsonIgnore
    @Override
    public String getUuid() {
        return super.getUuid();
    }

}
