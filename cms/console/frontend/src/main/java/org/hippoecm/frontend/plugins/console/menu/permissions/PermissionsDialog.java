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
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.model.ReadOnlyModel;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.security.HippoAccessManager;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.security.DomainInfoPrivilege;
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
    private final MultiLineLabel privilegesLabel;
    private String multiLinePrivileges;
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

        privilegesLabel = new MultiLineLabel("privileges",  new PropertyModel<String>(this, "multiLinePrivileges"));
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
            final SortedMap<String, DomainInfoPrivilege> privileges = getPrivileges(subjectPath, selectedUserJcrSession);
            if (privileges.containsKey(JCR_WRITE)) {
                JCR_WRITE_PRIVILEGES.forEach(s -> privileges.remove(s));
            }
            if (privileges.containsKey(JCR_ALL)) {
                JCR_ALL_PRIVILEGES.forEach(s -> privileges.remove(s));
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
                multiLinePrivileges = "<<none>>";
            } else {

                final StringBuilder builder = new StringBuilder();
                privileges.forEach((name, domainInfoPrivilege) -> {
                    builder.append("\n").append(name).append(" : ");
                    domainInfoPrivilege.getDomainsProvidingPrivilege().stream()
                            .forEach(domainPath -> builder.append("\n\t").append("domain: ").append(domainPath));

                });

                multiLinePrivileges = builder.toString();
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

    private static SortedMap<String, DomainInfoPrivilege> getPrivileges(final String nodePath, final Session session) throws RepositoryException {
        final DomainInfoPrivilege[] domainInfoPrivileges = ((HippoAccessManager) session.getAccessControlManager()).getPrivileges(nodePath);
        return privilegesToSortedMap(domainInfoPrivileges);


    }

    static SortedMap<String, DomainInfoPrivilege> privilegesToSortedMap(final DomainInfoPrivilege[] domainInfoPrivileges) {
        // there will never be two DomainInfoPrivilege with the same getName hence duplicate key merge exception won't happen
        return Arrays.stream(domainInfoPrivileges).collect(Collectors.toMap(
                DomainInfoPrivilege::getName,
                Function.identity(),
                (priv1, priv2) -> {
                    throw new IllegalStateException(String.format("Found two DomainInfoPrivilege objects with same name '%s' which " +
                            "should never be possible from HippoAccessManager#getPrivileges()", priv1.getName()));
                },
                () -> new TreeMap<>()));
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
