/*
 * Copyright 2018-2020 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.ValueFormatException;

import org.apache.jackrabbit.value.ValueFactoryImpl;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.util.NamespaceUtils;
import org.onehippo.cms.channelmanager.content.documenttype.validation.CompoundContext;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo.Reason;
import org.onehippo.repository.mock.MockNode;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.onehippo.cms.channelmanager.content.ValidateAndWrite.validateAndWriteTo;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.dom.*", "com.sun.org.apache.xalan.*", "javax.activation.*", "javax.net.ssl.*"})
@PrepareForTest({JcrUtils.class, NamespaceUtils.class})
public class DateAndTimeFieldTypeTest {

    private static final String PROPERTY = "test:id";

    @Before
    public void setup() {
        PowerMock.mockStatic(JcrUtils.class);
        PowerMock.mockStatic(NamespaceUtils.class);
    }

    @Test
    public void writeToSingleDate() throws Exception {
        final Node node = MockNode.root();
        final PropertyFieldType fieldType = new DateAndTimeFieldType();
        final Calendar oldValue = Calendar.getInstance();
        final String oldValueString = ValueFactoryImpl.getInstance().createValue(oldValue).getString();
        final Calendar newValue = Calendar.getInstance();
        newValue.add(Calendar.DAY_OF_YEAR, 1); // just to be sure it differs from oldValue

        fieldType.setId(PROPERTY);
        fieldType.setJcrType(PropertyType.TYPENAME_DATE);
        node.setProperty(PROPERTY, oldValue);

        try {
            validateAndWriteTo(node, fieldType, Collections.singletonList(null));
            fail("Must not be missing");
        } catch (final BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(Reason.INVALID_DATA));
        }
        assertThat(node.getProperty(PROPERTY).getString(), equalTo(oldValueString));

        try {
            validateAndWriteTo(node, fieldType, Collections.emptyList());
            fail("Must have 1 entry");
        } catch (final BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(Reason.INVALID_DATA));
        }

        try {
            validateAndWriteTo(node, fieldType, Arrays.asList(valueOf("2015-08-26T08:53:00.000+02:00"),
                    valueOf("2016-08-26T08:53:00.000+02:00")));
            fail("Must have 1 entry");
        } catch (final BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(Reason.INVALID_DATA));
        }

        validateAndWriteTo(node, fieldType, listOf(valueOf("" + newValue)));
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("" + newValue));
    }

    @Test
    public void testGetFieldValueForNullValue() {
        final PropertyFieldType fieldType = new DateAndTimeFieldType();
        final FieldValue fieldValue = fieldType.getFieldValue(null);
        assertThat(fieldValue.getValue(), equalTo(""));
    }

    @Test
    public void testGetFieldValueForBlankValue() {
        final PropertyFieldType fieldType = new DateAndTimeFieldType();
        final FieldValue fieldValue = fieldType.getFieldValue(" ");
        assertThat(fieldValue.getValue(), equalTo(""));
    }

    @Test
    public void testGetFieldValueForUnparsableValue() {
        final PropertyFieldType fieldType = new DateAndTimeFieldType();
        final FieldValue fieldValue = fieldType.getFieldValue("doesnotparse");
        assertThat(fieldValue.getValue(), equalTo(""));
    }

    @Test
    public void testGetFieldValueForParsableValue() {
        final PropertyFieldType fieldType = new DateAndTimeFieldType();
        final FieldValue fieldValue = fieldType.getFieldValue("2015-08-24T06:53:00.000Z");
        assertThat(fieldValue.getValue(), equalTo("2015-08-24T06:53:00.000Z"));
    }

    @Test
    public void writeIncorrectValueDoesNotOverwriteExistingValue() throws Exception {
        final Node node = createMock(Node.class);
        final Property oldProperty = createMock(Property.class);
        final String invalidValue = "12345-08-24T06:53:00.000Z";

        expect(node.hasProperty(eq("test:id"))).andReturn(true);
        expect(node.getProperty(eq("test:id"))).andReturn(oldProperty).anyTimes();
        expect(oldProperty.isMultiple()).andReturn(false).anyTimes();
        expect(node.setProperty(eq("test:id"), eq(invalidValue), eq(5))).andThrow(new ValueFormatException());
        expect(JcrUtils.getNodePathQuietly(node)).andReturn("/");
        replayAll();

        final PropertyFieldType fieldType = new DateAndTimeFieldType();

        fieldType.setId(PROPERTY);
        fieldType.setJcrType(PropertyType.TYPENAME_DATE);
        validateAndWriteTo(node, fieldType, listOf(valueOf(invalidValue)));
    }

    private static List<FieldValue> listOf(final FieldValue value) {
        return Collections.singletonList(value);
    }

    private static FieldValue valueOf(final String value) {
        return new FieldValue(value);
    }
}
