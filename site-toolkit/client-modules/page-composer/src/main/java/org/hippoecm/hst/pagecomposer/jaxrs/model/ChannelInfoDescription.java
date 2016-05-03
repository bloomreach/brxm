/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.pagecomposer.jaxrs.model;

import java.util.List;
import java.util.Map;

import org.hippoecm.hst.rest.beans.FieldGroupInfo;

public class ChannelInfoDescription {
    private List<FieldGroupInfo> fieldGroups;

    private Map<String, String> i18nResources;

    public ChannelInfoDescription(final List<FieldGroupInfo> fieldGroups, final Map<String, String> i18nResources) {
        this.fieldGroups = fieldGroups;
        this.i18nResources = i18nResources;
    }

    public List<FieldGroupInfo> getFieldGroups() {
        return fieldGroups;
    }

    public Map<String, String> getI18nResources() {
        return i18nResources;
    }
}
