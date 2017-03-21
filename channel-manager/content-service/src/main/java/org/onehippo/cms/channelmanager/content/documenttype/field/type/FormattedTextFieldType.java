/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms.channelmanager.content.documenttype.field.type;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import org.onehippo.ckeditor.CKEditorConfig;
import org.onehippo.cms.channelmanager.content.documenttype.util.CKEditorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FormattedTextFieldType extends StringFieldType {

    private static final Logger log = LoggerFactory.getLogger(FormattedTextFieldType.class);

    private final String defaultConfig;

    public FormattedTextFieldType() {
        this(CKEditorConfig.DEFAULT_FORMATTED_TEXT_CONFIG);
    }

    protected FormattedTextFieldType(final String defaultConfig) {
        setType(Type.HTML);
        this.defaultConfig = defaultConfig;
    }

    public JsonNode getConfig() {
        try {
            return CKEditorUtils.readConfig(defaultConfig);
        } catch (IOException e) {
            log.warn("Cannot read config of field '{}'", getId(), e);
        }
        return null;
    }
}
