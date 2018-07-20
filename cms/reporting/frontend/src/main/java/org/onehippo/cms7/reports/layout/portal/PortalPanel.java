/*
 *  Copyright 2011-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.reports.layout.portal;


import java.util.Arrays;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.hippoecm.frontend.extjs.ExtHippoThemeReportingBehavior;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.session.UserSession;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.cms7.reports.ExtPlugin;
import org.onehippo.cms7.reports.plugins.ReportPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.ExtComponent;
import org.wicketstuff.js.ext.ExtPanel;
import org.wicketstuff.js.ext.util.ExtClass;


@ExtClass("Hippo.Reports.Portal")
public class PortalPanel extends ExtPanel {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(PortalPanel.class);
    private static final CssResourceReference REPORTS_PORTALS_CSS = new CssResourceReference(PortalPanel.class, "Hippo.Reports.Portal.css");
    private static final JavaScriptResourceReference REPORTS_PORTALS_JS = new JavaScriptResourceReference(PortalPanel.class, "Hippo.Reports.Portal.js");

    private IPluginContext context;
    private boolean rendered = false;
    private String serviceName;

    public PortalPanel(String id, IPluginContext context, String serviceName) {
        super(id);

        this.context = context;
        this.serviceName = serviceName;

        add(new ExtHippoThemeReportingBehavior());
    }

    @Override
    public void internalRenderHead(final HtmlHeaderContainer container) {
        final IHeaderResponse response = container.getHeaderResponse();
        response.render(CssHeaderItem.forReference(REPORTS_PORTALS_CSS));
        response.render(new JavaScriptReferenceHeaderItem(REPORTS_PORTALS_JS, null, null, false, null, null) {
            @Override
            public List<HeaderItem> getDependencies() {
                return Arrays.asList(JavaScriptReferenceHeaderItem.forReference(ReportPanel.REPORTS_PORTLET_JS));
            }
        });
        super.internalRenderHead(container);
    }

    @Override
    protected void onBeforeRender() {
        try {
            Session session = UserSession.get().getJcrSession();
            session.save();
            session.refresh(false);
        } catch (RepositoryException repositoryException) {
            log.error("Error refreshing jcr session.", repositoryException);
        }
        if (!rendered) {
            final List<ExtPlugin> reportPluginServices = context.getServices(this.serviceName, ExtPlugin.class);

            for (ExtPlugin reportExtPlugin : reportPluginServices) {
                final ExtComponent pluginComponent = reportExtPlugin.getExtComponent();
                add(pluginComponent);
            }
            this.rendered = true;
        }
        super.onBeforeRender();
    }

    @Override
    protected void onRenderProperties(JSONObject properties) throws JSONException {
        super.onRenderProperties(properties);
    }

}
