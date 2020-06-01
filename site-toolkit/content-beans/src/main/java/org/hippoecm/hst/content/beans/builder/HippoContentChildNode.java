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

import org.apache.commons.collections.CollectionUtils;
import org.onehippo.cms7.services.contenttype.ContentTypeChild;

/**
 * Wrapper of a {@link ContentTypeChild} object to use in Runtime Bean Generation.
 */
public class HippoContentChildNode {

    private static final String CONTENT_BLOCKS_VALIDATOR = "contentblocks-validator";
    private final String name;
    private final boolean multiple;
    private final String type;
    private final boolean contentBlocks;

    public HippoContentChildNode(final String name, final ContentTypeChild contentType) {
        this.name = name;
        this.type = contentType.getEffectiveType();
        this.multiple = contentType.isMultiple();

        if (CollectionUtils.isEmpty(contentType.getValidators())) {
            this.contentBlocks = false;
        } else {
            this.contentBlocks = contentType.getValidators().stream()
                    .anyMatch(validator -> validator.contains(CONTENT_BLOCKS_VALIDATOR));
        }
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public boolean isContentBlocks() {
        return contentBlocks;
    }

    public boolean isMultiple() {
        return multiple;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("HippoContentChildNode{");
        sb.append(", name='").append(name).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append(", contentBlocks='").append(contentBlocks).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
