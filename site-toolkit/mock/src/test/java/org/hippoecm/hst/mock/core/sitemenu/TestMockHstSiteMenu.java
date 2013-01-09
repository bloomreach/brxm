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
import org.hippoecm.hst.core.sitemenu.EditableMenu;
import org.hippoecm.hst.core.sitemenu.HstSiteMenuItem;
import org.hippoecm.hst.core.sitemenu.HstSiteMenus;
import org.hippoecm.hst.mock.util.MockBeanTestHelper;
import org.junit.Test;

public class TestMockHstSiteMenu {
    
    @Test
    public void testSimpleProperties() throws Exception {
        MockHstSiteMenu bean = new MockHstSiteMenu();
        
        HstSiteMenus hstSiteMenus = EasyMock.createNiceMock(HstSiteMenus.class);
        MockBeanTestHelper.verifyReadWriteProperty(bean, "hstSiteMenus", hstSiteMenus);
        
        HstSiteMenuItem deepestExpandedItem = EasyMock.createNiceMock(HstSiteMenuItem.class);
        MockBeanTestHelper.verifyReadWriteProperty(bean, "deepestExpandedItem", deepestExpandedItem);
        
        HstSiteMenuItem selectSiteMenuItem = EasyMock.createNiceMock(HstSiteMenuItem.class);
        MockBeanTestHelper.verifyReadWriteProperty(bean, "selectSiteMenuItem", selectSiteMenuItem);
        
        EditableMenu editableMenu = EasyMock.createNiceMock(EditableMenu.class);
        MockBeanTestHelper.verifyReadWriteProperty(bean, "editableMenu", editableMenu);
        
        MockBeanTestHelper.verifyReadWriteProperty(bean, "name", "test-name");
        MockBeanTestHelper.verifyReadWriteProperty(bean, "expanded", true);
    }
    
}
