/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.sdk.api.model.rest;

/**
 * ContentTypeInstance exposes properties of a specific instance of a content type.
 */
public class ContentTypeInstance {
    private final String jcrPath;
    private final String jcrType;
    private final String displayName;

    public ContentTypeInstance(final String jcrPath, final String jcrType, final String displayName) {
        this.jcrPath = jcrPath;
        this.jcrType = jcrType;
        this.displayName = displayName;
    }

    public String getJcrPath() {
        return jcrPath;
    }

    public String getJcrType() {
        return jcrType;
    }

    public String getDisplayName() {
        return displayName;
    }
}
