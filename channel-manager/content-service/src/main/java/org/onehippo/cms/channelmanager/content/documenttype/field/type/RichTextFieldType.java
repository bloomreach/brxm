/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.ckeditor.CKEditorConfig;
import org.hippoecm.frontend.plugins.ckeditor.hippopicker.HippoPicker;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.cms.channelmanager.content.picker.RichTextImagePicker;
import org.onehippo.cms.channelmanager.content.picker.RichTextNodePicker;
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
public class RichTextFieldType extends FormattedTextFieldType implements NodeFieldType {

    private static final Logger log = LoggerFactory.getLogger(RichTextFieldType.class);

    private static final String DEFAULT_HTMLPROCESSOR_ID = "richtext";

    // Images are rendered with a relative path, pointing to the binaries servlet. The binaries servlet always
    // runs at the same level; two directories up from the angular app. Because of this we need to prepend
    // all internal images with a prefix as shown below.
    private static final TagVisitor RELATIVE_IMAGE_PATH_VISITOR = new RelativePathImageVisitor("../../");

    public RichTextFieldType() {
        super(CKEditorConfig.DEFAULT_RICH_TEXT_CONFIG, DEFAULT_HTMLPROCESSOR_ID);
    }

    @Override
    public FieldsInformation init(final FieldTypeContext fieldContext) {
        final FieldsInformation fieldsInfo = super.init(fieldContext);

        final ObjectNode hippoPickerConfig = getConfig().with(HippoPicker.CONFIG_KEY);
        hippoPickerConfig.set(HippoPicker.InternalLink.CONFIG_KEY, RichTextNodePicker.build(fieldContext));
        hippoPickerConfig.set(HippoPicker.Image.CONFIG_KEY, RichTextImagePicker.build(fieldContext));

        return fieldsInfo;
    }

    @Override
    public Optional<List<FieldValue>> readFrom(final Node node) {
        final List<FieldValue> values = readValues(node);

        trimToMaxValues(values);

        if (values.size() < getMinValues()) {
            log.error("No values available for node of type '{}' of document at {}. This document type cannot be " +
                    "used to create new documents in the Channel Manager.", getId(), JcrUtils.getNodePathQuietly(node));
        }

        return values.isEmpty() ? Optional.empty() : Optional.of(values);
    }

    protected List<FieldValue> readValues(final Node node) {
        try {
            final NodeIterator children = node.getNodes(getId());
            final List<FieldValue> values = new ArrayList<>((int) children.getSize());
            for (final Node child : new NodeIterable(children)) {
                final FieldValue value = readValue(child);
                if (value.hasValue()) {
                    values.add(value);
                }
            }

            return values;
        } catch (final RepositoryException e) {
            log.warn("Failed to read rich text field '{}'", getId(), e);
        }
        return Collections.emptyList();
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
    public void writeValues(final Node node,
                            final Optional<List<FieldValue>> optionalValues,
                            final boolean validateValues) throws ErrorWithPayloadException {
        final String valueName = getId();
        final List<FieldValue> values = optionalValues.orElse(Collections.emptyList());

        if (validateValues) {
            checkCardinality(values);
        }

        try {
            final NodeIterator children = node.getNodes(valueName);
            FieldTypeUtils.writeNodeValues(children, values, getMaxValues(), this);
        } catch (final RepositoryException e) {
            log.warn("Failed to write rich text field '{}'", valueName, e);
            throw new InternalServerErrorException();
        }
    }

    @Override
    public void writeValue(final Node node, final FieldValue fieldValue) throws ErrorWithPayloadException, RepositoryException {
        final String html = write(fieldValue.getValue(), node);
        node.setProperty(HippoStdNodeType.HIPPOSTD_CONTENT, html);
    }

    @Override
    public boolean validateValue(final FieldValue value) {
        return !isRequired() || validateSingleRequired(value);
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
        public void onRead(final Tag parent, final Tag tag) throws RepositoryException {
            if (tag != null
                    && StringUtils.equalsIgnoreCase(RichTextImageTagProcessor.TAG_IMG, tag.getName())
                    && tag.hasAttribute(RichTextImageTagProcessor.ATTRIBUTE_SRC)
                    && tag.hasAttribute(FacetTagProcessor.ATTRIBUTE_DATA_UUID)) {
                final String src = tag.getAttribute(RichTextImageTagProcessor.ATTRIBUTE_SRC);
                tag.addAttribute(RichTextImageTagProcessor.ATTRIBUTE_SRC, pathPrefix + src);
            }
        }

        @Override
        public void onWrite(final Tag parent, final Tag tag) throws RepositoryException {}

        @Override
        public void before() {}

        @Override
        public void after() {}
    }
}
