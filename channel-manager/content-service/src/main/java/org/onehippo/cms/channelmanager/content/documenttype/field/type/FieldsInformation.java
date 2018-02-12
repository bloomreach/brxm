/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.onehippo.cms.channelmanager.content.documenttype.field.FieldValidators;
import org.onehippo.cms7.services.contenttype.ContentTypeItem;

public class FieldsInformation {

    private static final List<String> REPORTABLE_MISSING_FIELD_TYPES = Arrays.asList(
            "Boolean", "CalendarDate", "Date", "Docbase", "DynamicDropdown", "Reference", "StaticDropdown"
    );
    private static final List<String> REPORTABLE_MISSING_FIELD_NAMESPACES = Arrays.asList(
            "hippo:", "hippogallerypicker:", "hippostd:", "hipposys:", "hippotaxonomy:", "poll:", "selection:"
    );

    private boolean allFieldsIncluded;
    private boolean canCreateAllRequiredFields;
    private final SortedSet<String> unsupportedFieldTypes;
    private final SortedSet<String> unsupportedRequiredFieldTypes;

    public static FieldsInformation allSupported() {
        return new FieldsInformation(true, true);
    }

    public static FieldsInformation noneSupported() {
        return new FieldsInformation(false, false);
    }

    private FieldsInformation(final boolean allFieldsIncluded, final boolean canCreateAllRequiredFields) {
        this();
        this.allFieldsIncluded = allFieldsIncluded;
        this.canCreateAllRequiredFields = canCreateAllRequiredFields;
    }

    public FieldsInformation() {
        unsupportedFieldTypes = new TreeSet<>();
        unsupportedRequiredFieldTypes = new TreeSet<>();
    }

    public boolean isAllFieldsIncluded() {
        return allFieldsIncluded;
    }

    public void setAllFieldsIncluded(final boolean allFieldsIncluded) {
        this.allFieldsIncluded = allFieldsIncluded;
    }

    public boolean getCanCreateAllRequiredFields() {
        return canCreateAllRequiredFields;
    }

    public void setCanCreateAllRequiredFields(final boolean canCreateAllRequiredFields) {
        this.canCreateAllRequiredFields = canCreateAllRequiredFields;
    }

    public Set<String> getUnsupportedFieldTypes() {
        return unsupportedFieldTypes;
    }

    public Set<String> getUnsupportedRequiredFieldTypes() {
        return unsupportedRequiredFieldTypes;
    }

    public void add(final FieldsInformation fieldInfo) {
        allFieldsIncluded &= fieldInfo.allFieldsIncluded;
        canCreateAllRequiredFields &= fieldInfo.canCreateAllRequiredFields;
        unsupportedFieldTypes.addAll(fieldInfo.unsupportedFieldTypes);
        unsupportedRequiredFieldTypes.addAll(fieldInfo.unsupportedRequiredFieldTypes);
    }

    public void addUnsupportedField(final ContentTypeItem item) {
        final String fieldTypeName = item.getItemType();

        this.addUnsupportedField(fieldTypeName);

        // Unsupported fields cannot be created, so only when the unsupported field is not required
        // it may still be possible to create all required fields
        final boolean isRequired = item.getValidators().contains(FieldValidators.REQUIRED);
        canCreateAllRequiredFields &= !isRequired;

        if (isRequired) {
            unsupportedRequiredFieldTypes.add(reportedFieldTypeName(fieldTypeName));
        }
    }

    public void addUnsupportedField(final String fieldTypeName) {
        // The unsupported field is not included, so not all fields are included
        allFieldsIncluded = false;

        unsupportedFieldTypes.add(reportedFieldTypeName(fieldTypeName));
    }

    private static String reportedFieldTypeName(final String name) {
        if (REPORTABLE_MISSING_FIELD_TYPES.contains(name)
                || REPORTABLE_MISSING_FIELD_NAMESPACES.stream().anyMatch(name::startsWith)) {
            return name;
        } else {
            return "Custom";
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final FieldsInformation that = (FieldsInformation) o;
        return allFieldsIncluded == that.allFieldsIncluded
                && canCreateAllRequiredFields == that.canCreateAllRequiredFields
                && Objects.equals(unsupportedFieldTypes, that.unsupportedFieldTypes)
                && Objects.equals(unsupportedRequiredFieldTypes, that.unsupportedRequiredFieldTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(allFieldsIncluded, canCreateAllRequiredFields, unsupportedFieldTypes, unsupportedRequiredFieldTypes);
    }
}
