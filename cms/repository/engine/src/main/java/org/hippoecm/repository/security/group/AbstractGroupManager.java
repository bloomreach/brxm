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
package org.hippoecm.repository.security.group;

import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.transaction.NotSupportedException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.security.ManagerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractGroupManager implements GroupManager {

    @SuppressWarnings("unused")
    final static String SVN_ID = "$Id$";

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

    public final boolean hasGroup(String groupId) throws RepositoryException {
        if (!isInitialized()) {
            throw new IllegalStateException("Not initialized.");
        }

        // dirLevels 0, common case and default users
        if (session.getRootNode().hasNode(groupsPath + "/" + groupId)) {
            return true;
        }
        if (dirLevels > 0) {
            return session.getRootNode().hasNode(buildGroupPath(groupId, dirLevels));
        }
        return false;
    }
    

    public final Node getGroup(String groupId) throws RepositoryException {
        if (!isInitialized()) {
            throw new IllegalStateException("Not initialized.");
        }
        // dirLevels 0, common case
        try {
            return session.getRootNode().getNode(groupsPath + "/" + groupId);
        } catch (PathNotFoundException e) {
            if (dirLevels > 0) {
                try {
                    return session.getRootNode().getNode(buildGroupPath(groupId, dirLevels));
                } catch (PathNotFoundException e1) {
                    // noop
                }
            }
        }
        return null;
    }

    /**
     * Create a group node. Use the getNodeType to determine the type the node 
     * should be.
     */
    public final Node createGroup(String groupId) throws RepositoryException {
        if (!isInitialized()) {
            throw new IllegalStateException("Not initialized.");
        }
        log.trace("Creating node for group: {} in path: {}", groupId, groupsPath);
        int length = groupId.length();
        Node groupsNode = session.getRootNode().getNode(groupsPath);
        for (int i = 0; i < dirLevels && i < length; i++) {
            String c = NodeNameCodec.encode(Character.toLowerCase(groupId.charAt(i)));
            if (!groupsNode.hasNode(c)) {
                groupsNode = groupsNode.addNode(c, HippoNodeType.NT_GROUPFOLDER);
            } else {
                groupsNode = groupsNode.getNode(c);
            }
        }
        Node group = groupsNode.addNode(groupId, getNodeType());
        group.setProperty(HippoNodeType.HIPPO_MEMBERS, new Value[] {});
        if (!org.hippoecm.repository.security.SecurityManager.INTERNAL_PROVIDER.equals(providerId)) {
            group.setProperty(HippoNodeType.HIPPO_SECURITYPROVIDER, providerId);
            log.debug("Group: {} created by {} ", groupId, providerId);
        }
        return group;
    }


    private String buildGroupPath(String groupId, int dirLevels) {
        int length = groupId.length();
        StringBuilder path = new StringBuilder(groupsPath);
        for (int i = 0; i < dirLevels && i < length; i++) {
            path.append('/').append(NodeNameCodec.encode(Character.toLowerCase(groupId.charAt(i))));
        }
        path.append('/').append(groupId);
        return path.toString();
    }
    

    private final void setDirLevels() {
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
            log.debug("Using dirlevels '"+dirLevels+"' for provider: " + providerId);
        }
    }
    
    public final Node getOrCreateGroup(String groupId) throws RepositoryException {
        if (hasGroup(groupId)) {
            return getGroup(groupId);
        }
        return createGroup(groupId);
    }

    public final boolean isManagerForGroup(Node group) throws RepositoryException {
        if (group.hasProperty(HippoNodeType.HIPPO_SECURITYPROVIDER)) {
            return providerId.equals(group.getProperty(HippoNodeType.HIPPO_SECURITYPROVIDER).getString());
        } else {
            return org.hippoecm.repository.security.SecurityManager.INTERNAL_PROVIDER.equals(providerId);
        }
    }

    public final Set<String> getMemberships(String userId) {
        return getMemberships(userId, null);
    }

    public final Set<String> getMemberships(String userId, String providerId) {
        Set<String> memberships = new HashSet<String>();

        StringBuffer statement = new StringBuffer();
        // Triggers: https://issues.apache.org/jira/browse/JCR-1573 don't use path in query for now
        //statement.append("//").append(groupsPath).append("//element");
        statement.append("//element");
        statement.append("(*, ").append(HippoNodeType.NT_GROUP).append(")");
        statement.append('[');
        statement.append("(@").append(HippoNodeType.HIPPO_MEMBERS).append(" = '").append(userId).append("'");
        statement.append(" or @").append(HippoNodeType.HIPPO_MEMBERS).append(" = '*')");
        if (providerId != null) {
            statement.append(" and @");
            statement.append(HippoNodeType.HIPPO_SECURITYPROVIDER).append("= '").append(providerId).append("'");
        }
        statement.append(']');
        //log.info("Searching for memberships for user '{}' with query '{}'", userId, statement);

        // find
        try {
            Query q = session.getWorkspace().getQueryManager().createQuery(statement.toString(), Query.XPATH);
            QueryResult result = q.execute();
            NodeIterator groupsIter = result.getNodes();
            while (groupsIter.hasNext()) {
                String groupId = groupsIter.nextNode().getName();
                log.debug("User '{}' is member of group: {}", userId, groupId);
                memberships.add(groupId);
            }
            return memberships;
        } catch (RepositoryException e) {
            log.error("Error while finding memberships: ", e);
            return new HashSet<String>(0);
        }
    }

    public final void syncMemberships(Node user) throws RepositoryException {
        if (org.hippoecm.repository.security.SecurityManager.INTERNAL_PROVIDER.equals(providerId)) {
            // no sync needed for internal users and groups
            return;
        }
        String userId = user.getName();
        Set<String> repositoryMemberships = getMemberships(userId, providerId);
        Set<String> backendMemberships = backendGetMemberships(user);
        Set<String> inSync = new HashSet<String>();
        for (String groupId : repositoryMemberships) {
            if (backendMemberships.contains(groupId)) {
                inSync.add(groupId);
            }
        }
        repositoryMemberships.removeAll(inSync);
        backendMemberships.removeAll(inSync);

        Node group;
        // remove memberships that have been removed in the backend
        if (repositoryMemberships.size() > 0) {
            for (String groupId : repositoryMemberships) {
                group = getGroup(groupId);
                if (group != null) {
                    log.debug("Remove membership of user '{}' for group '{}' by provider '{}'", 
                            new Object[] { userId, groupId, providerId });
                    removeMember(group, userId);
                }
            }
        }
        // add memberships that have been added in the backend
        if (backendMemberships.size() > 0) {
            for (String groupId : backendMemberships) {
                group = getOrCreateGroup(groupId);
                log.debug("Add membership of user '{}' for group '{}' by provider '{}'", new Object[] { userId,
                        groupId, providerId });
                addMember(group, userId);
            }
        }
    }

    public final Set<String> getMembers(Node group) throws RepositoryException {
        Value[] vals = group.getProperty(HippoNodeType.HIPPO_MEMBERS).getValues();
        Set<String> memebers = new HashSet<String>(vals.length);
        for (Value val : vals) {
            memebers.add(val.getString());
        }
        return memebers;
    }

    public final void setMembers(Node group, Set<String> members) throws RepositoryException {
        if (!isManagerForGroup(group)) {
            log.warn("Group '" + group.getName() + "' is nog managed by provider '" + providerId
                    + "' skipping setMembers");
            return;
        }
        group.setProperty(HippoNodeType.HIPPO_MEMBERS, members.toArray(new String[members.size()]));
    }

    public final void addMember(Node group, String userId) throws RepositoryException {
        if (!isManagerForGroup(group)) {
            log.warn("Group '" + group.getName() + "' is nog managed by provider '" + providerId
                    + "' skipping addMember '" + userId + "'");
            return;
        }
        Set<String> members = getMembers(group);
        if (!members.contains(userId)) {
            members.add(userId);
            setMembers(group, members);
        }
    }

    public final void removeMember(Node group, String userId) throws RepositoryException {
        if (!isManagerForGroup(group)) {
            log.warn("Group '" + group.getName() + "' is nog managed by provider '" + providerId
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
        session.refresh(true);
        session.getRootNode().getNode(groupsPath).save();
    }

    public boolean backendCreateGroup(String groupId) throws NotSupportedException, RepositoryException {
        throw new NotSupportedException("Add group not supported.");
    }

    public boolean backendDeleteGroup(String groupId) throws NotSupportedException, RepositoryException {
        throw new NotSupportedException("Delete group not supported.");
    }
}
