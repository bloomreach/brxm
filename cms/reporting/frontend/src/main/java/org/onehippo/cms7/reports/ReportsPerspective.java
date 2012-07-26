/*
 *  Copyright 2011 Hippo.
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

import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.datetime.PatternDateConverter;
import org.apache.wicket.datetime.markup.html.basic.DateLabel;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.panelperspective.PanelPluginPanel;
import org.hippoecm.frontend.plugins.standards.panelperspective.PanelPluginPerspective;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.session.UserSession;

public class ReportsPerspective extends PanelPluginPerspective {
    
    private static final long serialVersionUID = 1L;
    
    public static final String REPORTING_SERVICE = "reporting.service";

    private final WebMarkupContainer refreshGroup;
    private final DateLabel lastRefreshDateLabel;

    public ReportsPerspective(IPluginContext context, IPluginConfig config) {
        super(context, config);
        setOutputMarkupId(true);

        refreshGroup = new WebMarkupContainer("refresh-group");
        refreshGroup.add(new Label("latest-refresh-label", new StringResourceModel("last-refresh-label", this, null)));
        lastRefreshDateLabel = new DateLabel("latest-refresh-date", new PatternDateConverter("HH:mm", false));
        lastRefreshDateLabel.setOutputMarkupId(true);
        refreshGroup.add(lastRefreshDateLabel);

        AjaxLink<String> refreshLink = new AjaxLink<String>("refresh-link") {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                try {
                    Session session = ((UserSession) getSession()).getJcrSession();
                    session.save();
                    session.refresh(false);
                    lastRefreshDateLabel.setDefaultModel(new Model<Date>(new Date()));
                    target.addComponent(lastRefreshDateLabel);
                } catch (RepositoryException repositoryException) {
                    log.error("Error refreshing jcr session.", repositoryException);
                }

                target.appendJavascript("if (typeof Hippo.Reports.RefreshObservableInstance !== 'undefined') { Hippo.Reports.RefreshObservableInstance.fireEvent('refresh'); }");
            }
        };
        refreshLink.add(new Label("refresh-link-label", new StringResourceModel("refresh-link-text", this, null)));
        refreshGroup.add(refreshLink);
        add(refreshGroup);

        add(CSSPackageResource.getHeaderContribution(ReportsPerspective.class, "layout/reporting.css"));
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
            lastRefreshDateLabel.setDefaultModel(new Model<Date>(new Date()));
        }
    }

    @Override
    public IModel<String> getTitle() {
        return new StringResourceModel("perspective-title", this, new Model<String>("Reports"));
    }

    @Override
    public ResourceReference getIcon(IconSize type) {
        return new ResourceReference(ReportsPerspective.class, "reports-" + type.getSize() + ".png");
    }

}
