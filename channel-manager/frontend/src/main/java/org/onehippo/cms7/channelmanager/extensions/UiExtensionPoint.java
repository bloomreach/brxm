/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.channelmanager.extensions;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The UI extension point: the place in the UI where the extension will be shown.
 */
public enum UiExtensionPoint {

    PAGESIDEPANEL("channel.page.tools");

    private final String extensionPoint;

    UiExtensionPoint(final String extensionPoint) {
        this.extensionPoint = extensionPoint;    
    }

    public static UiExtensionPoint getForConfigProperty(final String configProperty) {
        for(UiExtensionPoint uiExtensionPoint : UiExtensionPoint.values()) {
            if (uiExtensionPoint.getConfigProperty().equalsIgnoreCase(configProperty)) {
                return uiExtensionPoint;
            }
        }
        throw new IllegalArgumentException(String.format("UiExtensionPoint for '%s' does not exist.", configProperty));
    }
    
    public String getConfigProperty() {
        return this.extensionPoint;
    }
    
    @JsonValue
    public String getLowerCase() {
        return this.name().toLowerCase();
    }
}
