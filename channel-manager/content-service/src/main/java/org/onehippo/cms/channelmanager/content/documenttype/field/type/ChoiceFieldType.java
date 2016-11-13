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
import java.util.List;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.util.LocalizationUtils;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.cms7.services.contenttype.ContentTypeItem;
import org.onehippo.repository.l10n.ResourceBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ChoiceFieldType represents the Content Blocks functionality, which allows users to choose from a list of
 * compound types to create instances in a document.
 */
public class ChoiceFieldType extends FieldType {
    private static final Logger log = LoggerFactory.getLogger(ChoiceFieldType.class);
    private static final String PROPERTY_PROVIDER_ID = "cpItemsPath";
    private static final String PROPERTY_COMPOUND_LIST = "compoundList";

    public static boolean isChoiceField(final FieldTypeContext context) {
        return context.getEditorConfigNode().map(node -> {
            try {
                // Provider-based choice?
                if (node.hasProperty(PROPERTY_PROVIDER_ID)) {
                    return node;
                }

                // List-based choice?
                if (node.hasProperty(PROPERTY_COMPOUND_LIST)) {
                    return node;
                }
            } catch (RepositoryException e) {
                log.warn("Failed to determine if field is of type CHOICE", e);
            }
            return null;
        }).isPresent();
    }

    private final List<CompoundFieldType> choices = new ArrayList<>();

    public ChoiceFieldType() {
        setType(Type.CHOICE);
    }

    public List<CompoundFieldType> getChoices() {
        return choices;
    }

    @Override
    public boolean isValid() {
        return !choices.isEmpty();
    }

    @Override
    public void init(final FieldTypeContext fieldContext) {
        super.init(fieldContext);

        fieldContext.getEditorConfigNode()
                .ifPresent(node -> {
                    final ContentTypeContext parentContext = fieldContext.getParentContext();

                    populateProviderBasedChoices(node, parentContext);
                    populateListBasedChoices(node, parentContext);
                });
    }

    /**
     * Provider-based choices use a custom compound content type, known to the content type service to specify
     * the available choices. We retrieve the name of the provider compound, derive its content type and loop
     * over its children/fields to populate our list of choices.
     */
    private void populateProviderBasedChoices(final Node editorFieldNode, final ContentTypeContext parentContext) {
        getProviderId(editorFieldNode)
                .ifPresent(providerId -> ContentTypeContext.getContentType(providerId)
                        .ifPresent(provider -> populateChoicesForProvider(provider, parentContext)));
    }

    private Optional<String> getProviderId(final Node editorFieldNode) {
        try {
            if (editorFieldNode.hasProperty(PROPERTY_PROVIDER_ID)) {
                return Optional.of(editorFieldNode.getProperty(PROPERTY_PROVIDER_ID).getString());
            }
        } catch (RepositoryException e) {
            log.warn("Failed to determine provider-based choices for field {}",
                    JcrUtils.getNodePathQuietly(editorFieldNode), e);
        }
        return Optional.empty();
    }

    private void populateChoicesForProvider(final ContentType provider, final ContentTypeContext parentContext) {
        for (ContentTypeItem item : provider.getChildren().values()) {
            ContentTypeContext.getContentType(item.getItemType()).ifPresent(contentType -> {
                if (contentType.isCompoundType()) {
                    createChoiceFromFieldType(new FieldTypeContext(item, parentContext));
                }
            });
        }
    }

    /**
     * Typically, our provider compound has no editor configuration, and therefore no caption, and likely also no
     * translated field names. In such a case, we "patch" the choice's display name by falling back to the compound's
     * localized name.
     */
    private void createChoiceFromFieldType(final FieldTypeContext context) {
        final CompoundFieldType choice = new CompoundFieldType();
        choice.init(context);
        if (choice.isValid()) {
            choice.setId(context.getContentTypeItem().getItemType());
            patchDisplayNameForChoice(choice, context);
            choices.add(choice);
        }
    }

    private void patchDisplayNameForChoice(final CompoundFieldType choice, final FieldTypeContext context) {
        context.createContextForCompound().ifPresent(compoundContext -> patchDisplayNameForChoice(choice, compoundContext));
    }

    /**
     * List-based choices use a property on the choice field's editor comfiguration node, specifying the names of
     * the available compound types. We retrieve and normalize these names. Since this choice relationship bypasses
     * the content type service's model, no FieldTypeContext is available for any choice. Instead, we create a
     * ContentTypeContext for each choice, and use that to populate our list of choices.
     */
    private void populateListBasedChoices(final Node editorFieldNode, final ContentTypeContext parentContext) {
        final String[] choiceNames = getListBasedChoiceNames(editorFieldNode);

        for (String choiceName : choiceNames) {
            final String choiceId = normalizeChoiceName(choiceName, parentContext);

            ContentTypeContext.createFromParent(choiceId, parentContext).ifPresent(context -> {
                if (context.getContentType().isCompoundType()) {
                    createChoiceFromContentType(context);
                }
            });
        }
    }

    private String[] getListBasedChoiceNames(final Node editorFieldNode) {
        try {
            if (editorFieldNode.hasProperty(PROPERTY_COMPOUND_LIST)) {
                return editorFieldNode.getProperty(PROPERTY_COMPOUND_LIST).getString().split("\\s*,\\s*");
            }
        } catch (RepositoryException e) {
            log.warn("Failed to determine list-based choices for field {}",
                    JcrUtils.getNodePathQuietly(editorFieldNode), e);
        }
        return new String[0];
    }

    private String normalizeChoiceName(final String choiceName, final ContentTypeContext context) {
        return choiceName.contains(":") ? choiceName : context.getContentType().getPrefix() + ":" + choiceName;
    }

    /**
     * Since no FieldTypeContext is available for a list-based choice,
     * we have to initialize our compound field manually.
     */
    private void createChoiceFromContentType(final ContentTypeContext context) {
        final CompoundFieldType choice = new CompoundFieldType();
        FieldTypeUtils.populateFields(choice.getFields(), context);
        if (choice.isValid()) {
            choice.setId(context.getContentType().getName());
            patchDisplayNameForChoice(choice, context);
            choices.add(choice);
        }
    }

    private void patchDisplayNameForChoice(final CompoundFieldType choice, final ContentTypeContext context) {
        if (choice.getDisplayName() == null) {
            final Optional<ResourceBundle> resourceBundle = context.getResourceBundle();
            LocalizationUtils.determineDocumentDisplayName(choice.getId(), resourceBundle)
                    .ifPresent(choice::setDisplayName);
        }
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
            log.warn("Failed to read nodes for compound type '{}'", getId(), e);
        }
        return values;
    }

    @Override
    public void writeTo(final Node node, final Optional<Object> optionalValue) throws ErrorWithPayloadException {
        final List<FieldValue> values = checkValue(optionalValue);

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
            log.warn("Failed to write compound value to node {}", getId(), e);
            throw new InternalServerErrorException();
        }
    }

    private void writeSingleTo(final Node node, final FieldValue value) throws ErrorWithPayloadException, RepositoryException {
        // each value must specify a choice ID
        final String choiceId = value.findChoiceId().orElseThrow(INVALID_DATA);

        final String nodeType = node.getPrimaryNodeType().getName();
        if (!nodeType.equals(choiceId)) {
            // existing node is of different type than requested value (reordering not supported)
            throw INVALID_DATA.get();
        }

        // each choiceId must be a valid choice
        final CompoundFieldType compound = findChoice(choiceId).orElseThrow(INVALID_DATA);

        // each value must specify a choice value
        final FieldValue choiceValue = value.findChoiceValue().orElseThrow(INVALID_DATA);

        compound.writeSingleTo(node, choiceValue);
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
        // #readValues guarantees that the value has a valid choiceId, and a choiceValue
        final String choiceId = value.findChoiceId().get();
        final CompoundFieldType compound = findChoice(choiceId).get();
        final FieldValue choiceValue = value.findChoiceValue().get();

        return compound.validateSingle(choiceValue);
    }

    private Optional<CompoundFieldType> findChoice(final String choiceId) {
        return choices.stream()
                .filter(choice -> choice.getId().equals(choiceId))
                .findAny();
    }
}
