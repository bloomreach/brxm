/**
  * Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.panelperspective.PanelPlugin;

public abstract class AdminPanelPlugin extends PanelPlugin {

    public static final String ADMIN_PANEL_SERVICE_ID = "admin.panel";

    public AdminPanelPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);
    }

    public String getPanelServiceId() {
        return ADMIN_PANEL_SERVICE_ID;
    }

}
