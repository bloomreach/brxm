/*
 * Copyright 2008 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.sa.template.config;

import org.hippoecm.frontend.sa.plugin.IPlugin;
import org.hippoecm.frontend.sa.plugin.config.IClusterConfig;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.hippoecm.frontend.sa.plugin.config.impl.JavaClusterConfig;
import org.hippoecm.frontend.sa.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.sa.service.render.ListViewService;
import org.hippoecm.frontend.sa.service.render.RenderService;
import org.hippoecm.frontend.sa.template.ITemplateEngine;
import org.hippoecm.frontend.sa.template.ITemplateStore;
import org.hippoecm.frontend.sa.template.TypeDescriptor;
import org.hippoecm.frontend.sa.template.plugins.ValueTemplatePlugin;
import org.hippoecm.frontend.sa.template.plugins.field.FieldPlugin;
import org.hippoecm.frontend.sa.template.plugins.field.PropertyFieldPlugin;

public class JcrTemplateStore implements ITemplateStore {
    private static final long serialVersionUID = 1L;

    public IClusterConfig getTemplate(TypeDescriptor type, String mode) {
        if (type.getName().equals("defaultcontent:article")) {
            JavaClusterConfig template = new JavaClusterConfig();
            template.put(RenderService.MODEL_ID, "{template}.model");
            template.put("item", "{template}.item");

            IPluginConfig plugin = new JavaPluginConfig();
            plugin.put(IPlugin.CLASSNAME, PropertyFieldPlugin.class.getName());
            plugin.put(ITemplateEngine.MODE, mode);
            plugin.put(ITemplateEngine.ENGINE, "template:" + ITemplateEngine.ENGINE);
            plugin.put(FieldPlugin.FIELD, "title");
            plugin.put(ListViewService.ITEM, "template:item");
            plugin.put(RenderService.WICKET_ID, "template:" + RenderService.WICKET_ID);
            plugin.put(RenderService.MODEL_ID, "template:" + RenderService.MODEL_ID);
            plugin.put("template." + RenderService.WICKET_ID, "template:item");
            template.addPlugin(plugin);

            return template;
        } else if (type.getName().equals("String")) {
            JavaClusterConfig template = new JavaClusterConfig();
            template.addProperty(RenderService.WICKET_ID);
            template.put(RenderService.MODEL_ID, "{template}.model");

            IPluginConfig plugin = new JavaPluginConfig();
            plugin.put(IPlugin.CLASSNAME, ValueTemplatePlugin.class.getName());
            plugin.put(ITemplateEngine.ENGINE, "template:" + ITemplateEngine.ENGINE);
            plugin.put(ITemplateEngine.MODE, mode);
            plugin.put(RenderService.WICKET_ID, "template:" + RenderService.WICKET_ID);
            plugin.put(RenderService.MODEL_ID, "template:" + RenderService.MODEL_ID);
            template.addPlugin(plugin);

            return template;
        }
        return new JavaClusterConfig();
    }

}
