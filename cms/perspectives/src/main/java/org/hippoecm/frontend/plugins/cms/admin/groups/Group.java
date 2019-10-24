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
package org.hippoecm.frontend.plugins.cms.admin.groups;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.jackrabbit.util.ISO9075;
import org.apache.jackrabbit.util.Text;
import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.plugins.cms.admin.SecurityManagerHelper;
import org.hippoecm.frontend.plugins.cms.admin.users.DetachableUser;
import org.hippoecm.frontend.plugins.cms.admin.users.SystemUserDataProvider;
import org.hippoecm.frontend.plugins.cms.admin.users.User;
import org.hippoecm.frontend.plugins.cms.admin.users.UserDataProvider;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.util.EventBusUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.event.HippoEventConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bloomreach.xm.repository.security.Role;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_SYSTEM;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_USERROLES;

public class Group implements Comparable<Group>, IClusterable {

    private static final Logger log = LoggerFactory.getLogger(Group.class);

    private static final String PROP_DESCRIPTION = "hipposys:description";
    private static final String QUERY_ALL_LOCAL = "select * from hipposys:group where hipposys:securityprovider='internal' and (hipposys:system <> 'true' or hipposys:system IS NULL)";
    private static final String QUERY_ALL = "select * from hipposys:group";
    private static final String QUERY_GROUP = "SELECT * FROM hipposys:group WHERE fn:name()='{}'";
    private static final char SLASH = '/';

    private String path;
    private String groupname;

    private String description;
    private boolean external;
    private boolean system;
    private List<String> userRoles;

    private transient Node node;

    public static QueryManager getQueryManager() throws RepositoryException {
        return UserSession.get().getQueryManager();
    }

    public static boolean exists(final String groupName) {
        return getGroup(groupName) != null;
    }

    @FunctionalInterface
    private interface QueryResultMapper<T> {
        T map(final Node node) throws RepositoryException;
    }

    private static <T extends Comparable<T>> List<T> executeQuery(final String sql, final QueryResultMapper<T> mapper)
            throws RepositoryException {
        @SuppressWarnings("deprecation")
        final Query query = getQueryManager().createQuery(sql, Query.SQL);
        final NodeIterator iter = query.execute().getNodes();

        final List<T> result = new ArrayList<>();
        while (iter.hasNext()) {
            final Node node = iter.nextNode();
            if (node != null) {
                try {
                    result.add(mapper.map(node));
                } catch(final RepositoryException e) {
                    log.warn("Unable to map node to result list", e);
                }
            }
        }
        // TODO: remove when query can sort on node names
        Collections.sort(result);
        return result;
    }

    public static List<Group> getLocalGroups() {
        try {
            return executeQuery(QUERY_ALL_LOCAL, Group::new);
        } catch (final RepositoryException e) {
            log.error("Error while querying for a list of local groups", e);
        }
        return Collections.emptyList();
    }

    public static List<Group> getAllGroups() {
        try {
            return executeQuery(QUERY_ALL, Group::new);
        } catch (final RepositoryException e) {
            log.error("Error while querying for a list of all groups", e);
        }
        return Collections.emptyList();
    }

    /*
    * FIXME: should move to roles class or something the like when the admin perspective gets support for it
    *
    * @return A list of all roles defined in the system
    */
    public static List<String> getAllRoles() {
        return SecurityManagerHelper.getRolesProvider().getRoles()
                .stream().map(Role::getName).sorted().collect(Collectors.toList());
    }

    /**
     * Gets the Group with the specified name. If no Group with the specified name exists, null is returned.
     *
     * @param groupName the name of the Group to return
     * @return the Group with name groupName
     */
    public static Group getGroup(final String groupName) {
        final String escapedGroupName = Text.escapeIllegalJcr10Chars(ISO9075.encode(NodeNameCodec.encode(groupName, true)));
        final String queryString = QUERY_GROUP.replace("{}", escapedGroupName);
        try {
            @SuppressWarnings("deprecation")
            final Query query = getQueryManager().createQuery(queryString, Query.SQL);
            final QueryResult queryResult = query.execute();
            final NodeIterator iterator = queryResult.getNodes();
            if (!iterator.hasNext()) {
                return null;
            }
            return new Group(iterator.nextNode());
        } catch (RepositoryException e) {
            log.error("Unable to check if group '{}' exists, returning true", groupName, e);
            return null;
        }
    }

    public static Group newGroup(final String groupname)
    {
        Group group = new Group();
        group.setGroupname(groupname);
        return group;
    }

    public boolean isExternal() {
        return external;
    }

    public boolean isSystem() {
        return system;
    }

    public String getGroupname() {
        return groupname;
    }

    @SuppressWarnings({"unused"})
    public void setGroupname(final String groupname) {
        this.groupname = groupname;
    }

    public String getPath() {
        return path;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) throws RepositoryException {
        this.description = description;
    }

    //----------------------- constructors ---------//
    public Group() {
    }

    public Group(final Node node) throws RepositoryException {
        this.node = node;
        path = node.getPath().substring(1);
        groupname = NodeNameCodec.decode(node.getName());
        external = node.isNodeType(HippoNodeType.NT_EXTERNALGROUP);
        system = JcrUtils.getBooleanProperty(node, HIPPO_SYSTEM, false);
        userRoles = new ArrayList<>(JcrUtils.getStringSetProperty(node, HIPPO_USERROLES, Collections.emptySet()));
        Collections.sort(userRoles);
        if (node.hasProperty(PROP_DESCRIPTION)) {
            description = node.getProperty(PROP_DESCRIPTION).getString();
        } else if (node.hasProperty("description")) {
            description = node.getProperty("description").getString();
        }
    }

    /**
     * Returns all non-system users that are part of this group, including users that don't exist anymore
     * @return a list of {@link User}s
     * @throws RepositoryException
     */
    public List<String> getMembers() throws RepositoryException {
        return getMembers(true/* excludeSystemUsers */);
    }

    private List<String> getAllMembers() throws RepositoryException {
        return getMembers(false/* excludeSystemUsers */);
    }

    private List<String> getMembers(final boolean excludeSystemUsers) throws RepositoryException {
        final List<String> members = new ArrayList<>();
        if (node.hasProperty(HippoNodeType.HIPPO_MEMBERS)) {
            final Value[] storedMembers = node.getProperty(HippoNodeType.HIPPO_MEMBERS).getValues();

            // do query for system users only when needed
            final Set<String> systemUserNames = excludeSystemUsers ? getSystemUserNames() : Collections.emptySet();

            for (final Value value : storedMembers) {
                final String userName = value.getString();

                if (excludeSystemUsers && systemUserNames.contains(userName)) {
                    continue;
                }

                members.add(userName);
            }
        }
        Collections.sort(members);
        return members;
    }

    public List<String> getUserRoles() {
        return userRoles;
    }

    public void addUserRole(final String userRole) throws RepositoryException {
        Set<String> currentUserRoles = JcrUtils.getStringSetProperty(node, HIPPO_USERROLES, new HashSet<>());
        if (currentUserRoles.add(userRole)) {
            node.setProperty(HIPPO_USERROLES, currentUserRoles.toArray(new String[0]));
            node.getSession().save();
            userRoles = new ArrayList<>(currentUserRoles);
            Collections.sort(userRoles);
        }
    }

    public void removeUserRole(final String userRole) throws RepositoryException {
        Set<String> currentUserRoles = JcrUtils.getStringSetProperty(node, HIPPO_USERROLES, new HashSet<>());
        if (currentUserRoles.remove(userRole)) {
            if (currentUserRoles.isEmpty()) {
                Property userRolesProperty = JcrUtils.getPropertyIfExists(node, HIPPO_USERROLES);
                if (userRolesProperty != null) {
                    userRolesProperty.remove();
                }
            } else {
                node.setProperty(HIPPO_USERROLES, currentUserRoles.toArray(new String[0]));
            }
            node.getSession().save();
            userRoles = new ArrayList<>(currentUserRoles);
            Collections.sort(userRoles);
        }
    }

    public List<DetachableUser> getMembersAsDetachableUsers() {
        final List<String> usernames;
        try {
            usernames = getMembers();
        } catch (RepositoryException e) {
            throw new IllegalStateException("Cannot get members for this group", e);
        }

        final List<DetachableUser> users = new ArrayList<>();
        for (final String username : usernames) {
            if (!User.userExists(username)) {
                continue;
            }
            final User user = new User(username);
            final DetachableUser detachableUser = new DetachableUser(user);
            users.add(detachableUser);
        }

        return users;
    }

    private Set<String> getSystemUserNames() {
        final UserDataProvider dataProvider = new SystemUserDataProvider();
        final Iterator<User> iterator = dataProvider.iterator(0, dataProvider.size());
        final Set<String> names = new HashSet<>();
        while (iterator.hasNext()) {
            final String systemUserName = iterator.next().getUsername();
            names.add(systemUserName);
        }
        return names;
    }

    //-------------------- persistence helpers ----------//

    /**
     * Wrapper needed for spi layer which doesn't know if a property exists or not
     */
    private void setOrRemoveStringProperty(Node node, String name, String value) throws RepositoryException {
        if (value == null && !node.hasProperty(name)) {
            return;
        }
        node.setProperty(name, value);
    }

    /**
     * Create a new group
     *
     * @throws RepositoryException
     */
    public void create() throws RepositoryException {
        if (exists(getGroupname())) {
            throw new RepositoryException("Group already exists");
        }

        // FIXME: should be delegated to a groupmanager
        final String relPath = new StringBuilder()
                .append(HippoNodeType.CONFIGURATION_PATH)
                .append(SLASH)
                .append(HippoNodeType.GROUPS_PATH)
                .append(SLASH)
                .append(NodeNameCodec.encode(getGroupname(), true))
                .toString();

        node = UserSession.get().getRootNode().addNode(relPath, HippoNodeType.NT_GROUP);
        setOrRemoveStringProperty(node, PROP_DESCRIPTION, getDescription());
        // save parent when adding a node
        node.getParent().getSession().save();
    }

    /**
     * save the current group
     *
     * @throws RepositoryException
     */
    public void save() throws RepositoryException {
        if (node.isNodeType(HippoNodeType.NT_GROUP)) {
            setOrRemoveStringProperty(node, PROP_DESCRIPTION, getDescription());
            node.getSession().save();
        } else {
            throw new RepositoryException("Only hipposys:group's can be edited.");
        }
    }

    /**
     * Delete the current group.
     *
     * @throws RepositoryException
     */
    public void delete() throws RepositoryException {

        final Node parent = node.getParent();
        node.remove();
        parent.getSession().save();

        EventBusUtils.post("delete-group", HippoEventConstants.CATEGORY_GROUP_MANAGEMENT, "deleted group " + groupname);
    }

    public void removeMembership(String user) throws RepositoryException {
        final List<String> members = getAllMembers();
        members.remove(user);
        node.setProperty(HippoNodeType.HIPPO_MEMBERS, members.toArray(new String[members.size()]));
        node.getSession().save();
    }

    public void addMembership(String user) throws RepositoryException {
        final List<String> members = getAllMembers();
        members.add(user);
        node.setProperty(HippoNodeType.HIPPO_MEMBERS, members.toArray(new String[members.size()]));
        node.getSession().save();
    }

    //--------------------- default object -------------------//

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        final Group other = (Group) obj;
        return other.getPath().equals(getPath());
    }

    public int hashCode() {
        return path == null ? 0 : path.hashCode();
    }

    public int compareTo(Group o) {
        return groupname.compareTo(o.getGroupname());
    }
}
