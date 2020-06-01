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
package org.onehippo.cms7.reports.plugins;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.panelperspective.PanelPlugin;
import org.hippoecm.frontend.plugins.standards.panelperspective.breadcrumb.PanelPluginBreadCrumbPanel;
import org.onehippo.cms7.reports.layout.portal.PortalPanel;

public abstract class PortalPanelPlugin extends PanelPlugin {

    public PortalPanelPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);
    }

    private static abstract class ReportsBreadCrumbPanelPlugin extends PanelPluginBreadCrumbPanel {

        public ReportsBreadCrumbPanelPlugin(final String id, final IPluginContext context, final IBreadCrumbModel breadCrumbModel, final String serviceId) {
            super(id, breadCrumbModel);
            add(new PortalPanel("reports-panel", context, serviceId));
        }

    }

    @Override
    public PanelPluginBreadCrumbPanel create(final String componentId, final IBreadCrumbModel breadCrumbModel) {

        return new ReportsBreadCrumbPanelPlugin(componentId, getPluginContext(), breadCrumbModel, getPortalPanelServiceId()) {
            @Override
            public IModel<String> getTitle(final Component component) {
                return PortalPanelPlugin.this.getTitle();
            }
        };

    }

    public abstract String getPortalPanelServiceId();

}
