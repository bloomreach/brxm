/*
 *  Copyright 2008-2016 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Map;

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
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.DeleteDialog;
import org.hippoecm.frontend.plugins.standards.panelperspective.breadcrumb.PanelPluginBreadCrumbLink;
import org.hippoecm.frontend.session.UserSession;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.event.HippoEventConstants;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ViewUserPanel extends AdminBreadCrumbPanel {
    private static final long serialVersionUID = 1L;
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
        this.model = userModel;
        final User user = userModel.getObject();

        add(new Label("view-user-panel-title", new StringResourceModel("user-view-title", this, userModel)));
        // common user properties
        add(new Label("username", new PropertyModel(userModel, "username")));
        add(new Label("firstName", new PropertyModel(userModel, "firstName")));
        add(new Label("lastName", new PropertyModel(userModel, "lastName")));
        add(new Label("email", new PropertyModel(userModel, "email")));
        add(new Label("provider", new PropertyModel(userModel, "provider")));
        if (user.isActive()) {
            add(new Label("active", new ResourceModel("user-active-true")));
        } else {
            add(new Label("active", new ResourceModel("user-active-false")));
        }
        if (user.isPasswordExpired()) {
            add(new Label("expired", new ResourceModel("user-password-expired-true")));
        } else {
            add(new Label("expired", new ResourceModel("user-password-expired-false")));
        }

        // properties
        add(new Label("properties-label", new ResourceModel("user-properties")) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return (user.getPropertiesList().size() > 0);
            }
        });
        add(new ListView<Map.Entry<String, String>>("properties", user.getPropertiesList()) {
            private static final long serialVersionUID = 1L;

            protected void populateItem(final ListItem<Map.Entry<String, String>> item) {
                Map.Entry<String, String> entry = item.getModelObject();
                item.add(new Label("key", entry.getKey()));
                item.add(new Label("value", entry.getValue()));
            }
        });

        // actions
        PanelPluginBreadCrumbLink edit = new PanelPluginBreadCrumbLink("edit-user", breadCrumbModel) {
            @Override
            protected IBreadCrumbParticipant getParticipant(final String componentId) {
                return new EditUserPanel(componentId, breadCrumbModel, userModel);
            }
        };
        edit.setVisible(!user.isExternal());
        add(edit);

        PanelPluginBreadCrumbLink password = new PanelPluginBreadCrumbLink("set-user-password", breadCrumbModel) {
            @Override
            protected IBreadCrumbParticipant getParticipant(final String componentId) {
                return new SetPasswordPanel(componentId, breadCrumbModel, userModel, context);
            }
        };
        password.setVisible(!user.isExternal());
        add(password);

        add(new AjaxLinkLabel("delete-user", new ResourceModel("user-delete")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                context.getService(IDialogService.class.getName(), IDialogService.class).show(
                        new DeleteDialog<User>(user, this) {

                            @Override
                            protected void onOk() {
                                deleteUser(user);
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
        });
        add(new SetMembershipsPanel("set-member-ship-panel", context, breadCrumbModel, userModel));
    }

    private void deleteUser(final User user) {
        if (user == null) {
            log.info("No user model found when trying to delete user. Probably the Ok button was double clicked.");
            return;
        }
        String username = user.getUsername();
        try {
            user.delete();

            // Let the outside world know that this user got deleted
            HippoEventBus eventBus = HippoServiceRegistry.getService(HippoEventBus.class);
            if (eventBus != null) {
                final UserSession userSession = UserSession.get();
                HippoEvent event = new HippoEvent(userSession.getApplicationName())
                        .user(userSession.getJcrSession().getUserID()).action("delete-user")
                        .category(HippoEventConstants.CATEGORY_USER_MANAGEMENT)
                        .message("deleted user " + username);
                eventBus.post(event);
            }

            final String infoMsg = getString("user-removed", new Model<>(user));
            final IBreadCrumbParticipant parentBreadCrumb = activateParent();
            parentBreadCrumb.getComponent().info(infoMsg);

        } catch (RepositoryException e) {
            error(getString("user-remove-failed", new Model<>(user)));
            AjaxRequestTarget target = getRequestCycle().find(AjaxRequestTarget.class);
            if (target !=  null) {
                target.add(this);
            }
            log.error("Unable to delete user '" + username + "' : ", e);
        }

    }

    public IModel<String> getTitle(final Component component) {
        return new StringResourceModel("user-view-title", component, model);
    }

}
