/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ChoiceFieldType represents the Content Blocks functionality, which allows users to choose from a list of
 * compound types to create instances in a document.
 */
public class ChoiceFieldType extends FieldType {
    private static final Logger log = LoggerFactory.getLogger(ChoiceFieldType.class);

    // The order of the entries in the choice map matters, so we use a *linked* hash map.
    private final Map<String, CompoundFieldType> choices = new LinkedHashMap<>();

    public ChoiceFieldType() {
        setType(Type.CHOICE);
    }

    public Map<String, CompoundFieldType> getChoices() {
        return choices;
    }

    @Override
    public void init(final FieldTypeContext fieldContext) {
        super.init(fieldContext);

        fieldContext.getEditorConfigNode()
                .ifPresent(node -> {
                    final ContentTypeContext parentContext = fieldContext.getParentContext();

                    ChoiceFieldUtils.populateProviderBasedChoices(node, parentContext, choices);
                    ChoiceFieldUtils.populateListBasedChoices(node, parentContext, choices);
                });
    }

    @Override
    public Optional<List<FieldValue>> readFrom(Node node) {
        List<FieldValue> values = readValues(node);

        trimToMaxValues(values);

        return values.isEmpty() ? Optional.empty() : Optional.of(values);
    }

    private List<FieldValue> readValues(final Node node) {
        final String nodeName = getId();
        final List<FieldValue> values = new ArrayList<>();
        try {
            for (Node child : new NodeIterable(node.getNodes(nodeName))) {
                final String choiceId = child.getPrimaryNodeType().getName();

                findChoice(choiceId).ifPresent(choice -> {
                    final FieldValue choiceValue = choice.readSingleFrom(child);
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
    public void writeTo(final Node node, final Optional<List<FieldValue>> optionalValues) throws ErrorWithPayloadException {
        final List<FieldValue> values = optionalValues.orElse(Collections.emptyList());
        checkCardinality(values);

        try {
            removeInvalidChoices(node); // This is symmetric to ignoring them in #readValues.

            final NodeIterator iterator = node.getNodes(getId());
            long numberOfNodes = iterator.getSize();

            // Additional cardinality check due to not yet being able to create new
            // (or remove a subset of the old) compound nodes, unless there are more nodes than allowed
            if (!values.isEmpty() && values.size() != numberOfNodes && !(numberOfNodes > getMaxValues())) {
                throw new BadRequestException(new ErrorInfo(ErrorInfo.Reason.CARDINALITY_CHANGE));
            }

            for (FieldValue value : values) {
                writeSingleTo(iterator.nextNode(), value);
            }

            // delete excess nodes to match field type
            while (iterator.hasNext()) {
                iterator.nextNode().remove();
            }
        } catch (RepositoryException e) {
            log.warn("Failed to write value for choice type '{}'", getId(), e);
            throw new InternalServerErrorException();
        }
    }

    private void writeSingleTo(final Node node, final FieldValue value) throws ErrorWithPayloadException, RepositoryException {
        // each value must specify a chosen ID
        final String chosenId = value.findChosenId().orElseThrow(INVALID_DATA);

        final String choiceId = node.getPrimaryNodeType().getName();
        if (!choiceId.equals(chosenId)) {
            // existing node is of different type than requested value (reordering not supported)
            throw INVALID_DATA.get();
        }

        // each chosenId must be a valid choice
        final CompoundFieldType compound = findChoice(chosenId).orElseThrow(INVALID_DATA);

        // each value must specify a choice value
        final FieldValue chosenValue = value.findChosenValue().orElseThrow(INVALID_DATA);

        compound.writeSingleTo(node, chosenValue);
    }


    private void removeInvalidChoices(final Node node) throws RepositoryException {
        for (Node child : new NodeIterable(node.getNodes(getId()))) {
            final String nodeType = child.getPrimaryNodeType().getName();
            if (!findChoice(nodeType).isPresent()) {
                child.remove();
            }
        }
    }

    @Override
    public boolean validate(final List<FieldValue> valueList) {
        return validateValues(valueList, this::validateSingle);
    }

    private boolean validateSingle(final FieldValue value) {
        // dispatch validation of the values to the corresponding compound fields
        // #readValues guarantees that the value has a valid chosenId, and a choiceValue
        final String chosenId = value.findChosenId().get();
        final CompoundFieldType compound = findChoice(chosenId).get();
        final FieldValue choiceValue = value.findChosenValue().get();

        return compound.validateSingle(choiceValue);
    }

    private Optional<CompoundFieldType> findChoice(final String chosenId) {
        return Optional.ofNullable(choices.get(chosenId));
    }
}
