/*
 * Copyright 2017-2021 Hippo B.V. (http://www.onehippo.com)
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

import org.onehippo.ckeditor.CKEditorConfig;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms7.services.htmlprocessor.HtmlProcessorFactory;
import org.onehippo.cms7.services.htmlprocessor.model.HtmlProcessorModel;
import org.onehippo.cms7.services.htmlprocessor.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class FormattedTextFieldType extends StringFieldType implements HtmlField {

    private static final Logger log = LoggerFactory.getLogger(FormattedTextFieldType.class);

    private static final String HTMLPROCESSOR_ID = "htmlprocessor.id";
    private static final String DEFAULT_HTMLPROCESSOR_ID = "formatted";

    private final String defaultJson;
    private final String defaultHtmlProcessorId;

    private ObjectNode config;
    private HtmlProcessorFactory processorFactory;

    public FormattedTextFieldType() {
        this(CKEditorConfig.DEFAULT_FORMATTED_TEXT_CONFIG, DEFAULT_HTMLPROCESSOR_ID);
    }

    FormattedTextFieldType(final String defaultJson, final String defaultHtmlProcessorId) {
        setType(Type.HTML);
        this.defaultJson = defaultJson;
        this.defaultHtmlProcessorId = defaultHtmlProcessorId;
    }

    @Override
    public FieldsInformation init(final FieldTypeContext fieldContext) {
        final FieldsInformation fieldsInfo = super.init(fieldContext);

        initConfig(fieldContext);
        initProcessorFactory(fieldContext);

        return fieldsInfo;
    }

    private void initConfig(final FieldTypeContext fieldContext) {
        try {
            config = HtmlFieldConfig.readJson(fieldContext, defaultJson);
        } catch (IOException e) {
            log.warn("Error while reading config of formatted text field '{}'", getId(), e);
        }
    }

    private void initProcessorFactory(final FieldTypeContext fieldContext) {
        final String processorId = fieldContext.getStringConfig(HTMLPROCESSOR_ID).orElse(defaultHtmlProcessorId);
        processorFactory = HtmlProcessorFactory.of(processorId);
    }

    public ObjectNode getConfig() {
        return config;
    }

    @Override
    protected void afterReadValues(List<FieldValue> values) {
        for (final FieldValue value : values) {
            value.setValue(read(value.getValue()));
        }
    }

    @Override
    protected void beforeWriteValues(final List<FieldValue> values) {
        for (final FieldValue value : values) {
            value.setValue(write(value.getValue()));
        }
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
