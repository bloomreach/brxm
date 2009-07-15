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
package org.hippoecm.frontend.editor.workflow;

import java.util.Map;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.editor.layout.ILayoutDescriptor;
import org.hippoecm.frontend.editor.layout.ILayoutPad;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaClusterConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.render.ListViewPlugin;

public class TemplateFactory implements IClusterable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public IClusterConfig createTemplate(ILayoutDescriptor layout) {
        JavaClusterConfig clusterConfig = new JavaClusterConfig();
        clusterConfig.addProperty("mode");
        clusterConfig.addService("wicket.id");
        clusterConfig.addReference("wicket.model");
        clusterConfig.addReference("engine");

        JavaPluginConfig root = new JavaPluginConfig("root");
        root.put("plugin.class", layout.getPluginClass());
        root.put("wicket.id", "${wicket.id}");
        Map<String, ILayoutPad> pads = layout.getLayoutPads();
        String[] extensions = new String[pads.size()];
        int i = 0;
        for (Map.Entry<String, ILayoutPad> entry : pads.entrySet()) {
            ILayoutPad pad = entry.getValue();
            extensions[i] = "extension." + pad.getName();
            root.put(extensions[i], getWicketId(pad));
            i++;
        }
        root.put("wicket.extensions", extensions);
        clusterConfig.addPlugin(root);

        for (ILayoutPad pad : pads.values()) {
            if (pad.isList()) {
                clusterConfig.addPlugin(getListPlugin(pad));
            }
        }

        return clusterConfig;
    }

    private IPluginConfig getListPlugin(ILayoutPad pad) {
        JavaPluginConfig config = new JavaPluginConfig(pad.getName());
        config.put("plugin.class", ListViewPlugin.class.getName());
        config.put("wicket.id", getWicketId(pad));
        config.put("item", getWicketId(pad) + ".item");
        if (pad.getOrientation() == ILayoutPad.Orientation.HORIZONTAL) {
            config.put("variant", "row");
        }
        return config;
    }

    private String getWicketId(ILayoutPad pad) {
        return "${cluster.id}." + pad.getName();
    }

}
