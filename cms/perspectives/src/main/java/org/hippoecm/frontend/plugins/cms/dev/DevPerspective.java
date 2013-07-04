/**
 * Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.panelperspective.PanelPluginPerspective;
import org.hippoecm.frontend.service.IconSize;

/**
 * Perspective that will show a set of Panels exposing developer functionality..
 */
public class DevPerspective extends PanelPluginPerspective {

    private static final CssResourceReference PERSPECTIVE_SKIN = new CssResourceReference(DevPerspective.class, "dev-perspective.css");

    public DevPerspective(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(PERSPECTIVE_SKIN));
    }

    @Override
    public ResourceReference getIcon(IconSize type) {
        return new PackageResourceReference(DevPerspective.class, "dev-perspective-32.png");
    }

    @Override
    public String getPanelServiceId() {
        return DevPanelPlugin.DEV_PANEL_SERVICE_ID;
    }

}
