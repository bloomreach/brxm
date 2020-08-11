/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.builder;

import org.onehippo.cms7.services.contenttype.ContentTypeProperty;

/**
 * Wrapper of a {@link ContentTypeProperty} object to use in Runtime Bean Generation.
 */
public class HippoContentProperty {

    private final String name;
    private final boolean multiple;
    private final String type;
    private final String cmsType;

    public HippoContentProperty(final String name, final ContentTypeProperty contentType) {
        this.name = name;
        this.type = contentType.getEffectiveType();
        this.cmsType = contentType.getItemType();
        this.multiple = contentType.isMultiple();
    }

    public String getType() {
        return type;
    }

    public String getCmsType() {
        return cmsType;
    }

    public String getName() {
        return name;
    }

    public boolean isMultiple() {
        return multiple;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("HippoContentProperty{");
        sb.append(", name='").append(name).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append(", cmsType='").append(cmsType).append('\'');
        sb.append(", multiple='").append(multiple).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
