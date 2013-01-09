/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.mock.configuration.components;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.mock.util.MockBeanTestHelper;
import org.junit.Test;

public class TestMockHstComponentConfiguration {
    
    @Test
    public void testSimpleProperties() throws Exception {
        MockHstComponentConfiguration bean = new MockHstComponentConfiguration("test-id");
        
        MockBeanTestHelper.verifyReadOnlyProperty(bean, "id", "test-id");
        MockBeanTestHelper.verifyReadWriteProperty(bean, "name", "test-name");
        MockBeanTestHelper.verifyReadWriteProperty(bean, "canonicalStoredLocation", "test-canonicalStoredLocation");
        MockBeanTestHelper.verifyReadWriteProperty(bean, "referenceName", "test-referenceName");
        MockBeanTestHelper.verifyReadWriteProperty(bean, "renderPath", "test-renderPath");
        MockBeanTestHelper.verifyReadWriteProperty(bean, "serveResourcePath", "test-serveResourcePath");
        MockBeanTestHelper.verifyReadWriteProperty(bean, "componentClassName", "test-componentClassName");
        MockBeanTestHelper.verifyReadWriteProperty(bean, "canonicalIdentifier", "test-canonicalIdentifier");
        MockBeanTestHelper.verifyReadWriteProperty(bean, "namedRenderer", "test-namedRenderer");
        MockBeanTestHelper.verifyReadWriteProperty(bean, "namedResourceServer", "test-namedResourceServer");
        MockBeanTestHelper.verifyReadWriteProperty(bean, "pageErrorHandlerClassName", "test-pageErrorHandlerClassName");
        MockBeanTestHelper.verifyReadWriteProperty(bean, "XType", "test-XType");
        
        HstComponentConfiguration parent = new MockHstComponentConfiguration("test-parent-id");
        MockBeanTestHelper.verifyReadWriteProperty(bean, "parent", parent);
        
        HstComponentConfiguration.Type componentType = HstComponentConfiguration.Type.COMPONENT;
        MockBeanTestHelper.verifyReadWriteProperty(bean, "componentType", componentType);
    }
    
}
