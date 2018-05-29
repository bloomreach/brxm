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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FieldsInformationTest {

    @Test
    public void allSupported() {
        final FieldsInformation info = FieldsInformation.allSupported();
        assertTrue(info.isAllFieldsIncluded());
        assertTrue(info.getCanCreateAllRequiredFields());
        assertTrue(info.getUnsupportedFieldTypes().isEmpty());
        assertTrue(info.getUnsupportedRequiredFieldTypes().isEmpty());
    }

    @Test
    public void noneSupported() {
        final FieldsInformation info = FieldsInformation.noneSupported();
        assertFalse(info.isAllFieldsIncluded());
        assertFalse(info.getCanCreateAllRequiredFields());
        assertTrue(info.getUnsupportedFieldTypes().isEmpty());
        assertTrue(info.getUnsupportedRequiredFieldTypes().isEmpty());
    }

    @Test
    public void setters() {
        final FieldsInformation info = new FieldsInformation();

        info.setAllFieldsIncluded(true);
        info.setCanCreateAllRequiredFields(true);

        assertTrue(info.isAllFieldsIncluded());
        assertTrue(info.getCanCreateAllRequiredFields());
    }

    @Test
    public void addCustomOptionalField() {
        final FieldsInformation info = FieldsInformation.allSupported();
        info.addUnsupportedField("Test", Collections.emptyList());

        assertFalse(info.isAllFieldsIncluded());
        assertTrue(info.getCanCreateAllRequiredFields());
        assertThat(info.getUnsupportedFieldTypes(), equalTo(Collections.singleton("Custom")));
        assertTrue(info.getUnsupportedRequiredFieldTypes().isEmpty());
    }

    @Test
    public void addCustomRequiredField() {
        final FieldsInformation info = FieldsInformation.allSupported();
        info.addUnsupportedField("Test", Collections.singletonList("required"));

        assertFalse(info.isAllFieldsIncluded());
        assertFalse(info.getCanCreateAllRequiredFields());
        assertThat(info.getUnsupportedFieldTypes(), equalTo(Collections.singleton("Custom")));
        assertThat(info.getUnsupportedRequiredFieldTypes(), equalTo(Collections.singleton("Custom")));
    }

    @Test
    public void addReportableRequiredField() {
        final FieldsInformation info = FieldsInformation.allSupported();
        info.addUnsupportedField("DynamicDropdown", Collections.singletonList("required"));

        assertFalse(info.isAllFieldsIncluded());
        assertFalse(info.getCanCreateAllRequiredFields());
        assertThat(info.getUnsupportedFieldTypes(), equalTo(Collections.singleton("DynamicDropdown")));
        assertThat(info.getUnsupportedRequiredFieldTypes(), equalTo(Collections.singleton("DynamicDropdown")));
    }

    @Test
    public void addReportableUnsupportedPluginField() {
        final FieldsInformation info = FieldsInformation.allSupported();
        info.addUnsupportedField("org.hippoecm.test");

        assertFalse(info.isAllFieldsIncluded());
        assertThat(info.getUnsupportedFieldTypes(), equalTo(Collections.singleton("org.hippoecm.test")));
    }

    @Test
    public void addMultipleReportableFields() {
        final FieldsInformation info = FieldsInformation.allSupported();
        info.addUnsupportedField("StaticDropdown", Collections.emptyList());
        info.addUnsupportedField("DynamicDropdown", Collections.singletonList("required"));

        assertFalse(info.isAllFieldsIncluded());
        assertFalse(info.getCanCreateAllRequiredFields());

        final Set<String> expectedUnsupportedTypes = new LinkedHashSet<>(Arrays.asList("DynamicDropdown", "StaticDropdown"));
        assertThat(info.getUnsupportedFieldTypes(), equalTo(expectedUnsupportedTypes));

        assertThat(info.getUnsupportedRequiredFieldTypes(), equalTo(Collections.singleton("DynamicDropdown")));
    }

    @Test
    public void addCompoundWithUnknownRequiredField() {
        final FieldsInformation info = FieldsInformation.allSupported();

        final FieldsInformation compoundWithUnknownRequiredField = FieldsInformation.noneSupported();
        compoundWithUnknownRequiredField.addUnsupportedField("Test", Collections.singletonList("required"));
        info.add(compoundWithUnknownRequiredField);

        assertFalse(info.isAllFieldsIncluded());
        assertFalse(info.getCanCreateAllRequiredFields());
        assertThat(info.getUnsupportedFieldTypes(), equalTo(Collections.singleton("Custom")));
        assertThat(info.getUnsupportedRequiredFieldTypes(), equalTo(Collections.singleton("Custom")));
    }

    @Test
    public void testEquals() {
        assertTrue(FieldsInformation.allSupported().equals(FieldsInformation.allSupported()));
        assertTrue(FieldsInformation.noneSupported().equals(FieldsInformation.noneSupported()));
        assertFalse(FieldsInformation.allSupported().equals(FieldsInformation.noneSupported()));

        FieldsInformation unknown = new FieldsInformation();
        unknown.addUnsupportedField("Test");

        assertFalse(unknown.equals(new FieldsInformation()));

        assertTrue(unknown.equals(unknown)); // test object equality
        assertFalse(new FieldsInformation().equals(new FieldsInformation() {})); // test subclass
    }

    @Test
    public void testHashCode() {
        assertTrue(FieldsInformation.allSupported().hashCode() == FieldsInformation.allSupported().hashCode());
        assertTrue(FieldsInformation.noneSupported().hashCode() == FieldsInformation.noneSupported().hashCode());
        assertTrue(FieldsInformation.allSupported().hashCode() != FieldsInformation.noneSupported().hashCode());

        FieldsInformation unknown = new FieldsInformation();
        unknown.addUnsupportedField("Test");

        assertTrue(unknown.hashCode() != new FieldsInformation().hashCode());
    }
}
