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
package org.hippoecm.repository.security.ldap;

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

    /**
     * On sync save every after every SAVE_INTERVAL changes
     * TODO: make configurable
     */
    private final int SAVE_INTERVAL = 250;

    /**
     * Ldap servers often have a limit of 1000 results. Use paged searching
     * to avoid hitting this limit.
     * TODO: make configurable
     */
    private final int LDAP_SEARCH_PAGE_SIZE = 200;

    /**
     * The initialized ldap context factory
     */
    private LdapContextFactory lcf;

    /**
     * The attribute to property mappings
     */
    private final Set<LdapMapping> mappings = new HashSet<LdapMapping>();

    /**
     * The user searches
     */
    private final Set<LdapUserSearch> searches = new HashSet<LdapUserSearch>();

    /**
     * Logger
     */
    private static final Logger log = LoggerFactory.getLogger(LdapUserManager.class);

    /**
     * Use case sensitive uid matching
     * TODO: make configurable
     */
    private boolean isCaseSensitive = false;

    /**
     * initialize
     */
    public void initManager(ManagerContext context) throws RepositoryException {
        LdapManagerContext ldapContext = (LdapManagerContext) context;
        lcf = ldapContext.getLdapContextFactory();
        LdapContext ctx = null;
        try {
            // test connection
            ctx = lcf.getSystemLdapContext();
        } catch (NamingException e) {
            throw new RepositoryException("Unable to connect to the ldap server for: " + providerId, e);
        } finally {
            lcf.close(ctx);
        }
        Node providerNode = context.getSession().getRootNode().getNode(context.getProviderPath());
        loadSearches(providerNode);
        loadMappings(providerNode);
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
        LdapContext userCtx = null;
        try {
            userCtx = lcf.getLdapContext(dn, creds.getPassword());
            return true;
        } catch (NamingException e) {
            log.debug("Exception while trying to authenticate user {} : {}", creds.getUserID(), e.getMessage());
        } finally {
            LdapUtils.closeContext(userCtx);
        }
        return false;
    }

    /**
     * TODO: Add an option to check against an attribute in the ldap server if
     * a user is active, or sync the status to the hippo:active property.
     */
    @Override
    public boolean isActive(final String userId) throws RepositoryException {
        return true;
    }

    /**
     * TODO: This needs to be checked against the LDAP server
     */
    @Override
    public boolean isPasswordExpired(final String userId) throws RepositoryException {
        return false;
    }

    /**
     * Update the current user info in the repository with info mapped from the ldap server.
     * @param userId
     */
    public void syncUserInfo(String userId) {
        if (!isInitialized()) {
            throw new IllegalStateException("Not initialized: " + providerId);
        }
        String dn;
        Node user;
        try {
            user = getUser(userId);

            if (user != null) {
                if (!isManagerForUser(user)) {
                    log
                            .warn("Unable to sync user info. User '{}' not not managed by provider: {} ", userId,
                                    providerId);
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
            syncMappingInfo(user, attrs);
        } catch (NamingException e) {
            log.error("Unable to sync user: {} : {}", userId, e);
        } finally {
            lcf.close(ctx);
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
        long startTime = System.currentTimeMillis();

        // paged searching
        boolean usePagedSearch = true;

        // ldap search
        NamingEnumeration<SearchResult> results;
        SearchControls ctls = new SearchControls();
        ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        LdapContext ctx = null;

        // search results
        String dn;
        Node user;
        String userId = null;
        int count = 0;
        int total = 0;

        // loop over all ldap searches
        for (LdapUserSearch search : searches) {
            try {
                log.debug("Searching for users in '{}' with filter '{}'", search.getBaseDn(), search.getFilter());
                ctx = lcf.getSystemLdapContext();
                usePagedSearch = LdapUtils.enablePagedSearching(ctx, LDAP_SEARCH_PAGE_SIZE);

                // outer loop for paged searching
                do {
                    results = ctx.search(search.getBaseDn(), search.getFilter(), ctls);

                    // inner loop over paged resultset
                    while (results != null && results.hasMore()) {
                        try {
                            SearchResult sr = results.next();
                            Attributes attrs = sr.getAttributes();
                            Attribute uidAttr = attrs.get(search.getNameAttr());
                            dn = sr.getName() + "," + search.getBaseDn();
                            log.trace("Found dn: {}", dn);
                            if (uidAttr == null) {
                                log.warn("Skipping dn='" + sr.getName() + "' because the uid attribute is not found.");
                            } else {
                                try {
                                    userId = (String) uidAttr.get();
                                    user = getUser(userId);

                                    // create the user if it doesn't exists
                                    if (user == null) {
                                        user = createUser(userId);
                                        user.setProperty(LdapSecurityProvider.PROPERTY_LDAP_DN, dn);
                                    }

                                    // update the mappings
                                    if (isManagerForUser(user)) {
                                        count++;
                                        total++;
                                        syncMappingInfo(user, attrs);
                                    }
                                } catch (RepositoryException e) {
                                    log.warn("Unable to update user: " + userId + " by provider: " + providerId, e);
                                }
                                if (count >= SAVE_INTERVAL) {
                                    count = 0;
                                    try {
                                        log.debug("Saving {} ldap users for provider: {}", SAVE_INTERVAL, providerId);
                                        saveUsers();
                                    } catch (RepositoryException e) {
                                        log.error("Error while saving users node: " + usersPath, e);
                                    }
                                }
                            }
                        } catch (NamingException e) {
                            log.error("Error while trying fetching user info from ldap: " + providerId, e);
                        }
                    }

                    if (usePagedSearch) {
                        usePagedSearch = LdapUtils.advancePagedResultSet(ctx, LDAP_SEARCH_PAGE_SIZE);
                    }

                } while (usePagedSearch);

            } catch (NamingException e) {
                log.error("Error while trying fetching users from ldap: " + providerId, e);
            } finally {
                lcf.close(ctx);
            }
        }

        // save remaining unsaved user nodes
        try {
            log.debug("Saving {} ldap users for provider: {}", count, providerId);
            saveUsers();
        } catch (RepositoryException e) {
            log.error("Error while saving users node: " + usersPath, e);
        }
        long duration = System.currentTimeMillis() - startTime;
        log
                .info("Finished synchronizing {} ldap users for: {} in {} ms.", new Object[] { total, providerId,
                        duration });
    }

    /**
     * Try to find the dn in the ldap server based on the configured searches.
     * @param userId
     * @return the dn or null if the dn is not found.
     */
    private String getDnForUser(String userId) throws RepositoryException {
        // Try to find the user in the ldap server.
        LdapContext ctx = null;
        try {
            ctx = lcf.getSystemLdapContext();
            SearchControls ctls = new SearchControls();
            ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            NamingEnumeration<SearchResult> results;

            for (LdapUserSearch search : searches) {
                String filter = "(&(" + search.getFilter() + ")(" + search.getNameAttr() + "=" + userId + "))";
                log
                        .debug("Searching for user: '" + userId + "' with filter '" + filter + "' in: "
                                + search.getBaseDn());
                results = ctx.search(search.getBaseDn(), filter, ctls);
                // just use the first match found
                if (results.hasMore()) {
                    SearchResult sr = results.next();
                    String dn = sr.getName() + "," + search.getBaseDn();
                    return dn;
                }
            }
        } catch (NamingException e) {
            log.error("Error while trying fetching dn from ldap: " + providerId, e);
        } finally {
            lcf.close(ctx);
        }
        return null;
    }

    private void syncMappingInfo(Node user, Attributes attrs) {
        try {
            String userId = user.getName();
            SearchControls ctls = new SearchControls();
            ctls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
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
            updateSyncDate(user);
        } catch (RepositoryException e) {
            log.error("RepositoryException while updating ldap mappings for provider: " + providerId, e);
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
        statement.append("'").append(providerNode.getPath()).append("/").append(HippoNodeType.NT_USERPROVIDER).append(
                "/%'");

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
        statement.append("'").append(providerNode.getPath()).append("/").append(HippoNodeType.NT_USERPROVIDER).append(
                "/%'");

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

    public boolean isCaseSensitive() {
        return isCaseSensitive;
    }

    public boolean isAutoSave() {
        return true;
    }
}
