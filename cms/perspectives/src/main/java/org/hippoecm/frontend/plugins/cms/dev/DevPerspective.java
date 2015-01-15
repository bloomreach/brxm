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
import org.hippoecm.frontend.plugins.standards.panelperspective.PanelPluginPerspective;

/**
 * Perspective that will show a set of Panels exposing developer functionality.
 *
 * @deprecated The Developer perspective has been removed from the default CMS configuration.
 * Its contents has been moved to the {@link AdminPerspective}. Any custom Developer perspective
 * plugins should be moved to the Admin perspective too by changing their superclass from
 * {@link org.hippoecm.frontend.plugins.cms.dev.DevPanelPlugin} to
 * {@link org.hippoecm.frontend.plugins.cms.admin.AdminPanelPlugin} and moving their plugin
 * configuration node from /hippo:configuration/hippo:frontend/cms/cms-dev to
 * /hippo:configuration/hippo:frontend/cms/cms-admin.
 */
@Deprecated
public class DevPerspective extends PanelPluginPerspective {

    public DevPerspective(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    public String getPanelServiceId() {
        return DevPanelPlugin.DEV_PANEL_SERVICE_ID;
    }

}
