/*
 * Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
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

class UiExtensionBean implements UiExtension {

    private String id;
    private String displayName;
    private String url;
    private String config;
    private String extensionPoint;

    @Override
    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    @Override
    public String getConfig() {
        return config;
    }

    public void setConfig(final String config) {
        this.config = config;
    }

    @Override
    public String getExtensionPoint() {
        return extensionPoint;
    }

    public void setExtensionPoint(final String extensionPoint) {
        this.extensionPoint = extensionPoint;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final UiExtensionBean that = (UiExtensionBean) o;

        return id.equalsIgnoreCase(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}

