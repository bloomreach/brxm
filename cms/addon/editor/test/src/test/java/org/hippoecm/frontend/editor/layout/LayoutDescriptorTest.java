/*
 *  Copyright 2009 Hippo.
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

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.List;

import org.apache.wicket.util.resource.IResourceStream;
import org.junit.Test;

public class LayoutDescriptorTest {

    @Test
    public void testDeserialization() {
        ILayoutDescriptor descriptor = new XmlLayoutDescriptor(new ClassLoaderModel(), getClass().getPackage().getName() + ".Test");

        List<ILayoutPad> pads = descriptor.getLayoutPads();
        assertEquals(2, pads.size());

        List<String> transitions = pads.get(0).getTransitions();
        assertEquals(1, transitions.size());
        assertEquals("up", pads.get(0).getTransition(transitions.get(0)).getName());
    }

    @Test
    public void testNoIcon() throws Exception {
        ILayoutDescriptor descriptor = new XmlLayoutDescriptor(new ClassLoaderModel(), getClass().getPackage().getName() + ".NonExistingTest");
        IResourceStream stream = descriptor.getIcon();
        InputStream input = stream.getInputStream();
        input.close();
    }
    
}
