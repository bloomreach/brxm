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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.jcr.Node;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldValidators;
import org.onehippo.cms.channelmanager.content.documenttype.util.LocalizationUtils;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms7.services.contenttype.ContentTypeItem;
import org.onehippo.repository.l10n.ResourceBundle;

/**
 * This bean represents a field type, used for the fields of a {@link DocumentType}.
 * It can be serialized into JSON to expose it through a REST API.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public abstract class FieldType {

    protected static final Supplier<ErrorWithPayloadException> INVALID_DATA
            = () -> new BadRequestException(new ErrorInfo(ErrorInfo.Reason.INVALID_DATA));

    private String id;            // "namespace:fieldname", unique within a "level" of fields.
    private Type type;
    private String displayName;   // using the correct language/locale
    private String hint;          // using the correct language/locale

    @JsonIgnore
    private int minValues = 1;
    @JsonIgnore
    private int maxValues = 1;

    // private boolean orderable; // future improvement
    // private boolean readOnly;  // future improvement

    private Set<Validator> validators = new HashSet<>();

    public enum Type {
        STRING,
        MULTILINE_STRING,
        CHOICE, // "content blocks"
        COMPOUND
    }

    /**
     *  The 'REQUIRED' validator is meant to indicate that a primitive field must have content. What exactly that
     *  means depends on the field type. The 'REQUIRED' validator is *not* meant to indicate that at least one
     *  instance of a multiple field must be present.
     */
    public enum Validator {
        REQUIRED,
        UNSUPPORTED
    }

    public FieldType() {
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public Type getType() {
        return type;
    }

    protected void setType(final Type type) {
        this.type = type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(final String hint) {
        this.hint = hint;
    }

    public int getMinValues() {
        return minValues;
    }

    public void setMinValues(final int minValues) {
        this.minValues = minValues;
    }

    public int getMaxValues() {
        return maxValues;
    }

    public void setMaxValues(final int maxValues) {
        this.maxValues = maxValues;
    }

    public Set<Validator> getValidators() {
        return validators;
    }

    public void addValidator(final Validator validator) {
        validators.add(validator);
    }

    public boolean isRequired() {
        return getValidators().contains(Validator.REQUIRED);
    }

    /**
     * Check if an initialized field is "valid", i.e. should be present in a document type.
     *
     * @return true or false
     */
    public boolean isValid() {
        return true;
    }

    /**
     * Initialize a {@link FieldType}, given a field context.
     *
     * @param fieldContext  information about the field (as part of a parent content type)
     */
    public abstract void init(FieldTypeContext fieldContext);

    /**
     * Read a document field instance from a document variant node
     *
     * @param node JCR node to read the value from
     * @return     Object representing the values, or nothing, wrapped in an Optional
     */
    public abstract Optional<List<FieldValue>> readFrom(Node node);

    /**
     * Write the optional value of this field to the provided JCR node.
     *
     * We purposefully pass in the value as an optional, because the validation of the cardinality constraint
     * happens during this call. If we would not do the call if the field has no value, then the validation
     * against the field's cardinality constraints or against the field's current number of values would not
     * take place.
     *
     * @param node          JCR node to store the value on
     * @param optionalValue value to write, or nothing, wrapped in an Optional
     * @throws ErrorWithPayloadException
     *                      indicates that writing the provided value ran into an unrecoverable error
     */
    public abstract void writeTo(Node node, Optional<Object> optionalValue) throws ErrorWithPayloadException;

    /**
     * Validate the current value of this field against all applicable (and supported) validators.
     *
     * @param valueList list of field value(s) to validate
     * @return          true upon success, false if at least one validation error was encountered.
     */
    public abstract boolean validate(final List<FieldValue> valueList);

    /**
     * Initialize a field based on its field type context.
     *
     * Under normal circumstances (as opposed to #initFromContentType), a field gets initialized based on its
     * field type context, i.e. the field has a certain purpose inside a parent content type.
     * This method helps initializing the generic part of an implementation of {@link FieldType}.
     *
     * @param fieldContext  information about the field (as part of a content type)
     */
    protected void initFromFieldType(FieldTypeContext fieldContext) {
        final ContentTypeContext parentContext = fieldContext.getParentContext();
        final ContentTypeItem item = fieldContext.getContentTypeItem();
        final String fieldId = item.getName();

        setId(fieldId);

        // only load displayName and hints if locale-info is available.
        if (parentContext.getLocale().isPresent()) {
            final Optional<ResourceBundle> resourceBundle = parentContext.getResourceBundle();
            final Optional<Node> editorFieldConfig = fieldContext.getEditorConfigNode();

            LocalizationUtils.determineFieldDisplayName(fieldId, resourceBundle, editorFieldConfig).ifPresent(this::setDisplayName);
            LocalizationUtils.determineFieldHint(fieldId, resourceBundle, editorFieldConfig).ifPresent(this::setHint);
        }

        FieldTypeUtils.determineValidators(this, parentContext.getDocumentType(), item.getValidators());

        // determine cardinality
        if (item.getValidators().contains(FieldValidators.OPTIONAL)) {
            setMinValues(0);
            setMaxValues(1);
        }
        if (item.isMultiple()) {
            setMinValues(0);
            setMaxValues(Integer.MAX_VALUE);
        }
    }

    /**
     * Initialize a (complex) field based on its content type context.
     *
     * For now, this helper method is only used for compound fields that are children of a CHOICE field.
     * In such a situation, the field does not have a corresponding editorFieldConfig nor a ContentTypeService-
     * based parent context. As such, there is no hint capability, and the field's display name will always
     * be the localized name of the compound's content type.
     * Also, these fields are always assumed to be singular (minValues == maxValues == 1), and it is not possible
     * to specify validators for them.
     *
     * @param context context of the content type representing *this* field type
     */
    protected void initFromContentType(final ContentTypeContext context) {
        final String fieldId = context.getContentType().getName();

        setId(fieldId);

        // only load displayName if locale-info is available.
        if (context.getLocale().isPresent()) {
            final Optional<ResourceBundle> resourceBundle = context.getResourceBundle();
            LocalizationUtils.determineDocumentDisplayName(fieldId, resourceBundle).ifPresent(this::setDisplayName);
        }
    }

    protected void trimToMaxValues(final List list) {
        while (list.size() > maxValues) {
            list.remove(list.size() - 1);
        }
    }

    @SuppressWarnings("unchecked")
    protected List<FieldValue> checkValue(final Optional<Object> optionalValue)
            throws BadRequestException {
        final BadRequestException BAD_REQUEST_INVALID_DATA
                = new BadRequestException(new ErrorInfo(ErrorInfo.Reason.INVALID_DATA));
        final Object value = optionalValue.orElse(Collections.emptyList());
        if (!(value instanceof List)) {
            throw BAD_REQUEST_INVALID_DATA;
        }
        final List values = (List)value;

        // check cardinality
        if (values.size() < getMinValues()) {
            throw BAD_REQUEST_INVALID_DATA;
        }
        if (values.size() > getMaxValues()) {
            throw BAD_REQUEST_INVALID_DATA;
        }

        if (isRequired() && values.isEmpty()) {
            throw BAD_REQUEST_INVALID_DATA;
        }

        // check individual values
        for (Object v : values) {
            if (!(v instanceof FieldValue)) {
                throw BAD_REQUEST_INVALID_DATA;
            }
        }
        return (List<FieldValue>) values;
    }

    protected boolean validateValues(final List<FieldValue> valueList, final Predicate<FieldValue> validator) {
        boolean isValid = true;

        for (FieldValue value : valueList) {
            if (!validator.test(value)) {
                isValid = false;
            }
        }

        return isValid;
    }
}
