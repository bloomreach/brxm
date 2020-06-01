/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Map.Entry;

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbParticipant;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.Confirm;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.ReadOnlyModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabel;
import org.hippoecm.frontend.plugins.standards.panelperspective.breadcrumb.PanelPluginBreadCrumbLink;
import org.hippoecm.frontend.util.EventBusUtils;
import org.onehippo.cms7.event.HippoEventConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ViewUserPanel extends AdminBreadCrumbPanel {
    private static final Logger log = LoggerFactory.getLogger(ViewUserPanel.class);

    private final IModel<User> model;

    /**
     * @param id the ID for the Panel
     * @param context the PluginContext
     * @param breadCrumbModel the Model for the page breadcrumb
     * @param userModel the Model for the user to view
     */
    public ViewUserPanel(final String id, final IPluginContext context, final IBreadCrumbModel breadCrumbModel,
                         final IModel<User> userModel) {
        super(id, breadCrumbModel);
        setOutputMarkupId(true);
        model = userModel;

        add(new Label("view-user-panel-title", new StringResourceModel("user-view-title", this).setModel(userModel)));
        // common user properties
        add(new Label("username", new PropertyModel(userModel, "username")));
        add(new Label("firstName", new PropertyModel(userModel, "firstName")));
        add(new Label("lastName", new PropertyModel(userModel, "lastName")));
        add(new Label("email", new PropertyModel(userModel, "email")));
        add(new Label("provider", new PropertyModel(userModel, "provider")));
        add(new Label("active", ReadOnlyModel.of(this::getUserActiveLabel)));
        add(new Label("expired", ReadOnlyModel.of(this::getPasswordExpiredResourceModel)));

        final User user = userModel.getObject();

        // properties
        add(new Label("properties-label", new ResourceModel("user-properties")) {
            @Override
            public boolean isVisible() {
                return !user.getPropertiesList().isEmpty();
            }
        });
        add(new ListView<Entry<String, String>>("properties", user.getPropertiesList()) {
            @Override
            protected void populateItem(final ListItem<Entry<String, String>> item) {
                final Entry<String, String> entry = item.getModelObject();
                item.add(new Label("key", entry.getKey()));
                item.add(new Label("value", entry.getValue()));
            }
        });

        // actions
        final PanelPluginBreadCrumbLink edit = new PanelPluginBreadCrumbLink("edit-user", breadCrumbModel) {
            @Override
            protected IBreadCrumbParticipant getParticipant(final String componentId) {
                return new EditUserPanel(componentId, breadCrumbModel, userModel);
            }
        };
        edit.setVisible(!user.isExternal());
        add(edit);

        final PanelPluginBreadCrumbLink password = new PanelPluginBreadCrumbLink("set-user-password", breadCrumbModel) {
            @Override
            protected IBreadCrumbParticipant getParticipant(final String componentId) {
                return new SetPasswordPanel(componentId, breadCrumbModel, userModel, context);
            }
        };
        password.setVisible(!user.isExternal());
        add(password);

        add(new AjaxLinkLabel("delete-user", new ResourceModel("user-delete")) {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                final IDialogService dialogService = context.getService(IDialogService.class.getName(), IDialogService.class);
                final Confirm confirm = new Confirm(
                        getString("user-delete-title", userModel),
                        getString("user-delete-text", userModel)
                ).ok(() -> deleteUser(userModel.getObject()));

                dialogService.show(confirm);
            }
        });
        add(new SetMembershipsPanel("set-member-ship-panel", context, breadCrumbModel, userModel));
    }

    private String getPasswordExpiredResourceModel() {
        final User user = model.getObject();
        return getString(user.isPasswordExpired() ? "user-password-expired-true" : "user-password-expired-false");
    }

    private String getUserActiveLabel() {
        final User user = model.getObject();
        return getString(user.isActive() ? "user-active-true" : "user-active-false");
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

            final String infoMsg = getString("user-removed", Model.of(user));
            activateParentAndDisplayInfo(infoMsg);

        } catch (final RepositoryException e) {
            error(getString("user-remove-failed", Model.of(user)));
            log.error("Unable to delete user '{}' : ", username, e);
            redraw();
        }
    }

    @Override
    public IModel<String> getTitle(final Component component) {
        return new StringResourceModel("user-view-title", component).setModel(model);
    }

}
