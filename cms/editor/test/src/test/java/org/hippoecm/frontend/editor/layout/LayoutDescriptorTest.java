/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.layout;

import java.util.List;
import java.util.Map;

import org.hippoecm.frontend.PluginTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LayoutDescriptorTest extends PluginTest {

    /**
     * verify the parsing of the layout.xml file
     */
    @Test
    public void testDeserialization() {
        ILayoutDescriptor descriptor = new XmlLayoutDescriptor(new ClassLoaderModel(),
                                                               getClass().getPackage().getName() + ".Test");

        Map<String, ILayoutPad> pads = descriptor.getLayoutPads();
        assertEquals(2, pads.size());

        ILayoutPad first = pads.values().iterator().next();
        assertEquals("top", first.getName());

        List<String> transitions = first.getTransitions();
        assertEquals(1, transitions.size());
        assertEquals("down", first.getTransition(transitions.get(0)).getName());
    }

    /**
     * Verify that layout descriptor returns a default icon when there is none provided with the layout itself.
     */
    @Test
    public void testNoIcon() throws Exception {
        ILayoutDescriptor descriptor = new XmlLayoutDescriptor(new ClassLoaderModel(),
                                                               getClass().getPackage().getName() + ".NonExistingTest");
        tester.startResource(descriptor.getIcon());
    }

    /**
     * Verify that the properties file is used to provide user-readable names.
     */
    @Test
    public void testName() throws Exception {
        ILayoutDescriptor descriptor = new XmlLayoutDescriptor(new ClassLoaderModel(),
                                                               getClass().getPackage().getName() + ".Test");
        assertEquals("Test layout", descriptor.getName().getObject());
    }

}
