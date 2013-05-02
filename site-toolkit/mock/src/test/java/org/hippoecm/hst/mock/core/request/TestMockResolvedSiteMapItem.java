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
package org.hippoecm.hst.mock.core.request;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.easymock.EasyMock;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.mock.util.MockBeanTestHelper;
import org.junit.Test;

public class TestMockResolvedSiteMapItem {
    
    @Test
    public void testSimpleProperties() throws Exception {
        MockResolvedSiteMapItem bean = new MockResolvedSiteMapItem();
        
        MockBeanTestHelper.verifyReadWriteProperty(bean, "relativeContentPath", "test-relativeContentPath");
        MockBeanTestHelper.verifyReadWriteProperty(bean, "pathInfo", "test-pathInfo");
        
        HstSiteMapItem siteMapItem = EasyMock.createNiceMock(HstSiteMapItem.class);
        MockBeanTestHelper.verifyReadWriteProperty(bean, "hstSiteMapItem", siteMapItem);
        
        MockBeanTestHelper.verifyReadWriteProperty(bean, "statusCode", 403);
        MockBeanTestHelper.verifyReadWriteProperty(bean, "errorCode", 404);
        
        Set<String> roles = new HashSet<String>(Arrays.asList(new String [] { "editor", "author" }));
        MockBeanTestHelper.verifyReadWriteProperty(bean, "roles", roles);
        
        Set<String> users = new HashSet<String>(Arrays.asList(new String [] { "jane", "john" }));
        MockBeanTestHelper.verifyReadWriteProperty(bean, "users", users);

        MockBeanTestHelper.verifyReadWriteProperty(bean, "authenticated", true);

        HstComponentConfiguration hstComponentConfiguration = EasyMock.createNiceMock(HstComponentConfiguration.class);
        MockBeanTestHelper.verifyReadWriteProperty(bean, "hstComponentConfiguration", hstComponentConfiguration);
        
        MockBeanTestHelper.verifyReadWriteProperty(bean, "namedPipeline", "test-namedPipeline");
        
        ResolvedMount resolvedMount = EasyMock.createNiceMock(ResolvedMount.class);
        MockBeanTestHelper.verifyReadWriteProperty(bean, "resolvedMount", resolvedMount);
    }

}
