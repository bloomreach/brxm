/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.hippoecm.hst.container.event.HttpSessionCreatedEvent;
import org.hippoecm.hst.container.event.HttpSessionDestroyedEvent;
import org.hippoecm.hst.core.container.ComponentsException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;

public class TestSpringComponentManager {
    
    private static final String SIMPLE_BEANS_1 = "/META-INF/assembly/simple-beans-1.xml";
    private static final String SIMPLE_BEANS_2 = "/META-INF/assembly/simple-beans-2.xml";
    private static final String SIMPLE_BEANS_3 = "/META-INF/assembly/simple-beans-3.xml";
    private static final String SIMPLE_BEANS_4 = "/META-INF/assembly/simple-beans-4.xml";
    private static final String NON_EXISTING_BEANS = "/META-INF/assembly/non-existing-ones/*.xml";
    private static final String SIMPLE_BEANS_WITH_LISTENERS = "/META-INF/assembly/simple-beans-with-listeners.xml";
    private static final String SIMPLE_SERVLET_CONTEXT_AWARE_BEANS = "/META-INF/assembly/simple-servlet-context-aware-beans.xml";
    
    private static Logger log = LoggerFactory.getLogger(TestSpringComponentManager.class);
    
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
        System.clearProperty("props.greeting");
        System.clearProperty("props.greeting.nonexisting");
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
    public void testGetComponentByType() {
        Configuration configuration = new PropertiesConfiguration();
        configuration.setProperty("existing.key", "some value");
        SpringComponentManager componentManager = new SpringComponentManager(configuration);
        String [] configurationResources = new String [] { SIMPLE_BEANS_4 };
        componentManager.setConfigurationResources(configurationResources);

        componentManager.initialize();
        componentManager.start();

        SingleBean singleBean = componentManager.getComponent(SingleBean.class);
        assertNotNull(singleBean);

        try {
            String aGreeting = componentManager.getComponent(String.class);
            fail("Because there are multiple String beans, it must not be successful.");
        } catch (ComponentsException e) {
            // as expected.
        }

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

    @Test
    public void testHttpSessionApplicationEventListeners() {
        Configuration configuration = new PropertiesConfiguration();
        SpringComponentManager componentManager = new SpringComponentManager(configuration);
        String[] configurationResources = new String[] { SIMPLE_BEANS_WITH_LISTENERS };
        componentManager.setConfigurationResources(configurationResources);

        componentManager.initialize();
        componentManager.start();

        String greetingTemplate = componentManager.getComponent("greetingTemplate");
        assertNotNull(greetingTemplate);

        List<String> sessionIdStore = componentManager.getComponent("sessionIdStore");
        assertNotNull(sessionIdStore);
        assertEquals(0, sessionIdStore.size());

        List<MockHttpSession> sessionList = new ArrayList<MockHttpSession>();

        for (int i = 0; i < 10; i++) {
            sessionList.add(new MockHttpSession());
        }

        int sessionCount = 0;

        for (MockHttpSession session : sessionList) {
            componentManager.publishEvent(new HttpSessionCreatedEvent(session));
            ++sessionCount;

            assertEquals(sessionCount, sessionIdStore.size());
            assertTrue(sessionIdStore.contains(session.getId()));
        }

        log.debug("sessionIdStore: {}", sessionIdStore);

        for (MockHttpSession session : sessionList) {
            componentManager.publishEvent(new HttpSessionDestroyedEvent(session));
            --sessionCount;

            assertEquals(sessionCount, sessionIdStore.size());
            assertFalse(sessionIdStore.contains(session.getId()));
        }

        log.debug("sessionIdStore: {}", sessionIdStore);

        assertEquals(0, sessionIdStore.size());

        componentManager.stop();
        componentManager.close();
    }

    @Test
    public void testServletContextAwareBeans() {
        ServletContext servletContext = new MockServletContext();
        SpringComponentManager componentManager = new SpringComponentManager(servletContext, new PropertiesConfiguration());
        String [] configurationResources = new String [] { SIMPLE_SERVLET_CONTEXT_AWARE_BEANS };
        componentManager.setConfigurationResources(configurationResources);

        componentManager.initialize();
        componentManager.start();

        MyServletContextAwareSpringBean servletContextAwareBean =  componentManager.getComponent("servletContextAwareBean");
        assertTrue(servletContextAwareBean.getServletContext() == servletContext);

        componentManager.stop();
        componentManager.close();
    }



}
