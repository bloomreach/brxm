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
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.security.ManagerContext;
import org.hippoecm.repository.security.group.AbstractGroupManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GroupManager backend that fetches groups from LDAP and stores the groups inside the JCR repository
 * 
 */
public class LdapGroupManager extends AbstractGroupManager {

    /** SVN id placeholder */

    /**
     * On sync save every after every SAVE_INTERVAL changes
     * TODO: make configurable
     */
    private static final int SAVE_INTERVAL = 100;

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
    private final Set<LdapGroupSearch> searches = new HashSet<LdapGroupSearch>();

    /**
     * Logger
     */
    private static final Logger log = LoggerFactory.getLogger(LdapGroupManager.class);

    /**
     * Use case sensitive group matching
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
     * Update the group info in the repository. It parses the members as they are found in
     * the ldap server with the memberNameMatcher to find the corresponding uids in the
     * repository.
     * TODO: needs refactoring
     * @param dn the dn of the group
     * @param members the members as they are found in the ldap
     * @param memberNameMatcher match the ldap member strings with this pattern to find the uids
     */
    private void setGroup(Node group, String dn, List<String> members, String memberNameMatcher) {
        if (!isInitialized()) {
            throw new IllegalStateException("Not initialized: " + providerId);
        }
        try {
            if (log.isTraceEnabled()) {
                log.trace("Found " + members.size() + " members for group: " + dn);
            }
            Set<String> uids = new HashSet<String>();

            // do lookups
            if (memberNameMatcher.startsWith("<dn>:")) {
                for (String member : members) {
                    if (member != null) {
                        // do a ldap lookup to find userId
                        String uidAttr = memberNameMatcher.substring(5);
                        // do lookup
                        String uid = getAttrForDn(member, uidAttr);
                        if (uid != null) {
                            log.trace("Found uid '{}' for lookup '{}'", uid, member);
                            uids.add(uid);
                        } else {
                            log.debug("Unable to find '{}' with attribute '{}'", member, uidAttr);
                        }
                    }
                }
            }

            // parse matcher
            String prefix = "";
            String suffix = "";
            int pos = -1;
            int len = -1;
            boolean dnMatch = false;
            if (memberNameMatcher.contains(LdapGroupSearch.UID_MATCHER)) {
                pos = memberNameMatcher.indexOf(LdapGroupSearch.UID_MATCHER);
                len = LdapGroupSearch.UID_MATCHER.length();
            } else if (memberNameMatcher.contains(LdapGroupSearch.DN_MATCHER)) {
                pos = memberNameMatcher.indexOf(LdapGroupSearch.DN_MATCHER);
                len = LdapGroupSearch.DN_MATCHER.length();
                dnMatch = true;
            }
            prefix = memberNameMatcher.substring(0, pos);
            suffix = memberNameMatcher.substring(pos + len);

            // parse member dn string
            for (String member : members) {
                if (member != null) {

                    if (!"".equals(prefix)) {
                        // strip prefix
                        if (member.startsWith(prefix)) {
                            member = member.substring(prefix.length());
                        } else {
                            // no match
                            continue;
                        }
                    }

                    if (!"".equals(suffix)) {
                        // strip suffix
                        if (member.endsWith(suffix)) {
                            member = member.substring(member.length() - suffix.length(), member.length());
                        } else {
                            // no match
                            continue;
                        }
                    }

                    if (dnMatch) {
                        // just parse
                        // format: uid=user,ou=People,dc=onehippo,dc=org
                        int equalPos = member.indexOf('=');
                        int commaPos = member.indexOf(',');
                        if (commaPos > 0 && equalPos > 0) {
                            uids.add(member.substring(0, commaPos).substring(equalPos + 1));
                        } else {
                            // no match
                            continue;
                        }
                    } else {
                        // format: user
                        uids.add(member);
                    }
                }
            }
            group.setProperty(LdapSecurityProvider.PROPERTY_LDAP_DN, dn);
            setMembers(group, uids);
            updateSyncDate(group);
            log.trace("Updated {} members of for group: {}", uids.size(), dn);
        } catch (RepositoryException e) {
            log.warn("Unable to update members of group {} : {}", dn, e.getMessage());
            log.debug("Unable to update members of group", e);
        }

        if (mappings.size() == 0) {
            return;
        }

        LdapContext ctx = null;
        try {
            ctx = lcf.getSystemLdapContext();
            SearchControls ctls = new SearchControls();
            ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
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
                    log.debug("Skipping atturibute for group unable to get attributes: {} : {}", mapping.getSource(), e
                            .getMessage());
                } catch (RepositoryException e) {
                    log.debug("Skipping attribute for group unable to get/create property: {} : {}", mapping
                            .getTarget(), e.getMessage());
                }
            }
        } catch (NamingException e) {
            log.error("Unable to sync group attributes: {}", e.getMessage());
        } finally {
            lcf.close(ctx);
        }
    }

    /**
     * Get a set of the memberships of the user from the ldap server. For each search
     * configured in the repository a filter is created in with the MemberAttr and
     * the MemberNameMatcher. In the Membername matcher the uid and the dn are
     * substituted.
     */
    public Set<String> backendGetMemberships(Node user) throws RepositoryException {
        Set<String> groups = new HashSet<String>();
        NamingEnumeration<SearchResult> results = null;
        String dn = user.getProperty(LdapSecurityProvider.PROPERTY_LDAP_DN).getString();
        String userId = user.getName();
        LdapContext ctx = null;
        try {
            ctx = lcf.getSystemLdapContext();
            SearchControls ctls = new SearchControls();
            ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            for (LdapGroupSearch search : searches) {
                if (search.getBaseDn() == null || search.getNameAttr() == null || search.getFilter() == null) {
                    // skip wrongly configured search
                    log.warn("Skipping search base dn: " + search.getBaseDn() + " name attr: " + search.getNameAttr());
                    continue;
                }
                String memberNameMatcher = search.getMemberNameMatcher();
                if (memberNameMatcher.contains(":")) {
                    memberNameMatcher = memberNameMatcher.substring(0, memberNameMatcher.indexOf(":"));
                }
                String filter = "(&(" + search.getFilter() + ")(" + search.getMemberAttr() + "=" + memberNameMatcher
                        + "))";
                filter = filter.replaceFirst(LdapGroupSearch.UID_MATCHER, userId);
                filter = filter.replaceFirst(LdapGroupSearch.DN_MATCHER, dn);
                if (log.isDebugEnabled()) {
                    log.debug("Searching for memberships of user '" + userId + "' with filter '" + filter
                            + "' providerId: " + providerId);
                }
                results = ctx.search(search.getBaseDn(), filter, ctls);
                String groupId = null;
                while (results.hasMore()) {
                    SearchResult sr = results.next();
                    groupId = buildGroupName(search, sr);
                    if (groupId != null) {
                        groups.add(groupId);
                    }
                }
            }
        } catch (NamingException e) {
            log.error("Error while trying fetching groups from ldap", e);
        } finally {
            lcf.close(ctx);
        }
        return groups;
    }

    /**
     * Synchronize all ldap groups with the repository. This method can take a long time if
     * there are a lot of groups with a lot of members in the ldap and should run in it's
     * own thread. It is called from the LdapSecurityProvider.sync method.
     * The saves to the repository are done in batches of SAVE_INTERVAL size.
     */
    public synchronized void updateGroups() {
        log.info("Starting synchronizing ldap groups for: " + providerId);
        long startTime = System.currentTimeMillis();

        // paged searching
        boolean usePagedSearch = true;

        // ldap search
        NamingEnumeration<SearchResult> results = null;
        SearchControls ctls = new SearchControls();
        ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        LdapContext ctx = null;

        // search results
        String dn = null;
        int count = 0;
        int total = 0;

        // loop over all ldap searches
        for (LdapGroupSearch search : searches) {
            try {
                if (search.getBaseDn() == null || search.getNameAttr() == null || search.getFilter() == null) {
                    // skip wrongly configured search?
                    log.warn("Skipping search base dn: " + search.getBaseDn() + " name attr: " + search.getNameAttr());
                    continue;
                }

                log.debug("Searching for groups in '{}' with filter '{}'", search.getBaseDn(), search.getFilter());
                ctx = lcf.getSystemLdapContext();
                usePagedSearch = LdapUtils.enablePagedSearching(ctx, LDAP_SEARCH_PAGE_SIZE);

                // outer loop for paged searching
                do {
                    results = ctx.search(search.getBaseDn(), search.getFilter(), ctls);

                    // inner loop over paged resultset
                    while (results.hasMore()) {
                        try {
                            SearchResult sr = results.next();
                            Attributes attrs = sr.getAttributes();
                            dn = sr.getName() + "," + search.getBaseDn();
                            String groupId = buildGroupName(search, sr);
                            if (groupId != null) {
                                try {
                                    Node group = getOrCreateGroup(groupId);
                                    List<String> members = LdapUtils.getAllAttributeValues(attrs.get(search
                                            .getMemberAttr()));
                                    if (isManagerForGroup(group)) {
                                        setGroup(group, dn, members, search.getMemberNameMatcher());
                                        count++;
                                        total++;
                                    } else {
                                        log
                                                .debug(
                                                        "Not updating group {}, because it is not managed by this provider: {}",
                                                        dn, providerId);
                                    }
                                } catch (RepositoryException e) {
                                    log.error("Error while updating or creating group " + groupId + " by provider: "
                                            + providerId, e);
                                }
                                if (count == SAVE_INTERVAL) {
                                    count = 0;
                                    try {
                                        log.debug("Saving {} ldap groups for provider: {}", SAVE_INTERVAL, providerId);
                                        saveGroups();
                                    } catch (RepositoryException e) {
                                        log.error("Error while saving groups node: " + groupsPath, e);
                                    }
                                }
                            }
                        } catch (NamingException e) {
                            log.error("Error while trying fetching group info from ldap: " + providerId, e);
                        }
                    }
                    if (usePagedSearch) {
                        usePagedSearch = LdapUtils.advancePagedResultSet(ctx, LDAP_SEARCH_PAGE_SIZE);
                    }

                } while (usePagedSearch);
            } catch (NamingException e) {
                log.error("Error while trying fetching groups from ldap", e);
            } finally {
                lcf.close(ctx);
            }
        }

        // save remaining unsaved group nodes
        try {
            log.debug("Saving {} ldap groups for provider: {}", count, providerId);
            saveGroups();
        } catch (RepositoryException e) {
            log.error("Error while saving groups node: " + groupsPath, e);
        }
        long duration = System.currentTimeMillis() - startTime;
        log.info("Finished synchronizing {} ldap groups for: {} in {} ms.",
                new Object[] { total, providerId, duration });
    }

    /**
     * Load and parse the search configurations from the repository.
     * @param providerNode
     * @throws RepositoryException
     */
    private void loadSearches(Node providerNode) throws RepositoryException {
        StringBuffer statement = new StringBuffer();
        statement.append("SELECT * FROM ").append(LdapSecurityProvider.NT_LDAPGROUPSEARCH);
        statement.append(" WHERE");
        statement.append(" jcr:path LIKE ");
        statement.append("'").append(providerNode.getPath()).append("/").append(HippoNodeType.NT_GROUPPROVIDER).append(
                "/%'");

        Query q = session.getWorkspace().getQueryManager().createQuery(statement.toString(), Query.SQL);
        QueryResult result = q.execute();
        NodeIterator nodeIter = result.getNodes();
        while (nodeIter.hasNext()) {
            try {
                Node search = nodeIter.nextNode();
                String nameAttr = search.getProperty(LdapGroupSearch.PROPERTY_NAME_ATTR).getString();
                String baseDn = search.getProperty(LdapGroupSearch.PROPERTY_BASE_DN).getString();
                LdapGroupSearch ldapSearch = new LdapGroupSearch(baseDn, nameAttr);
                if (search.hasProperty(LdapGroupSearch.PROPERTY_FILTER)) {
                    ldapSearch.setFilter(search.getProperty(LdapGroupSearch.PROPERTY_FILTER).getString());
                }
                if (search.hasProperty(LdapGroupSearch.PROPERTY_MEMBER_ATTR)) {
                    ldapSearch.setMemberAttr(search.getProperty(LdapGroupSearch.PROPERTY_MEMBER_ATTR).getString());
                }
                if (search.hasProperty(LdapGroupSearch.PROPERTY_MEMBERNAME_MATCHER)) {
                    ldapSearch.setMemberNameMatcher(search.getProperty(LdapGroupSearch.PROPERTY_MEMBERNAME_MATCHER)
                            .getString());
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
        statement.append("'").append(providerNode.getPath()).append("/").append(HippoNodeType.NT_GROUPPROVIDER).append(
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

    public String getNodeType() {
        return HippoNodeType.NT_EXTERNALGROUP;
    }

    public boolean isCaseSensitive() {
        return isCaseSensitive;
    }

    public String buildGroupName(LdapGroupSearch search, SearchResult sr) {
        String groupId = null;
        Attributes attrs = sr.getAttributes();
        String nameAttrName = search.getNameAttr();
        groupId = null;
        if (LdapGroupSearch.DN_MATCHER.equals(nameAttrName)) {
            groupId = sr.getName();
        } else if (LdapGroupSearch.COMPACT_DN_MATCHER.equals(nameAttrName)) {
            String[] parts = sr.getName().split(",");
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (int i = 0; i < parts.length; i++) {
                if (parts[i] != null && parts[i].contains("=")) {
                    if (!first) {
                        sb.append('.');
                    }
                    sb.append(parts[i].substring(parts[i].indexOf("=") + 1, parts[i].length()));
                    first = false;
                }
            }
            groupId = sb.toString();
        } else if (LdapGroupSearch.REVERSE_COMPACT_DN_MATCHER.equals(nameAttrName)) {
            String[] parts = sr.getName().split(",");
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (int i = (parts.length - 1); i >= 0; i--) {
                if (parts[i] != null && parts[i].contains("=")) {
                    if (!first) {
                        sb.append('.');
                    }
                    sb.append(parts[i].substring(parts[i].indexOf("=") + 1, parts[i].length()));
                    first = false;
                }
            }
            groupId = sb.toString();
        } else {
            try {
                Attribute nameAttr = attrs.get(nameAttrName);
                if (nameAttr != null) {
                    groupId = (String) nameAttr.get();
                }
            } catch (NamingException e) {
                log.warn("Skipping dn='" + sr.getName() + "' because the naming attribute is not found.");
            }
        }
        return groupId;
    }

    /** 
     * Get an attribute from a dn
     * @param dn
     * @param attrName
     * @return the string representation of the attribute or null when the dn or attribute is not found
     * @throws RepositoryException
     */
    private String getAttrForDn(String dn, String attrName) throws RepositoryException {
        // Try to find the user in the ldap server.
        LdapContext ctx = null;
        try {
            ctx = lcf.getSystemLdapContext();
            Attributes attrs = ctx.getAttributes(dn, new String[] { attrName });
            if (attrs != null) {
                Attribute attr = attrs.get(attrName);
                if (attr != null) {
                    return (String) attr.get();
                }
            }
        } catch (NameNotFoundException e) {
            log.debug("DN {} not found: {}", dn, e.getMessage());
        } catch (NamingException e) {
            log.error("Error while trying fetching dn from ldap: " + providerId, e);
        } finally {
            lcf.close(ctx);
        }
        return null;
    }
}
