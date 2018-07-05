/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.cms.channelmanager.content.picker;

import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeConfig;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class ImagePicker {

    private static final String[] BOOLEAN_PROPERTIES = {
            "imagepicker.last.visited.enabled"
    };
    private static final String[] STRING_PROPERTIES = {
            "imagepicker.base.uuid",
            "imagepicker.cluster.name",
            "imagepicker.last.visited.key",
            "imagepicker.preferred.image.variant"
    };

    private static final String[] MULTIPLE_STRING_PROPERTIES = {
            "excluded.image.variants",
            "imagepicker.last.visited.nodetypes",
            "imagepicker.nodetypes",
            "included.image.variants"
    };
    private static final String REMOVED_PREFIX = "imagepicker.";

    public static ObjectNode init(final FieldTypeContext fieldContext) {
        return new FieldTypeConfig(fieldContext)
                .removePrefix(REMOVED_PREFIX)
                .booleans(BOOLEAN_PROPERTIES)
                .strings(STRING_PROPERTIES)
                .multipleStrings(MULTIPLE_STRING_PROPERTIES)
                .build();
    }
}
