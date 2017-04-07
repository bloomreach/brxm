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

import org.apache.commons.lang.StringUtils;
import org.onehippo.ckeditor.CKEditorConfig;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.util.CKEditorUtils;
import org.onehippo.cms7.services.processor.html.HtmlProcessorFactory;
import org.onehippo.cms7.services.processor.html.model.HtmlProcessorModel;
import org.onehippo.cms7.services.processor.html.model.Model;
import org.onehippo.cms7.services.processor.html.model.SimpleModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class FormattedTextFieldType extends StringFieldType {

    private static final Logger log = LoggerFactory.getLogger(FormattedTextFieldType.class);

    private final String defaultConfig;

    public FormattedTextFieldType() {
        this(CKEditorConfig.DEFAULT_FORMATTED_TEXT_CONFIG);
    }

    protected FormattedTextFieldType(final String defaultConfig) {
        setType(Type.HTML);
        this.defaultConfig = defaultConfig;
    }

    public JsonNode getConfig() {
        try {
            final JsonNode config = CKEditorUtils.readConfig(defaultConfig);
            disableCustomConfigLoadingIfNotConfigured(config);
            return config;
        } catch (IOException e) {
            log.warn("Cannot read config of field '{}'", getId(), e);
        }
        return null;
    }

    @Override
    protected List<FieldValue> readValues(final Node node) {
        List<FieldValue> values = super.readValues(node);
        final HtmlReader reader = new HtmlReader();
        for (final FieldValue value : values) {
            value.setValue(reader.read(value.getValue()));
        }
        return values;
    }

    @Override
    protected List<FieldValue> writeValues(final Optional<List<FieldValue>> optionalValues) {
        List<FieldValue> values = super.writeValues(optionalValues);
        final HtmlWriter writer = new HtmlWriter();
        for (final FieldValue value : values) {
            value.setValue(writer.write(value.getValue()));
        }
        return values;
    }

    private void disableCustomConfigLoadingIfNotConfigured(final JsonNode config) {
        if (!config.has(CKEditorConfig.CUSTOM_CONFIG)) {
            final ObjectNode mutableConfig = (ObjectNode) config;
            mutableConfig.put(CKEditorConfig.CUSTOM_CONFIG, StringUtils.EMPTY);
        }
    }

    private static class HtmlReader {
        String read(final String html) {
            final Model<String> htmlModel = new SimpleModel<>(html);
            final HtmlProcessorFactory processorFactory = HtmlProcessorFactory.of("formatted");
            final HtmlProcessorModel processorModel = new HtmlProcessorModel(htmlModel, processorFactory);
            return processorModel.get();
        }
    }

    private static class HtmlWriter {
        String write(final String html) {
            final Model<String> htmlModel = new SimpleModel<>("");
            final HtmlProcessorFactory processorFactory = HtmlProcessorFactory.of("formatted");
            final HtmlProcessorModel processorModel = new HtmlProcessorModel(htmlModel, processorFactory);
            processorModel.set(html);
            return htmlModel.get();
        }
    }
}
