/*
 *  Copyright 2012-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.List;

import org.easymock.EasyMock;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestHstComponentWindowImpl {

    @Test
    public void testInsertionOrderedChildWindowNames() {
     
        HstComponentWindow hstComponentWindow1 = EasyMock.createNiceMock(HstComponentWindow.class);
        EasyMock.expect(hstComponentWindow1.getName()).andReturn("z").anyTimes();
        HstComponentWindow hstComponentWindow2 = EasyMock.createNiceMock(HstComponentWindow.class);
        EasyMock.expect(hstComponentWindow2.getName()).andReturn("y").anyTimes();
        HstComponentWindow hstComponentWindow3 = EasyMock.createNiceMock(HstComponentWindow.class);
        EasyMock.expect(hstComponentWindow3.getName()).andReturn("x").anyTimes();
        HstComponentWindow hstComponentWindow4 = EasyMock.createNiceMock(HstComponentWindow.class);
        EasyMock.expect(hstComponentWindow4.getName()).andReturn("a").anyTimes();
        HstComponentWindow hstComponentWindow5 = EasyMock.createNiceMock(HstComponentWindow.class);
        EasyMock.expect(hstComponentWindow5.getName()).andReturn("b").anyTimes();

        EasyMock.replay(hstComponentWindow1);
        EasyMock.replay(hstComponentWindow2);
        EasyMock.replay(hstComponentWindow3);
        EasyMock.replay(hstComponentWindow4);
        EasyMock.replay(hstComponentWindow5);

        HstComponentWindowImpl hstComponentWindowImpl = new HstComponentWindowImpl(null, null, null, null, null, null, null);
        hstComponentWindowImpl.addChildWindow(hstComponentWindow1);
        hstComponentWindowImpl.addChildWindow(hstComponentWindow2);
        hstComponentWindowImpl.addChildWindow(hstComponentWindow3);
        hstComponentWindowImpl.addChildWindow(hstComponentWindow4);
        hstComponentWindowImpl.addChildWindow(hstComponentWindow5);

        List<String> names = hstComponentWindowImpl.getChildWindowNames();

        assertEquals("their should be 5 names", names.size(), 5);
        assertEquals("first name should be z", names.get(0), "z");
        assertEquals("second name should be y", names.get(1), "y");
        assertEquals("third name should be x", names.get(2), "x");
        assertEquals("fourth name should be a", names.get(3), "a");
        assertEquals("fifth name should be b", names.get(4), "b");
        
        HstComponentWindowImpl hstComponentWindowImplReverted = new HstComponentWindowImpl(null, null, null, null, null, null, null);
        hstComponentWindowImplReverted.addChildWindow(hstComponentWindow5);
        hstComponentWindowImplReverted.addChildWindow(hstComponentWindow4);
        hstComponentWindowImplReverted.addChildWindow(hstComponentWindow3);
        hstComponentWindowImplReverted.addChildWindow(hstComponentWindow2);
        hstComponentWindowImplReverted.addChildWindow(hstComponentWindow1);
        
        names = hstComponentWindowImplReverted.getChildWindowNames();
        assertEquals("their should be 5 names", names.size(), 5);
        assertEquals("first name should be z", names.get(0), "b");
        assertEquals("second name should be y", names.get(1), "a");
        assertEquals("third name should be x", names.get(2), "x");
        assertEquals("fourth name should be a", names.get(3), "y");
        assertEquals("fifth name should be b", names.get(4), "z");
    }


}
