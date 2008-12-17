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

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugins.cms.admin.groups.DetachableGroup;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.ConfirmDeleteDialog;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ViewUserPanel extends Panel {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ViewUserPanel.class);

    private final UsersPanel panel;

    public ViewUserPanel(final String id, final IModel userModel, final UsersPanel panel) {
        super(id);
        this.panel = panel;
        setOutputMarkupId(true);
        final User user = (User) userModel.getObject();

        // title
        add(new Label("title", new StringResourceModel("user-view-title", userModel)));

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

        // local memberships
        add(new Label("local-memberships-label", new ResourceModel("user-local-memberships")) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return (user.getLocalMemberships().size() > 0);
            }
        });
        add(new MembershipsListView("local-memberships", "local-membership", user.getLocalMemberships()));

        // external memberships
        add(new Label("external-memberships-label", new ResourceModel("user-external-memberships")) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return (user.getExternalMemberships().size() > 0);
            }
        });
        add(new MembershipsListView("external-memberships", "external-membership", user.getExternalMemberships()));

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
        add(new AjaxLinkLabel("edit-user", new ResourceModel("user-edit")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                panel.showEditForm(target, userModel);
            }
            @Override
            public boolean isVisible() {
                return !user.isExternal();
            }
        });
        add(new AjaxLinkLabel("set-user-password", new ResourceModel("user-set-password")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                panel.showPasswordForm(target, userModel);
            }
            @Override
            public boolean isVisible() {
                return !user.isExternal();
            }
        });
        
        add(new AjaxLinkLabel("delete-user", new ResourceModel("user-delete")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                panel.showDialog(new ConfirmDeleteDialog(userModel, this) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void ok() {
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
        String username = user.getUsername();
        try {
            user.delete();
            log.info("User '" + username + "' deleted by "
                    + ((UserSession) Session.get()).getCredentials().getStringValue("username"));
            UserDataProvider.countMinusOne();
            Session.get().info(getString("user-removed", model));
            panel.showList();
        } catch (RepositoryException e) {
            Session.get().warn(getString("user-remove-failed", model));
            log.error("Unable to delete user '" + username + "' : ", e);
        }
    }

    /** list view to be nested in the form. */
    private static final class MembershipsListView extends ListView {
        private static final long serialVersionUID = 1L;
        private String labelId;

        public MembershipsListView(final String id, final String labelId, final List<DetachableGroup> list) {
            super(id, list);
            this.labelId = labelId;
            setReuseItems(true);
        }

        protected void populateItem(ListItem item) {
            DetachableGroup dg = (DetachableGroup) item.getModelObject();
            item.add(new Label(labelId, dg.getGroup().getGroupname()));
        }
    }

}
