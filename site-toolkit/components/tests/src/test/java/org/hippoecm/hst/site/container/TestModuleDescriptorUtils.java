/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.site.addon.module.model.ModuleDefinition;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TestModuleDescriptorUtils {
    
    @Test
    public void testDefaultLoading() throws Exception {
        List<ModuleDefinition> moduleDefs = ModuleDescriptorUtils.collectAllModuleDefinitions();
        
        assertNotNull(moduleDefs);
        assertFalse(moduleDefs.isEmpty());

        ModuleDefinition moduleDef = findModuleDefinitionByName(moduleDefs, "org.example.analytics");
        assertNotNull(moduleDef);
        assertEquals("org.example.analytics", moduleDef.getName());
        
        List<String> configLocations = moduleDef.getConfigLocations();
        assertNotNull(configLocations);
        assertEquals(1, configLocations.size());
        assertEquals("classpath*:META-INF/hst-assembly/addon/org/example/analytics/*.xml",
                configLocations.get(0));
        
        List<ModuleDefinition> children = moduleDef.getModuleDefinitions();
        assertNotNull(children);
        assertFalse(children.isEmpty());
        
        moduleDef = findModuleDefinitionByName(children, "reports");
        assertNotNull(moduleDef);
        assertEquals("reports", moduleDef.getName());
        
        configLocations = moduleDef.getConfigLocations();
        assertNotNull(configLocations);
        assertEquals(1, configLocations.size());
        assertEquals("classpath*:META-INF/hst-assembly/addon/org/example/analytics/reports/*.xml",
                configLocations.get(0));
        
        moduleDef = findModuleDefinitionByName(children, "statistics");
        assertNotNull(moduleDef);
        assertEquals("statistics", moduleDef.getName());
        
        configLocations = moduleDef.getConfigLocations();
        assertNotNull(configLocations);
        assertEquals(1, configLocations.size());
        assertEquals("classpath*:META-INF/hst-assembly/addon/org/example/analytics/statistics/*.xml",
                configLocations.get(0));

    }

    @Test
    public void testModuleWithParent() throws Exception {

        List<ModuleDefinition> moduleDefs =
                ModuleDescriptorUtils.collectAllModuleDefinitions(Thread.currentThread().getContextClassLoader(), "META-INF/hst-assembly/addon/module-with-parent.xml");
        ModuleDefinition moduleDef = findModuleDefinitionByName(moduleDefs, "org.example.analytics.with.parent");
        assertEquals("org.example.analytics.with.parent", moduleDef.getName());
        assertEquals("org.example.analytics", moduleDef.getParent());
    }

    @Test
    public void test_module_envelope_for_actual_modules() throws Exception {
        List<ModuleDefinition> moduleDefs =
                ModuleDescriptorUtils.collectAllModuleDefinitions(Thread.currentThread().getContextClassLoader(), "META-INF/hst-assembly/addon/module-envelope-for-actual-modules.xml");
        assertEquals("Expected 4 modules since 2 <module> elements are only used as envelope", 4, moduleDefs.size());


        {
            ModuleDefinition moduleDef = findModuleDefinitionByName(moduleDefs, "org.example.noparent.reports");
            assertNull(moduleDef.getParent());
            assertNull(moduleDef.getModuleDefinitions());
        }
        {
            ModuleDefinition moduleDef = findModuleDefinitionByName(moduleDefs, "org.example.analytics1.reports");
            assertEquals("org.example.analytics1", moduleDef.getParent());
            assertNull(moduleDef.getModuleDefinitions());
        }
        {
            ModuleDefinition moduleDef = findModuleDefinitionByName(moduleDefs, "org.example.analytics2.statistics");
            assertEquals("org.example.analytics2", moduleDef.getParent());
            assertNull(moduleDef.getModuleDefinitions());
        }
        {
            ModuleDefinition moduleDef = findModuleDefinitionByName(moduleDefs, "org.example.analytics3.statistics");
            assertEquals("org.example.analytics3", moduleDef.getParent());
            assertEquals(2, moduleDef.getModuleDefinitions().size());
            assertNotNull(findModuleDefinitionByName(moduleDef.getModuleDefinitions(), "sub1"));
            assertNotNull(findModuleDefinitionByName(moduleDef.getModuleDefinitions(), "sub2"));
        }

    }

    @Test
    public void testMultiResourcePathsLoading() throws Exception {
        List<ModuleDefinition> moduleDefs = 
            ModuleDescriptorUtils.collectAllModuleDefinitions(getClass().getClassLoader(), 
                    ContainerConstants.DEFAULT_ADDON_MODULE_DESCRIPTOR_PATHS,
                    "META-INF/hst-assembly/addon/module2.xml");
        
        // from module.xml
        
        assertNotNull(moduleDefs);
        assertFalse(moduleDefs.isEmpty());
        
        ModuleDefinition moduleDef = findModuleDefinitionByName(moduleDefs, "org.example.analytics");
        assertNotNull(moduleDef);
        assertEquals("org.example.analytics", moduleDef.getName());
        
        List<String> configLocations = moduleDef.getConfigLocations();
        assertNotNull(configLocations);
        assertEquals(1, configLocations.size());
        assertEquals("classpath*:META-INF/hst-assembly/addon/org/example/analytics/*.xml",
                configLocations.get(0));
        
        List<ModuleDefinition> children = moduleDef.getModuleDefinitions();
        assertNotNull(children);
        assertFalse(children.isEmpty());
        
        moduleDef = findModuleDefinitionByName(children, "reports");
        assertNotNull(moduleDef);
        assertEquals("reports", moduleDef.getName());
        
        configLocations = moduleDef.getConfigLocations();
        assertNotNull(configLocations);
        assertEquals(1, configLocations.size());
        assertEquals("classpath*:META-INF/hst-assembly/addon/org/example/analytics/reports/*.xml",
                configLocations.get(0));
        
        moduleDef = findModuleDefinitionByName(children, "statistics");
        assertNotNull(moduleDef);
        assertEquals("statistics", moduleDef.getName());
        
        configLocations = moduleDef.getConfigLocations();
        assertNotNull(configLocations);
        assertEquals(1, configLocations.size());
        assertEquals("classpath*:META-INF/hst-assembly/addon/org/example/analytics/statistics/*.xml",
                configLocations.get(0));

        
        // from module2.xml
        
        moduleDef = findModuleDefinitionByName(moduleDefs, "org.example.analytics2");
        assertNotNull(moduleDef);
        assertEquals("org.example.analytics2", moduleDef.getName());
        
        configLocations = moduleDef.getConfigLocations();
        assertNotNull(configLocations);
        assertEquals(1, configLocations.size());
        assertEquals("classpath*:META-INF/hst-assembly/addon/org/example/analytics2/*.xml",
                configLocations.get(0));
        
        children = moduleDef.getModuleDefinitions();
        assertNotNull(children);
        assertFalse(children.isEmpty());
        
        moduleDef = findModuleDefinitionByName(children, "reports");
        assertNotNull(moduleDef);
        assertEquals("reports", moduleDef.getName());
        
        configLocations = moduleDef.getConfigLocations();
        assertNotNull(configLocations);
        assertEquals(1, configLocations.size());
        assertEquals("classpath*:META-INF/hst-assembly/addon/org/example/analytics2/reports/*.xml",
                configLocations.get(0));
        
        moduleDef = findModuleDefinitionByName(children, "statistics");
        assertNotNull(moduleDef);
        assertEquals("statistics", moduleDef.getName());
        
        configLocations = moduleDef.getConfigLocations();
        assertNotNull(configLocations);
        assertEquals(1, configLocations.size());
        assertEquals("classpath*:META-INF/hst-assembly/addon/org/example/analytics2/statistics/*.xml",
                configLocations.get(0));
    }
    
    private static ModuleDefinition findModuleDefinitionByName(List<ModuleDefinition> moduleDefs, String name) {
        for (ModuleDefinition moduleDef : moduleDefs) {
            if (StringUtils.equals(name, moduleDef.getName())) {
                return moduleDef;
            }
        }
        return null;
    }
}
