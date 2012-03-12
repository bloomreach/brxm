/*
 *  Copyright 2012 Hippo.
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

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

import java.util.TreeMap;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.component.GenericHstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.mock.configuration.components.MockHstComponentConfiguration;
import org.hippoecm.hst.mock.core.request.MockHstRequestContext;
import org.junit.Test;

public class TestComponentWindowCreationFilter {

    @Test
    public void testNoFilter() {
        HstComponentWindowFactoryImpl factory = new HstComponentWindowFactoryImpl();

        // set up request context
        MockHstRequestContext requestContext = new MockHstRequestContext();
        
       
        // mock environment
        HstContainerConfig mockHstContainerConfig = createNiceMock(HstContainerConfig.class);
        HstComponentConfiguration compConfig = createNiceMock(HstComponentConfiguration.class);
        expect(compConfig.getReferenceName()).andReturn("refName").anyTimes();
        HstComponentFactory compFactory = createNiceMock(HstComponentFactory.class);
        expect(compFactory.getComponentInstance(mockHstContainerConfig, compConfig)).andReturn(new GenericHstComponent());

        // container items with matching, non-matching and no HstComponentWindowCreationFilter
        TreeMap<String, HstComponentConfiguration> children = getContainerItemConfigurations();
        expect(compConfig.getChildren()).andReturn(children);

        // instantiate the window
        replay(mockHstContainerConfig, compConfig, compFactory);
        HstComponentWindow window = factory.create(mockHstContainerConfig, requestContext, compConfig, compFactory);

        // verify results
        verify(mockHstContainerConfig, compConfig, compFactory);
        assertNotNull(window.getChildWindow("comp1"));
        assertNotNull(window.getChildWindow("comp2"));
    }
    
    @Test
    public void testSimpleWindowCreationFilter() {
        HstComponentWindowFactoryImpl factory = new HstComponentWindowFactoryImpl();

        // set up request context
        MockHstRequestContext requestContext = new MockHstRequestContext();
        requestContext.addComponentWindowCreationFilters(new HstComponentWindowCreationFilter() {
            @Override
            public boolean skipComponentWindow(HstRequestContext requestContext, HstComponentConfiguration compConfig)
                    throws HstComponentException {
                if(compConfig.getName().equals("comp1")) {
                    return true;
                }
                return false;
            }
        });
        
        // mock environment
        HstContainerConfig mockHstContainerConfig = createNiceMock(HstContainerConfig.class);
        HstComponentConfiguration compConfig = createNiceMock(HstComponentConfiguration.class);
        expect(compConfig.getReferenceName()).andReturn("refName");
        HstComponentFactory compFactory = createNiceMock(HstComponentFactory.class);
        expect(compFactory.getComponentInstance(mockHstContainerConfig, compConfig)).andReturn(new GenericHstComponent());

        // container items with matching, non-matching and no HstComponentWindowCreationFilter
        TreeMap<String, HstComponentConfiguration> children = getContainerItemConfigurations();
        expect(compConfig.getChildren()).andReturn(children);

        // instantiate the window
        replay(mockHstContainerConfig, compConfig, compFactory);
        HstComponentWindow window = factory.create(mockHstContainerConfig, requestContext, compConfig, compFactory);

        // verify results
        verify(mockHstContainerConfig, compConfig, compFactory);
        // since comp1 is filtered, it should be null
        assertNull(window.getChildWindow("comp1"));
        assertNotNull(window.getChildWindow("comp2"));
        
        
    }

    
    private TreeMap<String, HstComponentConfiguration> getContainerItemConfigurations() {
        TreeMap<String, HstComponentConfiguration> children = new TreeMap<String, HstComponentConfiguration>();
        MockHstComponentConfiguration comp1 = new MockHstComponentConfiguration("comp1");
        comp1.setName("comp1");
        children.put("comp1", comp1);
        MockHstComponentConfiguration comp2 = new MockHstComponentConfiguration("comp2");
        comp2.setName("comp2");
        children.put("comp2", comp2);
        return children;
    }

}
