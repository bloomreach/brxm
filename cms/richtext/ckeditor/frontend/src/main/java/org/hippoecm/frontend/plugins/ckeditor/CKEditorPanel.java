/*
 * Copyright 2013-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptUrlReferenceHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.CmsHeaderItem;
import org.onehippo.ckeditor.CKEditorConfig;
import org.onehippo.ckeditor.Json;
import org.onehippo.cms7.ckeditor.CKEditorConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renders an instance of CKEditor to edit the HTML in the given model.
 * Additional extensions can be added via the {@link #addExtension(CKEditorPanelExtension)} method.
 */
public class CKEditorPanel extends Panel {

    private static final String WICKET_ID_EDITOR = "editor";
    private static final ResourceReference CKEDITOR_PANEL_JS = new PackageResourceReference(CKEditorPanel.class, "CKEditorPanel.js") {
        @Override
        public List<HeaderItem> getDependencies() {
            return Collections.singletonList(CmsHeaderItem.get());
        }
    };

    private static final Logger log = LoggerFactory.getLogger(CKEditorPanel.class);

    private final String editorConfigJson;
    private final String editorId;
    private final IModel<String> editorModel;
    private final List<CKEditorPanelExtension> extensions;

    public CKEditorPanel(final String id,
                  final String editorConfigJson,
                  final IModel<String> editorModel) {
        super(id);

        this.editorConfigJson = editorConfigJson;
        this.editorModel = editorModel;

        final TextArea<String> textArea = new TextArea<>(WICKET_ID_EDITOR, editorModel);
        textArea.setOutputMarkupId(true);
        add(textArea);

        editorId = textArea.getMarkupId();

        extensions = new LinkedList<>();
    }

    /**
     * @return the ID of the editor instance.
     */
    public String getEditorId() {
        return editorId;
    }

    /**
     * @return the model of the editor instance.
     */
    public IModel<String> getEditorModel() {
        return editorModel;
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

        response.render(JavaScriptUrlReferenceHeaderItem.forReference(CKEditorConstants.getCKEditorJsReference()));
        response.render(OnDomReadyHeaderItem.forScript(getJavaScriptForCKEditorTimestamp()));
        response.render(JavaScriptUrlReferenceHeaderItem.forReference(CKEDITOR_PANEL_JS));

        final ObjectNode editorConfig = getConfigurationForEditor();
        renderContentsCss(response, editorConfig);
        response.render(OnDomReadyHeaderItem.forScript(getJavaScriptForEditor(editorConfig)));
    }

    /**
     * Generates the script to set a unique cache-busting timestamp value used by CKEditor's resource loader.
     * This ensures that changes to plugins outside the minified CKEditor sources are picked up when the
     * CKEditor source itself does not change.
     */
    private String getJavaScriptForCKEditorTimestamp() {
        return "CKEDITOR.timestamp='" + CKEditorConstants.CKEDITOR_TIMESTAMP + "';";
    }

    private ObjectNode getConfigurationForEditor() {
        try {
            final ObjectNode editorConfig = Json.object(editorConfigJson);

            // configure extensions
            for (CKEditorPanelExtension extension : extensions) {
                extension.addConfiguration(editorConfig);
            }

            final String cmsLanguage = getLocale().getLanguage();
            CKEditorConfig.setDefaults(editorConfig, cmsLanguage);

            if (log.isInfoEnabled()) {
                log.info("CKEditor configuration:\n" + Json.prettyString(editorConfig));
            }

            return editorConfig;
        } catch (IOException e) {
            throw new IllegalStateException("Error creating CKEditor configuration.", e);
        }
    }

    static void renderContentsCss(IHeaderResponse response, ObjectNode editorConfig) {
        final JsonNode contentsCss = editorConfig.get(CKEditorConfig.CONTENTS_CSS);

        if (contentsCss == null) {
            return;
        } else if (contentsCss.isArray()) {
            for (int i = 0; i < contentsCss.size(); i++) {
                final String file = contentsCss.get(i).asText();
                response.render(CssHeaderItem.forUrl(file));
            }
        } else if (contentsCss.isTextual()) {
            final String file = contentsCss.asText();
            response.render(CssHeaderItem.forUrl(file));
        }
    }

    private String getJavaScriptForEditor(ObjectNode editorConfig) {
        return "Hippo.createCKEditor('" + editorId + "', " + editorConfig.toString() + ");";
    }

    @Override
    protected void onDetach() {
        extensions.forEach(CKEditorPanelExtension::detach);
        super.onDetach();
    }
}
