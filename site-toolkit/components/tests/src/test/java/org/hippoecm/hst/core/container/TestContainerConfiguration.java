/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.container;


import org.apache.commons.configuration.ConversionException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TestContainerConfiguration {

    @Test
    public void existing_property() {
        PropertiesConfiguration configuration = new PropertiesConfiguration();
        configuration.setProperty("foo", "bar");
        ContainerConfiguration containerConfiguration = new ContainerConfigurationImpl(configuration);
        assertEquals("bar", containerConfiguration.getString("foo"));
    }

    @Test(expected = ConversionException.class)
    public void existing_property_wrong_type_cause_not_boolean() {
        PropertiesConfiguration configuration = new PropertiesConfiguration();
        configuration.setProperty("foo", "bar");
        ContainerConfiguration containerConfiguration = new ContainerConfigurationImpl(configuration);
        containerConfiguration.getBoolean("foo");
    }

    @Test(expected = ConversionException.class)
    public void existing_property_wrong_type_cause_not_string() {
        PropertiesConfiguration configuration = new PropertiesConfiguration();
        configuration.setProperty("foo", true);
        ContainerConfiguration containerConfiguration = new ContainerConfigurationImpl(configuration);
        containerConfiguration.getString("foo");
    }

    @Test
    public void existing_property_string_value_boolean() {
        PropertiesConfiguration configuration = new PropertiesConfiguration();
        configuration.setProperty("foo", "true");
        ContainerConfiguration containerConfiguration = new ContainerConfigurationImpl(configuration);
        assertEquals(containerConfiguration.getBoolean("foo"), true);
    }

    @Test
    public void missing_property_for_string() {
        PropertiesConfiguration configuration = new PropertiesConfiguration();
        ContainerConfiguration containerConfiguration = new ContainerConfigurationImpl(configuration);
        String missing = containerConfiguration.getString("foo");
        assertNull(missing);
    }

    @Test
    public void missing_property_for_boolean() {
        PropertiesConfiguration configuration = new PropertiesConfiguration();
        ContainerConfiguration containerConfiguration = new ContainerConfigurationImpl(configuration);
        assertFalse(containerConfiguration.getBoolean("foo"));
    }

    @Test
    public void missing_property_for_double() {
        PropertiesConfiguration configuration = new PropertiesConfiguration();
        ContainerConfiguration containerConfiguration = new ContainerConfigurationImpl(configuration);
        assertEquals(0D, containerConfiguration.getDouble("foo"), 0.0001D);
    }

    @Test
    public void missing_property_default_value_string() {
        PropertiesConfiguration configuration = new PropertiesConfiguration();
        ContainerConfiguration containerConfiguration = new ContainerConfigurationImpl(configuration);
        assertEquals(containerConfiguration.getString("foo", "bar"), "bar");
    }

    @Test
    public void missing_property_default_value_boolean() {
        PropertiesConfiguration configuration = new PropertiesConfiguration();
        ContainerConfiguration containerConfiguration = new ContainerConfigurationImpl(configuration);
        assertTrue(containerConfiguration.getBoolean("foo", true));
    }

    @Test
    public void missing_property_default_value_double() {
        PropertiesConfiguration configuration = new PropertiesConfiguration();
        ContainerConfiguration containerConfiguration = new ContainerConfigurationImpl(configuration);
        assertEquals(containerConfiguration.getDouble("foo", 1D), 1D, 0.00001);
    }

    @Test
    public void existing_property_boolean_default_value_string() {
        PropertiesConfiguration configuration = new PropertiesConfiguration();
        configuration.setProperty("foo", true);
        ContainerConfiguration containerConfiguration = new ContainerConfigurationImpl(configuration);
        assertEquals("default", containerConfiguration.getString("foo", "default"));
    }

    @Test
    public void existing_property_Boolean_default_value_string() {
        PropertiesConfiguration configuration = new PropertiesConfiguration();
        configuration.setProperty("foo", Boolean.TRUE);
        ContainerConfiguration containerConfiguration = new ContainerConfigurationImpl(configuration);
        assertEquals("default", containerConfiguration.getString("foo", "default"));
    }

    @Test
    public void existing_property_string_default_value_boolean() {
        PropertiesConfiguration configuration = new PropertiesConfiguration();
        configuration.setProperty("foo", "bar");
        ContainerConfiguration containerConfiguration = new ContainerConfigurationImpl(configuration);
        assertTrue(containerConfiguration.getBoolean("foo", true));
        assertFalse(containerConfiguration.getBoolean("foo", false));
    }

    @Test
    public void existing_property_string_default_value_Boolean() {
        PropertiesConfiguration configuration = new PropertiesConfiguration();
        configuration.setProperty("foo", "bar");
        ContainerConfiguration containerConfiguration = new ContainerConfigurationImpl(configuration);
        assertTrue(containerConfiguration.getBoolean("foo", Boolean.TRUE));
        assertFalse(containerConfiguration.getBoolean("foo", Boolean.FALSE));
    }

}
