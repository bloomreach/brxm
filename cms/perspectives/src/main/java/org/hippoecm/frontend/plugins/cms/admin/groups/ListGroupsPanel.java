/*
 *  Copyright 2008-2012 Hippo.
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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbParticipant;
import org.apache.wicket.extensions.breadcrumb.panel.BreadCrumbPanel;
import org.apache.wicket.extensions.breadcrumb.panel.IBreadCrumbPanelFactory;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.validator.StringValidator;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.DefaultFocusBehavior;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AdminDataTable;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabel;
import org.hippoecm.frontend.plugins.standards.panelperspective.breadcrumb.PanelPluginBreadCrumbLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This panel displays a pageable, searchable list of groups.
 */
public class ListGroupsPanel extends AdminBreadCrumbPanel {

    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ListGroupsPanel.class);

    private AdminDataTable table;

    public ListGroupsPanel(final String id, final IPluginContext context, final IBreadCrumbModel breadCrumbModel,
                           final GroupDataProvider groupDataProvider) {
        super(id, breadCrumbModel);
        setOutputMarkupId(true);

        add(new PanelPluginBreadCrumbLink("create-group", breadCrumbModel) {
            @Override
            protected IBreadCrumbParticipant getParticipant(final String componentId) {
                return new CreateGroupPanel(componentId, breadCrumbModel);
            }
        });

        List<IColumn> columns = new ArrayList<IColumn>();

        columns.add(new AbstractColumn(new ResourceModel("group-name")) {
            private static final long serialVersionUID = 1L;

            public void populateItem(final Item item, final String componentId, final IModel model) {
                
                AjaxLinkLabel action = new AjaxLinkLabel(componentId, new PropertyModel(model, "groupname")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        activate(new IBreadCrumbPanelFactory()
                        {
                            public BreadCrumbPanel create(String componentId,
                                    IBreadCrumbModel breadCrumbModel)
                            {
                                return new ViewGroupPanel(componentId, context, breadCrumbModel, model);
                            }
                        });
                    }
                };
                item.add(action);
            }
        });

        columns.add(new PropertyColumn(new ResourceModel("group-description"), "description"));
        columns.add(new AbstractColumn(new ResourceModel("group-members")) {
            private static final long serialVersionUID = 1L;

            public void populateItem(Item cellItem, String componentId, IModel model) {
                Group group = (Group) model.getObject();
                StringBuilder sb = new StringBuilder();
                boolean first = true;
                try {
                    for (String user : group.getMembers()) {
                        if (first) {
                            sb.append(user);
                            first = false;
                        } else {
                            sb.append(',').append(user);
                        }
                    }
                } catch (RepositoryException e) {
                    sb.append("Failed to retrieve members.");
                    log.error("Failed to retrieve members of group", e);
                }
                cellItem.add(new Label(componentId, sb.toString()));
            }
        });

        final Form form = new Form("search-form");
        form.setOutputMarkupId(true);
        add(form);

        TextField search = new TextField("search-query", new PropertyModel(groupDataProvider, "query"));
        search.add(StringValidator.minimumLength(1));
        search.setRequired(false);
        search.add(new DefaultFocusBehavior());
        form.add(search);

        form.add(new AjaxButton("search-button", form) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                target.addComponent(table);
            }
        });

        table = new AdminDataTable("table", columns, groupDataProvider, 20);
        add(table);
    }

    public IModel<String> getTitle(Component component) {
        return new ResourceModel("admin-groups-title");
    }

}
