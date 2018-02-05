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

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class FieldsInformation {

    private boolean allFieldsIncluded;
    private boolean canCreateAllRequiredFields;
    private final SortedSet<String> unsupportedFieldTypes;

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

    public void add(final FieldsInformation fieldInfo) {
        allFieldsIncluded &= fieldInfo.allFieldsIncluded;
        canCreateAllRequiredFields &= fieldInfo.canCreateAllRequiredFields;
        unsupportedFieldTypes.addAll(fieldInfo.unsupportedFieldTypes);
    }

    public void addUnknownField(final String fieldTypeName, final boolean isRequired) {
        unsupportedFieldTypes.add(fieldTypeName);

        // The unknown field is not included, so not all fields are included
        allFieldsIncluded = false;

        // Unknown fields cannot be created, so only when the unknown field is not required
        // it may still be possible to create all required fields
        canCreateAllRequiredFields &= !isRequired;
    }
}
