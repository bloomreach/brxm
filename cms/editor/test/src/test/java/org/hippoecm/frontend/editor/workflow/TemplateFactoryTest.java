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
package org.hippoecm.frontend.editor.workflow;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.hippoecm.frontend.editor.layout.JavaLayoutDescriptor;
import org.hippoecm.frontend.editor.layout.JavaLayoutPad;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.ListViewPlugin;
import org.junit.Test;

public class TemplateFactoryTest {

    @Test
    public void testTwoPane() {
        JavaLayoutDescriptor layout = new JavaLayoutDescriptor("Test");
        JavaLayoutPad left = new JavaLayoutPad("left");
        layout.addPad(left);
        
        JavaLayoutPad right = new JavaLayoutPad("right");
        layout.addPad(right);
        
        IClusterConfig cluster = new TemplateFactory().createTemplate(layout);
        assertEquals(1, cluster.getPlugins().size());
        assertEquals("Test", cluster.getPlugins().get(0).get("plugin.class"));
    }

    @Test
    public void testList() {
        JavaLayoutDescriptor layout = new JavaLayoutDescriptor("Test");
        JavaLayoutPad left = new JavaLayoutPad("main");
        left.setIsList(true);
        layout.addPad(left);
        
        IClusterConfig cluster = new TemplateFactory().createTemplate(layout);
        List<IPluginConfig> plugins = cluster.getPlugins();
        assertEquals(2, plugins.size());
        assertEquals("Test", plugins.get(0).get("plugin.class"));
        assertEquals(ListViewPlugin.class.getName(), plugins.get(1).get("plugin.class"));
    }

}
