package org.hippoecm.repository.security;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.security.domain.Domain;
import org.hippoecm.repository.security.role.Role;
import org.hippoecm.repository.security.user.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecurityManager {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public static final String INTERNAL_PROVIDER = "internal";
    public static final String SECURITY_CONFIG_PATH = HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.SECURITY_PATH;

    private String usersPath;
    private String groupsPath;
    private String rolesPath;
    private String domainsPath;

    private EventListener listener;
    private final Map<String, SecurityProvider> providers = new LinkedHashMap<String, SecurityProvider>();

    // the (root) session
    private Session session;

    private static SecurityManager instance;

    /**
     * Logger
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Get a SecurityManager instance. This is java 5 thread safe.
     * @return
     */
    public static SecurityManager getInstance() {
        synchronized (SecurityManager.class) {
            if (instance == null) {
                instance = new SecurityManager();
            }
        }
        return instance;
    }

    public void init(Session session) throws RepositoryException {
        if (this.session == null || !this.session.isLive()) {
            this.session = session;
            
            // start a listener to check if provider are added or removed
            ObservationManager obMgr = session.getWorkspace().getObservationManager();
            listener = new EventListener() {
                public void onEvent(EventIterator events) {
                    try {
                        clearProviders();
                        createSecurityProviders();
                    } catch (RepositoryException e) {
                        log.info("Failed to reload config for provider: {}", e.getMessage());
                    }
                }
            };
            obMgr.addEventListener(listener, Event.NODE_ADDED | Event.NODE_REMOVED, "/" + SECURITY_CONFIG_PATH, true,
                    null, new String[] { HippoNodeType.NT_SECURITYPROVIDER }, true);
            
            // initial create
            createSecurityProviders();
        }
    }

    public boolean authenticate(String userId, char[] password) {
        try {
            // does the user already exists
            log.trace("Looking for user: {} in path: {}", userId, usersPath);
            String path = usersPath + "/" + userId;
            if (!session.getRootNode().hasNode(path)) {
                log.info("Unable to authenticate user, user not found: {}", userId);
                return false;
            }
            log.trace("Found user node: {}", path);

            // find the security provider
            String providerName = INTERNAL_PROVIDER;
            if (session.getRootNode().getNode(path).hasProperty(HippoNodeType.HIPPO_SECURITYPROVIDER)) {
                providerName = session.getRootNode().getNode(path).getProperty(HippoNodeType.HIPPO_SECURITYPROVIDER).getString();
            }
            if (!providers.containsKey(providerName)) {
                log.info("Unable to authenticate user: {}, no such provider: {}", userId, providerName);
                return false;
            }
            log.debug("Found provider: {} for user: {}", providerName, userId);

            // TODO: move to separate thread so it can run in background
            providers.get(providerName).sync();

            UserManager userMgr = providers.get(providerName).getUserManger();

            // check the password
            if (!userMgr.authenticate(userId, password)) {
                log.debug("Invalid username or password: {}", userId);
                return false;
            }

            // check if user is active
            if (session.getRootNode().getNode(usersPath + "/" + userId).getProperty(HippoNodeType.HIPPO_ACTIVE).getBoolean()) {
                userMgr.syncUser(userId);
                userMgr.updateLastLogin(userId);
                return true;
            }
            return false;
        } catch (RepositoryException e) {
            log.info("Unable to authenticate user: {} : ()", userId, e.getMessage());
            log.debug("Unable to authenticate user: ", e);
            return false;
        }
    }

    private void clearProviders() {
        // clear out 'old' providers
        for (SecurityProvider provider : providers.values()) {
            provider.remove();
        }
        providers.clear();
    }
    
    private void createSecurityProviders() throws RepositoryException {
        Node configNode = session.getRootNode().getNode(SECURITY_CONFIG_PATH);

        usersPath = configNode.getProperty(HippoNodeType.HIPPO_USERSPATH).getString();
        groupsPath = configNode.getProperty(HippoNodeType.HIPPO_GROUPSPATH).getString();
        rolesPath = configNode.getProperty(HippoNodeType.HIPPO_ROLESPATH).getString();
        domainsPath = configNode.getProperty(HippoNodeType.HIPPO_DOMAINSPATH).getString();

        StringBuffer statement = new StringBuffer();
        statement.append("SELECT * FROM ").append(HippoNodeType.NT_SECURITYPROVIDER);
        statement.append(" WHERE");
        statement.append(" jcr:path LIKE '/").append(SECURITY_CONFIG_PATH).append("/%").append("'");

        //log.debug("Searching for security providers: ", statement);

        // find
        QueryManager qm;
        qm = session.getWorkspace().getQueryManager();
        Query q = qm.createQuery(statement.toString(), Query.SQL);
        QueryResult result = q.execute();
        NodeIterator providerIter = result.getNodes();

        SecurityProviderFactory spf = new SecurityProviderFactory(SECURITY_CONFIG_PATH, usersPath, groupsPath, rolesPath, domainsPath);
        
        while (providerIter.hasNext()) {
            Node provider = providerIter.nextNode();
            String name = null;
            try {
                name = provider.getName();
                log.debug("Found secutiry provider: '{}'", name);
                providers.put(name, spf.create(provider));
                log.info("Security provider '{}' initialized.", name);
            } catch (ClassNotFoundException e) {
                log.error("Class not found for security provider: " + e.getMessage());
                log.debug("Stack: ", e);
            } catch (InstantiationException e) {
                log.error("Could not instantiate class for security provider: " + e.getMessage());
                log.debug("Stack: ", e);
            } catch (NoSuchMethodError e) {
                log.error("Method not found for security provider: " + e.getMessage());
                log.debug("Stack: ", e);
            } catch (IllegalAccessException e) {
                log.error("Not allowed to instantiate class for security provider: " + e.getMessage());
                log.debug("Stack: ", e);
            } catch (RepositoryException e) {
                log.error("Error while creating security provider: " + e.getMessage());
                log.debug("Stack: ", e);
            }
        }
        if (providers.size() == 0) {
            log.error("No security providers found: login will not be possible!");
        }
    }

    public Set<String> getMemeberships(String userId) {
        Set<String> memberships = new HashSet<String>();

        StringBuffer statement = new StringBuffer();
        statement.append("SELECT * FROM ").append(HippoNodeType.NT_GROUP);
        statement.append(" WHERE");
        statement.append(" jcr:path LIKE '/").append(groupsPath).append("/%").append("'");
        statement.append(" AND");
        statement.append(" (").append(HippoNodeType.HIPPO_MEMBERS).append("= '").append(userId).append("'");
        statement.append(" OR ").append(HippoNodeType.HIPPO_MEMBERS).append("= '*')");

        log.trace("Searching for membersips for user '{}' wiht query '{}'", userId, statement);

        // find
        QueryManager qm;
        try {
            qm = session.getWorkspace().getQueryManager();
            Query q = qm.createQuery(statement.toString(), Query.SQL);
            QueryResult result = q.execute();
            NodeIterator groupsIter = result.getNodes();
            while (groupsIter.hasNext()) {
                String groupId = groupsIter.nextNode().getName();
                log.debug("User '{}' is member of group: {}", userId, groupId);
                memberships.add(groupId);
            }
            return Collections.unmodifiableSet(memberships);
        } catch (RepositoryException e) {
            log.error("Error while finding memberships: ", e);
            return null;
        }
    }

    public Set<Domain> getDomainsForUser(String userId) {
        Set<Domain> domains = new HashSet<Domain>();

        StringBuffer statement = new StringBuffer();
        statement.append("SELECT * FROM ").append(HippoNodeType.NT_AUTHROLE);
        statement.append(" WHERE");
        statement.append(" jcr:path LIKE '/").append(domainsPath).append("/%").append("'");
        statement.append(" AND");
        statement.append(" CONTAINS(").append(HippoNodeType.HIPPO_USERS).append(",'").append(userId).append("')");

        //log.debug("Searching for domains for user '{}' wiht query '{}'", userId, statement);

        // find
        QueryManager qm;
        try {
            qm = session.getWorkspace().getQueryManager();
            Query q = qm.createQuery(statement.toString(), Query.SQL);
            QueryResult result = q.execute();
            NodeIterator nodeIter = result.getNodes();
            while (nodeIter.hasNext()) {
                // the parent of the auth role node is the domain node
                Domain domain = new Domain(nodeIter.nextNode().getParent());
                log.debug("Domain '{}' found for user: {}", domain.getName(), userId);
                domains.add(domain);
            }
            return domains;
        } catch (RepositoryException e) {
            log.error("Error while searching for domains for user: " + userId, e);
            return null;
        }
    }

    public Set<Domain> getDomainsForGroup(String groupId) {
        Set<Domain> domains = new HashSet<Domain>();

        StringBuffer statement = new StringBuffer();
        statement.append("SELECT * FROM ").append(HippoNodeType.NT_AUTHROLE);
        statement.append(" WHERE");
        statement.append(" jcr:path LIKE '/").append(domainsPath).append("/%").append("'");
        statement.append(" AND");
        statement.append(" CONTAINS(").append(HippoNodeType.HIPPO_GROUPS).append(",'").append(groupId).append("')");

        //log.debug("Searching for domains for group '{}' wiht query '{}'", groupId, statement);

        // find
        QueryManager qm;
        try {
            qm = session.getWorkspace().getQueryManager();
            Query q = qm.createQuery(statement.toString(), Query.SQL);
            QueryResult result = q.execute();
            NodeIterator nodeIter = result.getNodes();
            while (nodeIter.hasNext()) {
                // the parent of the auth role node is the domain node
                Domain domain = new Domain(nodeIter.nextNode().getParent());
                log.debug("Domain '{}' found for group: {}", domain.getName(), groupId);
                domains.add(domain);
            }
            return domains;
        } catch (RepositoryException e) {
            log.error("Error while searching for domains for group: " + groupId, e);
            return null;
        }
    }

    public int getJCRPermissionsForRole(String roleId) {
        int permissions = 0;
        Node roleNode;

        // does the role already exists
        log.debug("Looking for role: {} in path: {}", roleId, rolesPath);
        String path = rolesPath + "/" + roleId;
        try {
            try {
                roleNode = session.getRootNode().getNode(path);
                log.debug("Found role node: {}", roleNode.getName());
            } catch (PathNotFoundException e) {
                log.warn("Role not found: {}", roleId);
                return Role.NONE;
            }
            try {
                if (roleNode.getProperty(HippoNodeType.HIPPO_JCRREAD).getBoolean()) {
                    log.trace("Adding jcr read permissions for role: {}", roleId);
                    permissions += Role.READ;
                }
            } catch (PathNotFoundException e) {
                // ignore, role doesn't has the permission
            }

            try {
                if (roleNode.getProperty(HippoNodeType.HIPPO_JCRWRITE).getBoolean()) {
                    log.trace("Adding jcr write permissions for role: {}", roleId);
                    permissions += Role.WRITE;
                }
            } catch (PathNotFoundException e) {
                // ignore, role doesn't has the permission
            }

            try {
                if (roleNode.getProperty(HippoNodeType.HIPPO_JCRREMOVE).getBoolean()) {
                    log.trace("Adding jcr remove permissions for role: {}", roleId);
                    permissions += Role.REMOVE;
                }
            } catch (PathNotFoundException e) {
                // ignore, role doesn't has the permission
            }
        } catch (RepositoryException e) {
            log.error("Error while looking up role: " + roleId, e);
            return Role.NONE;
        }
        return permissions;
    }

}
