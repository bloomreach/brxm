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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.jcr.Node;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.validation.ValidationErrorInfo;
import org.onehippo.cms.channelmanager.content.documenttype.util.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.util.FieldValidators;
import org.onehippo.cms.channelmanager.content.documenttype.util.LocalizationUtils;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.cms7.services.contenttype.ContentTypeItem;
import org.onehippo.repository.l10n.ResourceBundle;

/**
 * This bean represents a field type, used for the fields of a {@link DocumentType}.
 * It can be serialized into JSON to expose it through a REST API.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class FieldType {

    protected static final BadRequestException BAD_REQUEST_INVALID_DATA
            = new BadRequestException(new ErrorInfo(ErrorInfo.Reason.INVALID_DATA));
    protected static final BadRequestException BAD_REQUEST_CARDINALITY_CHANGE
            = new BadRequestException(new ErrorInfo(ErrorInfo.Reason.CARDINALITY_CHANGE));

    private String id;            // "namespace:fieldname", unique within a "level" of fields.
    private Type type;
    private String displayName;   // using the correct language/locale
    private String hint;          // using the correct language/locale

    private boolean multiple;
    // private boolean orderable; // future improvement
    // private boolean readOnly;  // future improvement

    @JsonIgnore
    private boolean optional;     // optional field has cardinality 0 or 1.
                                  // the API exposes multiple=true for optional fields

    private Set<Validator> validators = new HashSet<>();

    // TODO: move up into CompoundFieldType? - currently not possible due to deserialization in MockResponse.
    protected List<FieldType> fields; // the child-fields of a complex field type (COMPOUND or CHOICE)

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

    public boolean isMultiple() {
        return multiple;
    }

    public void setMultiple(final boolean multiple) {
        this.multiple = multiple;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(final boolean optional) {
        this.optional = optional;
    }

    public Set<Validator> getValidators() {
        return validators;
    }

    public void addValidator(final Validator validator) {
        validators.add(validator);
    }

    public List<FieldType> getFields() {
        return fields;
    }

    public void setFields(final List<FieldType> fields) {
        this.fields = fields;
    }

    // TODO make abstract after phasing out the MockResponse.
    /**
     * Read a document field instance from a document variant node
     *
     * @param node JCR node to read the value from
     * @return     Object representing the value, or nothing, wrapped in an Optional
     */
    public Optional<Object> readFrom(Node node) {
        return Optional.empty();
    }

    // TODO make abstract after phasing out the MockResponse.
    /**
     * Write the optional value of this field to the provided JCR node.
     *
     * @param node          JCR node to store the value on
     * @param optionalValue value to write, or nothing, wrapped in an Optional
     * @throws ErrorWithPayloadException
     *                      indicates that writing the provided value ran into an unrecoverable error
     */
    public void writeTo(Node node, Optional<Object> optionalValue) throws ErrorWithPayloadException {
        throw new InternalServerErrorException();
    }

    // TODO make abstract after phasing out the MockResponse.
    /**
     * Validate the current value of this field against all applicable (and supported) validators.
     *
     * @param optionalValue value to validate, or nothing, wrapped in an Optional
     * @return     validation error or nothing, wrapped in an Optional.
     *             The validation error can be either a {@link ValidationErrorInfo} or a map of sub-validation errors.
     */
    public Optional<Object> validate(final Optional<Object> optionalValue) {
        return Optional.empty();
    }

    /**
     * Initialize a FieldType, given various information sources:
     *
     * @param context            field-specific information
     * @param contentTypeContext content type-specific information (may be document of compound)
     * @param docType            reference to the document type being assembled
     * @return                   itself
     */
    public Optional<FieldType> init(FieldTypeContext context, ContentTypeContext contentTypeContext, DocumentType docType) {
        final Optional<ResourceBundle> resourceBundle = contentTypeContext.getResourceBundle();
        final Node editorFieldConfig = context.getEditorConfigNode();
        final ContentTypeItem item = context.getContentTypeItem();
        final String fieldId = item.getName();

        setId(fieldId);

        // only load displayName and hints if locale-info is available.
        contentTypeContext.getLocale().ifPresent(dummy -> {
            LocalizationUtils.determineFieldDisplayName(fieldId, resourceBundle, editorFieldConfig).ifPresent(this::setDisplayName);
            LocalizationUtils.determineFieldHint(fieldId, resourceBundle, editorFieldConfig).ifPresent(this::setHint);
        });

        if (item.getValidators().contains(FieldValidators.OPTIONAL)) {
            setOptional(true);
        }
        if (item.isMultiple() || isOptional()) {
            setMultiple(true);
        }

        FieldTypeUtils.determineValidators(this, docType, item.getValidators());

        return Optional.of(this);
    }
}
