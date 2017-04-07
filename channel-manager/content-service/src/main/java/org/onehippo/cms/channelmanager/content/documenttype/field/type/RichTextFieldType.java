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
import javax.jcr.Session;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.ckeditor.CKEditorConfig;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.cms7.services.processor.html.HtmlProcessorFactory;
import org.onehippo.cms7.services.processor.html.model.HtmlProcessorModel;
import org.onehippo.cms7.services.processor.html.model.Model;
import org.onehippo.cms7.services.processor.html.model.SimpleModel;
import org.onehippo.cms7.services.processor.richtext.jcr.JcrNodeFactory;
import org.onehippo.cms7.services.processor.richtext.model.RichTextProcessorModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RichTextFieldType extends FormattedTextFieldType implements CompoundWriter {

    private static final Logger log = LoggerFactory.getLogger(RichTextFieldType.class);

    public RichTextFieldType() {
        super(CKEditorConfig.DEFAULT_RICH_TEXT_CONFIG);
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
            final RichTextReader processorReader = new RichTextReader();
            for (final Node child : new NodeIterable(children)) {
                values.add(new FieldValue(processorReader.read(child)));
            }
            return values;
        } catch (final RepositoryException e) {
            log.warn("Failed to read rich text field '{}'", getId(), e);
        }
        return Collections.emptyList();
    }

    @Override
    public void writeTo(final Node node, final Optional<List<FieldValue>> optionalValues) throws ErrorWithPayloadException {
        final String valueName = getId();
        final List<FieldValue> values = optionalValues.orElse(Collections.emptyList());
        checkCardinality(values);

        try {
            final NodeIterator children = node.getNodes(valueName);
            FieldTypeUtils.writeCompoundValues(children, values, getMaxValues(), this);
        } catch (final RepositoryException e) {
            log.warn("Failed to write rich text field '{}'", valueName, e);
            throw new InternalServerErrorException();
        }
    }

    @Override
    public void writeValue(final Node node, final FieldValue fieldValue) throws ErrorWithPayloadException, RepositoryException {
        final RichTextWriter processorWriter = new RichTextWriter();
        processorWriter.write(node, fieldValue.getValue());
    }

    private static class RichTextReader {

        String read(final Node node) throws RepositoryException {
            final String html = JcrUtils.getStringProperty(node, HippoStdNodeType.HIPPOSTD_CONTENT, null);
            final Model<String> htmlModel = new SimpleModel<>(html);
            final Model<Node> nodeModel = new SimpleModel<>(node);
            final JcrNodeFactory nodeFactory = new JcrNodeFactory() {
                @Override
                protected Session getSession() throws RepositoryException {
                    return node.getSession();
                }
            };
            final HtmlProcessorFactory processorFactory = HtmlProcessorFactory.of("richtext");
            final HtmlProcessorModel processorModel = new RichTextProcessorModel(htmlModel, nodeModel, processorFactory,
                                                                                 nodeFactory);
            return processorModel.get();
        }
    }

    private static class RichTextWriter {

        void write(final Node node, final String html) throws RepositoryException {
            final Model<Node> nodeModel = new SimpleModel<>(node);
            final JcrNodeFactory nodeFactory = new JcrNodeFactory() {
                @Override
                protected Session getSession() throws RepositoryException {
                    return node.getSession();
                }
            };
            final Model<String> htmlModel = new SimpleModel<>("");
            final HtmlProcessorFactory processorFactory = HtmlProcessorFactory.of("richtext");
            final HtmlProcessorModel processorModel = new RichTextProcessorModel(htmlModel, nodeModel, processorFactory,
                                                                                 nodeFactory);
            processorModel.set(html);
            node.setProperty(HippoStdNodeType.HIPPOSTD_CONTENT, htmlModel.get());
        }
    }
}
