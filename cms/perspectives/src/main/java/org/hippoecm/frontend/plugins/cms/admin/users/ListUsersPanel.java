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
package org.hippoecm.frontend.plugins.cms.admin.users;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbParticipant;
import org.apache.wicket.extensions.breadcrumb.panel.BreadCrumbPanel;
import org.apache.wicket.extensions.breadcrumb.panel.IBreadCrumbPanelFactory;
import org.apache.wicket.extensions.markup.html.basic.SmartLinkLabel;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.validator.StringValidator;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.groups.DetachableGroup;
import org.hippoecm.frontend.plugins.cms.admin.groups.Group;
import org.hippoecm.frontend.plugins.cms.admin.groups.ViewGroupPanel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AdminDataTable;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabelContainer;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabelListPanel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.ConfirmDeleteDialog;
import org.hippoecm.frontend.plugins.cms.admin.widgets.DefaultFocusBehavior;
import org.hippoecm.frontend.plugins.standards.panelperspective.breadcrumb.PanelPluginBreadCrumbLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;

/**
 * This panel displays a pageable, searchable list of users.
 */
public class ListUsersPanel extends AdminBreadCrumbPanel {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ListUsersPanel.class);

    private static final int NUMBER_OF_ITEMS_PER_PAGE = 20;

    private final AdminDataTable table;
    private final IPluginContext context;

    /**
     * Constructs a new ListUsersPanel.
     *
     * @param id               the id
     * @param context          the context
     * @param breadCrumbModel  the breadCrumbModel
     * @param userDataProvider the userDataProvider
     */
    public ListUsersPanel(final String id, final IPluginContext context, final IBreadCrumbModel breadCrumbModel,
                          final UserDataProvider userDataProvider) {
        super(id, breadCrumbModel);

        this.context = context;

        setOutputMarkupId(true);

        userDataProvider.setDirty();

        add(new PanelPluginBreadCrumbLink("create-user", breadCrumbModel) {
            @Override
            protected IBreadCrumbParticipant getParticipant(final String componentId) {
                return new CreateUserPanel(componentId, breadCrumbModel, context);
            }
        });

        List<IColumn> columns = new ArrayList<IColumn>();

        columns.add(new AbstractColumn(new ResourceModel("user-username"), "username") {
            private static final long serialVersionUID = 1L;

            public void populateItem(final Item item, final String componentId, final IModel model) {

                AjaxLinkLabel action = new AjaxLinkLabel(componentId, new PropertyModel(model, "username")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        //panel.showView(target, model);
                        activate(new IBreadCrumbPanelFactory() {
                            public BreadCrumbPanel create(final String componentId,
                                                          final IBreadCrumbModel breadCrumbModel) {
                                return new ViewUserPanel(componentId, context, breadCrumbModel, model);
                            }
                        });
                    }
                };
                item.add(action);
            }
        });
        columns.add(new PropertyColumn(new ResourceModel("user-firstname"), "frontend:firstname", "firstName"));
        columns.add(new PropertyColumn(new ResourceModel("user-lastname"), "frontend:lastname", "lastName"));
        columns.add(new AbstractColumn(new ResourceModel("user-email")) {
            @Override
            public void populateItem(final Item cellItem, final String componentId, final IModel model) {
                cellItem.add(new SmartLinkLabel(componentId, new PropertyModel<String>(model, "email")));
            }
        });
        columns.add(new AbstractColumn(new ResourceModel("user-group")) {
            @Override
            public void populateItem(final Item cellItem, final String componentId, final IModel model) {
                User user = (User) model.getObject();

                ArrayList<AjaxLinkLabelContainer> list = new ArrayList<AjaxLinkLabelContainer>();
                for (DetachableGroup detachableGroup : user.getLocalMemberships()) {
                    Group group = detachableGroup.getGroup();
                    final IModel<Group> groupModel = new Model<Group>(group);

                    AjaxLinkLabelContainer action = new ViewGroupActionLink(
                            componentId,
                            new PropertyModel(groupModel, "groupname"),
                            groupModel,
                            context,
                            ListUsersPanel.this
                    );

                    list.add(action);
                }

                AjaxLinkLabelListPanel multipleLinkLabel = new AjaxLinkLabelListPanel(componentId,
                        new Model<ArrayList<AjaxLinkLabelContainer>>(list));

                cellItem.add(multipleLinkLabel);
            }
        });
        columns.add(new AbstractColumn<User>(new ResourceModel("user-view-actions-title")) {
            @Override
            public void populateItem(final Item<ICellPopulator<User>> item, final String componentId,
                                     final IModel<User> model) {

                AjaxLinkLabel action = new AjaxLinkLabel(componentId, new ResourceModel("user-remove-action")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        context.getService(IDialogService.class.getName(), IDialogService.class)
                                .show(new ConfirmDeleteDialog<User>(model, this) {
                                    private static final long serialVersionUID = 1L;

                                    @Override
                                    protected void onOk() {
                                        deleteUser(model);
                                    }

                                    @Override
                                    protected String getTitleKey() {
                                        return "user-delete-title";
                                    }

                                    @Override
                                    protected String getTextKey() {
                                        return "user-delete-text";
                                    }
                                });
                    }
                };
                item.add(action);
            }
        });

        final Form form = new Form("search-form");
        form.setOutputMarkupId(true);
        add(form);

        final TextField<String> search = new TextField<String>(
                "search-query",
                new PropertyModel<String>(userDataProvider, "query")
        );
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

        table = new AdminDataTable("table", columns, userDataProvider, NUMBER_OF_ITEMS_PER_PAGE);
        table.setOutputMarkupId(true);
        add(table);
    }

    public IModel<String> getTitle(final Component component) {
        return new ResourceModel("admin-users-title");
    }

    private static class ViewGroupActionLink extends AjaxLinkLabelContainer {
        final IModel<Group> groupModel;
        final IPluginContext context;
        final BreadCrumbPanel breadCrumbPanel;

        private static final long serialVersionUID = 1L;

        private ViewGroupActionLink(final String id, final IModel labelTextModel, final IModel<Group> groupModel,
                                    final IPluginContext context, final BreadCrumbPanel breadCrumbPanel) {
            super(id, labelTextModel);

            this.groupModel = groupModel;
            this.context = context;
            this.breadCrumbPanel = breadCrumbPanel;
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

    /**
     * Deletes the user contained in the model.
     *
     * @param model the IModel containing the User to delete
     */
    private void deleteUser(final IModel<User> model) {
        User user = model.getObject();
        if (user == null) {
            log.info("No user model found when trying to delete user. Probably the Ok button was double clicked.");
            return;
        }
        String username = user.getUsername();
        try {
            user.delete();
            Session.get().info(getString("user-removed", model));
            // one up
            List<IBreadCrumbParticipant> l = getBreadCrumbModel().allBreadCrumbParticipants();
            getBreadCrumbModel().setActive(l.get(l.size() - 2));
            activate(new IBreadCrumbPanelFactory() {
                public BreadCrumbPanel create(final String componentId,
                                              final IBreadCrumbModel breadCrumbModel) {
                    return new ListUsersPanel(componentId, context, breadCrumbModel, new UserDataProvider());
                }
            });
        } catch (RepositoryException e) {
            Session.get().warn(getString("user-remove-failed", model));
            log.error("Unable to delete user '" + username + "' : ", e);
        }
    }

}
