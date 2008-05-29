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
package org.hippoecm.frontend.plugins.cms.dashboard.sa;

import org.hippoecm.frontend.plugins.standards.sa.perspective.Perspective;
import org.hippoecm.frontend.sa.plugin.IPluginContext;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;

public class DashboardPerspective extends Perspective {
    private static final long serialVersionUID = 1L;

    public DashboardPerspective(IPluginContext context, IPluginConfig config) {
        super(context, config);

        // addExtensionPoint("currentActivityPlugin");
        // addExtensionPoint("todoPlugin");

        addExtensionPoint("legacyDashboard");
    }
}
