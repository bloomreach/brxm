/*
 * Copyright 2016-2020 Hippo B.V. (http://www.onehippo.com)
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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.validation.CompoundContext;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ChoiceFieldType represents the Content Blocks functionality, which allows users to choose from a list of
 * compound types to create instances in a document.
 */
public class ChoiceFieldType extends NodeFieldType {
    private static final Logger log = LoggerFactory.getLogger(ChoiceFieldType.class);

    static final FieldValue UNSUPPORTED_FIELD_VALUE = new FieldValue();

    // The order of the entries in the choice map matters, so we use a *linked* hash map.
    private final Map<String, NodeFieldType> choices = new LinkedHashMap<>();

    public ChoiceFieldType() {
        setType(Type.CHOICE);
    }

    public Map<String, NodeFieldType> getChoices() {
        return choices;
    }

    @Override
    public FieldsInformation init(final FieldTypeContext fieldContext) {
        final FieldsInformation fieldsInfo = super.init(fieldContext);

        fieldContext.getEditorConfigNode()
                .ifPresent(node -> {
                    final ContentTypeContext parentContext = fieldContext.getParentContext();
                    ChoiceFieldUtils.populateProviderBasedChoices(node, parentContext, choices, fieldsInfo);
                    ChoiceFieldUtils.populateListBasedChoices(node, parentContext, choices, fieldsInfo);
                });

        // It's not possible yet to add choices, so a required choice field cannot be created.
        // So all required fields can only be created if this choice field is not required.
        fieldsInfo.setCanCreateAllRequiredFields(!isRequired());

        return fieldsInfo;
    }

    @Override
    public List<FieldValue> readValues(final Node node) {
        final String nodeName = getId();
        final List<FieldValue> values = new ArrayList<>();
        try {
            for (Node child : new NodeIterable(node.getNodes(nodeName))) {
                final String choiceId = child.getPrimaryNodeType().getName();

                findChoice(choiceId).ifPresent(choice -> {
                    final FieldValue choiceValue = choice.readValue(child);
                    values.add(new FieldValue(choiceId, choiceValue));
                });

                // nodes that have no corresponding choices are ignored
            }
        } catch (RepositoryException e) {
            log.warn("Failed to read nodes for choice type '{}'", getId(), e);
        }
        return values;
    }

    @Override
    public FieldValue readValue(final Node node) {
        throw new UnsupportedOperationException("Nested choices are not supported");
    }

    @Override
    public void writeValues(final Node node,
                               final Optional<List<FieldValue>> optionalValues) {
        final List<FieldValue> values = mergeUnsupportedValues(node, optionalValues.orElse(Collections.emptyList()));
        super.writeValues(node, Optional.of(values));
    }

    private List<FieldValue> mergeUnsupportedValues(final Node node, final List<FieldValue> supportedValues) {
        final List<FieldValue> values = new LinkedList<>();

        if (node == null){
            return values;
        }
        try {
            final NodeIterator nodes = node.getNodes(getId());
            int supportedIndex = 0;
            while (values.size() < getMaxValues() && nodes.hasNext()) {
                final Node child = nodes.nextNode();
                final String nodeType = child.getPrimaryNodeType().getName();
                if (!findChoice(nodeType).isPresent()) {
                    values.add(UNSUPPORTED_FIELD_VALUE);
                } else if (supportedIndex < supportedValues.size()) {
                    values.add(supportedValues.get(supportedIndex));
                    supportedIndex += 1;
                }
            }
        } catch (RepositoryException e) {
            log.error("Failed to read nodes for choice type '{}'", getId(), e);
            throw new InternalServerErrorException();
        }

        return values;
    }

    @Override
    public boolean writeField(final FieldPath fieldPath,
                              final List<FieldValue> values,
                              final CompoundContext context) {
        return FieldTypeUtils.writeFieldNodeValue(fieldPath, values, this, context);
    }

    @Override
    public boolean writeFieldValue(final FieldPath fieldPath,
                                   final List<FieldValue> values,
                                   final CompoundContext context) throws RepositoryException {
        final String chosenId = context.getNode().getPrimaryNodeType().getName();
        final NodeFieldType choice = findChoice(chosenId).orElseThrow(FieldTypeUtils.INVALID_DATA);
        return choice.writeFieldValue(fieldPath, values, context);
    }

    @Override
    public void writeValue(final Node node, final FieldValue value) throws RepositoryException {
        if (value == UNSUPPORTED_FIELD_VALUE) {
            return;
        }

        // each value must specify a chosen ID
        final String chosenId = value.findChosenId().orElseThrow(FieldTypeUtils.INVALID_DATA);

        final String choiceId = node.getPrimaryNodeType().getName();
        if (!choiceId.equals(chosenId)) {
            // existing node is of different type than requested value (reordering not supported)
            throw FieldTypeUtils.INVALID_DATA.get();
        }

        // each chosenId must be a valid choice
        final NodeFieldType choice = findChoice(chosenId).orElseThrow(FieldTypeUtils.INVALID_DATA);

        // each value must specify a choice value
        final FieldValue chosenValue = value.findChosenValue().orElseThrow(FieldTypeUtils.INVALID_DATA);

        choice.writeValue(node, chosenValue);
    }

    @Override
    public int validate(final List<FieldValue> valueList, final CompoundContext context) {
        final Node node = context.getNode();
        final List<FieldValue> values = mergeUnsupportedValues(node, valueList);
        return super.validate(values, context);
    }

    @Override
    public int validateValue(final FieldValue value, final CompoundContext context) {
        if (value == UNSUPPORTED_FIELD_VALUE) {
            return 0;
        }

        // dispatch validation of the values to the corresponding compound fields
        // #writeValue guarantees that the value has a valid chosenId, and a choiceValue
        final String chosenId = value.getChosenId();
        final NodeFieldType choice = choices.get(chosenId);
        final FieldValue choiceValue = value.getChosenValue();

        return choice.validateValue(choiceValue, context);
    }

    private Optional<NodeFieldType> findChoice(final String chosenId) {
        return Optional.ofNullable(choices.get(chosenId));
    }
}
