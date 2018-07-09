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

class Picker {

    private static final String[] BOOLEAN_PROPERTIES = {
            "last.visited.enabled",
    };

    private static final String[] STRING_PROPERTIES = {
            "base.uuid",
            "cluster.name",
            "last.visited.key",
    };

    private static final String[] MULTIPLE_STRING_PROPERTIES = {
            "last.visited.nodetypes",
            "nodetypes"
    };

    static FieldTypeConfig configure(final FieldTypeContext fieldContext) {
        return configure(new FieldTypeConfig(fieldContext));
    }

    static FieldTypeConfig configure(final FieldTypeConfig fieldConfig) {
        return fieldConfig
                .booleans(Picker.BOOLEAN_PROPERTIES)
                .strings(Picker.STRING_PROPERTIES)
                .multipleStrings(Picker.MULTIPLE_STRING_PROPERTIES);

    }
}
