/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.security.user;

import java.security.Principal;
import java.util.Calendar;
import java.util.Iterator;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.transaction.NotSupportedException;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.AuthorizableExistsException;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.util.ISO9075;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.security.ManagerContext;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UserManager provider that stores the users inside the JCR repository
 * 
 * The rawUserId's are the id's as provided by the backend. The userId's 
 * are the normalized id's. All id's MUST be normalized before they are
 * stored in the database.
 */
public abstract class AbstractUserManager implements HippoUserManager {


    /**
     * The system/root session
     */
    protected Session session;

    /**
     * The path from the root containing the users
     */
    protected String usersPath;

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
     * Don't use queries for now. It's too slow :(
     */
    private final boolean useQueries = false;

    private boolean maintenanceMode = false;

    /**
     * Logger
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Don't override. This is the general init. Use managerInit for implementation
     * specific initialization.
     */
    public final void init(ManagerContext context) throws RepositoryException {
        this.session = context.getSession();
        this.usersPath = context.getPath();
        this.providerId = context.getProviderId();
        this.providerPath = context.getProviderPath();
        this.maintenanceMode = context.isMaintenanceMode();
        setDirLevels();
        initManager(context);
    }

    public final boolean isInitialized() {
        return initialized;
    }

    public final boolean hasUser(String rawUserId) throws RepositoryException {
        if (!isInitialized()) {
            throw new IllegalStateException("Not initialized.");
        }
        if (useQueries) {
            String userId = sanitizeId(rawUserId);
            StringBuilder statement = new StringBuilder();
            // Triggers: https://issues.apache.org/jira/browse/JCR-1573 don't use path in query for now
            //statement.append("//").append(usersPath).append("//element");
            statement.append("//element");
            statement.append("(*, ").append(HippoNodeType.NT_USER).append(")");
            statement.append('[').append("fn:name() = ").append("'").append(ISO9075.encode(NodeNameCodec.encode(userId, true))).append("'").append(']');
            Query q = session.getWorkspace().getQueryManager().createQuery(statement.toString(), Query.XPATH);
            QueryResult result = q.execute();
            return result.getNodes().hasNext();
        } else {
            String path = buildUserPath(rawUserId);
            if (session.getRootNode().hasNode(path)) {
                Node user = session.getRootNode().getNode(path);
                if (user.getPrimaryNodeType().isNodeType(HippoNodeType.NT_USER)) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public final Node getUser(String rawUserId) throws RepositoryException {
        if (!isInitialized()) {
            throw new IllegalStateException("Not initialized.");
        }

        if (useQueries) {
            String userId = sanitizeId(rawUserId);
            StringBuilder statement = new StringBuilder();
            // Triggers: https://issues.apache.org/jira/browse/JCR-1573 don't use path in query for now
            //statement.append("//").append(usersPath).append("//element");
            statement.append("//element");
            statement.append("(*, ").append(HippoNodeType.NT_USER).append(")");
            statement.append('[').append("fn:name() = ").append("'").append(ISO9075.encode(NodeNameCodec.encode(userId, true))).append("'").append(']');
            Query q = session.getWorkspace().getQueryManager().createQuery(statement.toString(), Query.XPATH);
            QueryResult result = q.execute();
            NodeIterator nodeIter = result.getNodes();
            if (nodeIter.hasNext()) {
                return nodeIter.nextNode();
            }
        } else {
            String path = buildUserPath(rawUserId);
            if (session.getRootNode().hasNode(path)) {
                Node user = session.getRootNode().getNode(path);
                if (user.getPrimaryNodeType().isNodeType(HippoNodeType.NT_USER)) {
                    return user;
                } else {
                    return null;
                }
            } else {
                if (maintenanceMode) {
                    StringBuffer sb = new StringBuffer();
                    sb.append("User not one of existing users:");
                    for (NodeIterator nodeIter = session.getRootNode().getNode(usersPath).getNodes(); nodeIter.hasNext(); ) {
                        Node userNode = nodeIter.nextNode();
                        sb.append(" ");
                        sb.append(userNode.getName());
                    }
                    log.warn(new String(sb));
                }
            }
        }
        return null;
    }

    /**
     * Create a new user in the repository. Use getNodeType to determine the
     * node's node type.
     */
    @Override
    public final Node createUser(String rawUserId) throws RepositoryException {
        if (!isInitialized()) {
            throw new IllegalStateException("Not initialized.");
        }
        String userId = sanitizeId(rawUserId);
        log.trace("Creating node for user: {} in path: {}", userId, usersPath);
        int length = userId.length();
        int pos = 0;
        Node usersNode = session.getRootNode().getNode(usersPath);
        for (int i = 0; i < dirLevels; i++) {
            if (i < length) {
                pos = i;
            }
            String c = NodeNameCodec.encode(Character.toLowerCase(userId.charAt(pos)));
            if (!usersNode.hasNode(c)) {
                usersNode = usersNode.addNode(c, HippoNodeType.NT_USERFOLDER);
            } else {
                usersNode = usersNode.getNode(c);
            }
        }
        Node user = usersNode.addNode(NodeNameCodec.encode(userId, true), getNodeType());
        if (!org.hippoecm.repository.security.SecurityManager.INTERNAL_PROVIDER.equals(providerId)) {
            user.setProperty(HippoNodeType.HIPPO_SECURITYPROVIDER, providerId);
        }
        log.debug("User: {} created by {} ", userId, providerId);
        return user;
    }

    /**
     * Helper for building user path including the username itself. Takes care of the encoding
     * of the path AND the userId (the eventual node name)
     * @param rawUserId unencoded userId
     * @return the fully encoded normalized path
     */
    private String buildUserPath(String rawUserId) {
        String userId = sanitizeId(rawUserId);
        if (dirLevels == 0) {
            return usersPath + "/" + NodeNameCodec.encode(userId, true);
        }
        int length = userId.length();
        int pos = 0;
        StringBuilder path = new StringBuilder(usersPath);
        for (int i = 0; i < dirLevels; i++) {
            if (i < length) {
                pos = i;
            }
            path.append('/').append(NodeNameCodec.encode(Character.toLowerCase(userId.charAt(pos))));
        }
        path.append('/').append(NodeNameCodec.encode(userId, true));
        return path.toString();
    }

    /**
     * Sanitize the rawUserId: trim and convert to lowercase if needed. This
     * function does NOT encode the userId.
     * @param rawUserId
     * @return the trimmed and if needed converted to lowercase userId
     */
    private String sanitizeId(String rawUserId) {
        if (rawUserId == null) {
            // anonymous
            return null;
        }
        if (isCaseSensitive()) {
            return rawUserId.trim();
        } else {
            return rawUserId.trim().toLowerCase();
        }
    }

    private void setDirLevels() {
        dirLevels = 0;
        String relPath = providerPath + "/" + HippoNodeType.NT_USERPROVIDER;
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

    public final void updateSyncDate(Node user) throws RepositoryException {
        if (user.isNodeType(HippoNodeType.NT_EXTERNALUSER)) {
            user.setProperty(HippoNodeType.HIPPO_LASTSYNC, Calendar.getInstance());
        }
    }

    @Override
    public final NodeIterator listUsers(long offset, long limit) throws RepositoryException {
        return listUsers(null, offset, limit);
    }

    @Override
    public final NodeIterator listUsers(String providerId, long offset, long limit) throws RepositoryException {
        if (!isInitialized()) {
            throw new IllegalStateException("Not initialized.");
        }
        StringBuilder statement = new StringBuilder();
        // Triggers: https://issues.apache.org/jira/browse/JCR-1573 don't use path in query for now
        //statement.append("//").append(usersPath).append("//element");
        statement.append("//element");
        statement.append("(*, ").append(HippoNodeType.NT_USER).append(")");
        if (providerId != null) {
            statement.append('[');
            statement.append("@");
            statement.append(HippoNodeType.HIPPO_SECURITYPROVIDER).append("= '").append(providerId).append("'");
            statement.append(']');
        }
        statement.append(" order by @jcr:name");

        Query q = session.getWorkspace().getQueryManager().createQuery(statement.toString(), Query.XPATH);
        if (offset > 0) {
            q.setOffset(offset);
        }
        if (limit > 0) {
            q.setLimit(limit);
        }
        QueryResult result = q.execute();
        return result.getNodes();
    }

    public final boolean isManagerForUser(Node user) throws RepositoryException {
        if (user.hasProperty(HippoNodeType.HIPPO_SECURITYPROVIDER)) {
            return providerId.equals(user.getProperty(HippoNodeType.HIPPO_SECURITYPROVIDER).getString());
        } else {
            return org.hippoecm.repository.security.SecurityManager.INTERNAL_PROVIDER.equals(providerId);
        }
    }

    /**
     * Only last login date for external users.
     */
    @Override
    public void updateLastLogin(String rawUserId) {
        if (!isInitialized()) {
            throw new IllegalStateException("Not initialized.");
        }
        try {
            Node user = getUser(rawUserId);
            if (user != null) {
                if (user.isNodeType(HippoNodeType.NT_EXTERNALUSER)) {
                    user.setProperty(HippoNodeType.HIPPO_LASTLOGIN, Calendar.getInstance());
                }
            } else {
                log.debug("Unable to set lastlogin for user, user not found: " + sanitizeId(rawUserId));
            }
        } catch (RepositoryException e) {
            log.info("Unable to set lastlogin for user: {}", sanitizeId(rawUserId));
        }
    }

    @Override
    public boolean isActive(final String rawUserId) throws RepositoryException {
        return JcrUtils.getBooleanProperty(getUser(rawUserId), HippoNodeType.HIPPO_ACTIVE, true);
    }

    @Override
    public abstract boolean isPasswordExpired(final String rawUserId) throws RepositoryException;

    public boolean isSystemUser(String rawUserId) throws RepositoryException {
        return JcrUtils.getBooleanProperty(getUser(rawUserId), HippoNodeType.HIPPO_SYSTEM, false);
    }

    @Override
    public final void saveUsers() throws RepositoryException {
        try {
            session.refresh(true);
            session.getRootNode().getNode(usersPath).save();
        } catch (InvalidItemStateException e) {
            log.warn("Unable to save synced user data, this usually happens when the user node"
                    + " was simultaneously changed by another session: " + e.getMessage());
            log.debug("StackTrace: ", e);
            // discard changes in session
            session.refresh(false);
        }
    }

    public void backendSetPassword(String userId, char[] password) throws NotSupportedException, RepositoryException {
        throw new NotSupportedException("setPassword not supported");
    }

    public void backendSetActive(String userId, boolean active) throws NotSupportedException, RepositoryException {
        throw new NotSupportedException("setActive not supported");
    }

    public boolean backendAddUser(String userId, char[] password) throws NotSupportedException, RepositoryException {
        throw new NotSupportedException("addUser not supported");
    }

    public boolean backendDeleteUser(String userId) throws NotSupportedException, RepositoryException {
        throw new NotSupportedException("deleteUser not supported");
    }

    public void backendSetProperty(String userId, String key, String value) throws NotSupportedException, RepositoryException {
        throw new NotSupportedException("setProperty not supported");
    }

    /**
     * Default initialize the UserManager with the given {@link ManagerContext}.
     * Calls initManager after the general init which is handled by the
     * {@link AbstractUserManager}.
     * @param context The {@link ManagerContext} with params for the backend
     * @throws RepositoryException
     * @see ManagerContext
     */
    public abstract void initManager(ManagerContext context) throws RepositoryException;

    /**
     * Checks if the backend is case aware (ie, ldap usually isn't, the internal provider is)
     * @return
     */
    public abstract boolean isCaseSensitive();

    /**
     * Get the node type for new user nodes
     * @return the node type
     */
    public abstract String getNodeType();

    /**
     * Authenticate the user with the current provider's user manager
     * @param creds SimpleCredentials
     * @return true when successfully authenticate
     * @throws RepositoryException
     */
    @Override
    public abstract boolean authenticate(SimpleCredentials creds) throws RepositoryException;

    /**
     * Hook for the provider to sync from the backend with the repository.
     * Called just after authenticate.
     * @param userId
     */
    @Override
    public abstract void syncUserInfo(String userId);

    public Authorizable getAuthorizable(String id) throws RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Authorizable getAuthorizable(Principal principal) throws RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Iterator<Authorizable> findAuthorizables(String propertyName, String value) throws RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Iterator<Authorizable> findAuthorizables(String propertyName, String value, int searchType) throws RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Iterator<Authorizable> findAuthorizables(org.apache.jackrabbit.api.security.user.Query query) throws RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public User createUser(String userID, String password) throws AuthorizableExistsException, RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public User createUser(String userID, String password, Principal principal, String intermediatePath) throws AuthorizableExistsException, RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Group createGroup(Principal principal) throws AuthorizableExistsException, RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Group createGroup(Principal principal, String intermediatePath) throws AuthorizableExistsException, RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Group createGroup(String string) throws AuthorizableExistsException, RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Group createGroup(String string, Principal prncpl, String string1) throws AuthorizableExistsException, RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void autoSave(boolean enable) throws UnsupportedRepositoryOperationException, RepositoryException {
    }
}
