/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.security.group;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.transaction.NotSupportedException;

import org.apache.jackrabbit.commons.iterator.NodeIteratorAdapter;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.security.ManagerContext;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_SECURITYPROVIDER;
import static org.hippoecm.repository.api.HippoNodeType.NT_GROUP;
import static org.hippoecm.repository.api.HippoNodeType.NT_GROUPFOLDER;
import static org.hippoecm.repository.security.SecurityManager.INTERNAL_PROVIDER;
import static org.hippoecm.repository.util.JcrUtils.getStringProperty;

/**
 * Abstract group manager for managing groups.
 * <p>
 * The rawGroupId's are the id's as provided by the backend. The groupId's are the normalized id's. All id's MUST be
 * normalized before they are stored in the database.
 */
public abstract class AbstractGroupManager implements GroupManager {

    /**
     * The system/root session
     */
    protected Session session;

    /**
     * The path from the root containing the groups
     */
    protected String groupsPath;

    /**
     * The path from the root containing the users
     */
    protected String providerPath;

    /**
     * Is the class initialized
     */
    protected boolean initialized = false;

    /**
     * The id of the provider that this manager instance belongs to
     */
    protected String providerId;

    /**
     * Number of dir levels: /u/s/user etc.
     */
    private int dirLevels = 0;

    /**
     * Logger
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public void init(ManagerContext context) throws RepositoryException {
        this.session = context.getSession();
        this.groupsPath = context.getPath();
        this.providerId = context.getProviderId();
        this.providerPath = context.getProviderPath();
        setDirLevels();
        initManager(context);
    }

    public boolean isInitialized() {
        return initialized;
    }

    public final boolean hasGroup(String rawGroupId) throws RepositoryException {
        if (!isInitialized()) {
            throw new IllegalStateException("Not initialized.");
        }
        String path = buildGroupPath(rawGroupId);
        if (session.getRootNode().hasNode(path)) {
            Node group = session.getRootNode().getNode(path);
            if (group.getPrimaryNodeType().isNodeType(NT_GROUP)) {
                return true;
            }
        }
        return false;
    }

    public final Node getGroup(String rawGroupId) throws RepositoryException {
        if (!isInitialized()) {
            throw new IllegalStateException("Not initialized.");
        }
        String path = buildGroupPath(rawGroupId);
        if (session.getRootNode().hasNode(path)) {
            Node group = session.getRootNode().getNode(path);
            if (group.getPrimaryNodeType().isNodeType(NT_GROUP)) {
                return group;
            } else {
                return null;
            }
        }
        return null;
    }

    /**
     * Create a group node. Use the getNodeType to determine the type the node should be.
     */
    public final Node createGroup(String rawGroupId) throws RepositoryException {
        if (!isInitialized()) {
            throw new IllegalStateException("Not initialized.");
        }
        String groupId = sanitizeId(rawGroupId);
        log.trace("Creating node for group: {} in path: {}", groupId, groupsPath);
        int length = groupId.length();
        int pos = 0;
        Node groupsNode = session.getRootNode().getNode(groupsPath);
        for (int i = 0; i < dirLevels; i++) {
            if (i < length) {
                pos = i;
            }
            String c = NodeNameCodec.encode(Character.toLowerCase(groupId.charAt(pos)));
            if (!groupsNode.hasNode(c)) {
                groupsNode = groupsNode.addNode(c, NT_GROUPFOLDER);
            } else {
                groupsNode = groupsNode.getNode(c);
            }
        }
        Node group = groupsNode.addNode(NodeNameCodec.encode(groupId, true), getNodeType());
        group.setProperty(HippoNodeType.HIPPO_MEMBERS, new Value[]{});
        if (!INTERNAL_PROVIDER.equals(providerId)) {
            group.setProperty(HIPPO_SECURITYPROVIDER, providerId);
            log.debug("Group: {} created by {} ", groupId, providerId);
        }
        return group;
    }

    @Override
    public NodeIterator listGroups(long offset, long limit) throws RepositoryException {
        return listGroups(null, offset, limit);
    }

    @Override
    public NodeIterator listGroups(final String providerId, long offset, long limit) throws RepositoryException {
        if (!isInitialized()) {
            throw new IllegalStateException("Not initialized.");
        }
        final StringBuilder statement = new StringBuilder();
        statement.append("//element");
        statement.append("(*, ").append(NT_GROUP).append(")");
        if (providerId != null) {
            statement.append('[');
            statement.append("@");
            statement.append(HIPPO_SECURITYPROVIDER).append("= '").append(providerId).append("'");
            statement.append(']');
        }
        statement.append(" order by @jcr:name");

        final Query q = session.getWorkspace().getQueryManager().createQuery(statement.toString(), Query.XPATH);
        if (offset > 0) {
            q.setOffset(offset);
        }
        if (limit > 0) {
            q.setLimit(limit);
        }
        final QueryResult result = q.execute();
        return result.getNodes();
    }

    /**
     * Helper for building group path including the groupname itself. Takes care of the encoding of the path AND the
     * groupId (the eventual node name)
     *
     * @param rawGroupId unencoded groupId
     * @return the fully encoded normalized path
     */
    private String buildGroupPath(String rawGroupId) {
        String groupId = sanitizeId(rawGroupId);
        if (dirLevels == 0) {
            return groupsPath + "/" + NodeNameCodec.encode(groupId, true);
        }
        int length = groupId.length();
        int pos = 0;
        StringBuilder path = new StringBuilder(groupsPath);
        for (int i = 0; i < dirLevels; i++) {
            if (i < length) {
                pos = i;
            }
            path.append('/').append(NodeNameCodec.encode(Character.toLowerCase(groupId.charAt(pos))));
        }
        path.append('/').append(NodeNameCodec.encode(groupId, true));
        return path.toString();
    }

    /**
     * Sanitize the rawId: trim and convert to lowercase if needed. This function does NOT encode the groupId.
     *
     * @param rawGroupId
     * @return the trimmed and if needed converted to lowercase groupId
     */
    private String sanitizeId(final String rawGroupId) {
        if (rawGroupId == null) {
            // anonymous
            return null;
        }
        if (isCaseSensitive()) {
            return rawGroupId.trim();
        } else {
            return rawGroupId.trim().toLowerCase();
        }
    }

    private void setDirLevels() {
        dirLevels = 0;
        String relPath = providerPath + "/" + HippoNodeType.NT_GROUPPROVIDER;
        try {
            if (session.getRootNode().hasNode(relPath)) {
                Node n = session.getRootNode().getNode(relPath);
                if (n.hasProperty(HippoNodeType.HIPPO_DIRLEVELS)) {
                    dirLevels = (int) n.getProperty(HippoNodeType.HIPPO_DIRLEVELS).getLong();
                    // long -> int overflow
                    if (dirLevels < 0) {
                        dirLevels = 0;
                    }
                }
            }
        } catch (RepositoryException e) {
            log.info("Dirlevels setting not found, using 0 for user manager for provider: " + providerId);
        }
        if (log.isDebugEnabled()) {
            log.debug("Using dirlevels '" + dirLevels + "' for provider: " + providerId);
        }
    }

    public final Node getOrCreateGroup(String rawGroupId) throws RepositoryException {
        if (hasGroup(rawGroupId)) {
            return getGroup(rawGroupId);
        }
        return createGroup(rawGroupId);
    }

    protected final void updateSyncDate(Node group) throws RepositoryException {
        if (group.isNodeType(HippoNodeType.NT_EXTERNALUSER)) {
            group.setProperty(HippoNodeType.HIPPO_LASTSYNC, Calendar.getInstance());
        }
    }

    public final boolean isManagerForGroup(Node group) throws RepositoryException {
        if (group.hasProperty(HIPPO_SECURITYPROVIDER)) {
            return providerId.equals(group.getProperty(HIPPO_SECURITYPROVIDER).getString());
        } else {
            return INTERNAL_PROVIDER.equals(providerId);
        }
    }

    public final NodeIterator getMemberships(String rawUserId) throws RepositoryException {
        return getMemberships(rawUserId, null);
    }

    public final NodeIterator getMemberships(String rawUserId, String providerId) throws RepositoryException {
        final String userId = rawUserId != null ? NodeNameCodec.decode(sanitizeId(rawUserId)) : null;
        final Node groupsFolder = session.getRootNode().getNode(groupsPath);
        return new NodeIteratorAdapter(getMembershipsByPath(userId, providerId, groupsFolder, 0));
    }

    public final Set<String> getMembershipIds(String userId) {
        return getMembershipIds(userId, null);
    }

    public final Set<String> getMembershipIds(String userId, String providerId) {
        final Set<String> groupIds = new HashSet<>();
        try {
            Node groupsFolder = session.getRootNode().getNode(groupsPath);
            for (Node groupNode : getMembershipsByPath(userId, providerId, groupsFolder, 0)) {
                groupIds.add(NodeNameCodec.decode(groupNode.getName()));
            }
        } catch (RepositoryException e) {
            log.error("Error while getting membership ids", e);
        }
        return groupIds;
    }

    private Set<Node> getMembershipsByPath(final String userId, final String providerId, final Node groupFolder,
                                           final int level) {
        final Set<Node> groups = new HashSet<>();
        try {
            for (Node groupNode : new NodeIterable(groupFolder.getNodes())) {
                if (groupNode.isNodeType(NT_GROUP)) {
                    for (String memberId : getMembers(groupNode)) {
                        if ("*".equals(memberId) || memberId.equals(userId)) {
                            if (providerId == null ||
                                    providerId.equals(getStringProperty(groupNode, HIPPO_SECURITYPROVIDER, null))) {
                                groups.add(groupNode);
                            }
                        }
                    }
                } else if (groupNode.isNodeType(NT_GROUPFOLDER) && level < dirLevels) {
                    groups.addAll(getMembershipsByPath(userId, providerId, groupNode, level + 1));
                }
            }
        } catch (RepositoryException e) {
            log.error("Error while getting membership ids", e);
        }
        return groups;
    }

    public final void syncMemberships(Node user) throws RepositoryException {
        if (!isExternal()) {
            return;
        }
        String userId = user.getName();
        Set<String> repositoryMemberships = getMembershipIds(userId, providerId);
        Set<String> backendMemberships = new HashSet<>();
        for (String groupId : backendGetMemberships(user)) {
            backendMemberships.add(sanitizeId(groupId));
        }
        Set<String> inSync = new HashSet<>();
        for (String groupId : repositoryMemberships) {
            if (backendMemberships.contains(groupId)) {
                inSync.add(groupId);
            }
        }
        repositoryMemberships.removeAll(inSync);
        backendMemberships.removeAll(inSync);


        // remove memberships that have been removed in the backend
        for (String groupId : repositoryMemberships) {
            Node group = getGroup(groupId);
            if (group != null) {
                log.debug("Remove membership of user '{}' for group '{}' by provider '{}'", userId, groupId, providerId);
                removeMember(group, userId);
            }
        }
        // add memberships that have been added in the backend
        for (String groupId : backendMemberships) {
            Node group = getOrCreateGroup(groupId);
            log.debug("Add membership of user '{}' for group '{}' by provider '{}'", userId, groupId, providerId);
            addMember(group, userId);
        }
    }

    public final Set<String> getMembers(Node group) throws RepositoryException {
        final Property membersProperty = JcrUtils.getPropertyIfExists(group, HippoNodeType.HIPPO_MEMBERS);
        if (membersProperty != null) {
            Value[] values = membersProperty.getValues();
            final Set<String> members = new HashSet<>(values.length);
            for (final Value value : values) {
                members.add(value.getString());
            }
            return members;
        }
        return new HashSet<>();
    }

    public final void setMembers(Node group, Set<String> members) throws RepositoryException {
        if (!isManagerForGroup(group)) {
            log.warn("Group '" + group.getName() + "' is not managed by provider '" + providerId
                    + "' skipping setMembers");
            return;
        }
        String[] normalizedMembers = new String[members.size()];
        int i = 0;
        for (String member : members) {
            normalizedMembers[i] = sanitizeId(member);
            i++;
        }
        group.setProperty(HippoNodeType.HIPPO_MEMBERS, normalizedMembers);
    }

    public final void addMember(Node group, String rawUserId) throws RepositoryException {
        String userId = sanitizeId(rawUserId);
        if (!isManagerForGroup(group)) {
            log.warn("Group '" + group.getName() + "' is not managed by provider '" + providerId
                    + "' skipping addMember '" + userId + "'");
            return;
        }
        Set<String> members = getMembers(group);
        if (!members.contains(userId)) {
            members.add(userId);
            setMembers(group, members);
        }
    }

    public final void removeMember(Node group, String rawUserId) throws RepositoryException {
        String userId = sanitizeId(rawUserId);
        if (!isManagerForGroup(group)) {
            log.warn("Group '" + group.getName() + "' is not managed by provider '" + providerId
                    + "' skipping removeMember '" + userId + "'");
            return;
        }
        Set<String> members = getMembers(group);
        if (members.contains(userId)) {
            members.remove(userId);
            setMembers(group, members);
        }
    }

    public final void saveGroups() throws RepositoryException {
        try {
            session.refresh(true);
            session.getRootNode().getNode(groupsPath).save();
        } catch (InvalidItemStateException e) {
            log.warn("Unable to save synced group data, this usually happens when the group node"
                    + " was simultaneously changed by another session: " + e.getMessage());
            log.debug("StackTrace: ", e);
            // discard changes in session
            session.refresh(false);
        }
    }

    public boolean backendCreateGroup(String groupId) throws NotSupportedException, RepositoryException {
        throw new NotSupportedException("Add group not supported.");
    }

    public boolean backendDeleteGroup(String groupId) throws NotSupportedException, RepositoryException {
        throw new NotSupportedException("Delete group not supported.");
    }
}
