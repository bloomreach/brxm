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

package org.onehippo.cms.channelmanager.content.documenttype.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeFactory;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.CompoundFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.MultilineStringFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.StringFieldType;
import org.onehippo.cms7.services.contenttype.ContentTypeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FieldTypeUtils provides utility methods for populating and dealing with field types.
 */
public class FieldTypeUtils {
    private static final Logger log = LoggerFactory.getLogger(FieldTypeUtils.class);
    private static final String FIELD_TYPE_COMPOUND = "Compound";
    private static final String PROPERTY_FIELD_PLUGIN = "org.hippoecm.frontend.editor.plugins.field.PropertyFieldPlugin";
    private static final String NODE_FIELD_PLUGIN = "org.hippoecm.frontend.editor.plugins.field.NodeFieldPlugin";

    // Known non-validating validator values
    private static final Set<String> IGNORED_VALIDATORS;

    // Translate JCR level validator to FieldType.Validator
    private static final Map<String, FieldType.Validator> VALIDATOR_MAP;

    // Unsupported validators of which we know they have field-scope only
    private static final Set<String> FIELD_VALIDATOR_WHITELIST;

    // A map for associating supported JCR-level field types with relevant information
    public static final Map<String, TypeDescriptor> FIELD_TYPE_MAP;

    static {
        IGNORED_VALIDATORS = new HashSet<>();
        IGNORED_VALIDATORS.add(FieldValidators.OPTIONAL); // optional "validator" indicates that the field may be absent (cardinality).

        VALIDATOR_MAP = new HashMap<>();
        VALIDATOR_MAP.put(FieldValidators.REQUIRED, FieldType.Validator.REQUIRED);
        VALIDATOR_MAP.put(FieldValidators.NON_EMPTY, FieldType.Validator.REQUIRED);
        // Apparently, making a String field required puts above two(!) values onto the validator property.

        FIELD_VALIDATOR_WHITELIST = new HashSet<>();
        FIELD_VALIDATOR_WHITELIST.add(FieldValidators.EMAIL);
        FIELD_VALIDATOR_WHITELIST.add(FieldValidators.ESCAPED);
        FIELD_VALIDATOR_WHITELIST.add(FieldValidators.HTML);
        FIELD_VALIDATOR_WHITELIST.add(FieldValidators.IMAGE_REFERENCES);
        FIELD_VALIDATOR_WHITELIST.add(FieldValidators.REFERENCES);
        FIELD_VALIDATOR_WHITELIST.add(FieldValidators.REQUIRED);
        FIELD_VALIDATOR_WHITELIST.add(FieldValidators.RESOURCE_REQUIRED);

        FIELD_TYPE_MAP = new HashMap<>();
        FIELD_TYPE_MAP.put("String", new TypeDescriptor(StringFieldType.class, PROPERTY_FIELD_PLUGIN));
        FIELD_TYPE_MAP.put("Text", new TypeDescriptor(MultilineStringFieldType.class, PROPERTY_FIELD_PLUGIN));
        FIELD_TYPE_MAP.put(FIELD_TYPE_COMPOUND, new TypeDescriptor(CompoundFieldType.class, NODE_FIELD_PLUGIN));
    }

    private static class TypeDescriptor {
        public final Class<? extends FieldType> fieldTypeClass;
        public final String defaultPluginClass;

        public TypeDescriptor(final Class<? extends FieldType> fieldTypeClass, final String defaultPluginClass) {
            this.fieldTypeClass = fieldTypeClass;
            this.defaultPluginClass = defaultPluginClass;
        }
    }

    /**
     * Translate the set of validators specified at JCR level into a set of validators at the {@link FieldType} level.
     * When the list of validators contains an unknown one, the document type is marked as 'readonly due to
     * unknown validator'.
     *
     * @param fieldType  Specification of a field type
     * @param docType The document type the field is a part of
     * @param validators List of 0 or more validators specified at JCR level
     */
    public static void determineValidators(final FieldType fieldType, final DocumentType docType, final List<String> validators) {
        for (String validator : validators) {
            if (IGNORED_VALIDATORS.contains(validator)) {
                // Do nothing
            } else if (VALIDATOR_MAP.containsKey(validator)) {
                fieldType.addValidator(VALIDATOR_MAP.get(validator));
            } else if (FIELD_VALIDATOR_WHITELIST.contains(validator)) {
                fieldType.addValidator(FieldType.Validator.UNSUPPORTED);
            } else {
                docType.setReadOnlyDueToUnknownValidator(true);
            }
        }
    }

    /**
     * Check if a item represents a supported field type.
     */
    public static boolean isSupportedFieldType(final FieldTypeContext context) {
        return determineDescriptor(context.getContentTypeItem()).isPresent();
    }

    /**
     * Check if a (supported!) field makes use of its default CMS rendering plugin.
     *
     * Fields that use a different rendering plugin may have been set-up with a special meaning,
     * unknown to the content service. Therefore, such fields should not be included in the exposed document type.
     */
    public static boolean usesDefaultFieldPlugin(final FieldTypeContext context) {
        final Optional<TypeDescriptor> descriptor = determineDescriptor(context.getContentTypeItem());
        final Optional<String> pluginClass = NamespaceUtils.getPluginClassForField(context.getEditorConfigNode());

        return descriptor.isPresent() && descriptor.get().defaultPluginClass.equals(pluginClass.orElse(""));
    }

    /**
     * Create a FieldType of the appropriate sub-type and initialize it.
     *
     * @param context            Information relevant for the current field
     * @param contentTypeContext Information relevant for the current content type (document or compound)
     * @param docType            Reference to the document type being assembled
     * @return                   Initialized FieldType instance or nothing, wrapped in an Optional
     */
    public static Optional<FieldType> createAndInitFieldType(final FieldTypeContext context,
                                                                 final ContentTypeContext contentTypeContext,
                                                                 final DocumentType docType) {

        return determineDescriptor(context.getContentTypeItem())
                .map(descriptor -> descriptor.fieldTypeClass)
                .flatMap(clazz -> FieldTypeFactory.createFieldType((Class<? extends FieldType>)clazz))
                .flatMap(fieldType -> fieldType.init(context, contentTypeContext, docType));
    }

    private static Optional<TypeDescriptor> determineDescriptor(final ContentTypeItem item) {
        final String type = item.isProperty() ? item.getItemType() : FIELD_TYPE_COMPOUND;
        return Optional.ofNullable(FIELD_TYPE_MAP.get(type));
    }
}
