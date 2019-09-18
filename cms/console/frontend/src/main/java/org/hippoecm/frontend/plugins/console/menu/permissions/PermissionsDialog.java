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
    private final Label userRolesLabel;
    private final Label actionsLabel;
    private final Label privilegesLabel;
    private final Label exception;

    public PermissionsDialog(final PermissionsPlugin plugin) {
        setSize(DialogConstants.LARGE_AUTO);
        setModel(plugin.getModel());

        selectedUserModel = Model.of("<<unknown>>");

        membershipsLabel = new Label("memberships", "None");
        membershipsLabel.setOutputMarkupId(true);
        add(membershipsLabel);

        userRolesLabel = new Label("userRoles", "None");
        userRolesLabel.setOutputMarkupId(true);
        add(userRolesLabel);

        actionsLabel = new Label("actions", "None");
        actionsLabel.setOutputMarkupId(true);
        add(actionsLabel);

        privilegesLabel = new Label("privileges", "None");
        privilegesLabel.setOutputMarkupId(true);
        add(privilegesLabel);

        exception = new Label("exception", "None");
        exception.setOutputMarkupId(true);
        add(exception);

        final Node subject = getModelObject();
        try {
            final Session jcrSession = subject.getSession();
            selectedUserModel.setObject(jcrSession.getUserID());
        } catch (RepositoryException e) {
            log.error("Could not set selected user", e);
        }
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
                target.add(membershipsLabel, userRolesLabel, actionsLabel, privilegesLabel, exception);
            }
        });
        add(userDropDown);

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
            final Set<String> actions = getAllowedActions(subjectPath, selectedUserJcrSession, JCR_ACTIONS);
            final Set<String> userRoles = selectedUserJcrSession.getUser().getUserRoles();
            if (userRoles.isEmpty()) {
                userRolesLabel.setDefaultModel(Model.of("<<none>>"));
            } else {
                userRolesLabel.setDefaultModel(Model.of(StringUtils.join(userRoles, ", ")));
            }

            final Set<String> memberships = getMemberships(selectedUserJcrSession);
            if (memberships.isEmpty()) {
                membershipsLabel.setDefaultModel(Model.of("<<none>>"));
            } else {
                membershipsLabel.setDefaultModel(Model.of(StringUtils.join(memberships, ", ")));
            }
            if (actions.isEmpty()) {
                actionsLabel.setDefaultModel(Model.of("<<none>>"));
            } else {
                actionsLabel.setDefaultModel(Model.of(StringUtils.join(actions, ", ")));
            }
            if (privileges.isEmpty()) {
                privilegesLabel.setDefaultModel(Model.of("<<none>>"));
            } else {
                privilegesLabel.setDefaultModel(Model.of(StringUtils.join(privileges, ", ")));
            }
            exception.setDefaultModel(Model.of(""));
        } catch (RepositoryException ex) {
            userRolesLabel.setDefaultModel(Model.of(""));
            membershipsLabel.setDefaultModel(Model.of(""));
            actionsLabel.setDefaultModel(Model.of(""));
            privilegesLabel.setDefaultModel(Model.of(""));

            exception.setDefaultModel(Model.of(String.format("Exception happened: '%s': %s ", ex.getClass().getName(), ex.getMessage())));
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
