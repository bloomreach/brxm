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
package org.hippoecm.frontend.sa.template.editor;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.sa.adapter.Adapter;
import org.hippoecm.frontend.sa.plugin.IPlugin;
import org.hippoecm.frontend.sa.plugin.IPluginContext;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.hippoecm.frontend.sa.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.sa.service.IViewService;

public class LegacyEditorPlugin implements IPlugin, IViewService {
    private static final long serialVersionUID = 1L;

    private IPluginContext context;
    private IPluginConfig properties;
    private Adapter adapter;
    private String serviceId;

    public LegacyEditorPlugin(IPluginContext context, IPluginConfig properties) {
        this.context = context;
        this.properties = properties;
        this.serviceId = properties.getString("service.pid");

        IPluginConfig config = new JavaPluginConfig();
        config.put("legacy.base", "/hippo:configuration/hippo:frontend_deprecated/hippo:cms/rootPlugin/tabsPlugin/panel");
        config.put("legacy.plugin", "editPerspective");
        config.put("wicket.id", properties.getString("wicket.id"));

        adapter = new Adapter();
        adapter.init(context, config);

        context.registerService(this, serviceId);
    }

    public void stop() {
        adapter.destroy();

        context.unregisterService(this, serviceId);
    }

    public void view(IModel model) {
        adapter.setModel(model);
    }

    public String getServiceId() {
        return properties.getString("service.pid");
    }
}
