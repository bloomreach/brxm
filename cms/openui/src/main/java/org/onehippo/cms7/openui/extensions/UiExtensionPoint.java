/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.openui.extensions;

public enum UiExtensionPoint {

    DOCUMENT_FIELD("document.field"),
    CHANNEL_PAGE_TOOL("channel.page.tools"),
    UNKNOWN("unknown");

    private final String configValue;

    UiExtensionPoint(final String configValue) {
        this.configValue = configValue;
    }

    public static UiExtensionPoint getByConfigValue(final String configValue) {
        for (UiExtensionPoint uiExtensionPoint : UiExtensionPoint.values()) {
            if (uiExtensionPoint.configValue.equalsIgnoreCase(configValue)) {
                return uiExtensionPoint;
            }
        }
        return UNKNOWN;
    }
}
