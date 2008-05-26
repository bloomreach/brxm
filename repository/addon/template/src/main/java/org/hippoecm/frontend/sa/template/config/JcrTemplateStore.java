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
import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.hippoecm.frontend.sa.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.sa.plugin.impl.RenderPlugin;
import org.hippoecm.frontend.sa.plugin.render.ListViewPlugin;
import org.hippoecm.frontend.sa.template.ITemplateConfig;
import org.hippoecm.frontend.sa.template.ITemplateEngine;
import org.hippoecm.frontend.sa.template.ITemplateStore;
import org.hippoecm.frontend.sa.template.TypeDescriptor;
import org.hippoecm.frontend.sa.template.impl.JavaTemplateConfig;
import org.hippoecm.frontend.sa.template.plugins.ValueTemplatePlugin;
import org.hippoecm.frontend.sa.template.plugins.field.FieldPlugin;
import org.hippoecm.frontend.sa.template.plugins.field.PropertyFieldPlugin;

public class JcrTemplateStore implements ITemplateStore {
    private static final long serialVersionUID = 1L;

    public ITemplateConfig getTemplate(TypeDescriptor type, String mode) {
        if (type.getName().equals("defaultcontent:article")) {
            IPluginConfig[] plugins = new IPluginConfig[1];

            plugins[0] = new JavaPluginConfig();
            plugins[0].put(IPlugin.CLASSNAME, PropertyFieldPlugin.class.getName());
            plugins[0].put(ITemplateEngine.MODE, mode);
            plugins[0].put(ITemplateEngine.ENGINE, "template:" + ITemplateEngine.ENGINE);
            plugins[0].put(FieldPlugin.FIELD, "title");
            plugins[0].put(ListViewPlugin.ITEM, "template:item");
            plugins[0].put(RenderPlugin.WICKET_ID, "template:" + RenderPlugin.WICKET_ID);
            plugins[0].put(RenderPlugin.MODEL_ID, "template:" + RenderPlugin.MODEL_ID);
            plugins[0].put("template." + RenderPlugin.WICKET_ID, "template:item");

            JavaTemplateConfig template = new JavaTemplateConfig(plugins);
            template.put(RenderPlugin.MODEL_ID, "{template}.model");
            template.put("item", "{template}.item");
            return template;
        } else if (type.getName().equals("String")) {
            IPluginConfig[] plugins = new IPluginConfig[1];

            plugins[0] = new JavaPluginConfig();
            plugins[0].put(IPlugin.CLASSNAME, ValueTemplatePlugin.class.getName());
            plugins[0].put(ITemplateEngine.ENGINE, "template:" + ITemplateEngine.ENGINE);
            plugins[0].put(ITemplateEngine.MODE, mode);
            plugins[0].put(RenderPlugin.WICKET_ID, "template:" + RenderPlugin.WICKET_ID);
            plugins[0].put(RenderPlugin.MODEL_ID, "template:" + RenderPlugin.MODEL_ID);

            JavaTemplateConfig template = new JavaTemplateConfig(plugins);
            template.addProperty(RenderPlugin.WICKET_ID);
            template.put(RenderPlugin.MODEL_ID, "{template}.model");
            return template;
        }
        return new JavaTemplateConfig(new IPluginConfig[0]);
    }

}
