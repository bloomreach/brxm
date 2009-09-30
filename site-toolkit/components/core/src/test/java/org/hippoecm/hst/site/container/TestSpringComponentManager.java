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

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class TestSpringComponentManager {
    
    private static final String SIMPLE_BEANS_1 = "/META-INF/assembly/simple-beans-1.xml";
    private static final String SIMPLE_BEANS_2 = "/META-INF/assembly/simple-beans-2.xml";
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
    
}
