/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.behaviors.OnEnterAjaxBehavior;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.security.HippoAccessManager;
import org.onehippo.repository.security.DomainInfoPrivilege;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.repository.security.StandardPermissionNames.JCR_ALL;
import static org.onehippo.repository.security.StandardPermissionNames.JCR_ALL_PRIVILEGES;
import static org.onehippo.repository.security.StandardPermissionNames.JCR_WRITE;
import static org.onehippo.repository.security.StandardPermissionNames.JCR_WRITE_PRIVILEGES;

public class PermissionsDialog extends Dialog<Node> {

    private static final Logger log = LoggerFactory.getLogger(PermissionsDialog.class);

    @SuppressWarnings({"FieldCanBeLocal", "unused"}) private String selectedUser = "<<unknown>>";
    @SuppressWarnings({"FieldCanBeLocal", "unused"}) private String memberships = "None";
    @SuppressWarnings({"FieldCanBeLocal", "unused"}) private String userRoles = "None";
    @SuppressWarnings({"FieldCanBeLocal", "unused"}) private String privileges = "None";
    @SuppressWarnings({"FieldCanBeLocal", "unused"}) private String exception = "None";

    PermissionsDialog(final PermissionsPlugin plugin) {
        setSize(DialogConstants.LARGE_AUTO);
        setModel(plugin.getModel());
        setOkVisible(false);
        setCancelLabel("Close");
        setFocusOnCancel();

        final Label membershipsLabel = new Label("memberships", PropertyModel.of(this, "memberships"));
        membershipsLabel.setOutputMarkupId(true);
        add(membershipsLabel);

        final Label userRolesLabel = new Label("userRoles", PropertyModel.of(this, "userRoles"));
        userRolesLabel.setOutputMarkupId(true);
        add(userRolesLabel);

        final MultiLineLabel privilegesLabel = new MultiLineLabel("privileges", PropertyModel.of(this, "privileges"));
        privilegesLabel.setOutputMarkupId(true);
        privilegesLabel.setEscapeModelStrings(false);
        add(privilegesLabel);

        final Label exceptionLabel = new Label("exception", PropertyModel.of(this, "exception"));
        exceptionLabel.setOutputMarkupId(true);
        add(exceptionLabel);

        final Node subject = getModelObject();
        try {
            final Session jcrSession = subject.getSession();
            selectedUser = jcrSession.getUserID();
        } catch (RepositoryException e) {
            log.error("Could not set selected user", e);
        }

        final TextField<String> userField = new TextField<>("user", PropertyModel.of(this, "selectedUser"));
        userField.add(new OnEnterAjaxBehavior() {
            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                loadPermissions();
                target.add(PermissionsDialog.this);
            }
        });
        add(userField);

        final AjaxSubmitLink findUser = new AjaxSubmitLink("findUser") {
            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                loadPermissions();
                target.add(PermissionsDialog.this);
            }
        };
        add(findUser);

        loadPermissions();
    }

    private void loadPermissions() {
        final Node subject = getModelObject();

        HippoSession selectedUserJcrSession = null;
        try {
            final Session jcrSession = subject.getSession();
            selectedUserJcrSession = (HippoSession) jcrSession.impersonate(new SimpleCredentials(selectedUser, new char[]{}));

            try {
                // The path for the selected user can be *different* then the path from the subject node! This is because
                // SNS variants below a handle get reshuffled based on read access in the repository in
                // HippoLocalItemStateManager#reorderHandleChildNodeEntries : hence first try to fetch the node with the
                // user session and then get the path from the fetched node : as a result, for example the path from
                // subject.getNode().getPath() can be for example /content/documents/mydoc/mydoc[3] while below the
                // subjectPathForSelectedUser can have become /content/documents/mydoc/mydoc
                final String subjectPathForSelectedUser = selectedUserJcrSession.getNodeByIdentifier(
                        subject.getIdentifier()).getPath();
                final SortedMap<String, DomainInfoPrivilege> privilegesSet = getPrivileges(subjectPathForSelectedUser,
                        selectedUserJcrSession);
                if (privilegesSet.containsKey(JCR_WRITE)) {
                    JCR_WRITE_PRIVILEGES.forEach(privilegesSet::remove);
                }
                if (privilegesSet.containsKey(JCR_ALL)) {
                    JCR_ALL_PRIVILEGES.forEach(privilegesSet::remove);
                    privilegesSet.remove(JCR_WRITE);
                }

                privileges = getPrivileges(privilegesSet);

            } catch (ItemNotFoundException e) {

                privileges = "<<none>>";
                log.info("Node '{}' not readable by session '{}'", subject.getIdentifier(), selectedUser);
            }

            final Set<String> userRolesSet = new TreeSet<>(selectedUserJcrSession.getUser().getUserRoles());
            if (userRolesSet.isEmpty()) {
                userRoles = "<<none>>";
            } else {
                userRoles = StringUtils.join(userRolesSet, ", ");
            }

            final Set<String> membershipsSet = getMemberships(selectedUserJcrSession);
            if (membershipsSet.isEmpty()) {
                memberships = "<<none>>";
            } else {
                memberships = StringUtils.join(membershipsSet, ", ");
            }

            exception = "";
        } catch (LoginException ex) {
            resetLabels();
            exception = String.format("Failed to load permissions of user '%s'. Does the user actually exist? ", selectedUser);
        } catch (RepositoryException ex) {
            resetLabels();
            exception = String.format("Exception happened: '%s': %s ", ex.getClass().getName(), ex.getMessage());
        } finally {
            if (selectedUserJcrSession != null) {
                selectedUserJcrSession.logout();
            }
        }
    }

    private String getPrivileges(final SortedMap<String, DomainInfoPrivilege> privilegesSet) {
        final StringBuilder builder = new StringBuilder();
        privilegesSet.forEach((name, domainInfoPrivilege) -> {
            builder.append(name).append("<ul>");
            domainInfoPrivilege.getDomainPaths().forEach(domainPath ->
                    builder.append("<li>").append("domain: ").append(domainPath).append("</li>"));
            builder.append("</ul>");
        });
        return builder.toString();
    }

    private void resetLabels() {
        userRoles = "";
        memberships = "";
        privileges = "";
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
                TreeMap::new));
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
