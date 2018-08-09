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
package org.onehippo.cms7.reports;

import java.util.Date;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.datetime.PatternDateConverter;
import org.apache.wicket.datetime.markup.html.basic.DateLabel;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.panelperspective.PanelPluginPanel;
import org.hippoecm.frontend.plugins.standards.panelperspective.PanelPluginPerspective;
import org.hippoecm.frontend.session.UserSession;

public class ReportsPerspective extends PanelPluginPerspective {

    public static final String REPORTING_SERVICE = "reporting.service";

    private static final ResourceReference REPORTING_LAYOUT_CSS = new CssResourceReference(ReportsPerspective.class, "layout/reporting.css");

    private final WebMarkupContainer refreshGroup;
    private final DateLabel lastRefreshDateLabel;

    public ReportsPerspective(IPluginContext context, IPluginConfig config) {
        super(context, config, "reports");
        setOutputMarkupId(true);

        refreshGroup = new WebMarkupContainer("refresh-group");
        refreshGroup.add(new Label("latest-refresh-label", new StringResourceModel("last-refresh-label", this)));
        lastRefreshDateLabel = new DateLabel("latest-refresh-date", new PatternDateConverter("HH:mm", false));
        lastRefreshDateLabel.setOutputMarkupId(true);
        refreshGroup.add(lastRefreshDateLabel);

        AjaxLink<String> refreshLink = new AjaxLink<String>("refresh-link") {

            @Override
            public void onClick(final AjaxRequestTarget target) {
                try {
                    Session session = UserSession.get().getJcrSession();
                    session.save();
                    session.refresh(false);
                    lastRefreshDateLabel.setDefaultModel(Model.of(new Date()));
                    target.add(lastRefreshDateLabel);
                } catch (RepositoryException repositoryException) {
                    log.error("Error refreshing jcr session.", repositoryException);
                }

                target.appendJavaScript("if (typeof Hippo.Reports.RefreshObservableInstance !== 'undefined') { Hippo.Reports.RefreshObservableInstance.fireEvent('refresh'); }");
            }
        };
        refreshLink.add(new Label("refresh-link-label", new StringResourceModel("refresh-link-text", this)));
        refreshGroup.add(refreshLink);
        add(refreshGroup);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(REPORTING_LAYOUT_CSS));
    }

    @Override
    public String getPanelServiceId() {
        return REPORTING_SERVICE;
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        boolean refreshGroupVisible = !(getBreadCrumbBar().getActive() instanceof PanelPluginPanel);
        refreshGroup.setVisible(refreshGroupVisible);
        if (refreshGroupVisible) {
            lastRefreshDateLabel.setDefaultModel(Model.of(new Date()));
        }
    }

    @Override
    public IModel<String> getTitle() {
        return new StringResourceModel("perspective-title", this);
    }

}
