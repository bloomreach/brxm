/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.reports.layout.panel;

import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.onehippo.cms7.reports.ReportsPerspective;
import org.onehippo.cms7.reports.plugins.PortalPanelPlugin;

public class BrokenLinksPanelPlugin extends PortalPanelPlugin {

    private static final long serialVersionUID = 1L;
    public static final String BROKEN_LINKS_SERVICE_ID = "service.reports.brokenlinks";

    public BrokenLinksPanelPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);
    }

    public ResourceReference getImage() {
        return new PackageResourceReference(BrokenLinksPanelPlugin.class, "broken-links-48.png");
    }

    public IModel<String> getTitle() {
        return new ClassResourceModel("broken-links-panel-title", BrokenLinksPanelPlugin.class);
    }

    public IModel<String> getHelp() {
        return new ClassResourceModel("broken-links-panel-help", BrokenLinksPanelPlugin.class);
    }

    @Override
    public String getPanelServiceId() {
        return ReportsPerspective.REPORTING_SERVICE;
    }

    @Override
    public String getPortalPanelServiceId() {
        return BROKEN_LINKS_SERVICE_ID;
    }

}
