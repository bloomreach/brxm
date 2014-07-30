/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbParticipant;
import org.apache.wicket.extensions.markup.html.basic.SmartLinkLabel;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.validator.StringValidator;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.groups.Group;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AdminDataTable;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.DefaultFocusBehavior;
import org.hippoecm.frontend.plugins.standards.panelperspective.breadcrumb.PanelPluginBreadCrumbLink;

/**
 * This panel displays a pageable, searchable list of users.
 */
public class ListUsersPanel extends AdminBreadCrumbPanel implements IObserver<UserDataProvider> {
    private static final long serialVersionUID = 1L;

    private static final int NUMBER_OF_ITEMS_PER_PAGE = 20;

    private final IPluginConfig config;
    private final IPluginContext context;
    private final AdminDataTable table;
    private final UserDataProvider userDataProvider;

    /**
     * Constructs a new ListUsersPanel.
     *
     * @param id               the id
     * @param context          the context
     * @param breadCrumbModel  the breadCrumbModel
     * @param userDataProvider the userDataProvider
     */
    public ListUsersPanel(final String id, final IPluginContext context, final IPluginConfig config, final IBreadCrumbModel breadCrumbModel, final UserDataProvider userDataProvider) {
        super(id, breadCrumbModel);

        setOutputMarkupId(true);

        this.config = config;
        this.context = context;
        this.userDataProvider = userDataProvider;

        PanelPluginBreadCrumbLink createUserLink = new PanelPluginBreadCrumbLink("create-user-link", breadCrumbModel) {
            @Override
            protected IBreadCrumbParticipant getParticipant(final String componentId) {
                return new CreateUserPanel(componentId, breadCrumbModel, context, config);
            }

            @Override
            public boolean isVisible() {
                return isUserCreationEnabled();
            }
        };

        WebMarkupContainer createButtonContainer = new WebMarkupContainer("create-user-button-container") {

            @Override
            public boolean isVisible() {
                return isUserCreationEnabled();
            }
        };

        createButtonContainer.add(createUserLink);
        add(createButtonContainer);

        List<IColumn> columns = new ArrayList<IColumn>();

        columns.add(new AbstractColumn<User, String>(new ResourceModel("user-username"), "username") {
            private static final long serialVersionUID = 1L;

            public void populateItem(final Item<ICellPopulator<User>> item, final String componentId, final IModel<User> model) {

                ViewUserLinkLabel action = new ViewUserLinkLabel(componentId, model, ListUsersPanel.this, context);
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
        columns.add(new AbstractColumn<User, String>(new ResourceModel("user-group")) {
            @Override
            public void populateItem(final Item<ICellPopulator<User>> cellItem, final String componentId, final IModel<User> model) {
                User user = model.getObject();
                List<Group> groups = user.getLocalMembershipsAsListOfGroups();
                GroupsLinkListPanel groupsLinkListPanel = new GroupsLinkListPanel(componentId, groups, context,
                                                                                  ListUsersPanel.this);

                cellItem.add(groupsLinkListPanel);
            }
        });
        columns.add(new AbstractColumn<User, String>(new Model<String>("Type")) {
            private static final long serialVersionUID = 1L;

            public void populateItem(Item<ICellPopulator<User>> cellItem, String componentId, IModel<User> model) {
                User user = model.getObject();
                if (user.isExternal()) {
                    cellItem.add(new Label(componentId, "external"));
                } else {
                    cellItem.add(new Label(componentId, "repository"));
                }
            }
        });
        columns.add(new AbstractColumn<User, String>(new ResourceModel("user-view-actions-title")) {
            @Override
            public void populateItem(final Item<ICellPopulator<User>> item, final String componentId, final IModel<User> model) {

                AjaxLinkLabel action = new AjaxLinkLabel(componentId, new ResourceModel("user-remove-action")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        context.getService(IDialogService.class.getName(), IDialogService.class).show(
                                new DeleteUserDialog(model, this, context, ListUsersPanel.this));
                    }
                };
                item.add(action);
            }
        });

        final Form form = new Form("search-form");
        form.setOutputMarkupId(true);
        add(form);

        final TextField<String> search = new TextField<String>("search-query",
                                                               new PropertyModel<String>(userDataProvider, "searchTerm"));
        search.add(StringValidator.minimumLength(1));
        search.setRequired(false);
        search.add(new DefaultFocusBehavior());
        form.add(search);

        form.add(new AjaxButton("search-button", form) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form form) {
                target.add(table);
            }
        });

        table = new AdminDataTable("table", columns, userDataProvider, NUMBER_OF_ITEMS_PER_PAGE);
        table.setOutputMarkupId(true);
        add(table);
    }

    protected boolean isUserCreationEnabled() {
        return config.getAsBoolean(ListUsersPlugin.USER_CREATION_ENABLED_KEY, true);
    }

    public IModel<String> getTitle(final Component component) {
        return new ResourceModel("admin-users-title");
    }

    @Override
    protected void onAddedToBreadCrumbsBar() {
        context.registerService(this, IObserver.class.getName());
    }

    @Override
    protected void onRemovedFromBreadCrumbsBar() {
        context.unregisterService(this, IObserver.class.getName());
    }

    @Override
    public UserDataProvider getObservable() {
        return userDataProvider;
    }

    @Override
    public void onEvent(final Iterator<? extends IEvent<UserDataProvider>> events) {
        redraw();
    }

}
