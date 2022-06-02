/*
 *  Copyright 2017-2022 Bloomreach (https://www.bloomreach.com)
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
package org.hippoecm.frontend.dialog;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.onehippo.cms.json.Json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

/**
 * Base manager for showing a dialog.
 * <p>
 * To open a dialog, the frontend requests a URL that is generated by the {@link DialogBehavior}. This request can
 * contain the following two request-parameters to pass data to the dialog:
 * <ul>
 *     <li>dialogConfig: an object containing configuration properties for the dialog</li>
 *     <li>dialogContext: an object containing properties that describe the context in which the dialog is opened</li>
 * </ul>
 * Both of these parameters are expected to be stringified JSON blobs and will be parsed as follows:
 * <ul>
 *     <li>Nested properties are ignored</li>
 *     <li>Text values are parsed as a String</li>
 *     <li>Boolean values are parsed as a Boolean</li>
 *     <li>Arrays are parsed as a String arrays</li>
 * </ul>
 * </p>
 */
@Slf4j
public class DialogManager<ModelType> implements IDetachable {

    private static final String DIALOG_CONFIG = "dialogConfig";
    private static final String DIALOG_CONTEXT = "dialogContext";

    private final IPluginContext context;
    private final IPluginConfig config;
    private final DialogBehavior behavior;

    private ScriptAction<ModelType> cancelAction;
    private ScriptAction<ModelType> closeAction;

    public DialogManager(final IPluginContext context, final IPluginConfig config) {
        this.context = context;
        this.config = config;

        behavior = new DialogBehavior() {
            @Override
            protected void showDialog(final Map<String, String> parameters) {
                onShowDialog(parameters);
            }
        };
    }

    public DialogBehavior getBehavior() {
        return behavior;
    }

    private void onShowDialog(final Map<String, String> parameters) {
        beforeShowDialog(parameters);

        final String paramsDialogConfig = parameters.get(DIALOG_CONFIG);
        final String paramsDialogContext = parameters.get(DIALOG_CONTEXT);

        final IPluginConfig dialogPluginConfigTemplate = mergeJsonWithConfig(config, paramsDialogConfig);
        final IPluginConfig dialogPluginContext = mergeJsonWithConfig(new JavaPluginConfig(), paramsDialogContext);

        final IPluginConfig dialogPluginConfig = createPluginConfig(dialogPluginConfigTemplate, dialogPluginContext);
        final Dialog<ModelType> dialog = createDialog(context, dialogPluginConfig, parameters);

        if (cancelAction != null) {
            dialog.setCancelAction(cancelAction);
        }
        if (closeAction != null) {
            dialog.setCloseAction(closeAction);
        }

        getDialogService().show(dialog);
    }

    protected void beforeShowDialog(final Map<String, String> parameters) {
    }

    protected Dialog<ModelType> createDialog(final IPluginContext context, final IPluginConfig config, final Map<String, String> parameters) {
        return new Dialog<>();
    }

    protected IPluginConfig createPluginConfig(final IPluginConfig template, final IPluginConfig context) {
        return template;
    }

    private IDialogService getDialogService() {
        return context.getService(IDialogService.class.getName(), IDialogService.class);
    }

    public void setCancelAction(final ScriptAction<ModelType> cancelAction) {
        this.cancelAction = cancelAction;
    }

    public void setCloseAction(final ScriptAction<ModelType> closeAction) {
        this.closeAction = closeAction;
    }

    @Override
    public void detach() {
    }

    private IPluginConfig mergeJsonWithConfig(final IPluginConfig defaultConfig, final String jsonConfig) {
        if (StringUtils.isEmpty(jsonConfig)) {
            return defaultConfig;
        }

        final JavaPluginConfig config = new JavaPluginConfig();
        config.putAll(defaultConfig);

        try {
            final ObjectNode dialogConfig = Json.object(jsonConfig);
            addJsonToConfig(dialogConfig, config);
        } catch (IOException e) {
            log.warn("Could not parse dialog configuration '{}'. Using default configuration.",
                    jsonConfig, e);
        }
        return config;
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
        }

        if (jsonValue.isBoolean()) {
            return jsonValue.asBoolean();
        }

        if (jsonValue.isArray()) {
            // always assume an array of strings
            final ArrayNode array = (ArrayNode) jsonValue;
            final String[] values = new String[array.size()];
            for (int i = 0; i < values.length; i++) {
                values[i] = array.get(i).asText();
            }
            return values;
        }

        log.warn("Skipped JSON value of unknown type: '{}'", jsonValue);
        return null;
    }
}
