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

package org.onehippo.cms.channelmanager.content.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.jcr.Node;

import org.onehippo.cms.channelmanager.content.model.documenttype.DocumentType;
import org.onehippo.cms.channelmanager.content.model.documenttype.FieldType;
import org.onehippo.cms.channelmanager.content.model.documenttype.MultilineStringFieldType;
import org.onehippo.cms.channelmanager.content.model.documenttype.StringFieldType;
import org.onehippo.cms7.services.contenttype.ContentTypeProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FieldTypeUtils provides utility methods for populating and dealing with field types.
 */
public class FieldTypeUtils {
    private static final Logger log = LoggerFactory.getLogger(FieldTypeUtils.class);
    private static final String PROPERTY_FIELD_PLUGIN = "org.hippoecm.frontend.editor.plugins.field.PropertyFieldPlugin";

    // Known non-validating validator values
    private static final Set<String> IGNORED_VALIDATORS;

    // Translate JCR level validator to FieldType.Validator
    private static final Map<String, FieldType.Validator> VALIDATOR_MAP;

    // Unsupported validators of which we know they have field-scope only
    private static final Set<String> FIELD_VALIDATOR_WHITELIST;

    // Set of to-be-ignored root-level document property namespaces
    // We ignore then because they represent CMS-internal state-keeping which we don't want to expose.
    private static final Set<String> NAMESPACE_BLACKLIST;

    // A map for associating supported JCR-level field types with relevant information
    private static final Map<String, TypeDescriptor> FIELD_TYPE_MAP;

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

        NAMESPACE_BLACKLIST = new HashSet<>();
        NAMESPACE_BLACKLIST.add("hippo");
        NAMESPACE_BLACKLIST.add("hippostd");
        NAMESPACE_BLACKLIST.add("hippostdpubwf");
        NAMESPACE_BLACKLIST.add("hippotranslation");
        NAMESPACE_BLACKLIST.add("jcr");

        FIELD_TYPE_MAP = new HashMap<>();
        FIELD_TYPE_MAP.put("String", new TypeDescriptor(StringFieldType.class, PROPERTY_FIELD_PLUGIN));
        FIELD_TYPE_MAP.put("Text", new TypeDescriptor(MultilineStringFieldType.class, PROPERTY_FIELD_PLUGIN));
    }

    private static class TypeDescriptor {
        public final Class<? extends FieldType> fieldType;
        public final String defaultPluginClass;

        public TypeDescriptor(final Class<? extends FieldType> fieldType, final String defaultPluginClass) {
            this.fieldType = fieldType;
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
     * Check if a property matches the namespace blacklist, and if not, consider it to be a "project property".
     */
    public static boolean isProjectProperty(final ContentTypeProperty property) {
        final String id = property.getName();
        final int offset = id.indexOf(":");

        if (offset < 0) {
            return true; // non-namespaced property name is assumed project-specific
        }

        final String namespace = id.substring(0, offset);
        return !NAMESPACE_BLACKLIST.contains(namespace);
    }

    /**
     * Check if a property represents a supported field type.
     */
    public static boolean isSupportedFieldType(final ContentTypeProperty property) {
        return FIELD_TYPE_MAP.containsKey(property.getItemType());
    }

    /**
     * Check if a (supported!) field makes use of its default CMS rendering plugin.
     *
     * Fields that use a different rendering plugin may have been set-up with a special meaning,
     * unknown to the content service. Therefore, such fields should not be included in the exposed document type.
     */
    public static boolean usesDefaultFieldPlugin(final ContentTypeProperty property, final Node documentTypeRootNode) {
        final TypeDescriptor descriptor = FIELD_TYPE_MAP.get(property.getItemType());

        if (descriptor == null) {
            return false;
        }

        Optional<String> pluginClass = NamespaceUtils.getPluginClassForField(documentTypeRootNode, property.getName());
        return descriptor.defaultPluginClass.equals(pluginClass.orElse(""));
    }

    /**
     * Translate the JCR type of a (supported!) field into its corresponding {@link FieldType}.Type value
     */
    public static Optional<? extends FieldType> createFieldType(final ContentTypeProperty property) {
        final String jcrType = property.getItemType();

        if (FIELD_TYPE_MAP.containsKey(jcrType)) {
            try {
                final Class<? extends FieldType> fieldTypeClass = FIELD_TYPE_MAP.get(jcrType).fieldType;
                return Optional.of(fieldTypeClass.newInstance());
            } catch (InstantiationException|IllegalAccessException e) {
                log.debug("Problem creating a field type for type '{}'", jcrType, e);
            }
        }
        return Optional.empty();
    }
}
