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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.hippoecm.hst.site.addon.module.model.ModuleDefinition;
import org.junit.Test;

public class TestSpringComponentManagerWithAddonModules {
    
    private static final String WITH_ADDON_MODULES = "/META-INF/assembly/with-addon-modules.xml";
    
    @Test
    public void testWithModuleDefinitions() throws Exception {
        Configuration configuration = new PropertiesConfiguration(getClass().getResource("/META-INF/assembly/with-addon-modules-config.properties"));
        SpringComponentManager componentManager = new SpringComponentManager(configuration);
        componentManager.setConfigurationResources(new String [] { WITH_ADDON_MODULES });
        
        // addon module definitions
        List<ModuleDefinition> addonModuleDefs = new ArrayList<ModuleDefinition>();
        
        // build and add analytics module definition
        ModuleDefinition def1 = new ModuleDefinition();
        def1.setName("org.example.analytics");
        def1.setConfigLocations(Arrays.asList("classpath*:META-INF/hst-assembly/addon/org/example/analytics/*.xml"));
        // build and add analytics/reports module definition
        ModuleDefinition def11 = new ModuleDefinition();
        def11.setName("reports");
        def11.setConfigLocations(Arrays.asList("classpath*:META-INF/hst-assembly/addon/org/example/analytics/reports/*.xml"));
        // build and add analytics/statistics module definition
        ModuleDefinition def12 = new ModuleDefinition();
        def12.setName("statistics");
        def12.setConfigLocations(Arrays.asList("classpath*:META-INF/hst-assembly/addon/org/example/analytics/statistics/*.xml"));
        def1.setModuleDefinitions(Arrays.asList(def11, def12));
        addonModuleDefs.add(def1);
        
        // build and add analytics2 module definition 
        ModuleDefinition def2 = new ModuleDefinition();
        def2.setName("org.example.analytics2");
        def2.setConfigLocations(Arrays.asList("classpath*:META-INF/hst-assembly/addon/org/example/analytics2/*.xml"));
        // build and add analytics2/reports module definition
        ModuleDefinition def21 = new ModuleDefinition();
        def21.setName("reports");
        def21.setConfigLocations(Arrays.asList("classpath*:META-INF/hst-assembly/addon/org/example/analytics2/reports/*.xml"));
        // build and add analytics2/statistics module definition
        ModuleDefinition def22 = new ModuleDefinition();
        def22.setName("statistics");
        def22.setConfigLocations(Arrays.asList("classpath*:META-INF/hst-assembly/addon/org/example/analytics2/statistics/*.xml"));
        def2.setModuleDefinitions(Arrays.asList(def21, def22));
        addonModuleDefs.add(def2);
        
        componentManager.setAddonModuleDefinitions(addonModuleDefs);
        
        componentManager.initialize();
        componentManager.start();
        
        // from the component manager spring assemblies...
        assertEquals("[ComponentManager] Hello from with-addon-modules-config.properties", 
                componentManager.getComponent("myGreeting"));
        assertEquals("Hello from container", 
                componentManager.getComponent("containerGreeting"));
        
        // from the analytics module...
        assertEquals("[Analytics] Hello from with-addon-modules-config.properties", 
                componentManager.getComponent("myGreeting", "org.example.analytics"));
        assertEquals("Hello from analytics", 
                componentManager.getComponent("analyticsGreeting", "org.example.analytics"));
        assertNull(componentManager.getComponent("analytics2Greeting", "org.example.analytics"));
        assertEquals("Hello from container", 
                componentManager.getComponent("containerGreeting", "org.example.analytics"));
        List<String> greetingList = componentManager.getComponent("greetingList", "org.example.analytics");
        assertNotNull(greetingList);
        assertEquals(2, greetingList.size());
        assertEquals("Hello from container", greetingList.get(0));
        assertEquals("Hello from analytics", greetingList.get(1));
        
        // from the analytics/reports module...
        assertEquals("[Analytics Reports] Hello from with-addon-modules-config.properties", 
                componentManager.getComponent("myGreeting", "org.example.analytics", "reports"));
        assertEquals("Hello from analytics", 
                componentManager.getComponent("analyticsGreeting", "org.example.analytics", "reports"));
        assertNull(componentManager.getComponent("analytics2Greeting", "org.example.analytics", "reports"));
        assertEquals("Hello from analytics reports", 
                componentManager.getComponent("analyticsReportsGreeting", "org.example.analytics", "reports"));
        assertNull(componentManager.getComponent("analytics2ReportsGreeting", "org.example.analytics", "reports"));
        assertEquals("Hello from container", 
                componentManager.getComponent("containerGreeting", "org.example.analytics", "reports"));
        greetingList = componentManager.getComponent("greetingList", "org.example.analytics", "reports");
        assertNotNull(greetingList);
        assertEquals(3, greetingList.size());
        assertEquals("Hello from container", greetingList.get(0));
        assertEquals("Hello from analytics", greetingList.get(1));
        assertEquals("Hello from analytics reports", greetingList.get(2));

        // from the analytics/statistics module...
        assertEquals("[Analytics Statistics] Hello from with-addon-modules-config.properties", 
                componentManager.getComponent("myGreeting", "org.example.analytics", "statistics"));
        assertEquals("Hello from analytics", 
                componentManager.getComponent("analyticsGreeting", "org.example.analytics", "statistics"));
        assertNull(componentManager.getComponent("analytics2Greeting", "org.example.analytics", "statistics"));
        assertEquals("Hello from analytics statistics", 
                componentManager.getComponent("analyticsStatisticsGreeting", "org.example.analytics", "statistics"));
        assertNull(componentManager.getComponent("analytics2StatisticsGreeting", "org.example.analytics", "statistics"));
        assertEquals("Hello from container", 
                componentManager.getComponent("containerGreeting", "org.example.analytics", "statistics"));
        greetingList = componentManager.getComponent("greetingList", "org.example.analytics", "statistics");
        assertNotNull(greetingList);
        assertEquals(3, greetingList.size());
        assertEquals("Hello from container", greetingList.get(0));
        assertEquals("Hello from analytics", greetingList.get(1));
        assertEquals("Hello from analytics statistics", greetingList.get(2));

        // from the analytics2 module...
        assertEquals("[Analytics2] Hello from with-addon-modules-config.properties", 
                componentManager.getComponent("myGreeting", "org.example.analytics2"));
        assertEquals("Hello from analytics2", 
                componentManager.getComponent("analytics2Greeting", "org.example.analytics2"));
        assertNull(componentManager.getComponent("analyticsGreeting", "org.example.analytics2"));
        assertEquals("Hello from container", 
                componentManager.getComponent("containerGreeting", "org.example.analytics2"));
        greetingList = componentManager.getComponent("greetingList", "org.example.analytics2");
        assertNotNull(greetingList);
        assertEquals(2, greetingList.size());
        assertEquals("Hello from container", greetingList.get(0));
        assertEquals("Hello from analytics2", greetingList.get(1));

        // from the analytics2/reports module...
        assertEquals("[Analytics2 Reports] Hello from with-addon-modules-config.properties", 
                componentManager.getComponent("myGreeting", "org.example.analytics2", "reports"));
        assertEquals("Hello from analytics2", 
                componentManager.getComponent("analytics2Greeting", "org.example.analytics2", "reports"));
        assertNull(componentManager.getComponent("analyticsGreeting", "org.example.analytics2", "reports"));
        assertEquals("Hello from analytics2 reports", 
                componentManager.getComponent("analytics2ReportsGreeting", "org.example.analytics2", "reports"));
        assertNull(componentManager.getComponent("analyticsReportsGreeting", "org.example.analytics2", "reports"));
        assertEquals("Hello from container", 
                componentManager.getComponent("containerGreeting", "org.example.analytics2", "reports"));
        greetingList = componentManager.getComponent("greetingList", "org.example.analytics2", "reports");
        assertNotNull(greetingList);
        assertEquals(3, greetingList.size());
        assertEquals("Hello from container", greetingList.get(0));
        assertEquals("Hello from analytics2", greetingList.get(1));
        assertEquals("Hello from analytics2 reports", greetingList.get(2));

        // from the analytics2/statistics module...
        assertEquals("[Analytics2 Statistics] Hello from with-addon-modules-config.properties", 
                componentManager.getComponent("myGreeting", "org.example.analytics2", "statistics"));
        assertEquals("Hello from analytics2", 
                componentManager.getComponent("analytics2Greeting", "org.example.analytics2", "statistics"));
        assertNull(componentManager.getComponent("analyticsGreeting", "org.example.analytics2", "statistics"));
        assertEquals("Hello from analytics2 statistics", 
                componentManager.getComponent("analytics2StatisticsGreeting", "org.example.analytics2", "statistics"));
        assertNull(componentManager.getComponent("analyticsStatisticsGreeting", "org.example.analytics2", "statistics"));
        assertEquals("Hello from container", 
                componentManager.getComponent("containerGreeting", "org.example.analytics2", "statistics"));
        greetingList = componentManager.getComponent("greetingList", "org.example.analytics2", "statistics");
        assertNotNull(greetingList);
        assertEquals(3, greetingList.size());
        assertEquals("Hello from container", greetingList.get(0));
        assertEquals("Hello from analytics2", greetingList.get(1));
        assertEquals("Hello from analytics2 statistics", greetingList.get(2));

        componentManager.stop();
        componentManager.close();
    }
}
