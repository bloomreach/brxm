/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.DeleteDialog;
import org.hippoecm.frontend.session.UserSession;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.event.HippoEventConstants;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prompts the user if he's sure that he wants to delete a user and then executes the delete and sends an update event.
 */
public class DeleteUserDialog extends DeleteDialog<User> {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(DeleteUserDialog.class);

    private final IPluginContext context;
    private final AdminBreadCrumbPanel breadCrumbPanel;

    public DeleteUserDialog(final IModel<User> userModel, final Component component,
                            final IPluginContext context, final AdminBreadCrumbPanel breadCrumbPanel) {
        super(userModel, component);
        this.context = context;
        this.breadCrumbPanel = breadCrumbPanel;
    }

    @Override
    protected void onOk() {
        deleteUser(getModel());
    }

    @Override
    protected String getTitleKey() {
        return "user-delete-title";
    }

    @Override
    protected String getTextKey() {
        return "user-delete-text";
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
            Session.get().info(breadCrumbPanel.getString("user-removed", model));

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
;
        } catch (RepositoryException e) {
            Session.get().warn(getString("user-remove-failed", model));
            log.error("Unable to delete user '" + username + "' : ", e);
        }
    }
}
