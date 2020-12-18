/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentInfo;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.util.DefaultKeyValue;
import org.hippoecm.hst.util.KeyValue;
import org.hippoecm.hst.util.PageErrorUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * TestPageErrors
 * 
 * @version $Id$
 */
public class TestPageErrors {
    
    private PageErrors pageErrors;
    private HstComponentInfo rootComponentInfo;
    private HstComponentInfo leftComponentInfo;
    private HstComponentInfo rightComponentInfo;
    
    @Before
    public void setUp() {
        List<KeyValue<HstComponentInfo, Collection<HstComponentException>>> componentExceptionPairs = 
            new ArrayList<KeyValue<HstComponentInfo, Collection<HstComponentException>>>();
        
        rootComponentInfo = new SimpleHstComponentInfo("root", "rootcomp", "examples.RootComponent", null, null);
        List<HstComponentException> exceptions = new ArrayList<HstComponentException>();
        exceptions.add(new HstComponentException("Root exception - 1", new ClassNotFoundException()));
        exceptions.add(new HstComponentException("Root exception - 2", new NoClassDefFoundError()));
        componentExceptionPairs.add(new DefaultKeyValue<HstComponentInfo, Collection<HstComponentException>>(rootComponentInfo, exceptions));
        
        leftComponentInfo = new SimpleHstComponentInfo("left", "leftcomp", "examples.LeftComponent", null, null);
        exceptions = new ArrayList<HstComponentException>();
        exceptions.add(new HstComponentException("Left exception - 1", new RuntimeException()));
        componentExceptionPairs.add(new DefaultKeyValue<HstComponentInfo, Collection<HstComponentException>>(leftComponentInfo, exceptions));
        
        rightComponentInfo = new SimpleHstComponentInfo("right", "rightcomp", "examples.RightComponent", null, null);
        exceptions = new ArrayList<HstComponentException>();
        exceptions.add(new HstComponentException("Right exception - 1", new IllegalArgumentException()));
        componentExceptionPairs.add(new DefaultKeyValue<HstComponentInfo, Collection<HstComponentException>>(rightComponentInfo, exceptions));
        
        pageErrors = new DefaultPageErrors(componentExceptionPairs);
    }
    
    @Test
    public void testPageErrors() throws Exception {
        Collection<HstComponentInfo> componentInfos = pageErrors.getComponentInfos();
        assertEquals(3, componentInfos.size());
        
        Collection<HstComponentException> exceptions = pageErrors.getAllComponentExceptions();
        assertEquals(4, exceptions.size());
        
        Iterator<HstComponentInfo> componentInfosIt = componentInfos.iterator();
        HstComponentInfo componentInfo = componentInfosIt.next();
        assertEquals("root", componentInfo.getId());
        assertEquals(2, pageErrors.getComponentExceptions(componentInfo).size());
        
        componentInfo = componentInfosIt.next();
        assertEquals("left", componentInfo.getId());
        assertEquals(1, pageErrors.getComponentExceptions(componentInfo).size());
        
        componentInfo = componentInfosIt.next();
        assertEquals("right", componentInfo.getId());
        assertEquals(1, pageErrors.getComponentExceptions(componentInfo).size());
        
        componentInfos = PageErrorUtils.getComponentInfosByCauseType(pageErrors, Throwable.class);
        assertEquals(3, componentInfos.size());
        
        componentInfos = PageErrorUtils.getComponentInfosByCauseType(pageErrors, Exception.class);
        assertEquals(3, componentInfos.size());
        
        componentInfos = PageErrorUtils.getComponentInfosByCauseType(pageErrors, Error.class);
        assertEquals(1, componentInfos.size());
        
        componentInfos = PageErrorUtils.getComponentInfosByCauseType(pageErrors, RuntimeException.class);
        assertEquals(2, componentInfos.size());

        exceptions = PageErrorUtils.getExceptionsByCauseType(pageErrors, RuntimeException.class);
        assertEquals(2, exceptions.size());
        
        exceptions = PageErrorUtils.getExceptionsByCauseType(pageErrors, rootComponentInfo, RuntimeException.class);
        assertEquals(0, exceptions.size());

        exceptions = PageErrorUtils.getExceptionsByCauseType(pageErrors, leftComponentInfo, IllegalArgumentException.class);
        assertEquals(0, exceptions.size());

        exceptions = PageErrorUtils.getExceptionsByCauseType(pageErrors, rightComponentInfo, RuntimeException.class);
        assertEquals(1, exceptions.size());

        assertEquals(2, PageErrorUtils.getExceptionsByComponentClassName(pageErrors, "examples.RootComponent").size());
        assertEquals(1, PageErrorUtils.getExceptionsByComponentClassName(pageErrors, "examples.LeftComponent").size());
        assertEquals(1, PageErrorUtils.getExceptionsByComponentClassName(pageErrors, "examples.RightComponent").size());
        
        assertEquals(2, PageErrorUtils.getExceptionsByComponentName(pageErrors, "rootcomp").size());
        assertEquals(1, PageErrorUtils.getExceptionsByComponentName(pageErrors, "leftcomp").size());
        assertEquals(1, PageErrorUtils.getExceptionsByComponentName(pageErrors, "rightcomp").size());

        assertEquals(2, PageErrorUtils.getExceptionsByComponentId(pageErrors, "root").size());
        assertEquals(1, PageErrorUtils.getExceptionsByComponentId(pageErrors, "left").size());
        assertEquals(1, PageErrorUtils.getExceptionsByComponentId(pageErrors, "right").size());
    }
    
    @Ignore
    private static class SimpleHstComponentInfo implements HstComponentInfo {
        private String id;
        private String name;
        private String className;
        private String parametersInfoClassName;
        private String label;

        private SimpleHstComponentInfo(String id, String name, String className, String parametersInfoClassName, String label) {
            this.id = id;
            this.name = name;
            this.className = className;
            this.parametersInfoClassName = parametersInfoClassName;
            this.label = label;
        }
        
        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }
        
        public String getComponentClassName() {
            return className;
        }

        public String getParametersInfoClassName() {
            return parametersInfoClassName;
        }

        @Override
        public boolean isStandalone() {
            return true;
        }
        @Override
        public boolean isAsync() {
            return false;
        }
        @Override
        public String getAsyncMode() {
            return null;
        }
        @Override
        public boolean isCompositeCacheable() {
            return false;
        }
        @Override
        public boolean isSuppressWasteMessage() {
            return false;
        }

        @Override
        public String getLabel() {
            return label;
        }

        @Override
        public HstComponentConfiguration.Type getComponentType() {
            return HstComponentConfiguration.Type.COMPONENT;
        }

        @Override
        public boolean getAndSetLogWasteMessageProcessed() {
            return false;
        }
    }
}
