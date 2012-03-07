/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.frontend.plugins.cms.admin.groups;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.panel.BreadCrumbPanel;
import org.apache.wicket.extensions.breadcrumb.panel.IBreadCrumbPanelFactory;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.IPluginContext;

public class ViewGroupActionLink extends AjaxFallbackLink<String> {
    private final IModel<Group> groupModel;
    private final IPluginContext context;
    private final BreadCrumbPanel breadCrumbPanel;

    private static final long serialVersionUID = 1L;

    public ViewGroupActionLink(final String id, final IModel<String> labelTextModel, final IModel<Group> groupModel,
                               final IPluginContext context, final BreadCrumbPanel breadCrumbPanel) {
        super(id, labelTextModel);

        this.groupModel = groupModel;
        this.context = context;
        this.breadCrumbPanel = breadCrumbPanel;

        Label label = new Label("label", labelTextModel);
        label.setRenderBodyOnly(true);
        add(label);
    }

    @Override
    public void onClick(AjaxRequestTarget target) {
        breadCrumbPanel.activate(new IBreadCrumbPanelFactory() {
            public BreadCrumbPanel create(String componentId, IBreadCrumbModel breadCrumbModel) {
                return new ViewGroupPanel(componentId, context, breadCrumbModel, groupModel);
            }
        });
    }
}