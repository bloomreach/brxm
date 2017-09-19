/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Map;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.util.LocalizationUtils;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.cms7.services.contenttype.ContentTypeItem;
import org.onehippo.repository.l10n.ResourceBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ChoiceFieldUtils help with the non-trivial construction of document types that contain fields of type CHOICE.
 */
public class ChoiceFieldUtils {
    private static final Logger log = LoggerFactory.getLogger(ChoiceFieldUtils.class);

    private static final String PROPERTY_PROVIDER_ID = "cpItemsPath";
    private static final String PROPERTY_COMPOUND_LIST = "compoundList";

    private ChoiceFieldUtils() { }

    /**
     * Check if a field represents a CHOICE field.
     *
     * @param context field type context, wrapping the field's editor config node.
     * @return        true if field represents CHOICE, false otherwise.
     */
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

    /**
     * Provider-based choices use a custom compound content type, known to the content type service, to specify
     * the available choices. We retrieve the name of the provider compound, derive its content type and loop
     * over its children/fields to populate our list of choices.
     *
     * @param editorFieldNode JCR node representing the field's editor configuration
     * @param parentContext   context of the choice field's parent content type
     * @param choices         map of fields to populate
     */
    public static void populateProviderBasedChoices(final Node editorFieldNode,
                                                    final ContentTypeContext parentContext,
                                                    final Map<String, NodeFieldType> choices) {
        getProviderId(editorFieldNode)
                .ifPresent(providerId -> ContentTypeContext.getContentType(providerId)
                        .ifPresent(provider -> populateChoicesForProvider(provider, parentContext, choices)));
    }

    private static Optional<String> getProviderId(final Node editorFieldNode) {
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

    private static void populateChoicesForProvider(final ContentType provider,
                                                   final ContentTypeContext parentContext,
                                                   final Map<String, NodeFieldType> choices) {
        for (ContentTypeItem item : provider.getChildren().values()) {
            ContentTypeContext.getContentType(item.getItemType()).ifPresent(contentType -> {

                if (contentType.isCompoundType() || contentType.isContentType(HippoStdNodeType.NT_HTML)
                        || contentType.isContentType(FieldTypeUtils.FIELD_TYPE_IMAGELINK)) {
                    // Suggestion: the provider compound may have an editor configuration, helping to initialize
                    //             the choice compound. We could try to find a node and add it to the fieldContext.
                    final FieldTypeContext fieldContext = new FieldTypeContext(item, parentContext);
                    final String choiceId = item.getItemType();

                    final NodeFieldType choice = createChoiceForProvider(contentType, fieldContext, choiceId);

                    if (choice != null) {
                        patchDisplayNameForChoice(choice, fieldContext);
                        choices.put(choiceId, choice);
                    }
                }
            });
        }
    }

    private static NodeFieldType createChoiceForProvider(final ContentType contentType,
                                                         final FieldTypeContext fieldContext,
                                                         final String choiceId) {
        if (contentType.isCompoundType()) {
            final CompoundFieldType compound = new CompoundFieldType();
            compound.initProviderBasedChoice(fieldContext, choiceId);
            return compound;
        } else if (contentType.isContentType(HippoStdNodeType.NT_HTML)) {
            final RichTextFieldType richText = new RichTextFieldType();
            richText.init(fieldContext);
            return richText;
        } else if (contentType.isContentType(FieldTypeUtils.FIELD_TYPE_IMAGELINK)) {
            final ImageLinkFieldType imageLink = new ImageLinkFieldType();
            imageLink.init(fieldContext);
            return imageLink;
        }
        return null;
    }

    private static void patchDisplayNameForChoice(final NodeFieldType choice, final FieldTypeContext fieldContext) {
        fieldContext.createContextForCompound()
                .ifPresent(choiceContext -> patchDisplayNameForChoice(choice, choiceContext));
    }

    /**
     * List-based choices specify the names of the available types on a property on the choice field's
     * editor configuration node. We retrieve and normalize these names. Since this choice relationship bypasses
     * the content type service's model, no FieldTypeContext is available for any choice. Instead, we create a
     * ContentTypeContext for each choice, and use that to populate our list of choices.
     *
     * @param editorFieldNode JCR node representing the field's editor configuration
     * @param parentContext   context of the choice field's parent content type
     * @param choices         list of fields to populate
     */
    public static void populateListBasedChoices(final Node editorFieldNode,
                                                final ContentTypeContext parentContext,
                                                final Map<String, NodeFieldType> choices) {
        final String[] choiceNames = getListBasedChoiceNames(editorFieldNode);

        for (String choiceName : choiceNames) {
            final String choiceId = normalizeChoiceName(choiceName, parentContext);

            ContentTypeContext.createFromParent(choiceId, parentContext).ifPresent(choiceContext -> {
                final ContentType contentType = choiceContext.getContentType();
                if (contentType.isCompoundType() || contentType.isContentType(HippoStdNodeType.NT_HTML)
                        || contentType.isContentType(FieldTypeUtils.FIELD_TYPE_IMAGELINK)) {
                    final String id = choiceContext.getContentType().getName();

                    final NodeFieldType choice = createListBasedChoice(contentType, choiceContext, id);

                    if (choice != null) {
                        patchDisplayNameForChoice(choice, choiceContext);
                        choices.put(id, choice);
                    }
                }
            });
        }
    }

    private static String[] getListBasedChoiceNames(final Node editorFieldNode) {
        try {
            if (editorFieldNode.hasProperty(PROPERTY_COMPOUND_LIST)) {
                return editorFieldNode.getProperty(PROPERTY_COMPOUND_LIST).getString().trim().split("\\s*,\\s*");
            }
        } catch (RepositoryException e) {
            log.warn("Failed to determine list-based choices for field {}",
                    JcrUtils.getNodePathQuietly(editorFieldNode), e);
        }
        return new String[0];
    }

    private static String normalizeChoiceName(final String choiceName, final ContentTypeContext context) {
        return choiceName.contains(":") ? choiceName : context.getContentType().getPrefix() + ":" + choiceName;
    }

    private static NodeFieldType createListBasedChoice(final ContentType contentType,
                                                       final ContentTypeContext choiceContext,
                                                       final String choiceId) {
        if (contentType.isCompoundType()) {
            final CompoundFieldType compound = new CompoundFieldType();
            compound.initListBasedChoice(choiceContext, choiceId);
            return compound;
        } else if (contentType.isContentType(HippoStdNodeType.NT_HTML)) {
            final RichTextFieldType richText = new RichTextFieldType();
            // FIXME: this init call will not load configuration
            richText.initListBasedChoice(choiceId);
            return richText;
        } else if(contentType.isContentType(FieldTypeUtils.FIELD_TYPE_IMAGELINK)) {
            // FIXME: this init call will not load configuration
            final ImageLinkFieldType imageLink = new ImageLinkFieldType();
            imageLink.initListBasedChoice(choiceId);
            return imageLink;

        }
        return null;
    }

    private static void patchDisplayNameForChoice(final NodeFieldType choice, final ContentTypeContext context) {
        if (choice.getDisplayName() == null) {
            final Optional<ResourceBundle> resourceBundle = context.getResourceBundle();
            LocalizationUtils.determineDocumentDisplayName(choice.getId(), resourceBundle)
                    .ifPresent(choice::setDisplayName);
        }
    }
}
