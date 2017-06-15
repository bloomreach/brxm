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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.ckeditor.CKEditorConfig;
import org.onehippo.ckeditor.HippoPicker;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.cms7.services.htmlprocessor.Tag;
import org.onehippo.cms7.services.htmlprocessor.TagVisitor;
import org.onehippo.cms7.services.htmlprocessor.model.HtmlProcessorModel;
import org.onehippo.cms7.services.htmlprocessor.model.Model;
import org.onehippo.cms7.services.htmlprocessor.richtext.image.RichTextImageTagProcessor;
import org.onehippo.cms7.services.htmlprocessor.richtext.jcr.JcrNodeFactory;
import org.onehippo.cms7.services.htmlprocessor.richtext.model.RichTextProcessorModel;
import org.onehippo.cms7.services.htmlprocessor.visit.FacetTagProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A document field of type hippostd:html.
 *
 * <smell>
 * The configuration of the link- and image pickers is looked up in the _default_ plugin cluster of hippostd:html
 * instead of in the 'root/linkpicker' and 'root/imagepicker' child nodes. The only difference is that the names in the
 * latter don't start with 'linkpicker.' and 'imagepicker.', respectively. To fix this, these prefixes are removed for
 * the keys of the JSON configuration of this field so the resulting configuration matches the properties expected by
 * the link- and image picker code.
 * </smell>
 */
public class RichTextFieldType extends FormattedTextFieldType implements NodeFieldType {

    private static final Logger log = LoggerFactory.getLogger(RichTextFieldType.class);

    private static final String[] LINKPICKER_BOOLEAN_PROPERTIES = {
            "linkpicker.language.context.aware",
            "linkpicker.last.visited.enabled",
            "linkpicker.open.in.new.window.enabled"
    };
    private static final String[] LINKPICKER_STRING_PROPERTIES = {
            "linkpicker.base.uuid",
            "linkpicker.cluster.name",
            "linkpicker.last.visited.key"
    };
    private static final String[] LINKPICKER_MULTIPLE_STRING_PROPERTIES = {
            "linkpicker.last.visited.nodetypes",
            "linkpicker.nodetypes"
    };
    private static final String LINKPICKER_REMOVED_PREFIX = "linkpicker.";

    private static final String[] IMAGEPICKER_BOOLEAN_PROPERTIES = {
            "imagepicker.last.visited.enabled"
    };
    private static final String[] IMAGEPICKER_STRING_PROPERTIES = {
            "imagepicker.base.uuid",
            "imagepicker.cluster.name",
            "imagepicker.last.visited.key",
            "imagepicker.preferred.image.variant"
    };

    private static final String[] IMAGEPICKER_MULTIPLE_PROPERTIES = {
            "excluded.image.variants",
            "imagepicker.last.visited.nodetypes",
            "imagepicker.nodetypes",
            "included.image.variants"
    };
    private static final String IMAGEPICKER_REMOVED_PREFIX = "imagepicker.";

    private static final String DEFAULT_HTMLPROCESSOR_ID = "richtext";

    // Images are rendered with a relative path, pointing to the binaries servlet. The binaries servlet always
    // runs at the same level; two directories up from the angular app. Because of this we need to prepend
    // all internal images with a prefix as shown below.
    private static final TagVisitor RELATIVE_IMAGE_PATH_VISITOR = new RelativePathImageVisitor("../../");

    public RichTextFieldType() {
        super(CKEditorConfig.DEFAULT_RICH_TEXT_CONFIG, DEFAULT_HTMLPROCESSOR_ID);
    }

    void initListBasedChoice(final String choiceId) {
        setId(choiceId);
    }

    @Override
    public void init(final FieldTypeContext fieldContext) {
        super.init(fieldContext);

        final ObjectNode hippoPickerConfig = getConfig().with(HippoPicker.CONFIG_KEY);
        initInternalLinkPicker(fieldContext, hippoPickerConfig);
        initImagePicker(fieldContext, hippoPickerConfig);
    }

    private void initInternalLinkPicker(final FieldTypeContext fieldContext, final ObjectNode hippoPickerConfig) {
        final ObjectNode internalLinkConfig = hippoPickerConfig.with(HippoPicker.InternalLink.CONFIG_KEY);
        readBooleanConfig(internalLinkConfig, LINKPICKER_BOOLEAN_PROPERTIES, LINKPICKER_REMOVED_PREFIX, fieldContext);
        readStringConfig(internalLinkConfig, LINKPICKER_STRING_PROPERTIES, LINKPICKER_REMOVED_PREFIX, fieldContext);
        readMultipleStringConfig(internalLinkConfig, LINKPICKER_MULTIPLE_STRING_PROPERTIES, LINKPICKER_REMOVED_PREFIX, fieldContext);
    }

    private void initImagePicker(final FieldTypeContext fieldContext, final ObjectNode hippoPickerConfig) {
        final ObjectNode imagePickerConfig = hippoPickerConfig.with(HippoPicker.Image.CONFIG_KEY);
        readBooleanConfig(imagePickerConfig, IMAGEPICKER_BOOLEAN_PROPERTIES, IMAGEPICKER_REMOVED_PREFIX, fieldContext);
        readStringConfig(imagePickerConfig, IMAGEPICKER_STRING_PROPERTIES, IMAGEPICKER_REMOVED_PREFIX, fieldContext);
        readMultipleStringConfig(imagePickerConfig, IMAGEPICKER_MULTIPLE_PROPERTIES, IMAGEPICKER_REMOVED_PREFIX, fieldContext);
    }

    private void readBooleanConfig(final ObjectNode config, final String[] propertyNames, final String removePrefix, final FieldTypeContext fieldContext) {
        for (String propertyName : propertyNames) {
            fieldContext.getBooleanConfig(propertyName).ifPresent((value) -> {
                final String key = StringUtils.removeStart(propertyName, removePrefix);
                config.put(key, value);
            });
        }
    }

    private void readStringConfig(final ObjectNode config, final String[] propertyNames, final String removePrefix, final FieldTypeContext fieldContext) {
        for (String propertyName : propertyNames) {
            fieldContext.getStringConfig(propertyName).ifPresent((value) -> {
                final String key = StringUtils.removeStart(propertyName, removePrefix);
                config.put(key, value);
            });
        }
    }

    private void readMultipleStringConfig(final ObjectNode config, final String[] propertyNames, final String removePrefix, final FieldTypeContext fieldContext) {
        for (String propertyName : propertyNames) {
            fieldContext.getMultipleStringConfig(propertyName).ifPresent((values -> {
                final String key = StringUtils.removeStart(propertyName, removePrefix);
                final ArrayNode array = config.putArray(key);
                for (String value : values) {
                    array.add(value);
                }
            }));
        }
    }

    @Override
    public Optional<List<FieldValue>> readFrom(final Node node) {
        final List<FieldValue> values = readValues(node);

        trimToMaxValues(values);

        return values.isEmpty() ? Optional.empty() : Optional.of(values);
    }

    protected List<FieldValue> readValues(final Node node) {
        try {
            final NodeIterator children = node.getNodes(getId());
            final List<FieldValue> values = new ArrayList<>((int)children.getSize());
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
    public boolean writeFieldValue(final Node node, final FieldPath fieldPath, final List<FieldValue> values) throws ErrorWithPayloadException, RepositoryException {
        if (!fieldPath.is(getId())) {
            return false;
        }
        if (values.isEmpty()) {
            throw INVALID_DATA.get();
        }
        writeValue(node, values.get(0));
        return true;
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
                nodeFactory);
        processorModel.getVisitors().add(RELATIVE_IMAGE_PATH_VISITOR);
        return processorModel.get();
    }

    private String write(final String html, final Node node) {
        final Model<Node> nodeModel = Model.of(node);
        final JcrNodeFactory nodeFactory = JcrNodeFactory.of(node);
        final Model<String> htmlModel = Model.of("");
        final HtmlProcessorModel processorModel = new RichTextProcessorModel(htmlModel, nodeModel, processorFactory,
                nodeFactory);
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

        @Override
        public void release() {}
    }
}
