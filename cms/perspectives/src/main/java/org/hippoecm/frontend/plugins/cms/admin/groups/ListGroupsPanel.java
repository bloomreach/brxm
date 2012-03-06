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

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbParticipant;
import org.apache.wicket.extensions.breadcrumb.panel.BreadCrumbPanel;
import org.apache.wicket.extensions.breadcrumb.panel.IBreadCrumbPanelFactory;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.validator.StringValidator;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AdminDataTable;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.ConfirmDeleteDialog;
import org.hippoecm.frontend.plugins.cms.admin.widgets.DefaultFocusBehavior;
import org.hippoecm.frontend.plugins.standards.panelperspective.breadcrumb.PanelPluginBreadCrumbLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;

/**
 * This panel displays a pageable, searchable list of groups.
 */
public class ListGroupsPanel extends AdminBreadCrumbPanel {

    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ListGroupsPanel.class);

    private static final int NUMBER_OF_ITEMS_PER_PAGE = 20;

    private AdminDataTable table;
    private IPluginContext context;

    /**
     * Constructs a new ListGroupsPanel.
     *
     * @param id                the id
     * @param context           the context
     * @param breadCrumbModel   the breadCrumbModel
     * @param groupDataProvider the groupDataProvider
     */
    public ListGroupsPanel(final String id, final IPluginContext context, final IBreadCrumbModel breadCrumbModel,
                           final GroupDataProvider groupDataProvider) {
        super(id, breadCrumbModel);
        setOutputMarkupId(true);

        this.context = context;

        groupDataProvider.setDirty();

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
                    public void onClick(final AjaxRequestTarget target) {
                        activate(new IBreadCrumbPanelFactory() {
                            public BreadCrumbPanel create(final String componentId,
                                                          final IBreadCrumbModel breadCrumbModel) {
                                return new ViewGroupPanel(componentId, context, breadCrumbModel, model);
                            }
                        });
                    }
                };
                item.add(action);
            }
        });

        columns.add(new PropertyColumn(new ResourceModel("group-description"), "description"));

        columns.add(new GroupDeleteLinkColumn(new ResourceModel("group-view-actions-title")));

        final Form form = new Form("search-form");
        form.setOutputMarkupId(true);
        add(form);

        TextField<String> search = new TextField<String>("search-query", new PropertyModel<String>(groupDataProvider, "query"));
        search.add(StringValidator.minimumLength(1));
        search.setRequired(false);
        search.add(new DefaultFocusBehavior());
        form.add(search);

        form.add(new AjaxButton("search-button", form) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form form) {
                target.addComponent(table);
            }
        });

        table = new AdminDataTable("table", columns, groupDataProvider, NUMBER_OF_ITEMS_PER_PAGE);
        add(table);
    }

    public IModel<String> getTitle(final Component component) {
        return new ResourceModel("admin-groups-title");
    }

    private class GroupDeleteLinkColumn extends AbstractColumn<Group> {
        private static final long serialVersionUID = 1L;

        private GroupDeleteLinkColumn(final IModel<String> displayModel) {
            super(displayModel);
        }

        @Override
        public void populateItem(final Item<ICellPopulator<Group>> item, final String componentId,
                                 final IModel<Group> model) {

            AjaxLinkLabel action = new DeleteGroupActionLink(componentId, new ResourceModel("group-remove-action"), model);
            item.add(action);
        }


    }

    private class DeleteGroupActionLink extends AjaxLinkLabel {
        private static final long serialVersionUID = 1L;
        private final IModel<Group> groupModel;

        private DeleteGroupActionLink(String id, IModel model, IModel<Group> groupModel) {
            super(id, model);
            this.groupModel = groupModel;
        }

        @Override
        public void onClick(final AjaxRequestTarget target) {
            context.getService(IDialogService.class.getName(), IDialogService.class).show(
                    new ConfirmDeleteDialog<Group>(groupModel, this) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected void onOk() {
                            deleteGroup(getModel());
                        }

                        @Override
                        protected String getTitleKey() {
                            return "group-delete-title";
                        }

                        @Override
                        protected String getTextKey() {
                            return "group-delete-text";
                        }
                    });
        }

        private void deleteGroup(final IModel<Group> model) {
            Group group = model.getObject();
            String groupname = group.getGroupname();
            try {
                group.delete();
                Session.get().info(getString("group-removed", model));

                List<IBreadCrumbParticipant> l = getBreadCrumbModel().allBreadCrumbParticipants();
                getBreadCrumbModel().setActive(l.get(l.size() - 2));
                activate(new IBreadCrumbPanelFactory() {
                    public BreadCrumbPanel create(final String componentId,
                                                  final IBreadCrumbModel breadCrumbModel) {
                        return new ListGroupsPanel(componentId, context, breadCrumbModel, new GroupDataProvider());
                    }
                });
            } catch (RepositoryException e) {
                Session.get().warn(getString("group-remove-failed", model));
                log.error("Unable to delete group '" + groupname + "' : ", e);
            }
        }
    }

}
