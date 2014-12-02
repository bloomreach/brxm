/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.component.GenericHstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.mock.configuration.components.MockHstComponentConfiguration;
import org.junit.Test;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TestComponentWindowFilter {

    @Test
    public void testNoFilter() {
        HstComponentWindowFactoryImpl factory = new HstComponentWindowFactoryImpl();

        Mount mount = createNiceMock(Mount.class);
        ResolvedMount resolvedMount = createNiceMock(ResolvedMount.class);
        expect(resolvedMount.getMount()).andReturn(mount).anyTimes();

        HstRequestContext requestContext = createNiceMock(HstRequestContext.class);
        expect(requestContext.getResolvedMount()).andReturn(resolvedMount).anyTimes();
        expect(requestContext.getComponentFilterTags()).andReturn(new HashSet<String>()).anyTimes();
        List<HstComponentWindowFilter> filters = new ArrayList<HstComponentWindowFilter>();
        expect(requestContext.getComponentWindowFilters()).andReturn(filters).anyTimes();
       
        // mock environment
        HstContainerConfig mockHstContainerConfig = createNiceMock(HstContainerConfig.class);
        HstComponentConfiguration compConfig = createNiceMock(HstComponentConfiguration.class);
        expect(compConfig.getReferenceName()).andReturn("refName").anyTimes();
        HstComponentFactory compFactory = createNiceMock(HstComponentFactory.class);
        expect(compFactory.getComponentInstance(mockHstContainerConfig, compConfig, mount)).andReturn(new GenericHstComponent());

        // container items with matching, non-matching and no HstComponentWindowFilter
        TreeMap<String, HstComponentConfiguration> children = getContainerItemConfigurations();
        expect(compConfig.getChildren()).andReturn(children);

        // instantiate the window
        replay(mockHstContainerConfig, compConfig, compFactory, mount, resolvedMount, requestContext);
        HstComponentWindow window = factory.create(mockHstContainerConfig, requestContext, compConfig, compFactory);

        // verify results
        verify(mockHstContainerConfig, compConfig, compFactory, mount, resolvedMount, requestContext);
        assertNotNull(window.getChildWindow("comp1"));
        assertNotNull(window.getChildWindow("comp2"));
    }
    
    @Test
    public void testDisableWindowFilter() {
        HstComponentWindowFactoryImpl factory = new HstComponentWindowFactoryImpl();

        Mount mount = createNiceMock(Mount.class);
        ResolvedMount resolvedMount = createNiceMock(ResolvedMount.class);
        expect(resolvedMount.getMount()).andReturn(mount).anyTimes();

        HstRequestContext requestContext = createNiceMock(HstRequestContext.class);
        expect(requestContext.getResolvedMount()).andReturn(resolvedMount).anyTimes();
        expect(requestContext.getComponentFilterTags()).andReturn(new HashSet<String>()).anyTimes();
        List<HstComponentWindowFilter> filters = new ArrayList<HstComponentWindowFilter>();
        filters.add(new HstComponentWindowFilter() {
            @Override
            public HstComponentWindow doFilter(HstRequestContext requestContext, HstComponentConfiguration compConfig,
                                               HstComponentWindow window) throws HstComponentException {
                if (compConfig.getName().equals("comp1")) {
                    return null;
                }
                return window;
            }
        });
        expect(requestContext.getComponentWindowFilters()).andReturn(filters).anyTimes();

        // mock environment
        HstContainerConfig mockHstContainerConfig = createNiceMock(HstContainerConfig.class);
        HstComponentConfiguration compConfig = createNiceMock(HstComponentConfiguration.class);
        expect(compConfig.getReferenceName()).andReturn("refName");
        HstComponentFactory compFactory = createNiceMock(HstComponentFactory.class);
        expect(compFactory.getComponentInstance(mockHstContainerConfig, compConfig, mount)).andReturn(new GenericHstComponent());

        // container items with matching, non-matching and no HstComponentWindowFilter
        TreeMap<String, HstComponentConfiguration> children = getContainerItemConfigurations();
        expect(compConfig.getChildren()).andReturn(children);

        // instantiate the window
        replay(mockHstContainerConfig, compConfig, compFactory, mount, resolvedMount, requestContext);
        HstComponentWindow window = factory.create(mockHstContainerConfig, requestContext, compConfig, compFactory);

        // verify results
        verify(mockHstContainerConfig, compConfig, compFactory, mount, resolvedMount, requestContext);
        // since comp1 is filtered, it should be null
        assertNull(window.getChildWindow("comp1"));
        assertNotNull(window.getChildWindow("comp2"));
    }
    
    @Test
    public void testHideWindowFilter() {
        HstComponentWindowFactoryImpl factory = new HstComponentWindowFactoryImpl();

        Mount mount = createNiceMock(Mount.class);
        ResolvedMount resolvedMount = createNiceMock(ResolvedMount.class);
        expect(resolvedMount.getMount()).andReturn(mount).anyTimes();

        HstRequestContext requestContext = createNiceMock(HstRequestContext.class);
        expect(requestContext.getResolvedMount()).andReturn(resolvedMount).anyTimes();
        expect(requestContext.getComponentFilterTags()).andReturn(new HashSet<String>()).anyTimes();
        List<HstComponentWindowFilter> filters = new ArrayList<HstComponentWindowFilter>();
        filters.add(new HstComponentWindowFilter() {
            @Override
            public HstComponentWindow doFilter(HstRequestContext requestContext, HstComponentConfiguration compConfig,
                                               HstComponentWindow window) throws HstComponentException {
                if(compConfig.getName().equals("comp1")) {
                    window.setVisible(false);
                }
                return window;
            }
        });
        expect(requestContext.getComponentWindowFilters()).andReturn(filters).anyTimes();

        // mock environment
        HstContainerConfig mockHstContainerConfig = createNiceMock(HstContainerConfig.class);
        HstComponentConfiguration compConfig = createNiceMock(HstComponentConfiguration.class);
        expect(compConfig.getReferenceName()).andReturn("refName");
        HstComponentFactory compFactory = createNiceMock(HstComponentFactory.class);
        expect(compFactory.getComponentInstance(mockHstContainerConfig, compConfig, mount)).andReturn(new GenericHstComponent());

        // container items with matching, non-matching and no HstComponentWindowFilter
        TreeMap<String, HstComponentConfiguration> children = getContainerItemConfigurations();
        expect(compConfig.getChildren()).andReturn(children);

        // instantiate the window
        replay(mockHstContainerConfig, compConfig, compFactory, mount, resolvedMount, requestContext);
        HstComponentWindow window = factory.create(mockHstContainerConfig, requestContext, compConfig, compFactory);

        // verify results
        verify(mockHstContainerConfig, compConfig, compFactory, mount, resolvedMount, requestContext);

        assertNotNull(window.getChildWindow("comp1"));
        assertNotNull(window.getChildWindow("comp2"));
        assertFalse(window.getChildWindow("comp1").isVisible());
        assertTrue(window.getChildWindow("comp2").isVisible());
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
