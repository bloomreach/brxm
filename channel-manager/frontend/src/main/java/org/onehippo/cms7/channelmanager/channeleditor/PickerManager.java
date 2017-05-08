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

package org.onehippo.cms7.channelmanager.channeleditor;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.editor.plugins.linkpicker.LinkPickerDialogConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.onehippo.ckeditor.Json;
import org.onehippo.cms7.services.processor.html.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for the manager of a picker for rich text fields. Contains the generic code for parsing AJAX parameters:
 *
 * - 'fieldId' contains the UUID of the compound node of the rich text field, so the dialog configuration can determine
 *   the field-node-specific settings
 * - 'dialogConfig' contains the configuration for the picker dialog for the rich text field as serialized JSON.
 */
class PickerManager implements IClusterable {

    private static final Logger log = LoggerFactory.getLogger(PickerManager.class);

    private final IPluginConfig defaultPluginConfig;
    private final JavaPluginConfig pickerConfig;
    private String fieldId;

    PickerManager(final IPluginConfig defaultPickerConfig) {
        this.defaultPluginConfig = defaultPickerConfig;
        pickerConfig = new JavaPluginConfig();
    }

    JavaPluginConfig getPickerConfig() {
        return pickerConfig;
    }

    String getFieldId() {
        return fieldId;
    }

    Node getFieldNode() {
        try {
            return UserSession.get().getJcrSession().getNodeByIdentifier(fieldId);
        } catch (IllegalArgumentException | RepositoryException e) {
            log.info("Cannot find document '{}' while opening link picker", fieldId);
        }
        return null;
    }

    Model<Node> getFieldNodeModel() {
        return new Model<Node>() {
            @Override
            public Node get() {
                return getFieldNode();
            }

            @Override
            public void set(final Node value) {
            }

            @Override
            public void release() {
            }
        };
    }

    void initPicker(final Map<String, String> parameters) {
        setFieldId(parameters);
        setPickerConfig(parameters);
    }

    private void setFieldId(final Map<String, String> parameters) {
        fieldId = parameters.get("fieldId");
    }

    private void setPickerConfig(final Map<String, String> parameters) {
        pickerConfig.clear();
        pickerConfig.putAll(defaultPluginConfig);

        final String dialogConfigJson = parameters.get("dialogConfig");
        try {
            final ObjectNode dialogConfig = Json.object(dialogConfigJson);
            addJsonToConfig(dialogConfig, pickerConfig);
        } catch (IOException e) {
            log.warn("Could not parse picker dialog configuration for field '{}': '{}'. Using default configuration.",
                    fieldId, dialogConfigJson, e);
        }

        final IPluginConfig dialogConfig = LinkPickerDialogConfig.fromPluginConfig(pickerConfig, this::getFieldNode);
        pickerConfig.putAll(dialogConfig);
    }

    private void addJsonToConfig(final ObjectNode json, final JavaPluginConfig config) {
        final Iterator<String> jsonFields = json.fieldNames();
        while (jsonFields.hasNext()) {
            final String jsonField = jsonFields.next();
            final JsonNode jsonValue = json.get(jsonField);
            config.put(jsonField, asJavaObject(jsonValue));
        }
    }

    private Object asJavaObject(final JsonNode jsonValue) {
        if (jsonValue.isTextual()) {
            return jsonValue.asText();
        } else if (jsonValue.isBoolean()) {
            return jsonValue.asBoolean();
        } else if (jsonValue.isArray()) {
            // always assume an array of strings
            final ArrayNode array = (ArrayNode) jsonValue;
            final String[] values = new String[array.size()];
            for (int i = 0; i < values.length; i++) {
                values[i] = array.get(i).asText();
            }
            return values;
        }
        log.warn("Skipped JSON value of unknown type: '{}'", jsonValue.toString());
        return null;
    }
}
