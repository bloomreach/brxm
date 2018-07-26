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

import org.apache.commons.lang.StringUtils;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeConfig;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Image Picker
 * <p>
 * <smell>
 * The configuration of this picker is looked up in the _default_ plugin cluster of hippostd:html
 * instead of in the 'root/imagepicker' child node. The only difference is that the names in the
 * latter don't start with 'imagepicker.'. To fix this, this prefix is appended to the keys in the JCR configuration
 * object.
 * </smell>
 */
public class RichTextImagePicker {

    private static final String[] STRING_PROPERTIES = {
            "preferred.image.variant"
    };

    private static final String[] MULTIPLE_STRING_PROPERTIES = {
            "excluded.image.variants",
            "included.image.variants"
    };

    private static final String PREFIX = "imagepicker.";

    public static ObjectNode build(final FieldTypeContext fieldContext) {
        final FieldTypeConfig fieldConfig = new FieldTypeConfig(fieldContext)
                .prefix(PREFIX);

        return Picker.configure(fieldConfig)
                .strings(STRING_PROPERTIES)
                .prefix(StringUtils.EMPTY)
                .multipleStrings(MULTIPLE_STRING_PROPERTIES)
                .build();
    }
}
