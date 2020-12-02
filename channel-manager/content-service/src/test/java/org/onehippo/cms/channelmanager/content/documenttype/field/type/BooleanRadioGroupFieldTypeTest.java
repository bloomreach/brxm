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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.Value;

import org.hippoecm.repository.util.JcrUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.util.LocalizationUtils;
import org.onehippo.cms.channelmanager.content.documenttype.util.NamespaceUtils;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo.Reason;
import org.onehippo.forge.selection.frontend.plugin.Config;
import org.onehippo.repository.mock.MockNode;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.onehippo.cms.channelmanager.content.ValidateAndWrite.validateAndWriteTo;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.dom.*", "com.sun.org.apache.xalan.*", "javax.activation.*", "javax.net.ssl.*"})
@PrepareForTest({JcrUtils.class, NamespaceUtils.class, LocalizationUtils.class, FieldTypeUtils.class})
public class BooleanRadioGroupFieldTypeTest {

    private static final String PROPERTY = "test:id";

    @Before
    public void setup() {
        PowerMock.mockStatic(JcrUtils.class);
        PowerMock.mockStatic(NamespaceUtils.class);
    }

    @Test
    public void testFieldConfig() {
        final BooleanRadioGroupFieldType booleanRadioGroupFieldType = new BooleanRadioGroupFieldType();
        final FieldTypeContext fieldTypeContext = new MockFieldTypeContext.Builder(booleanRadioGroupFieldType).build();
        expect(fieldTypeContext.getStringConfig(Config.FALSE_LABEL)).andReturn(Optional.of("Sad"));
        expect(fieldTypeContext.getStringConfig(Config.ORIENTATION)).andReturn(Optional.of("vertical"));
        expect(fieldTypeContext.getStringConfig(Config.SOURCE)).andReturn(Optional.of("/source/path"));
        expect(fieldTypeContext.getStringConfig(Config.TRUE_LABEL)).andReturn(Optional.of("Happy"));

        replayAll();

        booleanRadioGroupFieldType.init(fieldTypeContext);

        assertThat(booleanRadioGroupFieldType.getFalseLabel(), equalTo("Sad"));
        assertThat(booleanRadioGroupFieldType.getOrientation(), equalTo("vertical"));
        assertThat(booleanRadioGroupFieldType.getSource(), equalTo("/source/path"));
        assertThat(booleanRadioGroupFieldType.getTrueLabel(), equalTo("Happy"));

        verifyAll();
    }

    @Test
    public void testFieldConfigDefaultValues() {
        final BooleanRadioGroupFieldType booleanRadioGroupFieldType = new BooleanRadioGroupFieldType();
        final FieldTypeContext fieldTypeContext = new MockFieldTypeContext.Builder(booleanRadioGroupFieldType).build();
        expect(fieldTypeContext.getStringConfig(Config.TRUE_LABEL)).andReturn(Optional.empty());
        expect(fieldTypeContext.getStringConfig(Config.SOURCE)).andReturn(Optional.empty());
        expect(fieldTypeContext.getStringConfig(Config.FALSE_LABEL)).andReturn(Optional.of(""));
        expect(fieldTypeContext.getStringConfig(Config.ORIENTATION)).andReturn(Optional.empty());

        replayAll();

        booleanRadioGroupFieldType.init(fieldTypeContext);

        assertThat(booleanRadioGroupFieldType.getTrueLabel(), equalTo("true"));
        assertThat(booleanRadioGroupFieldType.getFalseLabel(), equalTo("false"));

        verifyAll();
    }

    @Test
    public void writeToSingleBoolean() throws Exception {
        final PropertyFieldType fieldType = new BooleanRadioGroupFieldType();
        final FieldTypeContext fieldTypeContext = new MockFieldTypeContext.Builder(fieldType)
                .jcrName(PROPERTY).build();

        expect(fieldTypeContext.getStringConfig(Config.TRUE_LABEL)).andReturn(Optional.empty());
        expect(fieldTypeContext.getStringConfig(Config.FALSE_LABEL)).andReturn(Optional.of(""));
        expect(fieldTypeContext.getStringConfig(Config.ORIENTATION)).andReturn(Optional.of("vertical"));
        expect(fieldTypeContext.getStringConfig(Config.SOURCE)).andReturn(Optional.of("/source/path"));

        replayAll();

        fieldType.init(fieldTypeContext);

        final Boolean oldValue = Boolean.TRUE;
        final Boolean newValue = Boolean.FALSE;
        final Node node = MockNode.root();
        node.setProperty(PROPERTY, oldValue);

        try {
            validateAndWriteTo(node, fieldType, Arrays.asList((FieldValue) null));
            fail("Must not be missing");
        } catch (final BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(Reason.INVALID_DATA));
        }
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("" + oldValue));

        try {
            validateAndWriteTo(node, fieldType, Collections.emptyList());
            fail("Must have 1 entry");
        } catch (final BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(Reason.INVALID_DATA));
        }

        try {
            validateAndWriteTo(node, fieldType, Arrays.asList(valueOf("true"), valueOf("false")));
            fail("Must have 1 entry");
        } catch (final BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(Reason.INVALID_DATA));
        }

        validateAndWriteTo(node, fieldType, listOf(valueOf("" + newValue)));
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("" + newValue));

        verifyAll();
    }

    @Test
    public void writeIncorrectValueDoesNotOverwriteExistingValue() throws Exception {
        final PropertyFieldType fieldType = new BooleanFieldType();
        final FieldTypeContext fieldTypeContext = new MockFieldTypeContext.Builder(fieldType)
                .jcrName(PROPERTY).build();
        expect(JcrUtils.getNodePathQuietly(anyObject(Node.class))).andReturn("/");
        replayAll();

        fieldType.init(fieldTypeContext);

        final Boolean oldValue = Boolean.TRUE;
        final String invalidValue = "foo";
        final Node node = MockNode.root();
        node.setProperty(PROPERTY, oldValue);

        validateAndWriteTo(node, fieldType, listOf(valueOf(invalidValue)));
        assertThat(node.getProperty(PROPERTY).getBoolean(), equalTo(oldValue));

        verifyAll();
    }

    @Test
    public void writeIncorrectValuesDoesNotOverwriteExistingValues() throws Exception {

        final PropertyFieldType fieldType = new BooleanFieldType();
        final FieldTypeContext fieldTypeContext = new MockFieldTypeContext.Builder(fieldType)
                .jcrName(PROPERTY)
                .multiple(true)
                .build();
        expect(JcrUtils.getNodePathQuietly(anyObject(Node.class))).andReturn("/");
        replayAll();

        fieldType.init(fieldTypeContext);
        fieldType.setMaxValues(2);

        final Boolean oldValue1 = Boolean.TRUE;
        final Boolean oldValue2 = Boolean.TRUE;
        final Node node = MockNode.root();
        node.setProperty(PROPERTY, new String[]{oldValue1 + "", oldValue2 + ""}, PropertyType.BOOLEAN);

        final String invalidValue1 = "foo";
        final List<FieldValue> es = Arrays.asList(valueOf(invalidValue1), valueOf(oldValue2 + ""));
        validateAndWriteTo(node, fieldType, es);

        final Value[] values = node.getProperty(PROPERTY).getValues();
        assertThat(values[0].getBoolean(), equalTo(oldValue1));
        assertThat(values[1].getBoolean(), equalTo(oldValue2));

        verifyAll();
    }


    private static List<FieldValue> listOf(final FieldValue value) {
        return Collections.singletonList(value);
    }

    private static FieldValue valueOf(final String value) {
        return new FieldValue(value);
    }
}
