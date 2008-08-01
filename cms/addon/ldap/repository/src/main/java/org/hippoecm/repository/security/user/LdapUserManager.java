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
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.security.LdapManagerContext;
import org.hippoecm.repository.security.LdapSecurityProvider;
import org.hippoecm.repository.security.ManagerContext;
import org.hippoecm.repository.security.ldap.LdapContextFactory;
import org.hippoecm.repository.security.ldap.LdapMapping;
import org.hippoecm.repository.security.ldap.LdapSearch;
import org.hippoecm.repository.security.ldap.LdapUtils;
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
    private final Set<LdapSearch> searches = new HashSet<LdapSearch>();

    /**
     * Logger
     */
    private final static Logger log = LoggerFactory.getLogger(LdapUserManager.class);

    //------------------------< Interface Impl >--------------------------//
    /**
     * {@inheritDoc}
     */
    public void initManager(ManagerContext context) throws RepositoryException {

        LdapManagerContext ldapContext = (LdapManagerContext) context;
        lcf = ldapContext.getLdapContextFactory();
        loadSearches(context.getProviderNode());
        loadMappings(context.getProviderNode());

        initialized = true;
        
        // initial update
        updateUsers();
    }

    @Override
    public boolean authenticate(String userId, char[] password) throws RepositoryException {
        if (!isInitialized()) {
            throw new IllegalStateException("Not initialized.");
        }

        // fetch (cached) dn
        String dn;
        try {
            dn = getUserNode(userId).getProperty(LdapSecurityProvider.PROPERTY_LDAP_DN).getString();
        } catch (PathNotFoundException e) {
            log.warn("Dn for ldap backed user '{}' not found: {}", userId, e.getMessage());
            return false;
        }

        LdapContext ctx = null;
        try {
            ctx = lcf.getLdapContext(dn, password);
            return true;
        } catch (NamingException e) {
            log.debug("Exception while trying to authenticate user {} : {}", userId, e.getMessage());
        } finally {
            LdapUtils.closeContext(ctx);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isActive(String userId) throws RepositoryException {
        return true;
    }

    /**
     * Update the current user with info mapped from the ldap server.
     * @param userId
     */
    @Override
    public void syncUser(String userId) {
        if (!isInitialized()) {
            throw new IllegalStateException("Not initialized.");
        }
        Node user = null;
        String dn = null;
        LdapContext ctx = null;

        try {
            user = getUserNode(userId);
            dn = user.getProperty(LdapSecurityProvider.PROPERTY_LDAP_DN).getString();
        } catch (RepositoryException e) {
            log.error("Failed to lookup user " + userId, e);
        }
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
                    log.debug("Skipping atturibute for user " + userId + " unable to get attributes: " + mapping.getSource() + " : " + e.getMessage());
                } catch (RepositoryException e) {
                    log.debug("Skipping attribute for user " + userId + " unable to get/create property: " + mapping.getTarget() + " : " + e.getMessage());
                }
            }
            user.setProperty(HippoNodeType.HIPPO_LASTSYNC, Calendar.getInstance());
            user.save();
        } catch (RepositoryException e) {
            log.error("Unable to get or create user node: " + userId, e);
        } catch (NamingException e) {
            log.error("Unable to sync user: {} : {}", userId, e);
        } finally {
            LdapUtils.closeContext(ctx);
        }
    }

    //------------------------< Helper methods >--------------------------//

    /**
     * Load all the users from the repository. All users are added to the Set of users
     * @see User
     */
    public synchronized void updateUsers() {

        log.debug("Starting synchronizing ldap users");

        NamingEnumeration<SearchResult> results = null;
        String dn = null;
        try {
            LdapContext ldapContext = lcf.getSystemLdapContext();
            SearchControls ctls = new SearchControls();
            ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);

            //
            for (LdapSearch search : searches) {
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
                        createUserIfNotExists((String) uidAttr.get(), dn);
                    }
                }
            }
        } catch (NamingException e) {
            log.error("Error while trying fetching users from ldap", e);
        }
    }

    private void createUserIfNotExists(String userId, String dn) {
        log.trace("Checking user: {} for dn: {}", userId, dn);
        Node user;

        try {
            user = getUserNode(userId);
            // user exists, don't mess with it.
            return;
        } catch (PathNotFoundException e) {
            // fall through, create new user
        } catch (RepositoryException e) {
            log.error("Failed to lookup user " + userId, e);
            return;
        }

        try {
            // user does not exist, create
            user = createUserNode(userId, HippoNodeType.NT_EXTERNALUSER);
            user.setProperty(HippoNodeType.HIPPO_SECURITYPROVIDER, providerId);
            user.setProperty(HippoNodeType.HIPPO_LASTSYNC, Calendar.getInstance());
            user.setProperty(LdapSecurityProvider.PROPERTY_LDAP_DN, dn);
            // save is needed on the parent
            user.getParent().save();
            log.info("User: {} created by by {} ", userId, providerId);
        } catch (RepositoryException e) {
            log.error("Failed to create user " + userId, e);
        }
        return;
    }

    private void loadSearches(Node providerNode) throws RepositoryException {
        StringBuffer statement = new StringBuffer();
        statement.append("SELECT * FROM ").append(LdapSecurityProvider.NT_LDAPSEARCH);
        statement.append(" WHERE");
        statement.append(" jcr:path LIKE ");
        statement.append("'").append(providerNode.getPath()).append("/").append(HippoNodeType.NT_USERPROVIDER).append("/%'");


        //log.debug("Searching for security searches: ", statement);

        QueryManager qm;
        qm = session.getWorkspace().getQueryManager();
        Query q = qm.createQuery(statement.toString(), Query.SQL);
        QueryResult result = q.execute();
        NodeIterator nodeIter = result.getNodes();

        while (nodeIter.hasNext()) {
            try {
                Node search = nodeIter.nextNode();
                String nameAttr = search.getProperty(LdapSearch.PROPERTY_NAME_ATTR).getString();
                String baseDn = search.getProperty(LdapSearch.PROPERTY_BASE_DN).getString();
                LdapSearch ldapSearch = new LdapSearch(baseDn, nameAttr);
                if (search.hasProperty(LdapSearch.PROPERTY_FILTER)) {
                    ldapSearch.setFilter(search.getProperty(LdapSearch.PROPERTY_FILTER).getString());
                }
                searches.add(ldapSearch);
            } catch (RepositoryException e) {
                log.warn("Unable to parse search: " + e.getMessage());
            }
        }
    }

    private void loadMappings(Node providerNode) throws RepositoryException {
        StringBuffer statement = new StringBuffer();
        statement.append("SELECT * FROM ").append(LdapSecurityProvider.NT_LDAPMAPPING);
        statement.append(" WHERE");
        statement.append(" jcr:path LIKE ");
        statement.append("'").append(providerNode.getPath()).append("/").append(HippoNodeType.NT_USERPROVIDER).append("/%'");

        //log.debug("Searching for security searches: ", statement);

        QueryManager qm;
        qm = session.getWorkspace().getQueryManager();
        Query q = qm.createQuery(statement.toString(), Query.SQL);
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
}
