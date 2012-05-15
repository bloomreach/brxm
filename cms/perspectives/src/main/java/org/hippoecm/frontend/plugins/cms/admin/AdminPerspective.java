/**
  * Copyright 2011 Hippo
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
package org.hippoecm.frontend.plugins.cms.admin;


import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.panelperspective.PanelPluginPerspective;
import org.hippoecm.frontend.service.IconSize;

public class AdminPerspective extends PanelPluginPerspective {

    private final IPluginConfig pluginConfig;

    public AdminPerspective(IPluginContext context, IPluginConfig config) {
        super(context, config);
        this.pluginConfig = config;
        add(CSSPackageResource.getHeaderContribution(AdminPerspective.class, "admin-perspective.css"));
    }

    @Override
    public ResourceReference getIcon(IconSize type) {
        return new ResourceReference(AdminPerspective.class, "admin-perspective-" + type.getSize() + ".png");
    }

    @Override
    public String getPanelServiceId() {
        return AdminPanelPlugin.ADMIN_PANEL_SERVICE_ID;
    }

    public IPluginConfig getPluginConfigs()
    {
        return this.pluginConfig;
    }
}
