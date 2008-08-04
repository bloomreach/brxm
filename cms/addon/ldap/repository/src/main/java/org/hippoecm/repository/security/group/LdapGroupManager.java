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


import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
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
 * GroupManager backend that fetches users from LDAP and stores the users inside the JCR repository
 */
public class LdapGroupManager extends AbstractGroupManager {

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
    private final static Logger log = LoggerFactory.getLogger(LdapGroupManager.class);

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
        updateGroups();
    }

    /**
     * Update the current user with info mapped from the ldap server.
     * @param userId
     */
    public void setGroup(Node group, String dn, List<String> members, String memberNameAttr) {
        if (!isInitialized()) {
            throw new IllegalStateException("Not initialized.");
        }
        try {
            log.debug("Found " + members.size() + " members for group: " + dn);
            List<String> uids = new ArrayList<String>();
            // parse member dn string
            // TODO: make more generic and support dn lookups in ldap.
            for (String member : members) {
                if (member != null && member.startsWith(memberNameAttr)) {
                    int pos = member.indexOf(',');
                    if (pos > 0) {
                        // format: uid=user,ou=People,dc=onehippo,dc=org
                        uids.add(member.substring(0, pos).substring(member.indexOf('=') + 1));
                    } else { 
                        // format: uid=user
                        uids.add(member.substring(member.indexOf('=') + 1));
                    }
                }
            }
            group.setProperty(HippoNodeType.HIPPO_MEMBERS, uids.toArray(new String[uids.size()]));
            group.setProperty(HippoNodeType.HIPPO_LASTSYNC, Calendar.getInstance());
            group.save();
            log.info("Updated members of for group: {}", dn);
        } catch (RepositoryException e) {
            log.warn("Unable to update members of group {} : {}", dn, e.getMessage());
        }

        if (mappings.size() == 0) {
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
                            group.setProperty(mapping.getTarget(), (String) o);
                        }
                    }
                } catch (NamingException e) {
                    log.debug("Skipping atturibute for group unable to get attributes: {} : {}", mapping.getSource(), e.getMessage());
                } catch (RepositoryException e) {
                    log.debug("Skipping attribute for group unable to get/create property: {} : {}", mapping.getTarget(), e.getMessage());
                }
            }
            group.save();
        } catch (RepositoryException e) {
            log.error("Unable sync group node attributes: {}", e.getMessage());
        } catch (NamingException e) {
            log.error("Unable to sync group attributes: {}", e.getMessage());
        } finally {
            LdapUtils.closeContext(ctx);
        }
    }

    //------------------------< Helper methods >--------------------------//

    /**
     * Load all the users from the repository. All users are added to the Set of users
     */
    public synchronized void updateGroups() {

        log.debug("Starting synchronizing ldap groups");

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
                    Attribute nameAttr = attrs.get(search.getNameAttr());
                    dn = sr.getName() + "," + search.getBaseDn();
                    if (nameAttr == null) {
                        log.warn("Skipping dn='" + sr.getName() + "' because the naming attribute is not found.");
                    } else {
                        Node group = getOrCreateGroup((String) nameAttr.get(), dn);
                        List<String> members = LdapUtils.getAllAttributeValues(attrs.get(search.getMemberAttr()));
                        if (group != null) {
                            setGroup(group, dn, members, search.getMemberNameAttr());
                        } else {
                            log.debug("Not updating group {}, because it is not managed by this provider: {}", dn, providerId);
                        }
                    }
                }
            }
        } catch (NamingException e) {
            log.error("Error while trying fetching users from ldap", e);
        }
    }

    private Node getOrCreateGroup(String groupId, String dn) {
        log.trace("Checking group: {} for dn: {}", groupId, dn);
        Node group;

        try {
            group = getGroupNode(groupId);
            if (group.hasProperty(HippoNodeType.HIPPO_SECURITYPROVIDER) &&
                    providerId.equals(group.getProperty(HippoNodeType.HIPPO_SECURITYPROVIDER).getString())) {
                // group is managed by this provider
                return group;
            }
            // not managed by this provider
            return null;
        } catch (PathNotFoundException e) {
            // fall through, create new gruop
        } catch (RepositoryException e) {
            log.error("Failed to lookup group " + groupId, e);
            return null;
        }

        try {
            // group does not exist, create
            group = createGroupNode(groupId, HippoNodeType.NT_EXTERNALGROUP);
            group.setProperty(HippoNodeType.HIPPO_SECURITYPROVIDER, providerId);
            group.setProperty(HippoNodeType.HIPPO_LASTSYNC, Calendar.getInstance());
            group.setProperty(LdapSecurityProvider.PROPERTY_LDAP_DN, dn);
            // save is needed on the parent
            group.getParent().save();
            log.info("Group: {} created by by {} ", groupId, providerId);
        } catch (RepositoryException e) {
            log.error("Failed to create group " + groupId, e);
            return null;
        }
        return group;
    }

    private void loadSearches(Node providerNode) throws RepositoryException {
        StringBuffer statement = new StringBuffer();
        statement.append("SELECT * FROM ").append(LdapSecurityProvider.NT_LDAPSEARCH);
        statement.append(" WHERE");
        statement.append(" jcr:path LIKE ");
        statement.append("'").append(providerNode.getPath()).append("/").append(HippoNodeType.NT_GROUPPROVIDER).append("/%'");

        log.debug("Searching for security searches: {}", statement);

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
                if (search.hasProperty(LdapSearch.PROPERTY_MEMBER_ATTR)) {
                    ldapSearch.setFilter(search.getProperty(LdapSearch.PROPERTY_MEMBER_ATTR).getString());
                }
                if (search.hasProperty(LdapSearch.PROPERTY_MEMBERNAME_ATTR)) {
                    ldapSearch.setFilter(search.getProperty(LdapSearch.PROPERTY_MEMBERNAME_ATTR).getString());
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
        statement.append("'").append(providerNode.getPath()).append("/").append(HippoNodeType.NT_GROUPPROVIDER).append("/%'");

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
