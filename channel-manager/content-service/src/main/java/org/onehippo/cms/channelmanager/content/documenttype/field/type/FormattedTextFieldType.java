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
import java.util.List;
import java.util.Optional;

import javax.jcr.Node;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.onehippo.ckeditor.CKEditorConfig;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms7.services.processor.html.HtmlProcessorFactory;
import org.onehippo.cms7.services.processor.html.model.HtmlProcessorModel;
import org.onehippo.cms7.services.processor.html.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FormattedTextFieldType extends StringFieldType {

    private static final Logger log = LoggerFactory.getLogger(FormattedTextFieldType.class);

    private static final String CKEDITOR_CONFIG_OVERLAYED_JSON = "ckeditor.config.overlayed.json";
    private static final String CKEDITOR_CONFIG_APPENDED_JSON = "ckeditor.config.appended.json";
    private static final String HTMLPROCESSOR_ID = "htmlprocessor.id";
    private static final String DEFAULT_HTMLPROCESSOR_ID = "formatted";

    private final String defaultJson;
    private ObjectNode config;
    private final String defaultHtmlProcessorId;

    protected HtmlProcessorFactory processorFactory;

    public FormattedTextFieldType() {
        this(CKEditorConfig.DEFAULT_FORMATTED_TEXT_CONFIG, DEFAULT_HTMLPROCESSOR_ID);
    }

    protected FormattedTextFieldType(final String defaultJson, final String defaultHtmlProcessorId) {
        setType(Type.HTML);
        this.defaultJson = defaultJson;
        this.defaultHtmlProcessorId = defaultHtmlProcessorId;
    }

    @Override
    public void init(final FieldTypeContext fieldContext) {
        super.init(fieldContext);

        final String overlayedJson = fieldContext.getStringConfig(CKEDITOR_CONFIG_OVERLAYED_JSON).orElse("");
        final String appendedJson = fieldContext.getStringConfig(CKEDITOR_CONFIG_APPENDED_JSON).orElse("");

        try {
            final ObjectNode combinedConfig = CKEditorConfig.combineConfig(defaultJson, overlayedJson, appendedJson);
            final String language = fieldContext.getParentContext().getLocale().getLanguage();
            config = CKEditorConfig.setDefaults(combinedConfig, language);
        } catch (IOException e) {
            log.warn("Error while reading config of HTML field '{}'", getId(), e);
        }

        final String processorId = fieldContext.getStringConfig(HTMLPROCESSOR_ID).orElse(defaultHtmlProcessorId);
        processorFactory = HtmlProcessorFactory.of(processorId);
    }

    public ObjectNode getConfig() {
        return config;
    }

    @Override
    protected List<FieldValue> readValues(final Node node) {
        List<FieldValue> values = super.readValues(node);
        for (final FieldValue value : values) {
            value.setValue(read(value.getValue()));
        }
        return values;
    }

    @Override
    protected List<FieldValue> writeValues(final Optional<List<FieldValue>> optionalValues) {
        List<FieldValue> values = super.writeValues(optionalValues);
        for (final FieldValue value : values) {
            value.setValue(write(value.getValue()));
        }
        return values;
    }

    private String read(final String html) {
        final Model<String> htmlModel = Model.of(html);
        final HtmlProcessorModel processorModel = new HtmlProcessorModel(htmlModel, processorFactory);
        return processorModel.get();

    }

    private String write(final String html) {
        final Model<String> htmlModel = Model.of("");
        final HtmlProcessorModel processorModel = new HtmlProcessorModel(htmlModel, processorFactory);
        processorModel.set(html);
        return htmlModel.get();
    }
}
