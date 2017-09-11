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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldValidators;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.documenttype.util.LocalizationUtils;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo.Reason;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms7.services.contenttype.ContentTypeItem;
import org.onehippo.repository.l10n.ResourceBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * This bean represents a field type, used for the fields of a {@link DocumentType}. It can be serialized into JSON to
 * expose it through a REST API.
 */
@JsonInclude(Include.NON_EMPTY)
public abstract class AbstractFieldType implements FieldType {

    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(AbstractFieldType.class);

    protected static final Supplier<ErrorWithPayloadException> INVALID_DATA
            = () -> new BadRequestException(new ErrorInfo(Reason.INVALID_DATA));

    private String id;            // "namespace:fieldname", unique within a "level" of fields.
    private Type type;
    private String displayName;   // using the correct language/locale
    private String hint;          // using the correct language/locale

    @JsonIgnore
    private int minValues = 1;
    @JsonIgnore
    private int maxValues = 1;
    @JsonIgnore
    private boolean isMultiple;

    // private boolean orderable; // future improvement
    // private boolean readOnly;  // future improvement

    private final Set<Validator> validators = new HashSet<>();

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(final String id) {
        this.id = id;
    }

    @Override
    public Type getType() {
        return type;
    }

    protected void setType(final Type type) {
        this.type = type;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getHint() {
        return hint;
    }

    @Override
    public void setHint(final String hint) {
        this.hint = hint;
    }

    @Override
    public int getMinValues() {
        return minValues;
    }

    @Override
    public void setMinValues(final int minValues) {
        this.minValues = minValues;
    }

    @Override
    public int getMaxValues() {
        return maxValues;
    }

    @Override
    public void setMaxValues(final int maxValues) {
        this.maxValues = maxValues;
    }

    @Override
    public boolean isMultiple() {
        return isMultiple;
    }

    @Override
    public void setMultiple(final boolean isMultiple) {
        this.isMultiple = isMultiple;
    }

    @Override
    public Set<Validator> getValidators() {
        return validators;
    }

    @Override
    public void addValidator(final Validator validator) {
        validators.add(validator);
    }

    @Override
    public boolean isRequired() {
        return getValidators().contains(Validator.REQUIRED);
    }

    @Override
    public boolean hasUnsupportedValidator() {
        return getValidators().contains(Validator.UNSUPPORTED);
    }

    @Override
    public boolean isValid() {
        return !hasUnsupportedValidator();
    }

    @Override
    public void init(final FieldTypeContext fieldContext) {
        final ContentTypeContext parentContext = fieldContext.getParentContext();
        final ContentTypeItem item = fieldContext.getContentTypeItem();
        final String fieldId = item.getName();

        setId(fieldId);

        // only load displayName and hints if locale-info is available.
        final Optional<ResourceBundle> resourceBundle = parentContext.getResourceBundle();
        final Optional<Node> editorFieldConfig = fieldContext.getEditorConfigNode();

        setLocalizedLabels(resourceBundle, editorFieldConfig);

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

        setMultiple(item.isMultiple());
    }

    @Override
    public final void writeTo(final Node node, final Optional<List<FieldValue>> optionalValues)
            throws ErrorWithPayloadException {
        writeValues(node, optionalValues, true);
    }

    @Override
    public boolean writeField(final Node node, final FieldPath fieldPath, final List<FieldValue> values) throws ErrorWithPayloadException {
        if (!fieldPath.is(getId())) {
            return false;
        }
        writeValues(node, Optional.of(values), false);
        return true;
    }

    protected abstract void writeValues(final Node node, final Optional<List<FieldValue>> optionalValues, boolean validateValues) throws ErrorWithPayloadException;

    protected void trimToMaxValues(final List list) {
        while (list.size() > maxValues) {
            list.remove(list.size() - 1);
        }
    }

    protected void checkCardinality(final List<FieldValue> values)
            throws ErrorWithPayloadException {
        if (values.size() < getMinValues()) {
            throw INVALID_DATA.get();
        }
        if (values.size() > getMaxValues()) {
            throw INVALID_DATA.get();
        }

        if (isRequired() && values.isEmpty()) {
            throw INVALID_DATA.get();
        }
    }

    protected static boolean validateValues(final List<FieldValue> valueList, final Predicate<FieldValue> validator) {
        boolean isValid = true;

        for (final FieldValue value : valueList) {
            if (!validator.test(value)) {
                isValid = false;
            }
        }

        return isValid;
    }

    /**
     * Hook for sub-classes to process values before writing them. The default implementation does nothing.
     *
     * @param optionalValues the values to process
     * @return the processed values
     */
    protected List<FieldValue> processValues(final Optional<List<FieldValue>> optionalValues) {
        return optionalValues.orElse(Collections.emptyList());
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

}
