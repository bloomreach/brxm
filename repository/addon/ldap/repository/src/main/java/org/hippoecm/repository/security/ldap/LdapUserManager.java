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
package org.hippoecm.repository.security.ldap;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.security.ManagerContext;
import org.hippoecm.repository.security.user.AbstractUserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UserManager backend that fetches users from LDAP and stores the users inside the JCR repository
 */
public class LdapUserManager extends AbstractUserManager {

    /** SVN id placeholder */
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    /**
     * On sync save every after every SAVE_INTERVAL changes
     */
    private final int SAVE_INTERVAL = 2500;

    /**
     * The initialized ldap context factory
     */
    private LdapContextFactory lcf;

    /**
     * The attribute to property mappings
     */
    Set<LdapMapping> mappings = new HashSet<LdapMapping>();

    /**
     * The user searches
     */
    private final Set<LdapUserSearch> searches = new HashSet<LdapUserSearch>();

    /**
     * Logger
     */
    private final static Logger log = LoggerFactory.getLogger(LdapUserManager.class);

    /**
     * initialize
     */
    public void initManager(ManagerContext context) throws RepositoryException {
        LdapManagerContext ldapContext = (LdapManagerContext) context;
        lcf = ldapContext.getLdapContextFactory();
        providerId = ldapContext.getProviderId();
        loadSearches(context.getProviderNode());
        loadMappings(context.getProviderNode());
        initialized = true;
    }

    /**
     * Authenticate against the ldap server. First try to find the dn of the user 
     * with the system user and then try to bind to the ldap server with the password
     * as the dn.
     */
    public boolean authenticate(SimpleCredentials creds) throws RepositoryException {
        if (!isInitialized()) {
            throw new IllegalStateException("Not initialized: " + providerId);
        }
        log.debug("Trying to authenticate {} with provider: {}", creds.getUserID(), providerId);
        String dn = getDnForUser(creds.getUserID());
        if (dn == null) {
            log.debug("User {} not found in ldap provider: {} ", creds.getUserID(), providerId);
            return false;
        }
        log.debug("Found dn {} with provider: {}", dn, providerId);
        LdapContext ctx = null;
        try {
            ctx = lcf.getLdapContext(dn, creds.getPassword());
            return true;
        } catch (NamingException e) {
            log.debug("Exception while trying to authenticate user {} : {}", creds.getUserID(), e.getMessage());
        } finally {
            LdapUtils.closeContext(ctx);
        }
        return false;
    }

    /**
     * TODO: Add an option to check against an attribute in the ldap server if
     * a user is active, or sync the status to the hippo:active property.
     */
    @Override
    public boolean isActive(String userId) throws RepositoryException {
        return true;
    }

    /**
     * Update the current user info in the repository with info mapped from the ldap server.
     * @param userId
     */
    public void syncUserInfo(String userId) {
        if (!isInitialized()) {
            throw new IllegalStateException("Not initialized: " + providerId);
        }
        String dn = null;
        Node user = null;
        try {
            if (hasUser(userId)) {
                user = getUser(userId);
                if (!isManagerForUser(user)) {
                    log.warn("Unable to sync user info. User '{}' not not managed by provider: {} ", userId, providerId);
                    return;
                }
                dn = user.getProperty(LdapSecurityProvider.PROPERTY_LDAP_DN).getString();
            } else {
                dn = getDnForUser(userId);
                if (dn == null) {
                    log.warn("Unable to sync user info: user {} not found in ldap provider: {} ", userId, providerId);
                    return;
                }
                user = createUser(userId);
                user.setProperty(LdapSecurityProvider.PROPERTY_LDAP_DN, dn);
                // This method is called after successful login. Set lastlogin here to prevent indexer race. 
                user.setProperty(HippoNodeType.HIPPO_LASTLOGIN, Calendar.getInstance());
            }
        } catch (RepositoryException e) {
            log.error("Failed to lookup user " + userId + " by ldap provider " + providerId, e);
            return;
        }
        
        if (user == null || dn == null) {
            // this really shouldn't happen
            log.error("Unable to sync user info for user " + userId + " by ldap provider " + providerId);
            return;
        }
        
        LdapContext ctx = null;
        try {
            ctx = lcf.getSystemLdapContext();
            SearchControls ctls = new SearchControls();
            ctls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
            Attributes attrs = ctx.getAttributes(dn);
            for (LdapMapping mapping : mappings) {
                try {
                    Attribute attr = attrs.get(mapping.getSource());
                    if (attr != null) {
                        Object o = attr.get();
                        if (o instanceof String) {
                            user.setProperty(mapping.getTarget(), (String) o);
                        }
                    }
                } catch (NamingException e) {
                    log.debug("Skipping atturibute for user " + userId + " unable to get attributes: "
                            + mapping.getSource() + " : " + e.getMessage());
                } catch (RepositoryException e) {
                    log.debug("Skipping attribute for user " + userId + " unable to get/create property: "
                            + mapping.getTarget() + " : " + e.getMessage());
                }
            }
            user.setProperty(HippoNodeType.HIPPO_LASTSYNC, Calendar.getInstance());
        } catch (RepositoryException e) {
            log.error("Unable to get or create user node: " + userId, e);
        } catch (NamingException e) {
            log.error("Unable to sync user: {} : {}", userId, e);
        } finally {
            LdapUtils.closeContext(ctx);
        }
    }

    /**
     * Synchronize all ldap users with the repository. This method can take a long time if
     * there are a lot of users in the ldap and should run in it's own thread. It is called 
     * from the LdapSecurityProvider.sync method.
     * The saves to the repository are done in batches of SAVE_INTERVAL size.
     */
    public synchronized void updateUsers() {
        log.info("Starting synchronizing ldap users for: " + providerId);
        NamingEnumeration<SearchResult> results = null;
        String dn = null;
        try {
            LdapContext ldapContext = lcf.getSystemLdapContext();
            SearchControls ctls = new SearchControls();
            ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            int count = 0;
            for (LdapUserSearch search : searches) {
                results = ldapContext.search(search.getBaseDn(), search.getFilter(), ctls);
                while (results.hasMore()) {
                    SearchResult sr = results.next();
                    Attributes attrs = sr.getAttributes();
                    Attribute uidAttr = attrs.get(search.getNameAttr());
                    dn = sr.getName() + "," + search.getBaseDn();
                    log.trace("Found dn: {}", dn);
                    if (uidAttr == null) {
                        log.warn("Skipping dn='" + sr.getName() + "' because the uid attribute is not found.");
                    } else {
                        if (createUserIfNotExists((String) uidAttr.get(), dn)) {
                            count++;
                            if (count == SAVE_INTERVAL) {
                                count = 0;
                                try {
                                    saveUsers();
                                } catch (RepositoryException e) {
                                    log.error("Error while saving users node: " + usersPath, e);
                                }
                            }
                        }
                    }
                }
            }
        } catch (NamingException e) {
            log.error("Error while trying fetching users from ldap: " + providerId, e);
        }

        // save remaining unsaved user nodes
        try {
            saveUsers();
        } catch (RepositoryException e) {
            log.error("Error while saving users node: " + usersPath, e);
        }
        log.info("Finished synchronizing ldap users for: " + providerId);
    }

    /**
     * Try to find the dn in the ldap server based on the configured searches.
     * @param userId
     * @return the dn or null if the dn is not found.
     */
    private String getDnForUser(String userId) throws RepositoryException {
        // Try to find the user in the ldap server.
        try {
            LdapContext ldapContext = lcf.getSystemLdapContext();
            SearchControls ctls = new SearchControls();
            ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            NamingEnumeration<SearchResult> results = null;

            for (LdapUserSearch search : searches) {
                String filter = "(&(" + search.getFilter() + ")(" + search.getNameAttr() + "=" + userId+ "))";
                log.debug("Searching for user: '" + userId + "' with filter '" + filter + "' in: " + search.getBaseDn());
                results = ldapContext.search(search.getBaseDn(), filter, ctls);
                // just use the first match found
                if (results.hasMore()) {
                    SearchResult sr = results.next();
                    String dn = sr.getName() + "," + search.getBaseDn();
                    return dn;
                }
            }
        } catch (NamingException e) {
            log.error("Error while trying fetching users from ldap: " + providerId, e);
        }
        return null;
    }

    /**
     * Create a user if it doesn't exist in the Repository.
     * @param userId
     * @param dn
     * @return true when a new user is created, false when the user already exists
     */
    private boolean createUserIfNotExists(String userId, String dn) {
        log.trace("Checking user: {} for dn: {}", userId, dn);
        try {
            if (hasUser(userId)) {
                // user exists, don't mess with it.
                return false;
            }
        } catch (RepositoryException e) {
            // something is wrong with the user node, log and leave it
            log.error("Failed to lookup user " + userId, e);
            return false;
        }
        try {
            Node user = createUser(userId);
            user.setProperty(HippoNodeType.HIPPO_SECURITYPROVIDER, providerId);
            user.setProperty(LdapSecurityProvider.PROPERTY_LDAP_DN, dn);
            log.debug("User: {} created by by {} ", userId, providerId);
            return true;
        } catch (RepositoryException e) {
            log.warn("Failed to create user " + userId + " :" + e.getMessage());
            log.debug("Failed to create user " + userId, e);
            return false;
        }
    }

    /**
     * Load and parse the search configurations from the repository.
     * @param providerNode
     * @throws RepositoryException
     */
    private void loadSearches(Node providerNode) throws RepositoryException {
        StringBuffer statement = new StringBuffer();
        statement.append("SELECT * FROM ").append(LdapSecurityProvider.NT_LDAPUSERSEARCH);
        statement.append(" WHERE");
        statement.append(" jcr:path LIKE ");
        statement.append("'").append(providerNode.getPath()).append("/").append(HippoNodeType.NT_USERPROVIDER).append("/%'");

        Query q = session.getWorkspace().getQueryManager().createQuery(statement.toString(), Query.SQL);
        QueryResult result = q.execute();
        NodeIterator nodeIter = result.getNodes();
        while (nodeIter.hasNext()) {
            try {
                Node search = nodeIter.nextNode();
                String nameAttr = search.getProperty(LdapUserSearch.PROPERTY_NAME_ATTR).getString();
                String baseDn = search.getProperty(LdapUserSearch.PROPERTY_BASE_DN).getString();
                LdapUserSearch ldapSearch = new LdapUserSearch(baseDn, nameAttr);
                if (search.hasProperty(LdapUserSearch.PROPERTY_FILTER)) {
                    ldapSearch.setFilter(search.getProperty(LdapUserSearch.PROPERTY_FILTER).getString());
                }
                searches.add(ldapSearch);
            } catch (RepositoryException e) {
                log.warn("Unable to parse search: " + e.getMessage());
            }
        }
    }

    /**
     * Load and parse the mapping configurations from the repository.
     * @param providerNode
     * @throws RepositoryException
     */
    private void loadMappings(Node providerNode) throws RepositoryException {
        StringBuffer statement = new StringBuffer();
        statement.append("SELECT * FROM ").append(LdapSecurityProvider.NT_LDAPMAPPING);
        statement.append(" WHERE");
        statement.append(" jcr:path LIKE ");
        statement.append("'").append(providerNode.getPath()).append("/").append(HippoNodeType.NT_USERPROVIDER).append("/%'");

        Query q = session.getWorkspace().getQueryManager().createQuery(statement.toString(), Query.SQL);
        QueryResult result = q.execute();
        NodeIterator nodeIter = result.getNodes();
        while (nodeIter.hasNext()) {
            try {
                Node mapping = nodeIter.nextNode();
                String source = mapping.getProperty(LdapMapping.PROPERTY_SOURCE).getString();
                String target = mapping.getProperty(LdapMapping.PROPERTY_TARGET).getString();
                boolean multi = mapping.getProperty(LdapMapping.PROPERTY_MULTI).getBoolean();
                mappings.add(new LdapMapping(source, target, multi));
            } catch (RepositoryException e) {
                log.warn("Unable to parse mapping: " + e.getMessage());
            }
        }
    }

    /**
     * Create new users with the hippo:externaluser type.
     */
    public String getNodeType() {
        return HippoNodeType.NT_EXTERNALUSER;
    }
}
