/*
 *  Copyright 2013-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.ckeditor;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.onehippo.ckeditor.CKEditorConfig;
import org.onehippo.ckeditor.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for CKEditor field plugins, which use CKEditor for editing fields that contain HTML.
 * Configuration properties:
 * <dl>
 *     <dt>ckeditor.config.overlayed.json</dt>
 *     <dd>Overlayed JSON configuration for the CKEditor instance. This JSON gets overlayed on top of the
 *         default configuration provided in the constructor using
 *         {@link Json#overlay(com.fasterxml.jackson.databind.node.ObjectNode, com.fasterxml.jackson.databind.JsonNode)}.
 *         Use this property to replace default configuration values. Will be ignored when empty or missing.</dd>
 *     <dt>ckeditor.config.appended.json</dt>
 *     <dd>Appended JSON configuration for the CKEditor instance. This JSON gets appended to the default configuration
 *         provided in the constructor overlayed with the JSON in ckeditor.config.overlayed.json (so overlaying goes
 *         first, appended goes seconds). The actual appending is done using
 *         {@link Json#append(com.fasterxml.jackson.databind.node.ObjectNode, com.fasterxml.jackson.databind.JsonNode)}.
 *         Use this property to append strings or values to existing comma-separated string properties or JSON arrays.
 *         Will be ignored when empty or missing.</dd>
 *     <li>htmlprocessor.id: String property with the ID of the HTML processor service to use. Use an empty string
 *     to use the default HTML cleaner and effectively disable server-side HTML filtering.
 *     Default value: "" (empty string)</li>
 * </ul>
 */
public abstract class AbstractCKEditorPlugin<ModelType> extends RenderPlugin {

    public static final String CONFIG_CKEDITOR_CONFIG_OVERLAYED_JSON = "ckeditor.config.overlayed.json";
    public static final String CONFIG_CKEDITOR_CONFIG_APPENDED_JSON = "ckeditor.config.appended.json";
    public static final String CONFIG_HTML_CLEANER_SERVICE_ID = "htmlcleaner.id";
    public static final String CONFIG_HTML_PROCESSOR_SERVICE_ID = "htmlprocessor.id";
    public static final String CONFIG_MODEL_COMPARE_TO = "model.compareTo";

    private static final String WICKET_ID_PANEL = "panel";
    private static final Logger log = LoggerFactory.getLogger(AbstractCKEditorPlugin.class);

    public AbstractCKEditorPlugin(final IPluginContext context, final IPluginConfig config, final String defaultEditorConfigJson) {
        super(context, config);

        IEditor.Mode mode = getMode();
        Panel panel = createPanel(mode, defaultEditorConfigJson);
        add(panel);
    }

    private IEditor.Mode getMode() {
        return IEditor.Mode.fromString(getPluginConfig().getString("mode"), IEditor.Mode.VIEW);
    }

    private Panel createPanel(final IEditor.Mode mode, final String defaultEditorConfigJson) {
        switch (mode) {
            case VIEW:
                return createViewPanel(WICKET_ID_PANEL);
            case EDIT:
                final String editorConfigJson = createEditorConfiguration(defaultEditorConfigJson);
                return createEditPanel(WICKET_ID_PANEL, editorConfigJson);
            case COMPARE:
                return createComparePanel(WICKET_ID_PANEL);
            default:
                throw new IllegalStateException("Unsupported editor mode: " + mode);
        }
    }

    /**
     * Creates the panel to show in 'view' mode. It typically shows a preview of the HTML contained in the model.
     *
     * @param id the Wicket ID of the panel
     * @return the panel to show in 'view' mode.
     */
    protected abstract Panel createViewPanel(final String id);

    private String createEditorConfiguration(final String defaultJson) {
        try {
            final IPluginConfig pluginConfig = getPluginConfig();
            final String overlayedJson = pluginConfig.getString(CONFIG_CKEDITOR_CONFIG_OVERLAYED_JSON);
            final String appendedJson = pluginConfig.getString(CONFIG_CKEDITOR_CONFIG_APPENDED_JSON);
            return CKEditorConfig.combineConfig(defaultJson, overlayedJson, appendedJson).toString();
        } catch (IOException e) {
            log.warn("Error while creating CKEditor configuration, using default configuration as-is:\n" + defaultJson, e);
            return defaultJson;
        }
    }

    /**
     * Creates a panel that renders a CKEditor instance to edit the model.
     * @param id the Wicket ID of the panel
     * @param editorConfigJson the JSON configuration of the CKEditor instance to create.
     * @return the panel that displays a CKEditor instance.
     */
    protected CKEditorPanel createEditPanel(final String id, final String editorConfigJson) {
        CKEditorPanel editPanel = new CKEditorPanel(id, editorConfigJson, createEditModel());
        addAutoSaveExtension(editPanel);
        return editPanel;
    }

    /**
     * @return the Wicket model behind a CKEditor instance.
     */
    protected abstract IModel<String> createEditModel();

    private void addAutoSaveExtension(final CKEditorPanel editPanel) {
        final AutoSaveBehavior autoSaveBehavior = new AutoSaveBehavior(editPanel.getEditorModel());
        final CKEditorPanelAutoSaveExtension autoSaveExtension = new CKEditorPanelAutoSaveExtension(autoSaveBehavior);
        editPanel.addExtension(autoSaveExtension);
    }

    private Panel createComparePanel(final String id) {
        final IModel<ModelType> baseModel = getBaseModelOrNull();
        if (baseModel == null) {
            log.warn("Plugin '{}' cannot instantiate compare mode, using regular HTML preview instead.",
                    getPluginConfig().getName());
            return createViewPanel(id);
        }

        final IModel<ModelType> currentModel = (IModel<ModelType>) getDefaultModel();

        return createComparePanel(id, baseModel, currentModel);
    }

    /**
     * Creates the panel the show in 'compare' mode. It typically displays the differences between the provided
     * base model and current model.
     * @param id the Wicket ID of the panel
     * @param baseModel the model with the old HTML
     * @param currentModel the model with the new HTML
     * @return a panel that compares the current model with the base model.
     */
    protected abstract Panel createComparePanel(final String id, final IModel<ModelType> baseModel, final IModel<ModelType> currentModel);

    private IModel<ModelType> getBaseModelOrNull() {
        final IPluginConfig config = getPluginConfig();
        if (!config.containsKey(CONFIG_MODEL_COMPARE_TO)) {
            log.warn("Plugin {} is missing configuration property '{}'", config.getName(), CONFIG_MODEL_COMPARE_TO);
            return null;
        }

        final String compareToServiceId = config.getString(CONFIG_MODEL_COMPARE_TO);
        IModel<ModelType> model = getModelFromServiceOrNull(compareToServiceId);
        if (model == null) {
            log.warn("Plugin {} cannot get the node model from service '{}'. Check the config property '{}'",
                new Object[]{config.getName(), compareToServiceId, CONFIG_MODEL_COMPARE_TO});
        }
        return model;
    }

    @SuppressWarnings("unchecked")
    private IModel<ModelType> getModelFromServiceOrNull(final String serviceName) {
        final IModelReference modelRef = getPluginContext().getService(serviceName, IModelReference.class);
        if (modelRef == null) {
            log.warn("The service '{}' is not available", serviceName);
            return null;
        }
        return modelRef.getModel();
    }

    /**
     * @return the model with the HTML markup string that will be edited with CKEditor.
     */
    protected abstract IModel<String> getHtmlModel();

    String getHtmlProcessorId() {
        final IPluginConfig config = getPluginConfig();
        String configKey = CONFIG_HTML_PROCESSOR_SERVICE_ID;

        if (config.containsKey(CONFIG_HTML_CLEANER_SERVICE_ID)) {
            log.warn("Configuration option '{}' has been replaced by '{}', please update the configuration.",
                     CONFIG_HTML_CLEANER_SERVICE_ID, CONFIG_HTML_PROCESSOR_SERVICE_ID);
            configKey = CONFIG_HTML_CLEANER_SERVICE_ID;
        }

        return config.getString(configKey, StringUtils.EMPTY);
    }

}
