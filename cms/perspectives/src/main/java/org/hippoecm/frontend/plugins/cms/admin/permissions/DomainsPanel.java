/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.panel.IBreadCrumbPanelFactory;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.domains.DomainDataProvider;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AdminDataTable;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabel;

import com.bloomreach.xm.repository.security.DomainAuth;

public class DomainsPanel extends AdminBreadCrumbPanel {

    public DomainsPanel(final String id, final IBreadCrumbModel breadCrumbModel, final IPluginContext pluginContext) {
        super(id, breadCrumbModel);

        final List<IColumn<DomainAuth, String>> columns = new ArrayList<>();

        columns.add(new AbstractColumn<DomainAuth, String>(new ResourceModel("permissions-column-domain-header"), "name") {
            public void populateItem(final Item<ICellPopulator<DomainAuth>> item, final String componentId,
                                     final IModel<DomainAuth> model) {
                final AjaxLinkLabel action = new AjaxLinkLabel(componentId, PropertyModel.of(model, "name")) {
                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        activate((IBreadCrumbPanelFactory) (componentId1, breadCrumbModel1) ->
                                new DomainPanel(componentId1, pluginContext, breadCrumbModel1, model));
                    }
                };
                item.add(action);
            }
        });
        columns.add(new AbstractColumn<DomainAuth, String>(new ResourceModel("permissions-column-folder-header"), "path") {

            public void populateItem(final Item<ICellPopulator<DomainAuth>> item, final String componentId,
                                     final IModel<DomainAuth> model) {
                final DomainAuth domain = model.getObject();
                item.add(new Label(componentId, domain.getFolderPath()));
            }
        });
        columns.add(new AbstractColumn<DomainAuth, String>(new ResourceModel("permissions-column-permissions-header")) {

            public void populateItem(final Item<ICellPopulator<DomainAuth>> item, final String componentId,
                                     final IModel<DomainAuth> model) {
                final DomainAuth domain = model.getObject();
                item.add(new Label(componentId, String.join(", ", domain.getAuthRolesMap().keySet())));
            }
        });
        final AdminDataTable<DomainAuth> table = new AdminDataTable<>("table", columns, new DomainDataProvider(), 20);
        add(table);
    }

    public IModel<String> getTitle(final Component component) {
        return new ResourceModel("admin-permissions-title");
    }

}
