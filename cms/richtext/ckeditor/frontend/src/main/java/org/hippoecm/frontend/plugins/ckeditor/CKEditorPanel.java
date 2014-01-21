/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.ckeditor;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Application;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptUrlReferenceHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.cms7.ckeditor.CKEditorConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renders an instance of CKEditor to edit the HTML in the given model.
 * Additional extensions can be added via the {@link #addExtension(CKEditorPanelExtension)} method.
 */
public class CKEditorPanel extends Panel {

    private static final String WICKET_ID_EDITOR = "editor";
    private static final ResourceReference CKEDITOR_PANEL_CSS = new PackageResourceReference(CKEditorPanel.class, "CKEditorPanel.css");
    private static final ResourceReference CKEDITOR_PANEL_JS = new PackageResourceReference(CKEditorPanel.class, "CKEditorPanel.js");
    private static final String CKEDITOR_TIMESTAMP = RandomStringUtils.randomAlphanumeric(4);
    private static final int LOGGED_EDITOR_CONFIG_INDENT_SPACES = 2;

    private static final Logger log = LoggerFactory.getLogger(CKEditorPanel.class);

    private final String editorConfigJson;
    private final String editorId;
    private final List<CKEditorPanelExtension> extensions;

    public CKEditorPanel(final String id,
                  final String editorConfigJson,
                  final IModel<String> editModel) {
        super(id);

        this.editorConfigJson = editorConfigJson;

        final TextArea<String> textArea = new TextArea<String>(WICKET_ID_EDITOR, editModel);
        textArea.setOutputMarkupId(true);
        add(textArea);

        editorId = textArea.getMarkupId();

        extensions = new LinkedList<CKEditorPanelExtension>();
    }

    /**
     * @return the ID of the editor instance.
     */
    public String getEditorId() {
        return editorId;
    }

    /**
     * Adds custom server-side behavior to this panel.
     * @param extension the behavior to add.
     */
    public void addExtension(CKEditorPanelExtension extension) {
        extensions.add(extension);

        for (Behavior behavior : extension.getBehaviors()) {
            add(behavior);
        }
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);

        response.render(CssHeaderItem.forReference(CKEDITOR_PANEL_CSS));
        response.render(JavaScriptUrlReferenceHeaderItem.forReference(getCKEditorJsReference()));
        response.render(OnDomReadyHeaderItem.forScript(getJavaScriptForCKEditorTimestamp()));
        response.render(JavaScriptUrlReferenceHeaderItem.forReference(CKEDITOR_PANEL_JS));

        JSONObject editorConfig = getConfigurationForEditor();
        renderContentsCss(response, editorConfig);
        response.render(OnDomReadyHeaderItem.forScript(getJavaScriptForEditor(editorConfig)));
    }

    public static ResourceReference getCKEditorJsReference() {
        if (Application.get().getConfigurationType().equals(RuntimeConfigurationType.DEVELOPMENT)) {
            log.info("Using non-optimized CKEditor sources.");
            return CKEditorConstants.CKEDITOR_SRC_JS;
        }
        log.info("Using optimized CKEditor sources");
        return CKEditorConstants.CKEDITOR_OPTIMIZED_JS;
    }

    /**
     * Generates the script to set a unique cache-busting timestamp value used by CKEditor's resource loader.
     * This ensures that changes to plugins outside the minified CKEditor sources are picked up when the
     * CKEditor source itself does not change.
     */
    private String getJavaScriptForCKEditorTimestamp() {
        return "CKEDITOR.timestamp='" + CKEDITOR_TIMESTAMP + "';";
    }

    private JSONObject getConfigurationForEditor() {
        try {
            JSONObject editorConfig = JsonUtils.createJSONObject(editorConfigJson);

            // configure extensions
            for (CKEditorPanelExtension extension : extensions) {
                extension.addConfiguration(editorConfig);
            }

            // always use the language of the current CMS locale
            final Locale locale = getLocale();
            editorConfig.put(CKEditorConstants.CONFIG_LANGUAGE, locale.getLanguage());

            // convert Hippo-specific 'declarative' keystrokes to numeric ones
            final JSONArray declarativeAndNumericKeystrokes = editorConfig.optJSONArray(CKEditorConstants.CONFIG_KEYSTROKES);
            final JSONArray numericKeystrokes = DeclarativeKeystrokesConverter.convertToNumericKeystrokes(declarativeAndNumericKeystrokes);
            editorConfig.putOpt(CKEditorConstants.CONFIG_KEYSTROKES, numericKeystrokes);

            // load the localized hippo styles if no other styles are specified
            JsonUtils.putIfAbsent(editorConfig, CKEditorConstants.CONFIG_STYLES_SET, HippoStyles.getConfigStyleSet(locale));

            // disable custom config loading if not configured
            JsonUtils.putIfAbsent(editorConfig, CKEditorConstants.CONFIG_CUSTOM_CONFIG, StringUtils.EMPTY);

            if (log.isInfoEnabled()) {
                log.info("CKEditor configuration:\n" + editorConfig.toString(LOGGED_EDITOR_CONFIG_INDENT_SPACES));
            }

            return editorConfig;
        } catch (JSONException e) {
            throw new IllegalStateException("Error creating CKEditor configuration.", e);
        }
    }

    static void renderContentsCss(IHeaderResponse response, JSONObject editorConfig) {
        final JSONArray array = editorConfig.optJSONArray(CKEditorConstants.CONFIG_CONTENTS_CSS);
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                final String file = array.optString(i);
                response.render(CssHeaderItem.forUrl(file));
            }
        } else {
            final String file = editorConfig.optString(CKEditorConstants.CONFIG_CONTENTS_CSS);
            response.render(CssHeaderItem.forUrl(file));
        }
    }

    private String getJavaScriptForEditor(JSONObject editorConfig) {
        return "Hippo.createCKEditor('" + editorId + "', " + editorConfig.toString() + ");";
    }

    @Override
    protected void onDetach() {
        for (CKEditorPanelExtension behavior : extensions) {
            behavior.detach();
        }
        super.onDetach();
    }

}
