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

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.security.Privilege;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.repository.api.HippoSession;
import org.onehippo.repository.security.Group;
import org.onehippo.repository.security.StandardPermissionNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PermissionsDialog extends Dialog<Node> {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(PermissionsDialog.class);

    public PermissionsDialog(PermissionsPlugin plugin) {
        final IModel<Node> nodeModel = (IModel<Node>) plugin.getDefaultModel();
        setModel(nodeModel);

        final Label usernameLabel = new Label("username", "Unknown");
        final Label membershipsLabel = new Label("memberships", "None");
        final Label allActionsLabel = new Label("all-actions", "None");
        final Label allPrivilegesLabel = new Label("all-privileges", "None");
        final Label actionsLabel = new Label("actions", "None");
        final Label privilegesLabel = new Label("privileges", "None");
        add(usernameLabel);
        add(membershipsLabel);
        add(allActionsLabel);
        add(allPrivilegesLabel);
        add(actionsLabel);
        add(privilegesLabel);
        try {
            Node subject = nodeModel.getObject();

            // getPrivileges() must be called before getSupportedPrivileges() to ensure they are all 'loaded'
            Set<String> privileges = getPrivileges(subject);
            if (privileges.contains(StandardPermissionNames.JCR_WRITE)) {
                privileges.removeAll(StandardPermissionNames.JCR_WRITE_PRIVILEGES);
            }
            if (privileges.contains(StandardPermissionNames.JCR_ALL)) {
                privileges.removeAll(StandardPermissionNames.JCR_ALL_PRIVILEGES);
                privileges.remove(StandardPermissionNames.JCR_WRITE);
            }
            Set<String> supportedPrivileges = getSupportedPrivileges(subject);
            Set<String> actions = getAllowedActions(subject, StandardPermissionNames.JCR_ACTIONS);

            final HippoSession userSession = (HippoSession) subject.getSession();
            usernameLabel.setDefaultModel(Model.of(userSession.getUserID()));
            membershipsLabel.setDefaultModel(Model.of(StringUtils.join(getMemberships(userSession), ", ")));
            allActionsLabel.setDefaultModel(Model.of(StringUtils.join(StandardPermissionNames.JCR_ACTIONS, ", ")));
            allPrivilegesLabel.setDefaultModel(Model.of(StringUtils.join(supportedPrivileges, ", ")));
            actionsLabel.setDefaultModel(Model.of(StringUtils.join(actions, ", ")));
            privilegesLabel.setDefaultModel(Model.of(StringUtils.join(privileges, ", ")));

        } catch (RepositoryException ex) {
            actionsLabel.setDefaultModel(Model.of(ex.getClass().getName() + ": " + ex.getMessage()));
        }
        setOkVisible(false);
        setFocusOnCancel();
    }

    private Set<String> getAllowedActions(Node node, Set<String> actions) throws RepositoryException {
        final Set<String> allowedActions = new TreeSet<>();
        for (String action : actions) {
            if (node.getSession().hasPermission(node.getPath(), action)) {
                allowedActions.add(action);
            }
        }
        return allowedActions;
    }

    private Set<String> getPrivileges(Node node) throws RepositoryException {
        return Arrays.stream(node.getSession().getAccessControlManager().getPrivileges(node.getPath()))
                .map(Privilege::getName)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private Set<String> getSupportedPrivileges(Node node) throws RepositoryException {
        return Arrays.stream(node.getSession().getAccessControlManager().getSupportedPrivileges(node.getPath()))
                .map(Privilege::getName)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private Set<String> getMemberships(final HippoSession userSession) throws RepositoryException {
        final Set<String> memberships = new TreeSet<>();
        for (Group group : userSession.getUser().getMemberships()) {
            memberships.add(group.getId());
        }
        return memberships;
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
