/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.console.menu.permissions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.security.Privilege;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.model.ReadOnlyModel;
import org.hippoecm.repository.api.HippoSession;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.security.SecurityService;
import org.onehippo.repository.security.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.repository.security.StandardPermissionNames.JCR_ACTIONS;
import static org.onehippo.repository.security.StandardPermissionNames.JCR_ALL;
import static org.onehippo.repository.security.StandardPermissionNames.JCR_ALL_PRIVILEGES;
import static org.onehippo.repository.security.StandardPermissionNames.JCR_WRITE;
import static org.onehippo.repository.security.StandardPermissionNames.JCR_WRITE_PRIVILEGES;

public class PermissionsDialog extends Dialog<Node> {

    private static final Logger log = LoggerFactory.getLogger(PermissionsDialog.class);

    private final IModel<String> selectedUserModel;
    private final Label membershipsLabel;
    private final Label actionsLabel;
    private final Label privilegesLabel;
    private final Label allPrivilegesLabel;

    public PermissionsDialog(final PermissionsPlugin plugin) {
        setSize(DialogConstants.LARGE_AUTO);
        setModel(plugin.getModel());

        selectedUserModel = Model.of("admin");

        final SecurityService securityService = HippoServiceRegistry.getService(SecurityService.class);
        final List<String> userIDs = new ArrayList<>();
        final Iterable<User> users = securityService.getUsers(0, 0);
        for (final User user : users) {
            userIDs.add(user.getId());
        }

        final DropDownChoice<String> userDropDown = new DropDownChoice<>("user", selectedUserModel,
                ReadOnlyModel.of(() -> userIDs));
        userDropDown.setRequired(true);
        userDropDown.setOutputMarkupId(true);
        userDropDown.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                loadPermissions();
                target.add(membershipsLabel, allPrivilegesLabel, actionsLabel, privilegesLabel);
            }
        });
        add(userDropDown);

        membershipsLabel = new Label("memberships", "None");
        membershipsLabel.setOutputMarkupId(true);
        add(membershipsLabel);

        final Label allActionsLabel = new Label("all-actions", "None");
        allActionsLabel.setDefaultModel(Model.of(StringUtils.join(JCR_ACTIONS, ", ")));
        add(allActionsLabel);

        allPrivilegesLabel = new Label("all-privileges", "None");
        allPrivilegesLabel.setOutputMarkupId(true);
        add(allPrivilegesLabel);

        actionsLabel = new Label("actions", "None");
        actionsLabel.setOutputMarkupId(true);
        add(actionsLabel);

        privilegesLabel = new Label("privileges", "None");
        privilegesLabel.setOutputMarkupId(true);
        add(privilegesLabel);

        loadPermissions();

        setOkVisible(false);
        setFocusOnCancel();
    }

    private void loadPermissions() {
        HippoSession selectedUserJcrSession = null;
        try {
            final Node subject = getModelObject();
            final String userID = selectedUserModel.getObject();
            final Session jcrSession = subject.getSession();
            selectedUserJcrSession = (HippoSession) jcrSession.impersonate(new SimpleCredentials(userID, new char[]{}));

            // getPrivileges() must be called before getSupportedPrivileges() to ensure they are all 'loaded'
            final String subjectPath = subject.getPath();
            final Set<String> privileges = getPrivileges(subjectPath, selectedUserJcrSession);
            if (privileges.contains(JCR_WRITE)) {
                privileges.removeAll(JCR_WRITE_PRIVILEGES);
            }
            if (privileges.contains(JCR_ALL)) {
                privileges.removeAll(JCR_ALL_PRIVILEGES);
                privileges.remove(JCR_WRITE);
            }
            final Set<String> supportedPrivileges = getSupportedPrivileges(subjectPath, selectedUserJcrSession);
            final Set<String> actions = getAllowedActions(subjectPath, selectedUserJcrSession, JCR_ACTIONS);

            membershipsLabel.setDefaultModel(Model.of(StringUtils.join(getMemberships(selectedUserJcrSession), ", ")));
            allPrivilegesLabel.setDefaultModel(Model.of(StringUtils.join(supportedPrivileges, ", ")));
            actionsLabel.setDefaultModel(Model.of(StringUtils.join(actions, ", ")));
            privilegesLabel.setDefaultModel(Model.of(StringUtils.join(privileges, ", ")));

        } catch (RepositoryException ex) {
            actionsLabel.setDefaultModel(Model.of(ex.getClass().getName() + ": " + ex.getMessage()));
        } finally {
            if (selectedUserJcrSession != null) {
                selectedUserJcrSession.logout();
            }
        }
    }

    private static Set<String> getAllowedActions(final String nodePath, final Session session, final Set<String> actions)
            throws RepositoryException {
        final Set<String> allowedActions = new TreeSet<>();
        for (final String action : actions) {
            if (session.hasPermission(nodePath, action)) {
                allowedActions.add(action);
            }
        }
        return allowedActions;
    }

    private static Set<String> getPrivileges(final String nodePath, final Session session) throws RepositoryException {
        return Arrays.stream(session.getAccessControlManager().getPrivileges(nodePath))
                .map(Privilege::getName)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static Set<String> getSupportedPrivileges(final String nodePath, final Session session) throws RepositoryException {
        return Arrays.stream(session.getAccessControlManager().getSupportedPrivileges(nodePath))
                .map(Privilege::getName)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static Set<String> getMemberships(final HippoSession userSession) throws RepositoryException {
        return new TreeSet<>(userSession.getUser().getMemberships());
    }

    public IModel<String> getTitle() {
        final IModel<Node> nodeModel = getModel();
        String path;
        try {
            path = nodeModel.getObject().getPath();
        } catch (RepositoryException e) {
            path = e.getMessage();
            log.warn("Unable to get path for : " + nodeModel);
        }
        return Model.of("Permissions for " + path);
    }
}
