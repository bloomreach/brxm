/**
 * Copyright 2011-2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.cms.dev;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.panelperspective.PanelPlugin;

/**
 * @deprecated The Developer perspective has been removed from the default CMS configuration.
 * It's contents has been moved to the Admin perspective. Let your plugin extend
 * {@link org.hippoecm.frontend.plugins.cms.admin.AdminPanelPlugin} instead
 * to make it part of the Admin perspective too.
 */
@Deprecated
public abstract class DevPanelPlugin extends PanelPlugin {

    public static final String DEV_PANEL_SERVICE_ID = "dev.panel";

    public DevPanelPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);
    }

    public String getPanelServiceId() {
        return DEV_PANEL_SERVICE_ID;
    }

}
