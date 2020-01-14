/*
 *  Copyright 2011-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.admin.permissions;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.admin.SystemPerspective;

@SuppressWarnings("unused")
public class SystemPermissionsPerspective extends SystemPerspective {

    public SystemPermissionsPerspective(final IPluginContext context, final IPluginConfig config) {
        super(context, config);
    }

    @Override
    protected DomainsPanel createPanel(final String componentId) {
        return new DomainsPanel("panel", getBreadCrumbBar(), getPluginContext());
    }
}
