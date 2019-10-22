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

import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.onehippo.cms7.services.contenttype.ContentTypeChild;

/**
 * Wrapper of a {@link ContentTypeChild} object to use in Runtime Bean Generation.
 */
public class HippoContentChildNode {

    private static final Pattern PREFIX_SPLITTER = Pattern.compile(":");
    private static final String CONTENT_BLOCKS_VALIDATOR = "contentblocks-validator";
    private final ContentTypeChild contentType;
    private final String prefix;
    private final String shortName;
    private final String name;
    private final boolean multiple;
    private final String type;
    private final String cmsType;
    private final boolean contentBlocks;

    public HippoContentChildNode(final ContentTypeChild contentType) {
        this.contentType = contentType;
        this.name = contentType.getName();
        if (name.indexOf(':') != -1) {
            final String[] fullName = PREFIX_SPLITTER.split(contentType.getName());
            this.shortName = fullName[1];
            this.prefix = fullName[0];
        } else {
            this.shortName = name;
            this.prefix = null;
        }
        this.type = contentType.getEffectiveType();
        this.cmsType = contentType.getItemType();
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

    public String getCmsType() {
        return cmsType;
    }

    public ContentTypeChild getContentType() {
        return contentType;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getShortName() {
        return shortName;
    }

    public String getName() {
        return name;
    }

    public boolean hasContentBlocks() {
        return contentBlocks;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("HippoContentChildNode{");
        sb.append("contentType=").append(contentType);
        sb.append(", prefix='").append(prefix).append('\'');
        sb.append(", shortName='").append(shortName).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append(", contentBlocks='").append(contentBlocks).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public boolean isMultiple() {
        return multiple;
    }
}
