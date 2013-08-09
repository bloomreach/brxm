/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugins.richtext.IHtmlCleanerService;
import org.hippoecm.frontend.plugins.richtext.view.RichTextComparePanel;
import org.hippoecm.frontend.plugins.richtext.view.RichTextPreviewPanel;
import org.hippoecm.frontend.plugins.standards.picker.NodePickerControllerSettings;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.HippoStdNodeType;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CKEditorPlugin extends RenderPlugin {

    public static final String CONFIG_CKEDITOR_CONFIG_JSON = "ckeditor.config.json";
    public static final String CONFIG_MODEL_COMPARE_TO = "model.compareTo";
    public static final String CONFIG_CHILD_IMAGE_PICKER = "imagepicker";
    public static final String CONFIG_CHILD_LINK_PICKER = "linkpicker";

    public static final IPluginConfig DEFAULT_IMAGE_PICKER_CONFIG = createNodePickerSettings(
            "cms-pickers/images", "ckeditor-imagepicker", "hippostd:gallery");

    public static final IPluginConfig DEFAULT_LINK_PICKER_CONFIG = createNodePickerSettings(
            "cms-pickers/documents", "ckeditor-linkpicker", "hippostd:folder");

    private static final String WICKET_ID_PANEL = "panel";

    private static final Logger log = LoggerFactory.getLogger(CKEditorPlugin.class);

    public CKEditorPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        IEditor.Mode mode = getMode();
        Panel panel = createPanel(mode);
        add(panel);
    }

    private IEditor.Mode getMode() {
        final String modeName = getPluginConfig().getString("mode", IEditor.Mode.VIEW.name().toLowerCase());
        return IEditor.Mode.fromString(modeName);
    }

    private Panel createPanel(final IEditor.Mode mode) {
        switch (mode) {
            case VIEW:
                return createViewPanel();
            case EDIT:
                return createEditPanel();
            case COMPARE:
                return createComparePanel();
            default:
                throw new IllegalStateException("Unsupported editor mode: " + mode);
        }
    }

    private Panel createViewPanel() {
        return new RichTextPreviewPanel(WICKET_ID_PANEL, getNodeModel(), getHtmlModel(), getBrowser());
    }

    private Panel createEditPanel() {
        final String editorConfigJson = readAndValidateEditorConfig();
        final IPluginConfig imagePickerConfig = getChildPluginConfig(CONFIG_CHILD_IMAGE_PICKER, DEFAULT_IMAGE_PICKER_CONFIG);
        final IPluginConfig linkPickerConfig = getChildPluginConfig(CONFIG_CHILD_LINK_PICKER, DEFAULT_LINK_PICKER_CONFIG);
        return new CKEditorPanel(WICKET_ID_PANEL, getPluginContext(), imagePickerConfig, linkPickerConfig, editorConfigJson,
                getNodeModel(), getHtmlModel(), getHtmlCleaner());
    }

    private Panel createComparePanel() {
        final JcrNodeModel baseNodeModel = getBaseNodeModelOrNull();
        if (baseNodeModel == null) {
            log.warn("Plugin '{}' cannot instantiate compare mode, using regular HTML preview instead.",
                    getPluginConfig().getName());
            return createViewPanel();
        }

        final JcrNodeModel currentNodeModel = (JcrNodeModel) getDefaultModel();

        return new RichTextComparePanel(WICKET_ID_PANEL, baseNodeModel, currentNodeModel, getBrowser());
    }

    private JcrNodeModel getBaseNodeModelOrNull() {
        final IPluginConfig config = getPluginConfig();
        if (!config.containsKey(CONFIG_MODEL_COMPARE_TO)) {
            log.warn("Plugin {} is missing configuration property '{}'", config.getName(), CONFIG_MODEL_COMPARE_TO);
            return null;
        }
        final String compareToServiceId = config.getString(CONFIG_MODEL_COMPARE_TO);
        final JcrNodeModel nodeModel = getNodeModelFromServiceOrNull(compareToServiceId);
        if (nodeModel == null) {
            log.warn("Plugin {} cannot get the service '{}'. Check the config property '{}'",
                    new Object[]{config.getName(), compareToServiceId, CONFIG_MODEL_COMPARE_TO});
        }
        return nodeModel;
    }

    private IPluginConfig getChildPluginConfig(final String key, IPluginConfig defaultConfig) {
        IPluginConfig childConfig = getPluginConfig().getPluginConfig(key);
        return childConfig != null ? childConfig : defaultConfig;
    }

    private String readAndValidateEditorConfig() {
        String jsonOrNull = getPluginConfig().getString(CONFIG_CKEDITOR_CONFIG_JSON);
        try {
            // validate JSON and return the sanitized version. This also strips additional extra JSON literals from the end.
            return JsonUtils.createJSONObject(jsonOrNull).toString();
        } catch (JSONException e) {
            log.warn("Ignoring CKEditor configuration variable '{}' because does not contain valid JSON, but \"{}\"",
                    CONFIG_CKEDITOR_CONFIG_JSON, jsonOrNull);
        }
        return StringUtils.EMPTY;
    }

    private JcrNodeModel getNodeModel() {
        return (JcrNodeModel) getDefaultModel();
    }

    private JcrNodeModel getNodeModelFromServiceOrNull(final String serviceName) {
        final IModelReference modelRef = getPluginContext().getService(serviceName, IModelReference.class);
        if (modelRef == null) {
            log.warn("The service '{}' is not available", serviceName);
            return null;
        }
        final IModel model = modelRef.getModel();
        if (model == null) {
            log.warn("The service '{}' does not provide a valid node model", serviceName);
            return null;
        }
        return (JcrNodeModel) model;
    }

    private IModel<String> getHtmlModel() {
        JcrNodeModel nodeModel = (JcrNodeModel) getDefaultModel();
        try {
            Node contentNode = nodeModel.getNode();
            Property contentProperty = contentNode.getProperty(HippoStdNodeType.HIPPOSTD_CONTENT);
            return new JcrPropertyValueModel<String>(new JcrPropertyModel(contentProperty));
        } catch (RepositoryException e) {
            log.warn("Cannot get value of HTML field in plugin {}, using null instead", getPluginConfig().getName());
        }
        return null;
    }

    private IBrowseService getBrowser() {
        final String browserId = getPluginConfig().getString(IBrowseService.BROWSER_ID);
        return getPluginContext().getService(browserId, IBrowseService.class);
    }

    private IHtmlCleanerService getHtmlCleaner() {
        return getPluginContext().getService(IHtmlCleanerService.class.getName(), IHtmlCleanerService.class);
    }

    private static IPluginConfig createNodePickerSettings(final String clusterName, final String lastVisitedKey, final String lastVisitedNodeTypes) {
        JavaPluginConfig config = new JavaPluginConfig();
        config.put("cluster.name", clusterName);
        config.put(NodePickerControllerSettings.LAST_VISITED_KEY, lastVisitedKey);
        config.put(NodePickerControllerSettings.LAST_VISITED_NODETYPES, lastVisitedNodeTypes);
        config.makeImmutable();
        return config;
    }

}
