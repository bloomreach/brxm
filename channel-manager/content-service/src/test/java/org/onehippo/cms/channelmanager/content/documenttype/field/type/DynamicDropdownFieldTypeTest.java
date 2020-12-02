/*
 * Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
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

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;

import javax.jcr.Node;
import javax.jcr.PropertyType;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.util.LocalizationUtils;
import org.onehippo.cms.channelmanager.content.documenttype.validation.CompoundContext;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.forge.selection.frontend.plugin.Config;
import org.onehippo.repository.mock.MockNode;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static junit.framework.TestCase.assertTrue;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.onehippo.cms.channelmanager.content.ValidateAndWrite.validateAndWriteTo;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.dom.*", "com.sun.org.apache.xalan.*", "javax.activation.*", "javax.net.ssl.*"})
@PrepareForTest({LocalizationUtils.class, FieldTypeUtils.class})
public class DynamicDropdownFieldTypeTest {

    private static final String PROPERTY = "test:id";

    @Test
    public void testFieldConfig() {
        final DynamicDropdownFieldType dynamicDropdownFieldType = new DynamicDropdownFieldType();
        final FieldTypeContext fieldTypeContext = new MockFieldTypeContext.Builder(dynamicDropdownFieldType)
                .jcrName("myproject:dynamicDropdown").build();

        expect(fieldTypeContext.getStringConfig(Config.SHOW_DEFAULT)).andReturn(Optional.of(""));
        expect(fieldTypeContext.getStringConfig(Config.SOURCE)).andReturn(Optional.of("/source/path"));
        expect(fieldTypeContext.getStringConfig(Config.SORT_COMPARATOR)).andReturn(Optional.of("my.Comparator"));
        expect(fieldTypeContext.getStringConfig(Config.SORT_BY)).andReturn(Optional.of("key"));
        expect(fieldTypeContext.getStringConfig(Config.SORT_ORDER)).andReturn(Optional.of("descending"));
        expect(fieldTypeContext.getStringConfig(Config.VALUELIST_PROVIDER))
                .andReturn(Optional.of("service.valuelist.default"));
        expect(fieldTypeContext.getStringConfig(Config.OBSERVABLE_ID)).andReturn(Optional.empty());
        expect(fieldTypeContext.getStringConfig(Config.OBSERVER_ID)).andReturn(Optional.empty());
        expect(fieldTypeContext.getStringConfig(Config.NAME_PROVIDER)).andReturn(Optional.empty());
        expect(fieldTypeContext.getStringConfig(Config.SOURCE_BASE_PATH)).andReturn(Optional.empty());

        replayAll();

        dynamicDropdownFieldType.init(fieldTypeContext);

        assertTrue(dynamicDropdownFieldType.isShowDefault());
        assertThat(dynamicDropdownFieldType.getSource(), equalTo("/source/path"));
        assertThat(dynamicDropdownFieldType.getSortComparator(), equalTo("my.Comparator"));
        assertThat(dynamicDropdownFieldType.getSortBy(), equalTo("key"));
        assertThat(dynamicDropdownFieldType.getSortOrder(), equalTo("descending"));

        verifyAll();
        assertThat(dynamicDropdownFieldType.getValueListProvider(), equalTo("service.valuelist.default"));
        assertTrue(dynamicDropdownFieldType.isSupported());
    }

    @Test
    public void testShowDefaultConfig() {
        final DynamicDropdownFieldType dynamicDropdownFieldType = new DynamicDropdownFieldType();
        final FieldTypeContext fieldTypeContext = new MockFieldTypeContext.Builder(dynamicDropdownFieldType)
                .jcrName("myproject:dynamicDropdown").build();

        expect(fieldTypeContext.getStringConfig(Config.SHOW_DEFAULT)).andReturn(Optional.of("false"));
        expect(fieldTypeContext.getStringConfig(Config.SOURCE)).andReturn(Optional.of("/source/path"));
        expect(fieldTypeContext.getStringConfig(Config.SORT_COMPARATOR)).andReturn(Optional.of("my.Comparator"));
        expect(fieldTypeContext.getStringConfig(Config.SORT_BY)).andReturn(Optional.of("key"));
        expect(fieldTypeContext.getStringConfig(Config.SORT_ORDER)).andReturn(Optional.of("descending"));
        expect(fieldTypeContext.getStringConfig(Config.VALUELIST_PROVIDER))
                .andReturn(Optional.of("service.valuelist.default"));
        expect(fieldTypeContext.getStringConfig(Config.OBSERVABLE_ID)).andReturn(Optional.empty());
        expect(fieldTypeContext.getStringConfig(Config.OBSERVER_ID)).andReturn(Optional.empty());
        expect(fieldTypeContext.getStringConfig(Config.NAME_PROVIDER)).andReturn(Optional.empty());
        expect(fieldTypeContext.getStringConfig(Config.SOURCE_BASE_PATH)).andReturn(Optional.empty());

        replayAll();

        dynamicDropdownFieldType.init(fieldTypeContext);

        assertFalse(dynamicDropdownFieldType.isShowDefault());

        verifyAll();
    }

    @Test
    public void testUnsupportedWithNonDefaultValueListProvider() {
        final DynamicDropdownFieldType dynamicDropdownFieldType = new DynamicDropdownFieldType();
        FieldTypeContext fieldTypeContext = new MockFieldTypeContext.Builder(dynamicDropdownFieldType)
                .jcrName("myproject:dynamicDropdown").build();
        expect(fieldTypeContext.getStringConfig(Config.SHOW_DEFAULT)).andReturn(Optional.of(""));
        expect(fieldTypeContext.getStringConfig(Config.SOURCE)).andReturn(Optional.of("/source/path"));
        expect(fieldTypeContext.getStringConfig(Config.SORT_COMPARATOR)).andReturn(Optional.of("my.Comparator"));
        expect(fieldTypeContext.getStringConfig(Config.SORT_BY)).andReturn(Optional.of("key"));
        expect(fieldTypeContext.getStringConfig(Config.SORT_ORDER)).andReturn(Optional.of("descending"));
        expect(fieldTypeContext.getStringConfig(Config.VALUELIST_PROVIDER))
                .andReturn(Optional.of("service.valuelist.custom"));
        expect(fieldTypeContext.getStringConfig(Config.OBSERVABLE_ID)).andReturn(Optional.empty());
        expect(fieldTypeContext.getStringConfig(Config.OBSERVER_ID)).andReturn(Optional.empty());
        expect(fieldTypeContext.getStringConfig(Config.NAME_PROVIDER)).andReturn(Optional.empty());
        expect(fieldTypeContext.getStringConfig(Config.SOURCE_BASE_PATH)).andReturn(Optional.empty());

        replayAll();

        dynamicDropdownFieldType.init(fieldTypeContext);

        assertFalse(dynamicDropdownFieldType.isSupported());

        verifyAll();
    }

    @Test
    public void testUnsupportedWithObservation() {
        final DynamicDropdownFieldType dynamicDropdownFieldType = new DynamicDropdownFieldType();
        FieldTypeContext fieldTypeContext = new MockFieldTypeContext.Builder(dynamicDropdownFieldType)
                .jcrName("myproject:dynamicDropdown").build();
        expect(fieldTypeContext.getStringConfig(Config.SHOW_DEFAULT)).andReturn(Optional.of(""));
        expect(fieldTypeContext.getStringConfig(Config.SOURCE)).andReturn(Optional.of("/source/path"));
        expect(fieldTypeContext.getStringConfig(Config.SORT_COMPARATOR)).andReturn(Optional.of("my.Comparator"));
        expect(fieldTypeContext.getStringConfig(Config.SORT_BY)).andReturn(Optional.of("key"));
        expect(fieldTypeContext.getStringConfig(Config.SORT_ORDER)).andReturn(Optional.of("descending"));
        expect(fieldTypeContext.getStringConfig(Config.VALUELIST_PROVIDER))
                .andReturn(Optional.of("service.valuelist.default"));
        expect(fieldTypeContext.getStringConfig(Config.OBSERVABLE_ID)).andReturn(Optional.of("observable.id"));
        expect(fieldTypeContext.getStringConfig(Config.OBSERVER_ID)).andReturn(Optional.of("observer.id"));
        expect(fieldTypeContext.getStringConfig(Config.NAME_PROVIDER)).andReturn(Optional.of("name.provider"));
        expect(fieldTypeContext.getStringConfig(Config.SOURCE_BASE_PATH)).andReturn(Optional.of("source.base.path"));

        replayAll();

        dynamicDropdownFieldType.init(fieldTypeContext);

        assertFalse(dynamicDropdownFieldType.isSupported());

        verifyAll();
    }

    @Test
    public void writeToSingleDouble() throws Exception {
        final Node node = MockNode.root();
        final PropertyFieldType fieldType = new DynamicDropdownFieldType();
        final String oldValue = "one";
        final String newValue = "two";

        fieldType.setId(PROPERTY);
        fieldType.setJcrType(PropertyType.TYPENAME_STRING);
        node.setProperty(PROPERTY, oldValue);

        try {
            validateAndWriteTo(node, fieldType, Collections.singletonList(null));
            fail("Must not be missing");
        } catch (final BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getProperty(PROPERTY).getString(), equalTo(oldValue));

        try {
            validateAndWriteTo(node, fieldType, Collections.emptyList());
            fail("Must have 1 entry");
        } catch (final BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }

        try {
            validateAndWriteTo(node, fieldType, Arrays.asList(valueOf("11"), valueOf("12")));
            fail("Must have 1 entry");
        } catch (final BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }

        validateAndWriteTo(node, fieldType, listOf(valueOf(newValue)));
        assertThat(node.getProperty(PROPERTY).getString(), equalTo(newValue));
    }

    private static List<FieldValue> listOf(final FieldValue value) {
        return Collections.singletonList(value);
    }

    private static FieldValue valueOf(final String value) {
        return new FieldValue(value);
    }
}
