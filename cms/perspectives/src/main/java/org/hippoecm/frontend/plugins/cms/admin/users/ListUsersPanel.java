/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
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
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.hippoecm.frontend.dialog.Confirm;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.groups.Group;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AdminDataTable;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.SearchTermPanel;
import org.hippoecm.frontend.plugins.standards.panelperspective.breadcrumb.PanelPluginBreadCrumbLink;
import org.hippoecm.frontend.util.EventBusUtils;
import org.onehippo.cms7.event.HippoEventConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This panel displays a pageable, searchable list of users.
 */
public class ListUsersPanel extends AdminBreadCrumbPanel implements IObserver<UserDataProvider> {
    private static final Logger log = LoggerFactory.getLogger(ListUsersPanel.class);

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
    public ListUsersPanel(final String id, final IPluginContext context, final IPluginConfig config,
                          final IBreadCrumbModel breadCrumbModel, final UserDataProvider userDataProvider) {
        super(id, breadCrumbModel);

        setOutputMarkupId(true);

        this.config = config;
        this.context = context;
        this.userDataProvider = userDataProvider;

        final PanelPluginBreadCrumbLink createUserLink = new PanelPluginBreadCrumbLink("create-user-link", breadCrumbModel) {

            @Override
            protected IBreadCrumbParticipant getParticipant(final String componentId) {
                return new CreateUserPanel(componentId, breadCrumbModel, context, config);
            }

            @Override
            public boolean isVisible() {
                return isUserCreationEnabled();
            }
        };

        final WebMarkupContainer createButtonContainer = new WebMarkupContainer("create-user-button-container") {
            @Override
            public boolean isVisible() {
                return isUserCreationEnabled();
            }
        };

        createButtonContainer.add(createUserLink);
        add(createButtonContainer);

        final List<IColumn<User, String>> columns = new ArrayList<>();

        columns.add(new AbstractColumn<User, String>(new ResourceModel("user-username"), "username") {
            @Override
            public void populateItem(final Item<ICellPopulator<User>> cellItem, final String componentId,
                                     final IModel<User> rowModel) {
                cellItem.add(new ViewUserLinkLabel(componentId, rowModel, ListUsersPanel.this, context));
            }
        });
        columns.add(new PropertyColumn<>(new ResourceModel("user-firstname"), "frontend:firstname", "firstName"));
        columns.add(new PropertyColumn<>(new ResourceModel("user-lastname"), "frontend:lastname", "lastName"));
        columns.add(new AbstractColumn<User, String>(new ResourceModel("user-email")) {
            @Override
            public void populateItem(final Item cellItem, final String componentId, final IModel rowModel) {
                cellItem.add(new SmartLinkLabel(componentId, new PropertyModel<>(rowModel, "email")));
            }
        });
        columns.add(new AbstractColumn<User, String>(new ResourceModel("user-group")) {
            @Override
            public void populateItem(final Item<ICellPopulator<User>> cellItem, final String componentId,
                                     final IModel<User> rowModel) {
                final User user = rowModel.getObject();
                final List<Group> groups = user.getLocalMembershipsAsListOfGroups(true);
                final GroupsLinkListPanel groupsLinkListPanel = new GroupsLinkListPanel(componentId, groups, context,
                                                                                  ListUsersPanel.this);

                cellItem.add(groupsLinkListPanel);
            }
        });
        columns.add(new AbstractColumn<User, String>(new ResourceModel("user-type")) {
            @Override
            public void populateItem(final Item<ICellPopulator<User>> cellItem, final String componentId,
                                     final IModel<User> rowModel) {
                final User user = rowModel.getObject();
                if (user.isExternal()) {
                    cellItem.add(new Label(componentId, new ResourceModel("user-type-external")));
                } else {
                    cellItem.add(new Label(componentId, new ResourceModel("user-type-repository")));
                }
            }
        });
        columns.add(new AbstractColumn<User, String>(new ResourceModel("user-view-actions-title")) {
            @Override
            public void populateItem(final Item<ICellPopulator<User>> cellItem, final String componentId,
                                     final IModel<User> rowModel) {

                cellItem.add(new DeleteUserActionLink(componentId, new ResourceModel("user-remove-action"), rowModel));
            }
        });

        final SearchTermPanel searchTermPanel = new SearchTermPanel("search-field") {
            @Override
            public void processSubmit(final AjaxRequestTarget target, final Form<?> form, final String searchTerm) {
                super.processSubmit(target, form, searchTerm);
                userDataProvider.setSearchTerm(searchTerm);
                target.add(table);
            }
        };
        add(searchTermPanel);

        table = new AdminDataTable<>("table", columns, userDataProvider, NUMBER_OF_ITEMS_PER_PAGE);
        table.setOutputMarkupId(true);
        add(table);
    }

    private class DeleteUserActionLink extends AjaxLinkLabel {
        private final IModel<User> userModel;

        private DeleteUserActionLink(final String id, final IModel<String> model, final IModel<User> userModel) {
            super(id, model);
            this.userModel = userModel;
        }

        @Override
        public void onClick(final AjaxRequestTarget target) {
            final IDialogService dialogService = context.getService(IDialogService.class.getName(), IDialogService.class);
            final Confirm confirm = new Confirm(
                    getString("user-delete-title", userModel),
                    getString("user-delete-text", userModel)
            ).ok(() -> deleteUser(userModel.getObject()));

            dialogService.show(confirm);
        }
    }

    private void deleteUser(final User user) {
        if (user == null) {
            log.info("No user model found when trying to delete user. Probably the Ok button was double clicked.");
            return;
        }
        final String username = user.getUsername();
        try {
            user.delete();

            // Let the outside world know that this user got deleted
            EventBusUtils.post("delete-user", HippoEventConstants.CATEGORY_USER_MANAGEMENT, "deleted user " + username);
            info(getString("user-removed", new Model<>(user)));
        } catch (final RepositoryException e) {
            error(getString("user-remove-failed", new Model<>(user)));
            log.error("Unable to delete user '{}' : ", username, e);
        }

        redraw();
    }

    protected boolean isUserCreationEnabled() {
        return config.getAsBoolean(ListUsersPlugin.USER_CREATION_ENABLED_KEY, true);
    }

    @Override
    public IModel<String> getTitle(final Component component) {
        return new ResourceModel("admin-users-title");
    }

    @Override
    protected void onAddedToBreadCrumbsBar() {
        context.registerService(this, IObserver.class.getName());
    }

    @Override
    protected void onRemovedFromBreadCrumbsBar() {
        userDataProvider.setSearchTerm(null);
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
