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
package org.hippoecm.hst.mock.core.linking;

import org.easymock.EasyMock;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.mock.util.MockBeanTestHelper;
import org.junit.Test;

public class TestMockHstLink {
    
    @Test
    public void testSimpleProperties() throws Exception {
        MockHstLink bean = new MockHstLink();
        
        MockBeanTestHelper.verifyReadWriteProperty(bean, "path", "test-path");
        MockBeanTestHelper.verifyReadWriteProperty(bean, "notFound", true);
        MockBeanTestHelper.verifyReadWriteProperty(bean, "containerResource", true);
        MockBeanTestHelper.verifyReadWriteProperty(bean, "subPath", "test-subPath");
        
        Mount mount = EasyMock.createNiceMock(Mount.class);
        MockBeanTestHelper.verifyReadWriteProperty(bean, "mount", mount);
    }
    
}
