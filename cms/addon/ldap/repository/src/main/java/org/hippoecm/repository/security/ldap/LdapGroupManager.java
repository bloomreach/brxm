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


import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
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
import org.hippoecm.repository.security.group.AbstractGroupManager;
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
     * On sync save every after every SAVE_INTERVAL changes
     */
    private final static int SAVE_INTERVAL = 2500;
    
    /**
     * The initialized ldap context factory
     */
    private LdapContextFactory lcf;

    /**
     * The initialized ldap context factory
     */
    LdapContext systemCtx = null;

    /**
     * The attribute to property mappings
     */
    Set<LdapMapping> mappings = new HashSet<LdapMapping>();

    /**
     * The user searches
     */
    private final Set<LdapGroupSearch> searches = new HashSet<LdapGroupSearch>();

    /**
     * Logger
     */
    private final static Logger log = LoggerFactory.getLogger(LdapGroupManager.class);

    /**
     * initialize
     */
    public void initManager(ManagerContext context) throws RepositoryException {
        LdapManagerContext ldapContext = (LdapManagerContext) context;
        lcf = ldapContext.getLdapContextFactory();
        try {
            systemCtx = lcf.getSystemLdapContext();
        } catch (NamingException e) {
            throw new RepositoryException("Unable to connect to the ldap server for: " + providerId, e);
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
            List<String> uids = new ArrayList<String>();
            // parse member dn string
            for (String member : members) {
                if (member != null) {
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
            group.setProperty(HippoNodeType.HIPPO_MEMBERS, uids.toArray(new String[uids.size()]));
            group.setProperty(HippoNodeType.HIPPO_LASTSYNC, Calendar.getInstance());
            log.debug("Updated {} members of for group: {}", uids.size(), dn);
        } catch (RepositoryException e) {
            log.warn("Unable to update members of group {} : {}", dn, e.getMessage());
        }

        if (mappings.size() == 0) {
            return;
        }
        
        try {
            SearchControls ctls = new SearchControls();
            ctls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
            Attributes attrs = systemCtx.getAttributes(dn);
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
        } catch (NamingException e) {
            log.error("Unable to sync group attributes: {}", e.getMessage());
        }
    }
    
    /**
     * Get a set of the memberships of the user from the ldap server. For each search 
     * configured in the repository a filter is created in with the MemberAttr and 
     * the MemberNameMatcher. In the Membername matcher the uid and the dn are 
     * substituted.
     */
    public Set<String> backendGetMemberships(Node user) throws RepositoryException {
        Set<String> groups  = new HashSet<String>();
        NamingEnumeration<SearchResult> results = null;
        String dn = user.getProperty(LdapSecurityProvider.PROPERTY_LDAP_DN).getString();
        String userId = user.getName();        
        try {
            SearchControls ctls = new SearchControls();
            ctls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
            for (LdapGroupSearch search : searches) { 
                if (search.getBaseDn() == null || search.getNameAttr() == null || search.getFilter() == null) {
                    // skip wrongly configured search
                    log.warn("Skipping search base dn: " + search.getBaseDn() + " name attr: " + search.getNameAttr());
                    continue;
                }

                String filter = "(&(" + search.getFilter() + ")(" + search.getMemberAttr() + "=" + search.getMemberNameMatcher() + "))";
                filter = filter.replaceFirst(LdapGroupSearch.UID_MATCHER, userId);
                filter = filter.replaceFirst(LdapGroupSearch.DN_MATCHER, dn);
                if (log.isDebugEnabled()) {
                    log.debug("Searching for memberships of user '" + userId + "' with filter '" + filter
                            + "' providerId: " + providerId);
                }
                results = systemCtx.search(search.getBaseDn(), filter, ctls);
                while (results.hasMore()) {
                    SearchResult sr = results.next();
                    Attributes attrs = sr.getAttributes();
                    Attribute nameAttr = attrs.get(search.getNameAttr());
                    if ( nameAttr != null) {
                        groups.add((String) nameAttr.get());
                    }
                }
            }
        } catch (NamingException e) {
            log.error("Error while trying fetching users from ldap", e);
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
        NamingEnumeration<SearchResult> results = null;
        String dn = null;
        try {
            SearchControls ctls = new SearchControls();
            ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);

            int count = 0;
            for (LdapGroupSearch search : searches) {
                if (search.getBaseDn() == null || search.getNameAttr() == null || search.getFilter() == null) {
                    // skip wrongly configured search?
                    log.warn("Skipping search base dn: " + search.getBaseDn() + " name attr: " + search.getNameAttr());
                    continue;
                }
                
                results = systemCtx.search(search.getBaseDn(), search.getFilter(), ctls);
                if (log.isDebugEnabled()) {
                    log.debug("Searching for groups in '"+search.getBaseDn()+"' with filter '"+search.getFilter()+"'");
                }
                while (results.hasMore()) {
                    SearchResult sr = results.next();
                    Attributes attrs = sr.getAttributes();
                    Attribute nameAttr = attrs.get(search.getNameAttr());
                    dn = sr.getName() + "," + search.getBaseDn();
                    if (nameAttr == null) {
                        log.warn("Skipping dn='" + sr.getName() + "' because the naming attribute is not found.");
                    } else {
                        String groupId = (String) nameAttr.get();
                        try {
                            Node group = getOrCreateGroup(groupId);
                            List<String> members = LdapUtils.getAllAttributeValues(attrs.get(search.getMemberAttr()));
                            if (isManagerForGroup(group)) {
                                setGroup(group, dn, members, search.getMemberNameMatcher());
                                count++;
                            } else {
                                log.debug("Not updating group {}, because it is not managed by this provider: {}", dn, providerId);
                            }
                        } catch (RepositoryException e) {
                            log.error("Error while updating or creating group " + groupId + " by provider: " + providerId, e);
                        }
                        if (count == SAVE_INTERVAL) {
                            count = 0;
                            try {
                                saveGroups();
                            } catch (RepositoryException e) {
                                log.error("Error while saving groups node: " + groupsPath, e);
                            }
                        }
                    }
                }
            }
        } catch (NamingException e) {
            log.error("Error while trying fetching users from ldap", e);
        }

        // save remaining unsaved group nodes
        try {
           saveGroups();
        } catch (RepositoryException e) {
            log.error("Error while saving groups node: " + groupsPath, e);
        }
        log.info("Finished synchronizing ldap groups for: " + providerId);
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
        statement.append("'").append(providerNode.getPath()).append("/").append(HippoNodeType.NT_GROUPPROVIDER).append("/%'");

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
                    ldapSearch.setMemberNameMatcher(search.getProperty(LdapGroupSearch.PROPERTY_MEMBERNAME_MATCHER).getString());
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
        statement.append("'").append(providerNode.getPath()).append("/").append(HippoNodeType.NT_GROUPPROVIDER).append("/%'");

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
}
