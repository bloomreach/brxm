/*
 *  Copyright 2012-2019 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.breadcrumb.panel.BreadCrumbPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.IPluginContext;

public class ViewGroupActionLink extends AjaxLink<String> {
    private final Group group;
    private final IPluginContext context;
    private final BreadCrumbPanel breadCrumbPanel;

    public ViewGroupActionLink(final String id, final IModel<String> labelTextModel, final Group group,
                               final IPluginContext context, final BreadCrumbPanel breadCrumbPanel) {
        super(id, labelTextModel);

        this.group = group;
        this.context = context;
        this.breadCrumbPanel = breadCrumbPanel;

        final Label label = new Label("label", labelTextModel);
        label.setRenderBodyOnly(true);
        add(label);
    }

    @Override
    public void onClick(final AjaxRequestTarget target) {
        breadCrumbPanel.activate((componentId, breadCrumbModel) ->
                new ViewGroupPanel(componentId, context, breadCrumbModel, group));
    }
}
