/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.site.container;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Test;

public class TestSpringComponentManager {
    
    private static final String SIMPLE_BEANS_1 = "/META-INF/assembly/simple-beans-1.xml";
    private static final String SIMPLE_BEANS_2 = "/META-INF/assembly/simple-beans-2.xml";
    private static final String SIMPLE_BEANS_3 = "/META-INF/assembly/simple-beans-3.xml";
    private static final String SIMPLE_BEANS_4 = "/META-INF/assembly/simple-beans-4.xml";
    private static final String NON_EXISTING_BEANS = "/META-INF/assembly/non-existing-ones/*.xml";
    
    @Test
    public void testSimpleBeans() {
        SpringComponentManager componentManager = new SpringComponentManager();
        String [] configurationResources = new String [] { SIMPLE_BEANS_1 };
        componentManager.setConfigurationResources(configurationResources);
        
        componentManager.initialize();
        componentManager.start();
        
        assertEquals("Hello, One World!", componentManager.getComponent("greeting"));
        
        componentManager.stop();
        componentManager.close();
    }
    
    @Test
    public void testOverridingBeans() {
        SpringComponentManager componentManager = new SpringComponentManager();
        String [] configurationResources = new String [] { SIMPLE_BEANS_1, SIMPLE_BEANS_2, NON_EXISTING_BEANS };
        componentManager.setConfigurationResources(configurationResources);
        
        componentManager.initialize();
        componentManager.start();
        
        assertEquals("Hello, Two Worlds!", componentManager.getComponent("greeting"));
        
        componentManager.stop();
        componentManager.close();
    }
    
    @Test
    public void testProperties() {
        SpringComponentManager componentManager = new SpringComponentManager();
        String [] configurationResources = new String [] { SIMPLE_BEANS_3 };
        componentManager.setConfigurationResources(configurationResources);
        
        componentManager.initialize();
        componentManager.start();
        
        Map<String, String> greetingMap = componentManager.getComponent("greetingMap");
        
        assertEquals("Hello from the property file!", greetingMap.get("props.greeting"));
        assertEquals("${props.greeting.nonexisting}", greetingMap.get("props.greeting.nonexisting"));
        
        componentManager.stop();
        componentManager.close();
    }
    
    @Test
    public void testConfigurationAndProperties() {
        Configuration configuration = new PropertiesConfiguration();
        configuration.setProperty("props.greeting", "Hello from the configuration!");
        SpringComponentManager componentManager = new SpringComponentManager(configuration);
        String [] configurationResources = new String [] { SIMPLE_BEANS_3 };
        componentManager.setConfigurationResources(configurationResources);
        
        componentManager.initialize();
        componentManager.start();
        
        Map<String, String> greetingMap = componentManager.getComponent("greetingMap");
        
        assertEquals("Hello from the configuration!", greetingMap.get("props.greeting"));
        assertEquals("${props.greeting.nonexisting}", greetingMap.get("props.greeting.nonexisting"));
        
        componentManager.stop();
        componentManager.close();
    }

    @Test
    public void testSystemPropertiesFallback() {
        System.setProperty("props.greeting", "Hello from the system props!");
        System.setProperty("props.greeting.nonexisting", "Hello from the system props!");
        SpringComponentManager componentManager = new SpringComponentManager();
        String [] configurationResources = new String [] { SIMPLE_BEANS_3 };
        componentManager.setConfigurationResources(configurationResources);
        
        componentManager.initialize();
        componentManager.start();
        
        Map<String, String> greetingMap = componentManager.getComponent("greetingMap");
        
        assertEquals("Hello from the property file!", greetingMap.get("props.greeting"));
        assertEquals("Hello from the system props!", greetingMap.get("props.greeting.nonexisting"));
        
        componentManager.stop();
        componentManager.close();
    }
    
    @Test
    public void testConditionalBeanRegistration() {
        Configuration configuration = new PropertiesConfiguration();
        configuration.setProperty("existing.key", "some value");
        SpringComponentManager componentManager = new SpringComponentManager(configuration);
        String [] configurationResources = new String [] { SIMPLE_BEANS_4 };
        componentManager.setConfigurationResources(configurationResources);
        
        componentManager.initialize();
        componentManager.start();
        
        assertNotNull(componentManager.getComponent("greeting1"));
        assertNull(componentManager.getComponent("greeting2"));
        assertNull(componentManager.getComponent("greeting3"));
        assertNotNull(componentManager.getComponent("greeting4"));
        assertNotNull(componentManager.getComponent("greeting5"));
        
        componentManager.stop();
        componentManager.close();
    }

    @Test
    public void testGetComponentsOfType() {
        Configuration configuration = new PropertiesConfiguration();
        configuration.setProperty("existing.key", "some value");
        SpringComponentManager componentManager = new SpringComponentManager(configuration);
        String [] configurationResources = new String [] { SIMPLE_BEANS_4 };
        componentManager.setConfigurationResources(configurationResources);
        
        componentManager.initialize();
        componentManager.start();
        
        Map<String, StringBuilder> exampleStringBuildersMap = componentManager.getComponentsOfType(StringBuilder.class);
        assertNotNull(exampleStringBuildersMap);
        assertFalse(exampleStringBuildersMap.isEmpty());
        assertEquals(2, exampleStringBuildersMap.size());
        assertNotNull(exampleStringBuildersMap.get("exampleStringBuilder1"));
        assertNotNull(exampleStringBuildersMap.get("exampleStringBuilder2"));
        assertEquals("Hello, World! (1)", exampleStringBuildersMap.get("exampleStringBuilder1").toString());
        assertEquals("Hello, World! (2)", exampleStringBuildersMap.get("exampleStringBuilder2").toString());
        
        componentManager.stop();
        componentManager.close();
    }
}
