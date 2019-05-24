/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms.channelmanager.content.documenttype.field.type;

import java.io.IOException;

import org.onehippo.ckeditor.CKEditorConfig;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;

import com.fasterxml.jackson.databind.node.ObjectNode;

class HtmlFieldConfig {

    private static final String CKEDITOR_CONFIG_OVERLAYED_JSON = "ckeditor.config.overlayed.json";
    private static final String CKEDITOR_CONFIG_APPENDED_JSON = "ckeditor.config.appended.json";

    private HtmlFieldConfig() {
    }

    static ObjectNode readJson(final FieldTypeContext fieldContext, final String defaultJson) throws IOException {
        final String overlayedJson = fieldContext.getStringConfig(CKEDITOR_CONFIG_OVERLAYED_JSON).orElse("");
        final String appendedJson = fieldContext.getStringConfig(CKEDITOR_CONFIG_APPENDED_JSON).orElse("");

        final ObjectNode combinedConfig = CKEditorConfig.combineConfig(defaultJson, overlayedJson, appendedJson);
        final String language = fieldContext.getParentContext().getLocale().getLanguage();

        return CKEditorConfig.setDefaults(combinedConfig, language);
    }
}
