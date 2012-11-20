/*
 *  Copyright 2011 Hippo.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.configuration.channel;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.test.AbstractHstTestCase;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class ChannelPropertyMapperTest extends AbstractHstTestCase {

    public static interface TestInfo extends ChannelInfo{
        @Parameter(name = "test-name")
        String getName();
    }

    public static interface TestInfoInteger extends ChannelInfo {
        @Parameter(name = "test-integer")
        int getInteger();
    }

    public static interface TestInfoIntegerWithDefaultValue extends ChannelInfo {
        @Parameter(name = "test-integer", defaultValue = "4")
        int getInteger();
    }

    public static interface TestInfoDefaultValuesMissing extends ChannelInfo {
        @Parameter(name = "integer")
        int getInteger();
        @Parameter(name = "boolean")
        boolean getBoolean();
        @Parameter(name = "double")
        double getDouble();
        @Parameter(name = "calendar")
        Calendar getCalendar();
    }

    public static interface TestInfoDefaultValuesPresent extends ChannelInfo {
        @Parameter(name = "integer", defaultValue = "3")
        int getInteger();
        @Parameter(name = "boolean", defaultValue = "true")
        boolean getBoolean();
        @Parameter(name = "double", defaultValue = "3.4")
        double getDouble();
        @Parameter(name = "calendar", defaultValue = "0")
        Calendar getCalendar();
    }

    public static interface TestInfoDefaultValuesPresentButWrongFormat extends ChannelInfo {
        @Parameter(name = "integer", defaultValue = "aaa")
        int getInteger();
        @Parameter(name = "boolean", defaultValue = "bbb")
        boolean getBoolean();
        @Parameter(name = "double", defaultValue = "ccc")
        double getDouble();
        @Parameter(name = "calendar", defaultValue = "ddd")
        Calendar getCalendar();
    }

    public static interface TestInfoUnsupportedReturnType extends ChannelInfo {
        @Parameter(name = "test-bigdecimal")
        BigDecimal getBigDecimal();
    }


    @Before
    public void setUp() throws Exception {
        super.setUp();
        Node root = getSession().getRootNode();
        root.addNode("test", "nt:unstructured");
        getSession().save();
    }

    @Test
    public void simplePropertyIsLoaded() throws RepositoryException {
        List<HstPropertyDefinition> definitions = ChannelInfoClassProcessor.getProperties(TestInfo.class);

        HstPropertyDefinition nameDef = definitions.get(0);
        getSession().getNode("/test").setProperty("test-name", "aap");
        Map<HstPropertyDefinition, Object> values = ChannelPropertyMapper.loadProperties(getSession().getNode("/test"), definitions);
        assertTrue(values.containsKey(nameDef));
        Object value = values.get(nameDef);
        assertEquals("aap", value);
    }

    @Test
    public void unsetPropertyHasNullValue() throws RepositoryException {
        List<HstPropertyDefinition> definitions = ChannelInfoClassProcessor.getProperties(TestInfo.class);

        HstPropertyDefinition nameDef = definitions.get(0);
        Map<HstPropertyDefinition, Object> values = ChannelPropertyMapper.loadProperties(getSession().getNode("/test"), definitions);
        assertTrue(values.containsKey(nameDef));
        assertNull(values.get(nameDef));
    }

    @Test
    public void simplePropertyIsStoredWithOwnName() throws RepositoryException {
        List<HstPropertyDefinition> definitions = ChannelInfoClassProcessor.getProperties(TestInfo.class);
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("test-name", "aap");
        ChannelPropertyMapper.saveProperties(getSession().getNode("/test"), definitions, values);

        assertTrue(getSession().itemExists("/test/test-name"));
        Property nameProperty = (Property) getSession().getItem("/test/test-name");
        assertEquals("aap", nameProperty.getString());
    }

    @Test
    public void integerPropertyIsStored() throws RepositoryException {
        List<HstPropertyDefinition> definitions = ChannelInfoClassProcessor.getProperties(TestInfoInteger.class);
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("test-integer", 42);
        ChannelPropertyMapper.saveProperties(getSession().getNode("/test"), definitions, values);

        assertTrue(getSession().itemExists("/test/test-integer"));
        Property integerProperty = (Property) getSession().getItem("/test/test-integer");
        assertEquals(42, integerProperty.getLong());
    }

    @Test
    public void tryToStoreIncorrectIntegerProperty() throws RepositoryException {
        List<HstPropertyDefinition> definitions = ChannelInfoClassProcessor.getProperties(TestInfoInteger.class);
        Map<String, Object> values = new HashMap<String, Object>();
        // foo is not a correct integer
        values.put("test-integer", "foo");
        ChannelPropertyMapper.saveProperties(getSession().getNode("/test"), definitions, values);

        assertTrue(getSession().itemExists("/test/test-integer"));
        Property integerProperty = (Property) getSession().getItem("/test/test-integer");
        // we should get the default value for the integer which is 0 because not present in
        // @Parameter(name = "test-integer")
        assertEquals(0, integerProperty.getLong());
    }

    @Test
    public void testUnsupportedReturnType() {
        try {
            List<HstPropertyDefinition> definitions = ChannelInfoClassProcessor.getProperties(TestInfoUnsupportedReturnType.class);
            assertTrue(definitions.isEmpty());
        } catch (Throwable e) {
            fail("Unsupported return type should be ignored and not throw exception");
        }
    }

    @Test
    public void tryToStoreIncorrectIntegerPropertyButDefaultValuePresent() throws RepositoryException {
        List<HstPropertyDefinition> definitions = ChannelInfoClassProcessor.getProperties(TestInfoIntegerWithDefaultValue.class);
        Map<String, Object> values = new HashMap<String, Object>();
        // foo is not a correct integer
        values.put("test-integer", "foo");
        ChannelPropertyMapper.saveProperties(getSession().getNode("/test"), definitions, values);

        assertTrue(getSession().itemExists("/test/test-integer"));
        Property integerProperty = (Property) getSession().getItem("/test/test-integer");
        // we should get the default value for the integer which is 4 because of
        //  @Parameter(name = "test-integer", defaultValue = "4")
        assertEquals(4L, integerProperty.getLong());
    }
    
    @Test
    public void defaultValuesTests() throws RepositoryException {
        List<HstPropertyDefinition> defaultsMissing = ChannelInfoClassProcessor.getProperties(TestInfoDefaultValuesMissing.class);
        List<HstPropertyDefinition> defaultsPresent = ChannelInfoClassProcessor.getProperties(TestInfoDefaultValuesPresent.class);
        List<HstPropertyDefinition> defaultsWrongFormat = ChannelInfoClassProcessor.getProperties(TestInfoDefaultValuesPresentButWrongFormat.class);

        for (HstPropertyDefinition propDef : defaultsMissing) {
            if ("integer".equals(propDef.getName())){
                assertEquals("Default for integer should be 0", 0, propDef.getDefaultValue());
            } else if ("boolean".equals(propDef.getName())){
                assertTrue("Default for boolean should be of type Boolean", propDef.getDefaultValue().getClass().getName().equals(Boolean.class.getName()));
                assertFalse("Default for boolean should be false", (Boolean)propDef.getDefaultValue());
            } else if ("double".equals(propDef.getName())){
                assertEquals("Default for double should be 0", 0D, propDef.getDefaultValue());
            } else if ("calendar".equals(propDef.getName())){
                assertTrue("Default for calendar should instance of type Calendar", propDef.getDefaultValue() instanceof Calendar);
                long diffInMs = (System.currentTimeMillis() - ((Calendar)propDef.getDefaultValue()).getTimeInMillis());
                // the diffInMs is surely less than 1 minute as otherwise this unit test would have taken way to long
                assertTrue("Default time for calendar should be current time", diffInMs < 1000* 60 );
            } else {
                fail("propDef should be either integer, double, boolean or calendar");
            }
                 
        }

        for (HstPropertyDefinition propDef : defaultsPresent) {
            if ("integer".equals(propDef.getName())){
                // because the annotation has : @Parameter(name = "integer", defaultValue = "3")
                assertEquals( 3, propDef.getDefaultValue());
            } else if ("boolean".equals(propDef.getName())){
                // because the annotation has : @Parameter(name = "boolean", defaultValue = "true")
                assertTrue((Boolean)propDef.getDefaultValue());
            } else if ("double".equals(propDef.getName())){
                // because the annotation has : @Parameter(name = "double", defaultValue = "3.4")
                assertEquals(3.4D, (Double)propDef.getDefaultValue(), 0.0001);
            } else if ("calendar".equals(propDef.getName())){
                Calendar cal = Calendar.getInstance();
                // because the annotation has : @Parameter(name = "calendar", defaultValue = "0")
                cal.setTimeInMillis(0);
                assertEquals(cal, propDef.getDefaultValue());
            } else {
                fail("propDef should be either integer, double, boolean or calendar");
            }
        }

        for (HstPropertyDefinition propDef : defaultsWrongFormat) {
            if ("integer".equals(propDef.getName())){
                assertEquals("Wrong format and default for integer should be 0", 0, propDef.getDefaultValue());
            } else if ("boolean".equals(propDef.getName())){
                assertTrue("Wrong format and default for boolean should be of type Boolean", propDef.getDefaultValue().getClass().getName().equals(Boolean.class.getName()));
                assertFalse("Wrong format and default for boolean should be false", (Boolean)propDef.getDefaultValue());
            } else if ("double".equals(propDef.getName())){
                assertEquals("Wrong format and default for double should be 0", 0D, propDef.getDefaultValue());
            } else if ("calendar".equals(propDef.getName())){
                assertTrue("Wrong format and default for calendar should instance of type Calendar", propDef.getDefaultValue() instanceof Calendar);
                long diffInMs = (System.currentTimeMillis() - ((Calendar)propDef.getDefaultValue()).getTimeInMillis());
                // the diffInMs is surely less than 1 minute as otherwise this unit test would have taken way to long
                assertTrue("Wrong format and default time for calendar should be current time", diffInMs < 1000* 60 );
            } else {
                fail("propDef should be either integer, double, boolean or calendar");
            }
        }

    }

}
