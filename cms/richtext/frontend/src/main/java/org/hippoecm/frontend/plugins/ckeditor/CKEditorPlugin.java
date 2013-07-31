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

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptUrlReferenceHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.richtext.RichTextModel;
import org.hippoecm.frontend.plugins.richtext.StripScriptModel;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.HippoStdNodeType;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.cms7.ckeditor.CKEditorConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CKEditorPlugin extends RenderPlugin {

    public static final String CONFIG_CKEDITOR_CONFIG_JSON = "ckeditor.config.json";
    public static final String DEFAULT_CKEDITOR_CONFIG_JSON = "{ customConfig: '', extraPlugins: 'onchange'}";

    private static final String FRAGMENT_ID = "fragment";
    private static final String VALUE_ID = "value";

    private static final ResourceReference SKIN = new CssResourceReference(CKEditorPlugin.class, "ckeditor.css");

    private static final Logger log = LoggerFactory.getLogger(CKEditorPlugin.class);

    private final String editorConfigJson;
    private String editorId;

    public CKEditorPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        editorConfigJson = readAndValidateEditorConfig(config);
        log.info("Using CKEditor configuration: \"" + editorConfigJson + "\"");

        Fragment components = createComponents();
        add(components);
    }

    private String readAndValidateEditorConfig(final IPluginConfig config) {
        String json = config.getString(CONFIG_CKEDITOR_CONFIG_JSON, DEFAULT_CKEDITOR_CONFIG_JSON);
        try {
            // validate JSON and return the sanitized version. This also strips additional extra JSON literals from the end.
            return new JSONObject(json).toString();
        } catch (JSONException e) {
            log.warn("CKEditor configuration variable '{}' does not contain valid JSON, but \"{}\". Using the default configuration \"{}\" instead",
                    new Object[]{ CONFIG_CKEDITOR_CONFIG_JSON, json, DEFAULT_CKEDITOR_CONFIG_JSON });
        }
        return DEFAULT_CKEDITOR_CONFIG_JSON;
    }

    private Fragment createComponents() {
        IEditor.Mode mode = getEditorMode();
        Fragment fragment = new Fragment(FRAGMENT_ID, mode.toString(), this);

        switch (mode) {
            case VIEW:
                addViewComponents(fragment);
                break;
            case EDIT:
                addEditComponentsAndHeaderItems(fragment);
                break;
            case COMPARE:
                addCompareComponents(fragment);
                break;
            default:
                log.error("Unsupported editor mode: {}", mode);
        }

        return fragment;
    }

    private IEditor.Mode getEditorMode() {
        final String modeName = getPluginConfig().getString("mode", IEditor.Mode.VIEW.name());
        return IEditor.Mode.fromString(modeName);
    }

    private void addViewComponents(Fragment fragment) {
        final IModel<String> viewModel = createViewModel();
        fragment.add(new WebMarkupContainer(VALUE_ID, viewModel) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag) {
                final String text = (String) getDefaultModelObject();
                if (text != null) {
                    replaceComponentTagBody(markupStream, openTag, text);
                } else {
                    super.onComponentTagBody(markupStream, openTag);
                }
            }
        });
    }

    private void addEditComponentsAndHeaderItems(final Fragment fragment) {
        final IModel<String> editModel = createEditModel();
        final TextArea<String> textArea = new TextArea<String>(VALUE_ID, editModel);
        textArea.setOutputMarkupId(true);
        fragment.add(textArea);

        editorId = textArea.getMarkupId();
    }

    private void addCompareComponents(final Fragment fragment) {
        // TODO
    }

    private IModel<String> createViewModel() {
        return new StripScriptModel(getValueModel());
    }

    private IModel<String> createEditModel() {
        return new RichTextModel(getValueModel());
    }

    private JcrPropertyValueModel getValueModel() {
        JcrNodeModel nodeModel = (JcrNodeModel) getDefaultModel();
        try {
            Node contentNode = nodeModel.getNode();
            Property contentProperty = contentNode.getProperty(HippoStdNodeType.HIPPOSTD_CONTENT);
            return new JcrPropertyValueModel(new JcrPropertyModel(contentProperty));
        } catch (RepositoryException e) {
            log.warn("Cannot get value of HTML field in plugin {}, using null instead", getPluginConfig().getName());
        }
        return null;
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);

        switch (getEditorMode()) {
            case VIEW:
                // no additional header items to render
                break;
            case EDIT:
                response.render(JavaScriptUrlReferenceHeaderItem.forReference(CKEditorConstants.CKEDITOR_JS));
                response.render(OnDomReadyHeaderItem.forScript(getJavaScriptForEditor()));
                break;
            case COMPARE:
                // no additional header items to render
                break;
        }

        response.render(CssHeaderItem.forReference(SKIN));
    }

    private String getJavaScriptForEditor() {
        return "(function() {"
            + "     var editor = CKEDITOR.replace('" + editorId + "', " + getConfigurationForEditor() + "); "
            + "     editor.on('change', editor.updateElement); "
            + "     HippoAjax.registerDestroyFunction(editor.element.$, function() { "
            + "         editor.destroy(); "
            + "     }, window); "
            + "}());";
    }

    private String getConfigurationForEditor() {
        try {
            JSONObject editorConfig = new JSONObject(editorConfigJson);
            editorConfig.put("language", getLocale().getLanguage());
            return editorConfig.toString();
        } catch (JSONException e) {
            log.error("Error creating editor configuration. Using the default editor configuration instead", e);
        }
        return DEFAULT_CKEDITOR_CONFIG_JSON;
    }

}
