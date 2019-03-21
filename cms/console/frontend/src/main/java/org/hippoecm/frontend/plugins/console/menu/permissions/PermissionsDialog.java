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

import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.Query;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.Dialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PermissionsDialog extends Dialog<Node> {

    private static final long serialVersionUID = 1L;

    /**
     * predefined action constants in checkPermission
     */
    public static final String READ_ACTION = "read";
    public static final String REMOVE_ACTION = "remove";
    public static final String ADD_NODE_ACTION = "add_node";
    public static final String SET_PROPERTY_ACTION = "set_property";

    public static final String[] JCR_ACTIONS = new String[]{READ_ACTION, REMOVE_ACTION, ADD_NODE_ACTION,
            SET_PROPERTY_ACTION};

    /**
     * Predefined jcr privileges
     */
    public static final String READ_PRIVILEGE = "jcr:read";
    public static final String WRITE_PRIVILEGE = "jcr:write";
    public static final String ALL_PRIVILEGE = "jcr:all";
    public static final String SET_PROPERTIES_PRIVILEGE = "jcr:setProperties";
    public static final String ADD_CHILD_PRIVILEGE = "jcr:addChildNodes";
    public static final String REMOVE_CHILD_PRIVILEGE = "jcr:removeChildNodes";

    /**
     * Predefined hippo privileges
     */
    public static final String AUTHOR_PRIVILEGE = "hippo:author";
    public static final String EDITOR_PRIVILEGE = "hippo:editor";
    public static final String ADMIN_PRIVILEGE = "hippo:admin";

    public static final String[] JCR_PRIVILEGES = new String[]{READ_PRIVILEGE, WRITE_PRIVILEGE, ALL_PRIVILEGE,
            SET_PROPERTIES_PRIVILEGE, ADD_CHILD_PRIVILEGE, REMOVE_CHILD_PRIVILEGE, AUTHOR_PRIVILEGE, EDITOR_PRIVILEGE,
            ADMIN_PRIVILEGE};

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
        Session privSession = null;
        try {
            Node subject = nodeModel.getObject();

            // FIXME: hardcoded workflowuser
            privSession = subject.getSession().impersonate(new SimpleCredentials("workflowuser", new char[]{}));

            String userID = subject.getSession().getUserID();
            String[] memberships = getMemberships(privSession, userID);
            String[] actions = getAllowedActions(subject, JCR_ACTIONS);
            String[] roles = getAllowedActions(subject, JCR_PRIVILEGES);

            usernameLabel.setDefaultModel(Model.of(userID));
            membershipsLabel.setDefaultModel(Model.of(StringUtils.join(memberships, ", ")));
            allActionsLabel.setDefaultModel(Model.of(StringUtils.join(JCR_ACTIONS, ", ")));
            allPrivilegesLabel.setDefaultModel(Model.of(StringUtils.join(JCR_PRIVILEGES, ", ")));
            actionsLabel.setDefaultModel(Model.of(StringUtils.join(actions, ", ")));
            privilegesLabel.setDefaultModel(Model.of(StringUtils.join(roles, ", ")));

        } catch (RepositoryException ex) {
            actionsLabel.setDefaultModel(Model.of(ex.getClass().getName() + ": " + ex.getMessage()));
        } finally {
            if (privSession != null) {
                privSession.logout();
            }
        }
        setOkVisible(false);
        setFocusOnCancel();
    }

    private boolean hasPermission(Node node, String actions) throws RepositoryException {
        try {
            node.getSession().checkPermission(node.getPath(), actions);
            return true;
        } catch (AccessControlException e) {
            return false;
        }
    }

    private String[] getAllowedActions(Node node, String[] actions) throws RepositoryException {
        final List<String> list = new ArrayList<>();
        for (String action : actions) {
            if (hasPermission(node, action)) {
                list.add(action);
            }
        }
        Collections.sort(list);
        return list.toArray(new String[0]);
    }

    private String[] getMemberships(Session session, String username) {
        final String queryString = "//element(*, hipposys:group)[jcr:contains(@hipposysedit:members, '" + username
                + "')]";
        final String queryType = "xpath";
        final List<String> list = new ArrayList<>();
        try {
            final Query query = session.getWorkspace().getQueryManager().createQuery(queryString, queryType);
            final NodeIterator nodeIter = query.execute().getNodes();
            log.debug("Number of memberships found with query '{}' : {}", queryString, nodeIter.getSize());
            while (nodeIter.hasNext()) {
                Node node = nodeIter.nextNode();
                if (node != null) {
                    list.add(node.getName());
                }
            }
        } catch (RepositoryException e) {
            log.error("Error executing query[" + queryString + "]", e);
        }
        Collections.sort(list);
        return list.toArray(new String[0]);
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
