/*
 * Copyright 2016-2019 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms.channelmanager.content.documenttype.field;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.addon.frontend.gallerypicker.GalleryPickerNodeType;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.sort.FieldSorter;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.BooleanFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.BooleanRadioGroupFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.ChoiceFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.ChoiceFieldUtils;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.CompoundFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.DateAndTimeFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.DateOnlyFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.DoubleFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.DynamicDropdownFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldsInformation;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.FormattedTextFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.ImageLinkFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.LongFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.MultilineStringFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.NodeFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.NodeLinkFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.OpenUiStringFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.RadioGroupFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.RichTextFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.StaticDropdownFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.StringFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.validation.CompoundContext;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.documenttype.util.JcrStringReader;
import org.onehippo.cms.channelmanager.content.documenttype.util.NamespaceUtils;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo.Reason;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.cms.services.validation.api.internal.ValidationService;
import org.onehippo.cms.services.validation.api.internal.ValidatorInstance;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FieldTypeUtils provides utility methods for populating and dealing with field types.
 */
public class FieldTypeUtils {

    private static final String FIELD_TYPE_COMPOUND = "Compound";
    private static final String FIELD_TYPE_CHOICE = "Choice";
    private static final String PROPERTY_FIELD_PLUGIN = "org.hippoecm.frontend.editor.plugins.field.PropertyFieldPlugin";
    private static final String NODE_FIELD_PLUGIN = "org.hippoecm.frontend.editor.plugins.field.NodeFieldPlugin";
    private static final String CONTENT_BLOCKS_PLUGIN = "org.onehippo.forge.contentblocks.ContentBlocksFieldPlugin";

    // Known non-validating validator values
    private static final Set<String> IGNORED_VALIDATORS;

    // A map for associating supported JCR-level field types with relevant information
    private static final Map<String, TypeDescriptor> FIELD_TYPE_MAP;

    // Wicket plugin classes that are used for layout structure
    private static final Set<String> STRUCTURE_PLUGIN_CLASSES;

    static {
        IGNORED_VALIDATORS = new HashSet<>();
        IGNORED_VALIDATORS.add(FieldValidators.CONTENT_BLOCKS); // takes care of recursion for content blocks. We implement this ourselves.

        FIELD_TYPE_MAP = new HashMap<>();
        FIELD_TYPE_MAP.put("String", new TypeDescriptor(StringFieldType.class, PROPERTY_FIELD_PLUGIN));
        FIELD_TYPE_MAP.put("Text", new TypeDescriptor(MultilineStringFieldType.class, PROPERTY_FIELD_PLUGIN));
        FIELD_TYPE_MAP.put("Html", new TypeDescriptor(FormattedTextFieldType.class, PROPERTY_FIELD_PLUGIN));
        FIELD_TYPE_MAP.put("Long", new TypeDescriptor(LongFieldType.class, PROPERTY_FIELD_PLUGIN));
        FIELD_TYPE_MAP.put("Double", new TypeDescriptor(DoubleFieldType.class, PROPERTY_FIELD_PLUGIN));
        FIELD_TYPE_MAP.put("DynamicDropdown", new TypeDescriptor(DynamicDropdownFieldType.class, PROPERTY_FIELD_PLUGIN));
        FIELD_TYPE_MAP.put("StaticDropdown", new TypeDescriptor(StaticDropdownFieldType.class, PROPERTY_FIELD_PLUGIN));
        FIELD_TYPE_MAP.put("Boolean", new TypeDescriptor(BooleanFieldType.class, PROPERTY_FIELD_PLUGIN));
        FIELD_TYPE_MAP.put("Date", new TypeDescriptor(DateAndTimeFieldType.class, PROPERTY_FIELD_PLUGIN));
        FIELD_TYPE_MAP.put("CalendarDate", new TypeDescriptor(DateOnlyFieldType.class, PROPERTY_FIELD_PLUGIN));
        FIELD_TYPE_MAP.put("selection:RadioGroup",
                new TypeDescriptor(RadioGroupFieldType.class, PROPERTY_FIELD_PLUGIN));
        FIELD_TYPE_MAP.put("selection:BooleanRadioGroup", new TypeDescriptor(BooleanRadioGroupFieldType.class,
                PROPERTY_FIELD_PLUGIN));
        FIELD_TYPE_MAP.put(HippoStdNodeType.NT_HTML, new TypeDescriptor(RichTextFieldType.class, NODE_FIELD_PLUGIN));
        FIELD_TYPE_MAP.put(FIELD_TYPE_COMPOUND, new TypeDescriptor(CompoundFieldType.class, NODE_FIELD_PLUGIN));
        FIELD_TYPE_MAP.put(FIELD_TYPE_CHOICE, new TypeDescriptor(ChoiceFieldType.class, CONTENT_BLOCKS_PLUGIN));
        FIELD_TYPE_MAP.put(GalleryPickerNodeType.NT_IMAGE_LINK, new TypeDescriptor(ImageLinkFieldType.class,
                NODE_FIELD_PLUGIN));
        FIELD_TYPE_MAP.put(HippoNodeType.NT_MIRROR, new TypeDescriptor(NodeLinkFieldType.class, NODE_FIELD_PLUGIN));
        FIELD_TYPE_MAP.put("OpenUiString", new TypeDescriptor(OpenUiStringFieldType.class, PROPERTY_FIELD_PLUGIN));

        STRUCTURE_PLUGIN_CLASSES = new HashSet<>();
        STRUCTURE_PLUGIN_CLASSES.add("org.hippoecm.frontend.editor.layout.");
        STRUCTURE_PLUGIN_CLASSES.add("org.hippoecm.frontend.service.render.ListViewPlugin");
    }

    public static final Supplier<ErrorWithPayloadException> INVALID_DATA
            = () -> new BadRequestException(new ErrorInfo(ErrorInfo.Reason.INVALID_DATA));

    private static final Logger log = LoggerFactory.getLogger(FieldTypeUtils.class);

    private FieldTypeUtils() {
    }

    public static void checkPluginsWithoutFieldDefinition(final FieldsInformation fieldsInformation, final ContentTypeContext context) {
        final List<Node> editorConfigFieldNodes = NamespaceUtils.getEditorFieldConfigNodes(context.getContentTypeRoot());
        for (final Node editorConfigFieldNode : editorConfigFieldNodes) {
            pluginWithoutFieldDefinition(editorConfigFieldNode)
                    .filter(FieldTypeUtils::isNotStructureElement)
                    .ifPresent(fieldsInformation::addUnsupportedField);
        }
    }

    private static Optional<String> pluginWithoutFieldDefinition(final Node editorConfigFieldNode) {
        final Optional<String> fieldProperty = JcrStringReader.get().read(editorConfigFieldNode, "field");
        if (!fieldProperty.isPresent()) {
            return JcrStringReader.get().read(editorConfigFieldNode, "plugin.class");
        } else {
            return Optional.empty();
        }
    }

    private static boolean isNotStructureElement(final String pluginClass) {
        return STRUCTURE_PLUGIN_CLASSES.stream().noneMatch(pluginClass::startsWith);
    }

    private static class TypeDescriptor {
        final Class<? extends FieldType> fieldTypeClass;
        final String defaultPluginClass;

        TypeDescriptor(final Class<? extends FieldType> fieldTypeClass, final String defaultPluginClass) {
            this.fieldTypeClass = fieldTypeClass;
            this.defaultPluginClass = defaultPluginClass;
        }
    }

    /**
     * Translate the set of validators specified at JCR level into a set of validators at the {@link FieldType} level.
     * When the list of validators contains an unsupported validator (i.e. a Wicket-specific one), the document type is
     * marked as 'readonly due to unsupported validator'.
     *
     * @param fieldType    Specification of a field type
     * @param fieldContext The context of the field
     * @param validatorNames List of 0 or more validator names specified at JCR level
     */
    public static void determineValidators(final FieldType fieldType, final FieldTypeContext fieldContext,
                                           final List<String> validatorNames) {
        if (validatorNames.isEmpty()) {
            return;
        }

        final ValidationService validationService = HippoServiceRegistry.getService(ValidationService.class);
        if (validationService == null) {
            log.error("Cannot load {} from service registry, field validation will be disabled",
                    ValidationService.class.getSimpleName());
            return;
        }

        for (final String validatorName : validatorNames) {
            if (!IGNORED_VALIDATORS.contains(validatorName)) {
                final ValidatorInstance validator = validationService.getValidator(validatorName);
                if (validator != null) {
                    fieldType.addValidatorName(validatorName);
                } else {
                    fieldType.setUnsupportedValidator(true);

                    final DocumentType docType = fieldContext.getParentContext().getDocumentType();
                    log.info("Field '{}' in document type '{}' has unsupported validator '{}', " +
                                    "documents of this type will be read only in the Channel Manager",
                            fieldType.getId(), docType.getId(), validatorName);
                    docType.setReadOnlyDueToUnsupportedValidator(true);
                }
            }
        }
    }

    public static ValidatorInstance getValidator(final String validatorName) {
        if (StringUtils.isBlank(validatorName)) {
            return null;
        }

        final ValidationService validationService = HippoServiceRegistry.getService(ValidationService.class);
        if (validationService == null) {
            log.error("Cannot load {} from service registry, field validation will be disabled",
                    ValidationService.class.getSimpleName());
            return null;
        }

        return validationService.getValidator(validatorName);
    }

    /**
     * Populate the list of fields of a content type, in the context of assembling a Document Type.
     * Note that compound fields use this method recursively to populate their fields.
     *
     * @param fields  list of fields to populate
     * @param context determines which fields are available
     * @return whether all fields in the document type have been included.
     */
    public static FieldsInformation populateFields(final List<FieldType> fields, final ContentTypeContext context) {
        return NamespaceUtils.retrieveFieldSorter(context.getContentTypeRoot())
                .map(sorter -> sortValidateAndAddFields(sorter, context, fields))
                .orElse(FieldsInformation.noneSupported());
    }

    private static FieldsInformation sortValidateAndAddFields(final FieldSorter sorter,
                                                              final ContentTypeContext context,
                                                              final List<FieldType> fields) {
        // start positive: assume all fields at this level are supported and will be included
        final FieldsInformation fieldsInformation = FieldsInformation.allSupported();

        final List<FieldTypeContext> fieldTypeContexts = sorter.sortFields(context);

        fieldTypeContexts.forEach(field -> createAndInit(field, fieldsInformation).ifPresent(fields::add));

        return fieldsInformation;
    }

    private static Optional<FieldType> createAndInit(final FieldTypeContext context,
                                                     final FieldsInformation allFieldsInfo) {
        Optional<FieldType> optionalFieldType = determineDescriptor(context)
                .flatMap(descriptor -> determineFieldTypeClass(context, descriptor))
                .flatMap(FieldTypeFactory::createFieldType);

        if (optionalFieldType.isPresent()) {
            final FieldType fieldType = optionalFieldType.get();
            final FieldsInformation fieldInfo = fieldType.init(context);

            allFieldsInfo.add(fieldInfo);

            if (fieldType.isSupported()) {
                return optionalFieldType;
            }

            if (fieldType.hasUnsupportedValidator()) {
                allFieldsInfo.addUnsupportedField(context.getType(), context.getValidators());
            }
            // Else the field is a known one, but still unsupported (example: an empty compound). Don't include
            // the field in the list of unsupported fields, but don't include it in the document type either.
        } else {
            allFieldsInfo.addUnsupportedField(context.getType(), context.getValidators());
        }

        return Optional.empty();
    }

    private static Optional<TypeDescriptor> determineDescriptor(final FieldTypeContext context) {
        return Optional.ofNullable(FIELD_TYPE_MAP.get(determineFieldType(context)));
    }

    private static Optional<Class<? extends FieldType>> determineFieldTypeClass(final FieldTypeContext context, final TypeDescriptor descriptor) {
        if (usesDefaultFieldPlugin(context, descriptor)) {
            return Optional.of(descriptor.fieldTypeClass);
        }
        return Optional.empty();
    }

    private static String determineFieldType(final FieldTypeContext context) {
        final String itemType = context.getType();

        if (FIELD_TYPE_MAP.containsKey(itemType)) {
            return itemType;
        }

        if (context.isProperty()) {
            // All supported property fields are part of the FIELD_TYPE_MAP, so this one is unsupported
            return null;
        }

        if (ChoiceFieldUtils.isChoiceField(context)) {
            return FIELD_TYPE_CHOICE;
        }

        if (isCompound(context)) {
            return FIELD_TYPE_COMPOUND;
        }

        return null;
    }

    private static boolean isCompound(final FieldTypeContext context) {
        return ContentTypeContext.getContentType(context.getType())
                .map(ContentType::isCompoundType)
                .orElse(false);
    }

    private static boolean usesDefaultFieldPlugin(final FieldTypeContext context, final TypeDescriptor descriptor) {
        return determinePluginClass(context)
                .filter(descriptor.defaultPluginClass::equals)
                .isPresent();
    }

    private static Optional<String> determinePluginClass(final FieldTypeContext context) {
        return context.getEditorConfigNode()
                .flatMap(NamespaceUtils::getPluginClassForField);
    }

    public static void checkCardinality(final FieldType field, final List<FieldValue> values) {
        if (values.size() < field.getMinValues()) {
            throw INVALID_DATA.get();
        }
        if (values.size() > field.getMaxValues()) {
            throw INVALID_DATA.get();
        }
        if (field.isRequired() && values.isEmpty()) {
            throw INVALID_DATA.get();
        }
    }

    public static void trimToMaxValues(final List list, final int maxValues) {
        while (list.size() > maxValues) {
            list.remove(list.size() - 1);
        }
    }

    /**
     * Try to read a list of fields from a node into a map of values.
     *
     * @param node     JCR node to read from
     * @param fields   fields to read
     * @param valueMap map of values to populate
     */
    public static void readFieldValues(final Node node,
                                       final List<FieldType> fields,
                                       final Map<String, List<FieldValue>> valueMap) {
        for (final FieldType field : fields) {
            field.readFrom(node).ifPresent(values -> valueMap.put(field.getId(), values));
        }
    }

    /**
     * Write the values of a set of fields to a (JCR) node, facilitated by a list of field types.
     * <p>
     * Values not defined in the list of field types are ignored.
     *
     * @param valueMap set of field type ID -> list of field values mappings. Values are not checked yet.
     * @param fields   set of field type definitions, specifying how to interpret the corresponding field values
     * @param node     the JCR node to write the field values to.
     * @throws ErrorWithPayloadException if fieldType#writeTo() bumps into an error.
     */
    public static void writeFieldValues(final Map<String, List<FieldValue>> valueMap,
                                        final List<FieldType> fields,
                                        final Node node) throws ErrorWithPayloadException {
        for (final FieldType fieldType : fields) {
            if (!fieldType.hasUnsupportedValidator()) {
                fieldType.writeTo(node, Optional.ofNullable(valueMap.get(fieldType.getId())));
            }
        }
    }

    public static boolean writeFieldValue(final FieldPath fieldPath,
                                          final List<FieldValue> fieldValues,
                                          final List<FieldType> fields,
                                          final CompoundContext context) throws ErrorWithPayloadException {
        if (fieldPath.isEmpty()) {
            return false;
        }
        for (final FieldType field : fields) {
            if (field.writeField(fieldPath, fieldValues, context)) {
                return true;
            }
        }
        return false;
    }

    public static boolean writeFieldNodeValue(final FieldPath fieldPath,
                                              final List<FieldValue> values,
                                              final NodeFieldType field,
                                              final CompoundContext context) throws ErrorWithPayloadException {
        if (!fieldPath.startsWith(field.getId())) {
            return false;
        }
        final Node parentNode = context.getNode();
        final String childName = fieldPath.getFirstSegment();
        try {
            if (!parentNode.hasNode(childName)) {
                throw new BadRequestException(new ErrorInfo(Reason.INVALID_DATA));
            }
            final Node child = parentNode.getNode(childName);
            final CompoundContext childContext = context.getChildContext(child);
            return field.writeFieldValue(fieldPath.getRemainingSegments(), values, childContext);
        } catch (final RepositoryException e) {
            log.error("Failed to write value of field '{}' to node '{}'", fieldPath, JcrUtils.getNodePathQuietly(parentNode), e);
            throw new InternalServerErrorException();
        }
    }

    public static boolean writeChoiceFieldValue(final FieldPath fieldPath,
                                                final List<FieldValue> values,
                                                final NodeFieldType field,
                                                final CompoundContext context) throws ErrorWithPayloadException, RepositoryException {
        if (!fieldPath.is(field.getId())) {
            return false;
        }
        if (values.isEmpty()) {
            throw new BadRequestException(new ErrorInfo(Reason.INVALID_DATA));
        }
        // Choices can never be multiple, there is always only one value.
        final FieldValue choiceFieldValue = values.get(0);
        field.writeValue(context.getNode(), choiceFieldValue);
        field.validateValue(choiceFieldValue, context);
        return true;
    }

    /**
     * Validate the values of a set of fields against a list of field types.
     * <p>
     * Values not defined in the list of field types are ignored.
     *
     * @param valueMap set of field type ID -> to be validated list of field values mappings
     * @param fields   set of field type definitions, including the applicable validators
     * @param context  context of the fields
     * @return the number of violations found
     */
    public static int validateFieldValues(final Map<String, List<FieldValue>> valueMap,
                                          final List<FieldType> fields,
                                          final CompoundContext context) throws ErrorWithPayloadException {
        int violationCount = 0;

        for (final FieldType fieldType : fields) {
            final String fieldId = fieldType.getId();
            if (valueMap.containsKey(fieldId)) {
                final List<FieldValue> fieldValues = valueMap.get(fieldId);
                violationCount += fieldType.validate(fieldValues, context);
            }
        }

        return violationCount;
    }

    public static int validateNodeValues(final NodeIterator nodes,
                                       final List<FieldValue> values,
                                       final NodeFieldType field,
                                       final CompoundContext context) {
        final long count = nodes.getSize();
        if (values.size() != count) {
            throw new BadRequestException(new ErrorInfo(Reason.INVALID_DATA));
        }

        int violationCount = 0;

        for (final FieldValue value : values) {
            final Node child = nodes.nextNode();
            final CompoundContext childContext = context.getChildContext(child);
            violationCount += field.validateValue(value, childContext);
        }

        return violationCount;
    }
}
