/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.dashboard;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.model.SystemInfoDataProvider;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.perspective.Perspective;
import org.hippoecm.frontend.service.IconSize;

public class DashboardPerspective extends Perspective {

    private static final long serialVersionUID = 1L;

    private SystemInfoDataProvider systemDataProvider = new SystemInfoDataProvider();

    public DashboardPerspective(IPluginContext context, IPluginConfig config) {
        super(context, config);
        add(new Label("version-label", systemDataProvider.getReleaseVersion()));
        EditionPanel editionPanel = new EditionPanel("edition-panel", config.getString("edition", "community").toLowerCase());
        add(editionPanel);
    }

    @Override
    public ResourceReference getIcon(IconSize type) {
        return new PackageResourceReference(DashboardPerspective.class, "dashboard-perspective-" + type.getSize() + ".png");
    }

}
