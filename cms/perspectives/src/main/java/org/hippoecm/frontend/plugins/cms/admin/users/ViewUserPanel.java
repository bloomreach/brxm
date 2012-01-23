/*
 *  Copyright 2008 Hippo.
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

import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbParticipant;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.groups.DetachableGroup;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.ConfirmDeleteDialog;
import org.hippoecm.frontend.plugins.standards.panelperspective.breadcrumb.PanelPluginBreadCrumbLink;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ViewUserPanel extends AdminBreadCrumbPanel {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ViewUserPanel.class);

    private final IModel model;

    public ViewUserPanel(final String id, final IPluginContext context, final IBreadCrumbModel breadCrumbModel,
            final IModel model) {
        super(id, breadCrumbModel);
        setOutputMarkupId(true);
        
        
        this.model = model;
        final User user = (User) model.getObject();

        // common user properties
        add(new Label("username", new PropertyModel(model, "username")));
        add(new Label("firstName", new PropertyModel(model, "firstName")));
        add(new Label("lastName", new PropertyModel(model, "lastName")));
        add(new Label("email", new PropertyModel(model, "email")));
        add(new Label("provider", new PropertyModel(model, "provider")));
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

        // local memberships
        add(new Label("local-memberships-label", new ResourceModel("user-local-memberships")));
        add(new MembershipsListView("local-memberships", "local-membership", new PropertyModel(user, "localMemberships")));

        // external memberships
        Label external = new Label("external-memberships-label", new ResourceModel("user-external-memberships"));
        external.setVisible((user.getExternalMemberships().size() > 0));
        add(external);
        add(new MembershipsListView("external-memberships", "external-membership", new PropertyModel(user, "externalMemberships")));

        // properties
        add(new Label("properties-label", new ResourceModel("user-properties")) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return (user.getPropertiesList().size() > 0);
            }
        });
        add(new ListView("properties", user.getPropertiesList()) {
            private static final long serialVersionUID = 1L;

            protected void populateItem(ListItem item) {
                Map.Entry<String, String> entry = (Map.Entry<String, String>) item.getModelObject();
                item.add(new Label("key", (String) entry.getKey()));
                item.add(new Label("value", (String) entry.getValue()));
            }
        });

        // actions
        PanelPluginBreadCrumbLink edit = new PanelPluginBreadCrumbLink("edit-user", breadCrumbModel) {
            @Override
            protected IBreadCrumbParticipant getParticipant(final String componentId) {
                return new EditUserPanel(componentId, breadCrumbModel, model);
            }
        };
        edit.setVisible(!user.isExternal());
        add(edit);
        
        PanelPluginBreadCrumbLink password = new PanelPluginBreadCrumbLink("set-user-password", breadCrumbModel) {
            @Override
            protected IBreadCrumbParticipant getParticipant(final String componentId) {
                return new SetPasswordPanel(componentId, breadCrumbModel, model, context);
            }
        };
        password.setVisible(!user.isExternal());
        add(password);
        
        PanelPluginBreadCrumbLink memberships = new PanelPluginBreadCrumbLink("set-user-memberships", breadCrumbModel) {
            @Override
            protected IBreadCrumbParticipant getParticipant(final String componentId) {
                return new SetMembershipsPanel(componentId, breadCrumbModel, model);
            }
        };
        add(memberships);
        
        add(new AjaxLinkLabel("delete-user", new ResourceModel("user-delete")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                context.getService(IDialogService.class.getName(), IDialogService.class).show(
                        new ConfirmDeleteDialog(model, this) {
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
        });
    }

    private void deleteUser(IModel model) {
        User user = (User) model.getObject();
        if (user == null) {
            log.info("No user model found when trying to delete user. Probably the Ok button was double clicked.");
            return;
        }
        String username = user.getUsername();
        try {
            for (DetachableGroup dg : user.getLocalMemberships()) {
                dg.getGroup().removeMembership(username);
            }
            user.delete();
            log.info("User '" + username + "' deleted by "
                    + ((UserSession) Session.get()).getJcrSession().getUserID());
            Session.get().info(getString("user-removed", model));
            // one up
            List<IBreadCrumbParticipant> l = getBreadCrumbModel().allBreadCrumbParticipants();
            getBreadCrumbModel().setActive(l.get(l.size() -2));
        } catch (RepositoryException e) {
            Session.get().warn(getString("user-remove-failed", model));
            log.error("Unable to delete user '" + username + "' : ", e);
        }
    }

    /** list view to be nested in the form. */
    private static final class MembershipsListView extends ListView {
        private static final long serialVersionUID = 1L;
        private String labelId;

        public MembershipsListView(final String id, final String labelId, IModel listModel) {
            super(id, listModel);
            this.labelId = labelId;
            setReuseItems(false);
        }

        protected void populateItem(ListItem item) {
            DetachableGroup dg = (DetachableGroup) item.getModelObject();
            item.add(new Label(labelId, dg.getGroup().getGroupname()));
        }
    }

    public IModel<String> getTitle(Component component) {
        return new StringResourceModel("user-view-title", component, model);
    }

}
