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

/**
 * Internal Link Picker
 * <p>
 * <smell>
 * The configuration of this picker is looked up in the _default_ plugin cluster of hippostd:html
 * instead of in the 'root/linkpicker' child node. The only difference is that the names in the
 * latter don't start with 'linkpicker.'. To fix this, this prefix is removed for the keys of the JSON configuration 
 * of this field so the resulting configuration matches the properties expected by the link picker code.
 * </smell>
 */
public class InternalLinkPicker {

    private static final String[] BOOLEAN_PROPERTIES = {
            "linkpicker.language.context.aware",
            "linkpicker.last.visited.enabled",
            "linkpicker.open.in.new.window.enabled"
    };
    private static final String[] STRING_PROPERTIES = {
            "linkpicker.base.uuid",
            "linkpicker.cluster.name",
            "linkpicker.last.visited.key"
    };
    private static final String[] MULTIPLE_STRING_PROPERTIES = {
            "linkpicker.last.visited.nodetypes",
            "linkpicker.nodetypes"
    };
    private static final String REMOVED_PREFIX = "linkpicker.";

    public static ObjectNode init(final FieldTypeContext fieldContext) {
        return new FieldTypeConfig(fieldContext)
                .removePrefix(REMOVED_PREFIX)
                .booleans(BOOLEAN_PROPERTIES)
                .strings(STRING_PROPERTIES)
                .multipleStrings(MULTIPLE_STRING_PROPERTIES)
                .build();
    }
}
