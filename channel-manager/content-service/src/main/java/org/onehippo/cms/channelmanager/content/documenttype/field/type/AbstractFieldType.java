/*
 * Copyright 2016-2021 Hippo B.V. (http://www.onehippo.com)
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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldValidators;
import org.onehippo.cms.channelmanager.content.documenttype.validation.CompoundContext;
import org.onehippo.cms.channelmanager.content.documenttype.validation.ValidationUtils;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.documenttype.util.LocalizationUtils;
import org.onehippo.cms.services.validation.api.ValueContext;
import org.onehippo.repository.l10n.ResourceBundle;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * This bean represents a field type, used for the fields of a {@link DocumentType}. It can be serialized into JSON to
 * expose it through a REST API.
 */
@JsonInclude(Include.NON_EMPTY)
public abstract class AbstractFieldType implements BaseFieldType {

    private static final String PROPERTY_MAX_ITEMS = "maxitems";

    private String id;            // "namespace:fieldname", unique within a "level" of fields.
    private Type type;
    private String displayName;   // using the correct language/locale
    private String hint;          // using the correct language/locale
    private boolean hasUnsupportedValidator;
    private String jcrType;
    private String effectiveType;
    private boolean orderable;

    @JsonIgnore
    private int minValues = 1;
    @JsonIgnore
    private int maxValues = 1;
    @JsonIgnore
    private boolean isMultiple;

    // private boolean readOnly;  // future improvement

    private final Set<String> validatorNames = new LinkedHashSet<>();

    @Override
    public final String getId() {
        return id;
    }

    @Override
    public final void setId(final String id) {
        this.id = id;
    }

    @Override
    public final Type getType() {
        return type;
    }

    protected final void setType(final Type type) {
        this.type = type;
    }

    @Override
    public final String getDisplayName() {
        return displayName;
    }

    @Override
    public final void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    @Override
    public final String getHint() {
        return hint;
    }

    @Override
    public final void setHint(final String hint) {
        this.hint = hint;
    }

    @Override
    public final int getMinValues() {
        return minValues;
    }

    @Override
    public final void setMinValues(final int minValues) {
        this.minValues = minValues;
    }

    @Override
    public final int getMaxValues() {
        return maxValues;
    }

    @Override
    public final void setMaxValues(final int maxValues) {
        this.maxValues = maxValues;
    }

    @Override
    public final boolean isMultiple() {
        return isMultiple;
    }

    @Override
    public final void setMultiple(final boolean isMultiple) {
        this.isMultiple = isMultiple;
    }

    @Override
    public final void addValidatorName(final String validatorName) {
        validatorNames.add(validatorName);
    }

    @Override
    public final boolean isRequired() {
        return validatorNames.contains(FieldValidators.REQUIRED);
    }

    @Override
    public boolean isSupported() {
        return !this.hasUnsupportedValidator();
    }

    @Override
    public final boolean hasUnsupportedValidator() {
        return hasUnsupportedValidator;
    }

    @Override
    public final void setUnsupportedValidator(final boolean hasUnsupportedValidator) {
        this.hasUnsupportedValidator = hasUnsupportedValidator;
    }

    final String getJcrType() {
        return jcrType;
    }

    @Override
    public boolean isOrderable() {
        return orderable;
    }

    @Override
    public void setOrderable(final boolean orderable) {
        this.orderable = orderable;
    }

    final void setJcrType(final String jcrType) {
        this.jcrType = jcrType;
    }

    final String getEffectiveType() {
        return effectiveType;
    }

    final void setEffectiveType(final String effectiveType) {
        this.effectiveType = effectiveType;
    }

    final Set<String> getValidatorNames() {
        return validatorNames;
    }

    @Override
    public FieldsInformation init(final FieldTypeContext fieldContext) {
        final ContentTypeContext parentContext = fieldContext.getParentContext();
        final String fieldId = fieldContext.getJcrName();

        setId(fieldId);

        // only load displayName and hints if locale-info is available.
        final Optional<ResourceBundle> resourceBundle = parentContext.getResourceBundle();
        final Optional<Node> editorFieldConfig = fieldContext.getEditorConfigNode();

        setLocalizedLabels(resourceBundle, editorFieldConfig);

        final List<String> validators = fieldContext.getValidators();

        FieldTypeUtils.determineValidators(this, fieldContext, validators);

        // determine cardinality
        if (validators.contains(FieldValidators.OPTIONAL)) {
            setMinValues(0);
            setMaxValues(1);
        }

        if (fieldContext.isMultiple()) {
            setMinValues(0);
            setMaxValues(loadMaxValues(fieldContext));
        }

        setMultiple(fieldContext.isMultiple());
        setOrderable(fieldContext.isOrderable());

        jcrType = fieldContext.getJcrType();
        effectiveType = fieldContext.getType();

        return FieldsInformation.allSupported();
    }


    private int loadMaxValues(final FieldTypeContext fieldContext) {
        return fieldContext.getStringConfig(PROPERTY_MAX_ITEMS)
                .map(Integer::parseInt)
                .orElse(Integer.MAX_VALUE);
    }

    @Override
    public final void writeTo(final Node node, final Optional<List<FieldValue>> optionalValues) {
        writeValues(node, optionalValues);
    }

    protected static boolean hasProperty(final Node node, final String propertyName) throws RepositoryException {
        if (!node.hasProperty(propertyName)) {
            return false;
        }
        final Property property = node.getProperty(propertyName);
        if (!property.isMultiple()) {
            return true;
        }
        // empty multiple property is equivalent to no property.
        return property.getValues().length > 0;
    }

    private void setLocalizedLabels(final Optional<ResourceBundle> resourceBundle, final Optional<Node> editorFieldConfig) {
        final String fieldId = getId();
        LocalizationUtils.determineFieldDisplayName(fieldId, resourceBundle, editorFieldConfig).ifPresent(this::setDisplayName);
        LocalizationUtils.determineFieldHint(fieldId, resourceBundle, editorFieldConfig).ifPresent(this::setHint);
    }

    /**
     * Validates a single value of this field using all configured validators. Will be called for every value of a
     * multiple field. When the value is not valid, the errorInfo of the value should be set to indicate the problem.
     *
     * @param value value to validate
     * @param context the context of the field
     * @return 1 if a validator deemed the value invalid, 0 otherwise
     */
    @Override
    public int validateValue(final FieldValue value, final CompoundContext context) {
        if (validatorNames.isEmpty()) {
            return 0;
        }

        final Object validatedValue = getValidatedValue(value, context);
        final ValueContext valueContext = context.getValueContext(getId(), jcrType, effectiveType);
        return ValidationUtils.validateValue(value, valueContext, validatorNames, validatedValue);
    }
}
