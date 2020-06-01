/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ChoiceFieldType represents the Content Blocks functionality, which allows users to choose from a list of
 * compound types to create instances in a document.
 */
public class ChoiceFieldType extends AbstractFieldType implements NodeFieldType {
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
    public Optional<List<FieldValue>> readFrom(Node node) {
        List<FieldValue> values = readValues(node);

        trimToMaxValues(values);

        if (values.size() < getMinValues()) {
            log.error("No values available for node of type '{}' of document at {}. This document type cannot be " +
                    "used to create new documents in the Channel Manager.", getId(), JcrUtils.getNodePathQuietly(node));
        }

        return values.isEmpty() ? Optional.empty() : Optional.of(values);
    }

    private List<FieldValue> readValues(final Node node) {
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
    protected void writeValues(final Node node,
                               final Optional<List<FieldValue>> optionalValues,
                               final boolean validateValues) throws ErrorWithPayloadException {
        final List<FieldValue> values = mergeUnsupportedValues(node, optionalValues.orElse(Collections.emptyList()));
        if (validateValues) {
            checkCardinality(values);
        }

        try {
            final NodeIterator children = node.getNodes(getId());
            FieldTypeUtils.writeNodeValues(children, values, getMaxValues(), this);
        } catch (RepositoryException e) {
            log.warn("Failed to write value for choice type '{}'", getId(), e);
            throw new InternalServerErrorException();
        }
    }

    private List<FieldValue> mergeUnsupportedValues(final Node node, final List<FieldValue> supportedValues) throws ErrorWithPayloadException {
        final List<FieldValue> values = new LinkedList<>();

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
            log.warn("Failed to read nodes for choice type '{}'", getId(), e);
            throw new InternalServerErrorException();
        }

        return values;
    }

    @Override
    public boolean writeField(final Node node, final FieldPath fieldPath, final List<FieldValue> values) throws ErrorWithPayloadException {
        return FieldTypeUtils.writeFieldNodeValue(node, fieldPath, values, this);
    }

    @Override
    public boolean writeFieldValue(final Node node, final FieldPath fieldPath, final List<FieldValue> values) throws ErrorWithPayloadException, RepositoryException {
        final String chosenId = node.getPrimaryNodeType().getName();
        final NodeFieldType choice = findChoice(chosenId).orElseThrow(INVALID_DATA);
        return choice.writeFieldValue(node, fieldPath, values);
    }

    @Override
    public void writeValue(final Node node, final FieldValue value) throws ErrorWithPayloadException, RepositoryException {
        if (value == UNSUPPORTED_FIELD_VALUE) {
            return;
        }

        // each value must specify a chosen ID
        final String chosenId = value.findChosenId().orElseThrow(INVALID_DATA);

        final String choiceId = node.getPrimaryNodeType().getName();
        if (!choiceId.equals(chosenId)) {
            // existing node is of different type than requested value (reordering not supported)
            throw INVALID_DATA.get();
        }

        // each chosenId must be a valid choice
        final NodeFieldType choice = findChoice(chosenId).orElseThrow(INVALID_DATA);

        // each value must specify a choice value
        final FieldValue chosenValue = value.findChosenValue().orElseThrow(INVALID_DATA);

        choice.writeValue(node, chosenValue);
    }

    @Override
    public boolean validate(final List<FieldValue> valueList) {
        return validateValues(valueList, this::validateValue);
    }

    @Override
    public boolean validateValue(final FieldValue value) {
        // dispatch validation of the values to the corresponding compound fields
        // #readValues guarantees that the value has a valid chosenId, and a choiceValue
        final String chosenId = value.findChosenId().get();
        final NodeFieldType choice = findChoice(chosenId).get();
        final FieldValue choiceValue = value.findChosenValue().get();

        return choice.validateValue(choiceValue);
    }

    private Optional<NodeFieldType> findChoice(final String chosenId) {
        return Optional.ofNullable(choices.get(chosenId));
    }
}
