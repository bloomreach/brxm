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

import org.hippoecm.hst.platform.api.beans.FieldGroupInfo;
import org.hippoecm.hst.platform.api.beans.HstPropertyDefinitionInfo;

public class ChannelInfoDescription {
    private List<FieldGroupInfo> fieldGroups;
    private Map<String, HstPropertyDefinitionInfo> propertyDefinitions;
    private Map<String, String> i18nResources;
    private String lockedBy;
    private boolean editable;

    public ChannelInfoDescription(final List<FieldGroupInfo> fieldGroups,
                                  final Map<String, HstPropertyDefinitionInfo> propertyDefinitions,
                                  final Map<String, String> i18nResources,
                                  final String lockedBy,
                                  final boolean editable) {
        this.fieldGroups = fieldGroups;
        this.propertyDefinitions = propertyDefinitions;
        this.i18nResources = i18nResources;
        this.lockedBy = lockedBy;
        this.editable = editable;
    }

    public List<FieldGroupInfo> getFieldGroups() {
        return fieldGroups;
    }

    public Map<String, String> getI18nResources() {
        return i18nResources;
    }

    public Map<String, HstPropertyDefinitionInfo> getPropertyDefinitions() {
        return propertyDefinitions;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public boolean isEditable() {
        return editable;
    }
}
