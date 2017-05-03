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
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugins.ckeditor.CKEditorNodePlugin;
import org.hippoecm.frontend.plugins.richtext.dialog.AbstractRichTextEditorDialog;
import org.hippoecm.frontend.plugins.richtext.dialog.RichTextEditorAction;
import org.hippoecm.frontend.plugins.richtext.dialog.links.LinkPickerBehavior;
import org.hippoecm.frontend.plugins.richtext.dialog.links.RichTextEditorLinkService;
import org.hippoecm.frontend.plugins.richtext.model.RichTextEditorDocumentLink;
import org.hippoecm.frontend.plugins.richtext.processor.WicketNodeFactory;
import org.hippoecm.frontend.session.UserSession;
import org.onehippo.ckeditor.Json;
import org.onehippo.cms7.services.processor.richtext.link.RichTextLinkFactory;
import org.onehippo.cms7.services.processor.richtext.link.RichTextLinkFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the picker dialog for internal links in rich text fields. The behavior can be called by the frontend to
 * open the link picker. Besides the normal link picker dialog parameters, this class reads the following properties
 * from the Wicket Ajax post parameters:
 *
 * - 'fieldId' contains the UUID of the compound node of the rich text field, so the dialog configuration can determine
 *   the field-node-specific settings (e.g. the language-context-aware base UUID).
 * - 'dialogConfig' contains the configuration for the link picker dialog for the rich text field as serialized JSON.
 */
class LinkPickerManager implements IClusterable {

    private static final Logger log = LoggerFactory.getLogger(LinkPickerManager.class);

    private final JavaPluginConfig linkPickerConfig;
    private final LinkPickerBehavior behavior;
    private String fieldId;

    LinkPickerManager(final IPluginContext context, final String channelEditorId) {
        linkPickerConfig = new JavaPluginConfig();

        final RichTextLinkFactory linkFactory = new RichTextLinkFactoryImpl(() -> fieldId, WicketNodeFactory.INSTANCE);
        final RichTextEditorLinkService linkService = new RichTextEditorLinkService(linkFactory);
        behavior = new StatelessLinkPickerBehavior(context, linkPickerConfig, linkService);
        behavior.setCloseAction(new LinkPickedAction(channelEditorId));
    }

    LinkPickerBehavior getBehavior() {
        return behavior;
    }

    private class StatelessLinkPickerBehavior extends LinkPickerBehavior {

        StatelessLinkPickerBehavior(final IPluginContext context,
                                           final IPluginConfig dialogConfig,
                                           final RichTextEditorLinkService linkService) {
            super(context, dialogConfig, linkService);
        }

        @Override
        protected AbstractRichTextEditorDialog<RichTextEditorDocumentLink> createDialog() {
            final Map<String, String> parameters = getParameters();

            setDocumentId(parameters);
            setLinkPickerConfig(parameters);

            return super.createDialog();
        }

        private void setDocumentId(final Map<String, String> parameters) {
            fieldId = parameters.get("fieldId");
        }

        private void setLinkPickerConfig(final Map<String, String> parameters) {
            linkPickerConfig.clear();
            linkPickerConfig.putAll(CKEditorNodePlugin.DEFAULT_LINK_PICKER_CONFIG);

            final String dialogConfigJson = parameters.get("dialogConfig");
            try {
                final ObjectNode dialogConfig = Json.object(dialogConfigJson);
                addJsonToConfig(dialogConfig, linkPickerConfig);
            } catch (IOException e) {
                log.warn("Could not parse link picker dialog configuration for field '{}': '{}'. Using default configuration.",
                        fieldId, dialogConfigJson, e);
            }

            final IPluginConfig dialogConfig = LinkPickerDialogConfig.fromPluginConfig(linkPickerConfig, this::getDocumentNode);
            linkPickerConfig.putAll(dialogConfig);
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

        private Node getDocumentNode() {
            try {
                return UserSession.get().getJcrSession().getNodeByIdentifier(fieldId);
            } catch (IllegalArgumentException | RepositoryException e) {
                log.info("Cannot find document '{}' while opening link picker", fieldId);
            }
            return null;
        }
    }

    private class LinkPickedAction implements RichTextEditorAction<RichTextEditorDocumentLink> {

        private final String channelEditorId;

        LinkPickedAction(final String channelEditorId) {
            this.channelEditorId = channelEditorId;
        }

        @Override
        public String getJavaScript(final RichTextEditorDocumentLink documentLink) {
            return "Ext.getCmp('" + channelEditorId + "').onLinkPicked(" + documentLink.toJsString() + ");";
        }
    }
}
