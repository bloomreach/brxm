/*
 *  Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.plugins.openui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.json.JSONObject;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.TextRequestHandler;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.string.Strings;
import org.hippoecm.frontend.editor.editor.EditorPlugin;
import org.hippoecm.frontend.editor.viewer.ComparePlugin;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.onehippo.cms.json.Json;
import org.onehippo.cms7.openui.extensions.UiExtension;
import org.onehippo.cms7.openui.extensions.UiExtensionPoint;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.cms7.services.contenttype.ContentTypeChild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class OpenUiStringPlugin extends RenderPlugin<String> implements OpenUiPlugin {

    private static final Logger log = LoggerFactory.getLogger(OpenUiStringPlugin.class);

    private static final String CONFIG_PROPERTY_UI_EXTENSION = "ui.extension";

    private final String hiddenValueId;
    private final IEditor.Mode documentEditorMode;
    private final String compareValue;
    private final AutoSaveBehavior autoSaveBehavior;
    private final DocumentFieldsBehavior documentFieldsBehavior;
    private final OpenUiBehavior openUiBehavior;

    public OpenUiStringPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        final HiddenField<String> value = new HiddenField<String>("value", getModel()) {
            {
                setFlag(FLAG_CONVERT_EMPTY_INPUT_STRING_TO_NULL, false);
            }
        };
        value.setOutputMarkupId(true);
        value.add(autoSaveBehavior = new AutoSaveBehavior());
        value.add(documentFieldsBehavior = new DocumentFieldsBehavior());
        queue(value);
        hiddenValueId = value.getMarkupId();

        final String extensionName = config.getString(CONFIG_PROPERTY_UI_EXTENSION);
        add(openUiBehavior = new OpenUiBehavior(this, extensionName, UiExtensionPoint.DOCUMENT_FIELD, context, config));

        final Label errorMessage = new Label("errorMessage",
                new StringResourceModel("load-error", this).setParameters(extensionName));
        errorMessage.setVisible(!openUiBehavior.isActive());
        queue(errorMessage);

        documentEditorMode = IEditor.Mode.fromString(config.getString("mode"), IEditor.Mode.VIEW);
        compareValue = getCompareValue(context, config).orElse(null);
    }

    private Optional<String> getCompareValue(final IPluginContext context, final IPluginConfig config) {
        if (documentEditorMode == IEditor.Mode.COMPARE && config.containsKey("model.compareTo")) {
            final IModel<?> compareModel = context.getService(config.getString("model.compareTo"),
                    IModelReference.class).getModel();
            if (compareModel != null) {
                return Optional.of(Strings.toString(compareModel.getObject()));
            }
        }
        return Optional.empty();
    }

    @Override
    public ObjectNode getJavaScriptParameters() {
        final ObjectNode parameters = Json.object();
        parameters.put("autoSaveDelay", 2000);
        parameters.put("autoSaveUrl", autoSaveBehavior.getCallbackUrl().toString());
        parameters.put("compareValue", compareValue);
        parameters.put("documentEditorMode", documentEditorMode.toString());
        parameters.put("documentFieldsUrl", documentFieldsBehavior.getCallbackUrl().toString());
        parameters.put("hiddenValueId", hiddenValueId);
        parameters.put("initialHeightInPixels", openUiBehavior.getUiExtension().getInitialHeightInPixels());

        getVariantNode().ifPresent(node -> addDocumentMetaData(parameters, node));

        return parameters;
    }

    private Optional<Node> getVariantNode() {
        final RenderPlugin plugin = getDocumentPlugin();
        if (plugin != null && plugin.getDefaultModel() instanceof JcrNodeModel) {
            return Optional.of(((JcrNodeModel) plugin.getDefaultModel()).getNode());
        }
        log.warn("Cannot find parent plugin to retrieve document meta data.");
        return Optional.empty();
    }

    private Optional<Node> getCompareNode() {
        if (documentEditorMode != IEditor.Mode.COMPARE) {
            return Optional.empty();
        }

        final ComparePlugin comparePlugin = findParent(ComparePlugin.class);
        if (comparePlugin == null) {
            return Optional.empty();
        }

        final IModel<Node> model = comparePlugin.getCompareNodeModel();
        if (model == null || model.getObject() == null) {
            return Optional.empty();
        }

        return Optional.of(model.getObject());
    }

    /**
     * Get the plugin containing the document information.
     */
    private RenderPlugin getDocumentPlugin() {
        if (documentEditorMode == IEditor.Mode.EDIT) {
            return findParent(EditorPlugin.class);
        } else {
            return findParent(ComparePlugin.class);
        }
    }

    private static void addDocumentMetaData(final ObjectNode parameters, final Node variant) {
        try {
            parameters.put("documentVariantId", variant.getIdentifier());
            if (variant.hasProperty(HippoTranslationNodeType.LOCALE)) {
                parameters.put("documentLocale", variant.getProperty(HippoTranslationNodeType.LOCALE).getString());
            }

            final Node handle = variant.getParent();
            parameters.put("documentId", handle.getIdentifier());
            final String urlName = handle.getName();
            final String displayName;
            if (handle.hasProperty(HippoNodeType.HIPPO_NAME)) {
                displayName = handle.getProperty(HippoNodeType.HIPPO_NAME).getString();
            } else {
                displayName = urlName;
            }
            parameters.put("documentDisplayName", displayName);
            parameters.put("documentUrlName", urlName);
        } catch (RepositoryException e) {
            log.error("Error retrieving document meta data.", e);
        }
    }

    private class AutoSaveBehavior extends AbstractDefaultAjaxBehavior {

        private static final String POST_PARAM_DATA = "data";

        @Override
        protected void respond(final AjaxRequestTarget target) {
            final Request request = RequestCycle.get().getRequest();
            final IRequestParameters requestParameters = request.getPostParameters();
            final StringValue data = requestParameters.getParameterValue(POST_PARAM_DATA);

            final UiExtension extension = openUiBehavior.getUiExtension();
            if (data.isNull()) {
                log.warn("Cannot auto-save value of OpenUI field '{}' because the request parameter '{}' is missing",
                        extension.getId(), POST_PARAM_DATA);
            } else {
                log.debug("Auto-saving OpenUI field '{}' with value '{}'", extension.getId(), data);
                getModel().setObject(data.toString());
            }
        }

    }

    private static final FieldSerializerService fieldSerializerService = new FieldSerializerService(Optional.empty());

    private static final FieldLookupService fieldLookupService = new FieldLookupService(
        fieldSerializerService,
        Optional.empty(),
        Optional.of(log)
    );

    private class DocumentFieldsBehavior extends AbstractDefaultAjaxBehavior {
        private static final String QUERY_PARAM_PATH = "fieldpath";
        private static final String QUERY_PARAM_COMPARE = "compare";
        private static final String CURRENT_CONTAINER_COMPOUND = ".";

        @Override
        protected void respond(final AjaxRequestTarget target) {
            final RequestCycle requestCycle = RequestCycle.get();
            final Request request = requestCycle.getRequest();
            final IRequestParameters queryParameters = request.getQueryParameters();
            final StringValue compare = queryParameters.getParameterValue(QUERY_PARAM_COMPARE);
            final Optional<Node> nodeOption = getNode(compare);
            final List<StringValue> path = convertCurrentContainerCompoundPathSegment(
                    queryParameters.getParameterValues(QUERY_PARAM_PATH), nodeOption);
            final JSONObject result = new JSONObject();

            result.put("data", JSONObject.NULL);

            if (!Objects.isNull(path) && !path.isEmpty()) {
                nodeOption.ifPresent(node -> result.put("data", getValue(path, node)));
            }

            requestCycle.replaceAllRequestHandlers(new TextRequestHandler("application/json", "UTF-8", result.toString()));
        }

        private Optional<Node> getNode(final StringValue compare) {
            return compare.toString("false").equalsIgnoreCase("true")
                    ? getCompareNode()
                    : getVariantNode();
        }

        private Object getValue(final List<StringValue> path, final Node node) {
            return fieldLookupService.lookup(node, path.stream()
                    .map(StringValue::toString)
                    .toArray(String[]::new));
        }

        private List<StringValue> convertCurrentContainerCompoundPathSegment(final List<StringValue> sourcePath,
                final Optional<Node> nodeOption) {
            final String compoundPath = (sourcePath != null && !sourcePath.isEmpty()) ? sourcePath.get(0).toString()
                    : null;

            if (!CURRENT_CONTAINER_COMPOUND.equals(compoundPath)) {
                return sourcePath;
            }

            try {
                final JcrPropertyValueModel<?> propValueModel = (JcrPropertyValueModel<?>) getModel();
                final Node compoundNode = propValueModel.getJcrPropertymodel().getProperty().getParent();

                if (!compoundNode.isNodeType(HippoNodeType.NT_COMPOUND)) {
                    return sourcePath;
                }

                final List<StringValue> convertedPath = new ArrayList<>(sourcePath);

                // replace the current compound reference, '.', by the real compound field name.
                final String compoundName = compoundNode.getName();
                convertedPath.set(0, StringValue.valueOf(compoundName));

                if (!nodeOption.isPresent()) {
                    return convertedPath;
                }

                final Node node = nodeOption.get();
                final ContentType contentType = fieldLookupService.getContentTypeForNode(node);
                final ContentTypeChild compoundType = contentType != null
                        ? fieldLookupService.getContentTypeChildByKey(contentType, compoundName)
                        : null;

                if (compoundType == null || !compoundType.isMultiple()) {
                    return convertedPath;
                }

                // if the compound type is multiple, but if the second path param is not in digits,
                // let's insert the compound index number for convenience.
                if (convertedPath.size() > 1 && !NumberUtils.isDigits(convertedPath.get(1).toString())) {
                    convertedPath.add(1, StringValue.valueOf(compoundNode.getIndex() - 1));
                }

                return convertedPath;
            } catch (RepositoryException e) {
                log.error("Failed to resolve the container compound node.", e);
            }

            return sourcePath;
        }
    }
}
