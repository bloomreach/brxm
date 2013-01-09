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
package org.hippoecm.hst.mock.core.sitemenu;

import org.easymock.EasyMock;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.sitemenu.HstSiteMenu;
import org.hippoecm.hst.core.sitemenu.HstSiteMenuItem;
import org.hippoecm.hst.mock.util.MockBeanTestHelper;
import org.junit.Test;

public class TestMockHstSiteMenuItem {
    
    @Test
    public void testSimpleProperties() throws Exception {
        MockHstSiteMenuItem bean = new MockHstSiteMenuItem();

        MockBeanTestHelper.verifyReadWriteProperty(bean, "name", "test-name");
        MockBeanTestHelper.verifyReadWriteProperty(bean, "expanded", true);
        
        HstSiteMenuItem parentItem = EasyMock.createNiceMock(HstSiteMenuItem.class);
        MockBeanTestHelper.verifyReadWriteProperty(bean, "parentItem", parentItem);
        
        HstSiteMenu hstSiteMenu = EasyMock.createNiceMock(HstSiteMenu.class);
        MockBeanTestHelper.verifyReadWriteProperty(bean, "hstSiteMenu", hstSiteMenu);
        
        HstLink hstLink = EasyMock.createNiceMock(HstLink.class);
        MockBeanTestHelper.verifyReadWriteProperty(bean, "hstLink", hstLink);
        
        MockBeanTestHelper.verifyReadWriteProperty(bean, "externalLink", "test-externalLink");
        MockBeanTestHelper.verifyReadWriteProperty(bean, "repositoryBased", true);
        MockBeanTestHelper.verifyReadWriteProperty(bean, "depth", 3);
        MockBeanTestHelper.verifyReadWriteProperty(bean, "selected", true);
    }
    
}
