/*
 * Copyright 2017-2019 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.frontend.plugins.ckeditor.hippopicker.HippoPicker;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.ckeditor.CKEditorConfig;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.picker.RichTextImagePicker;
import org.onehippo.cms.channelmanager.content.picker.RichTextNodePicker;
import org.onehippo.cms7.services.htmlprocessor.HtmlProcessorFactory;
import org.onehippo.cms7.services.htmlprocessor.Tag;
import org.onehippo.cms7.services.htmlprocessor.TagVisitor;
import org.onehippo.cms7.services.htmlprocessor.model.HtmlProcessorModel;
import org.onehippo.cms7.services.htmlprocessor.model.Model;
import org.onehippo.cms7.services.htmlprocessor.richtext.URLEncoder;
import org.onehippo.cms7.services.htmlprocessor.richtext.image.RichTextImageTagProcessor;
import org.onehippo.cms7.services.htmlprocessor.richtext.jcr.JcrNodeFactory;
import org.onehippo.cms7.services.htmlprocessor.richtext.model.RichTextProcessorModel;
import org.onehippo.cms7.services.htmlprocessor.visit.FacetTagProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A document field of type hippostd:html.
 */
public class RichTextFieldType extends NodeFieldType implements LeafFieldType, HtmlField {

    private static final Logger log = LoggerFactory.getLogger(RichTextFieldType.class);

    private static final String DEFAULT_HTMLPROCESSOR_ID = "richtext";

    // Images are rendered with a relative path, pointing to the binaries servlet. The binaries servlet always
    // runs at the same level; two directories up from the angular app. Because of this we need to prepend
    // all internal images with a prefix as shown below.
    private static final TagVisitor RELATIVE_IMAGE_PATH_VISITOR = new RelativePathImageVisitor("../../");

    private final String defaultJson;
    private final String defaultHtmlProcessorId;

    private ObjectNode config;
    private HtmlProcessorFactory processorFactory;

    public RichTextFieldType() {
        this(CKEditorConfig.DEFAULT_RICH_TEXT_CONFIG, DEFAULT_HTMLPROCESSOR_ID);
    }

    RichTextFieldType(final String defaultJson, final String defaultHtmlProcessorId) {
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

            final ObjectNode hippoPickerConfig = config.with(HippoPicker.CONFIG_KEY);
            hippoPickerConfig.set(HippoPicker.InternalLink.CONFIG_KEY, RichTextNodePicker.build(fieldContext));
            hippoPickerConfig.set(HippoPicker.Image.CONFIG_KEY, RichTextImagePicker.build(fieldContext));
        } catch (IOException e) {
            log.warn("Error while reading config of rich text field '{}'", getId(), e);
        }
    }

    private void initProcessorFactory(final FieldTypeContext fieldContext) {
        final String processorId = fieldContext.getStringConfig(HTMLPROCESSOR_ID).orElse(defaultHtmlProcessorId);
        processorFactory = HtmlProcessorFactory.of(processorId);
    }

    @Override
    public ObjectNode getConfig() {
        return config;
    }

    @Override
    public FieldValue readValue(final Node node) {
        final FieldValue value = new FieldValue();
        try {
            final String storedHtml = JcrUtils.getStringProperty(node, HippoStdNodeType.HIPPOSTD_CONTENT, null);
            final String processedHtml = read(storedHtml, node);
            value.setValue(processedHtml);
            value.setId(node.getIdentifier());
        } catch (final RepositoryException e) {
            log.warn("Failed to read rich text field '{}' from node '{}'", getId(), JcrUtils.getNodePathQuietly(node), e);
        }
        return value;
    }

    @Override
    public void writeValue(final Node node, final FieldValue fieldValue) throws RepositoryException {
        final String html = write(fieldValue.getValue(), node);
        node.setProperty(HippoStdNodeType.HIPPOSTD_CONTENT, html);
    }

    private String read(final String html, final Node node) {
        final Model<String> htmlModel = Model.of(html);
        final Model<Node> nodeModel = Model.of(node);
        final JcrNodeFactory nodeFactory = JcrNodeFactory.of(node);
        final HtmlProcessorModel processorModel = new RichTextProcessorModel(htmlModel, nodeModel, processorFactory,
                nodeFactory, URLEncoder.OPAQUE);
        processorModel.getVisitors().add(RELATIVE_IMAGE_PATH_VISITOR);
        return processorModel.get();
    }

    private String write(final String html, final Node node) {
        final Model<Node> nodeModel = Model.of(node);
        final JcrNodeFactory nodeFactory = JcrNodeFactory.of(node);
        final Model<String> htmlModel = Model.of("");
        final HtmlProcessorModel processorModel = new RichTextProcessorModel(htmlModel, nodeModel, processorFactory,
                nodeFactory, URLEncoder.OPAQUE);
        processorModel.set(html);
        return htmlModel.get();
    }

    private static final class RelativePathImageVisitor implements TagVisitor {

        private final String pathPrefix;

        private RelativePathImageVisitor(final String pathPrefix) {
            this.pathPrefix = pathPrefix;
        }

        @Override
        public void onRead(final Tag parent, final Tag tag) {
            if (tag != null
                    && StringUtils.equalsIgnoreCase(RichTextImageTagProcessor.TAG_IMG, tag.getName())
                    && tag.hasAttribute(RichTextImageTagProcessor.ATTRIBUTE_SRC)
                    && tag.hasAttribute(FacetTagProcessor.ATTRIBUTE_DATA_UUID)) {
                final String src = tag.getAttribute(RichTextImageTagProcessor.ATTRIBUTE_SRC);
                tag.addAttribute(RichTextImageTagProcessor.ATTRIBUTE_SRC, pathPrefix + src);
            }
        }

        @Override
        public void onWrite(final Tag parent, final Tag tag) {}

        @Override
        public void before() {}

        @Override
        public void after() {}
    }
}
