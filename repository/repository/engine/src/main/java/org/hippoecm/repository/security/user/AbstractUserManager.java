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
package org.hippoecm.repository.security.user;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.transaction.NotSupportedException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.security.ManagerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UserManager provider that stores the users inside the JCR repository
 */
public abstract class AbstractUserManager implements UserManager {

    /** SVN id placeholder */
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

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
            String userId = normalizeUserId(rawUserId);
            StringBuilder statement = new StringBuilder();
            // Triggers: https://issues.apache.org/jira/browse/JCR-1573 don't use path in query for now
            //statement.append("//").append(usersPath).append("//element");
            statement.append("//element");
            statement.append("(*, ").append(HippoNodeType.NT_USER).append(")");
            statement.append('[').append("fn:name() = ").append("'").append(NodeNameCodec.encode(userId, true)).append("'").append(']');
            Query q = session.getWorkspace().getQueryManager().createQuery(statement.toString(), Query.XPATH);
            QueryResult result = q.execute();
            if (result.getNodes().hasNext()) {
                return true;
            } else {
                return false;
            }
        } else {
            return session.getRootNode().hasNode(buildUserPath(rawUserId));
        }
    }
    
    public final Node getUser(String rawUserId) throws RepositoryException {
        if (!isInitialized()) {
            throw new IllegalStateException("Not initialized.");
        }
        
        if (useQueries) {
            String userId = normalizeUserId(rawUserId);
            StringBuilder statement = new StringBuilder();
            // Triggers: https://issues.apache.org/jira/browse/JCR-1573 don't use path in query for now
            //statement.append("//").append(usersPath).append("//element");
            statement.append("//element");
            statement.append("(*, ").append(HippoNodeType.NT_USER).append(")");
            statement.append('[').append("fn:name() = ").append("'").append(NodeNameCodec.encode(userId, true)).append("'").append(']');
            Query q = session.getWorkspace().getQueryManager().createQuery(statement.toString(), Query.XPATH);
            QueryResult result = q.execute();
            NodeIterator nodeIter = result.getNodes();
            if (nodeIter.hasNext()) {
                return nodeIter.nextNode();
            }
        } else {
            String path = buildUserPath(rawUserId);
            if (session.getRootNode().hasNode(path)) {
                try {
                    return session.getRootNode().getNode(path);
                } catch (PathNotFoundException e) {
                    // noop
                }
            }
        }
        return null;
    }
    
    /**
     * Create a new user in the repository. Use getNodeType to determine the 
     * node's node type.
     */
    public final Node createUser(String rawUserId) throws RepositoryException {
        if (!isInitialized()) {
            throw new IllegalStateException("Not initialized.");
        }
        String userId = normalizeUserId(rawUserId);
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
     * @param dirLevels
     * @return the fully encoded normalized path
     */
    private String buildUserPath(String rawUserId) {
        String userId = normalizeUserId(rawUserId);
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
     * Normalize the userId: trim and convert to lowercase if needed. This
     * function does NOT encode the userId.
     * @param rawUserId
     * @return the trimmed and if needed converted to lowercase userId
     */
    private String normalizeUserId(String rawUserId) {
        if (isCaseSensitive()) {
            return rawUserId.trim();
        } else {
            return rawUserId.trim().toLowerCase();
        }
    }
    
    private final void setDirLevels() {
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
    
    public final NodeIterator listUsers() throws RepositoryException {
        return listUsers(null);
    }
    
    public final NodeIterator listUsers(String providerId) throws RepositoryException {
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
        
        Query q = session.getWorkspace().getQueryManager().createQuery(statement.toString(), Query.XPATH);
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
                log.debug("Unable to set lastlogin for user, user not found: " + normalizeUserId(rawUserId));
            }
        } catch (RepositoryException e) {
            log.info("Unable to set lastlogin for user: {}", normalizeUserId(rawUserId));
        }
    }

    public boolean isActive(String userId) throws RepositoryException {
        return true;
    }

    public final void saveUsers() throws RepositoryException {
        session.refresh(true);
        session.getRootNode().getNode(usersPath).save();
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
}
