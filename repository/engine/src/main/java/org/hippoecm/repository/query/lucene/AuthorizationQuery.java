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
package org.hippoecm.repository.query.lucene;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.security.auth.Subject;

import org.apache.jackrabbit.core.query.lucene.FieldNames;
import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.jackrabbit.core.security.SystemPrincipal;
import org.apache.jackrabbit.core.security.UserPrincipal;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.hippoecm.repository.jackrabbit.InternalHippoSession;
import org.hippoecm.repository.security.AuthorizationFilterPrincipal;
import org.hippoecm.repository.security.FacetAuthConstants;
import org.hippoecm.repository.security.domain.DomainRule;
import org.hippoecm.repository.security.domain.QFacetRule;
import org.hippoecm.repository.security.principals.FacetAuthPrincipal;
import org.hippoecm.repository.security.principals.GroupPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthorizationQuery {

    /**
     * The logger instance for this class
     */
    private static final Logger log = LoggerFactory.getLogger(AuthorizationQuery.class);

    private static final String MESSAGE_ZEROMATCH_QUERY = "returning a match zero nodes query";
    public static final String NODETYPE = "nodetype";
    public static final String NODENAME = "nodename";

    /**
     * The lucene query
     */
    private final BooleanQuery query;


    public AuthorizationQuery(final Subject subject,
                              final NamespaceMappings nsMappings,
                              final ServicingIndexingConfiguration indexingConfig,
                              final NodeTypeManager ntMgr,
                              final Session session) throws RepositoryException {
        // set the max clauses for booleans higher than the default 1024.
        BooleanQuery.setMaxClauseCount(Integer.MAX_VALUE);
        if (!(session instanceof InternalHippoSession)) {
            throw new RepositoryException("Session is not an instance of o.a.j.core.SessionImpl");
        }
      
        if (!subject.getPrincipals(SystemPrincipal.class).isEmpty()) {
            this.query = new BooleanQuery(true);
            this.query.add(new MatchAllDocsQuery(), Occur.MUST);
        } else {
            final Set<String> memberships = new HashSet<String>();
            for (GroupPrincipal groupPrincipal : subject.getPrincipals(GroupPrincipal.class)) {
                memberships.add(groupPrincipal.getName());
            }
            final Set<String> userIds = new HashSet<String>();
            for (UserPrincipal userPrincipal : subject.getPrincipals(UserPrincipal.class)) {
                userIds.add(userPrincipal.getName());
            }
            long start = System.currentTimeMillis();
            this.query = initQuery(subject.getPrincipals(FacetAuthPrincipal.class),
                                    subject.getPrincipals(AuthorizationFilterPrincipal.class),
                                    userIds,
                                    memberships,
                                    (InternalHippoSession)session,
                                    indexingConfig,
                                    nsMappings,
                                    ntMgr);
            log.info("Creating authorization query took {} ms. Query: {}", String.valueOf(System.currentTimeMillis() - start), query);
        }
    }

    private BooleanQuery initQuery(final Set<FacetAuthPrincipal> facetAuths,
                                   final Set<AuthorizationFilterPrincipal> authorizationFilterPrincipals,
                                   final Set<String> userIds,
                                   final Set<String> memberships,
                                   final InternalHippoSession session,
                                   final ServicingIndexingConfiguration indexingConfig,
                                   final NamespaceMappings nsMappings,
                                   final NodeTypeManager ntMgr) {

        Map<String, Collection<QFacetRule>> extendedFacetRules = new HashMap<String, Collection<QFacetRule>>();
        for (AuthorizationFilterPrincipal afp : authorizationFilterPrincipals) {
            final Map<String, Collection<QFacetRule>> facetRules = afp.getExpandedFacetRules(facetAuths);
            for (Map.Entry<String, Collection<QFacetRule>> entry : facetRules.entrySet()) {
                final String domainPath = entry.getKey();
                if (!extendedFacetRules.containsKey(domainPath)) {
                    extendedFacetRules.put(domainPath, new ArrayList<QFacetRule>());
                }
                extendedFacetRules.get(domainPath).addAll(entry.getValue());
            }
        }

        BooleanQuery authQuery = new BooleanQuery(true);
        for (final FacetAuthPrincipal facetAuthPrincipal : facetAuths) {
            if (!facetAuthPrincipal.getPrivileges().contains("jcr:read")) {
                continue;
            }
            for (final DomainRule domainRule : facetAuthPrincipal.getRules()) {
                BooleanQuery facetQuery = new BooleanQuery(true);
                for (final QFacetRule facetRule : getFacetRules(domainRule, extendedFacetRules)) {
                    Query q = getFacetRuleQuery(facetRule, userIds, memberships, facetAuthPrincipal.getRoles(), indexingConfig, nsMappings, session, ntMgr);
                    if (q == null) {
                        continue;
                    }
                    log.debug("Adding to FacetQuery: FacetRuleQuery = {}", q);
                    log.debug("FacetRuleQuery has {} clauses.", (q instanceof BooleanQuery) ? ((BooleanQuery) q).getClauses().length : 1);
                    facetQuery.add(q, Occur.MUST);
                }
                log.debug("Adding to Authorization query: FacetQuery = {}", facetQuery);
                log.debug("FacetQuery has {} clauses.", facetQuery.getClauses().length);
                if (facetQuery.getClauses().length == 1 && facetQuery.getClauses()[0].getQuery() instanceof MatchAllDocsQuery) {
                    log.info("found a MatchAllDocsQuery that will be OR-ed with other constraints for user '{}'. This means, the user can read " +
                            "everywhere. Short circuit the auth query and return MatchAllDocsQuery", session.getUserID());
                    // directly return the BooleanQuery that only contains the MatchAllDocsQuery : This is more efficient
                    return facetQuery;
                }
                authQuery.add(facetQuery, Occur.SHOULD);
            }
        }

        log.debug("Authorization query is : " + authQuery);
        log.debug("Authorization query has {} clauses", authQuery.getClauses().length);
        return authQuery;
    }

    private Set<QFacetRule> getFacetRules(final DomainRule domainRule, Map<String, Collection<QFacetRule>> extendedFacetRules) {
        if (extendedFacetRules != null) {
            final String domainRulePath = domainRule.getDomainName() + "/" + domainRule.getName();
            final Collection<QFacetRule> extendedRules = extendedFacetRules.get(domainRulePath);
            if (extendedRules != null) {
                final Set<QFacetRule> facetRules = new HashSet<QFacetRule>(domainRule.getFacetRules());
                facetRules.addAll(extendedRules);
                return facetRules;
            }
        }
        return domainRule.getFacetRules();
    }

    private Query getFacetRuleQuery(final QFacetRule facetRule,
                                    final Set<String> userIds,
                                    final Set<String> memberships,
                                    final Set<String> roles,
                                    final ServicingIndexingConfiguration indexingConfig,
                                    final NamespaceMappings nsMappings,
                                    final InternalHippoSession session,
                                    final NodeTypeManager ntMgr) {
        String value = facetRule.getValue();
        switch (facetRule.getType()) {
            case PropertyType.STRING:
                Name facetName = facetRule.getFacetName();
                try {
                    if (NameConstants.JCR_UUID.equals(facetName)) {
                        final Query tq = new TermQuery(new Term(FieldNames.UUID, value));
                        // note no check required for isFacetOptional since every node has a uuid
                        if (facetRule.isEqual()) {
                            return tq;
                        } else {
                            return QueryHelper.negateQuery(tq);
                        }
                    }
                    if (NameConstants.JCR_PATH.equals(facetName)) {
                        final Query tq = new TermQuery(new Term(ServicingFieldNames.HIPPO_UUIDS, value));
                        // note no check required for isFacetOptional since every node has a uuid
                        if (facetRule.isEqual()) {
                            return tq;
                        } else {
                            return QueryHelper.negateQuery(tq);
                        }
                    }
                    else if (indexingConfig.isFacet(facetName)) {
                        String fieldName = ServicingNameFormat.getInternalFacetName(facetName, nsMappings);
                        String internalNameTerm = nsMappings.translateName(facetName);
                        Query tq;
                        if (FacetAuthConstants.WILDCARD.equals(value)) {
                            tq = new TermQuery(new Term(ServicingFieldNames.FACET_PROPERTIES_SET, internalNameTerm));
                        } else if (FacetAuthConstants.EXPANDER_USER.equals(value)) {
                            tq = expandUser(fieldName, userIds);
                        } else if (FacetAuthConstants.EXPANDER_ROLE.equals(value)) {
                            tq = expandRole(fieldName, roles);
                        } else if (FacetAuthConstants.EXPANDER_GROUP.equals(value)) {
                            tq = expandGroup(fieldName, memberships);
                        } else {
                            tq = new TermQuery(new Term(fieldName, value));
                        }
                        if (facetRule.isFacetOptional()) {
                            BooleanQuery bq = new BooleanQuery(true);
                            // all the docs that do *not* have the property:
                            Query docsThatMissPropertyQuery = QueryHelper.negateQuery(
                                    new TermQuery(new Term(ServicingFieldNames.FACET_PROPERTIES_SET, internalNameTerm)));
                            bq.add(docsThatMissPropertyQuery, Occur.SHOULD);
                            // and OR that one with the equals
                            if (facetRule.isEqual()) {
                                bq.add(tq, Occur.SHOULD);
                                return bq;
                            } else {
                                Query not =  QueryHelper.negateQuery(tq);
                                bq.add(not, Occur.SHOULD);
                                return bq;
                            }
                        } else {
                            // Property MUST exist and it MUST (or MUST NOT) equal the value,
                            // depending on QFacetRule#isEqual.
                            if (facetRule.isEqual()) {
                                return tq;
                            } else {
                                return QueryHelper.negateQuery(tq);
                            }
                        }
                    } else {
                        log.warn("Property " + facetName.getNamespaceURI() + ":" + facetName.getLocalName() + " not allowed for faceted search. " +
                                         "Add the property to the indexing configuration to be defined as FACET");
                    }
                } catch (IllegalNameException e) {
                    log.error(e.toString());
                }
                break;
            case PropertyType.NAME:
                String nodeNameString = facetRule.getFacet();
                if (FacetAuthConstants.WILDCARD.equals(value)) {
                    if (facetRule.isEqual()) {
                        return new MatchAllDocsQuery();
                    } else {
                        return QueryHelper.getNoHitsQuery();
                    }
                } else if (NODETYPE.equalsIgnoreCase(nodeNameString)) {
                    return getNodeTypeDescendantQuery(facetRule, ntMgr, session, nsMappings);
                } else if (NODENAME.equalsIgnoreCase(nodeNameString)) {
                    return getNodeNameQuery(facetRule, userIds, roles, memberships, nsMappings);
                } else {
                    try {
                        if ("jcr:primaryType".equals(nodeNameString)) {
                            return getNodeTypeQuery(ServicingFieldNames.HIPPO_PRIMARYTYPE, facetRule, session, nsMappings);
                        } else if ("jcr:mixinTypes".equals(nodeNameString)) {
                            return getNodeTypeQuery(ServicingFieldNames.HIPPO_MIXINTYPE, facetRule, session, nsMappings);
                        } else {
                            log.error("Ignoring facetrule with facet '" + nodeNameString + "'. hippo:facet must be either 'nodetype', 'jcr:primaryType' " +
                                              "or 'jcr:mixinTypes' when hipposys:type = Name.");
                        }
                    } catch (IllegalNameException ine) {
                        log.warn("Illegal name in facet rule", ine);
                    } catch(NamespaceException ne) {
                        log.warn("Namespace exception in facet rule", ne);
                    }
                }
                break;
        }
        log.error("Incorrect FacetRule: returning a match zero nodes query");
        return QueryHelper.getNoHitsQuery();
    }

    private Query getNodeTypeDescendantQuery(final QFacetRule facetRule,
                                             final NodeTypeManager ntMgr,
                                             final InternalHippoSession session,
                                             final NamespaceMappings nsMappings) {
        List<Term> terms = new ArrayList<Term>();
        try {

            NodeType base = ntMgr.getNodeType(facetRule.getValue());
            terms.add(getTerm(base, session, nsMappings));

            // now search for all node types that are derived from base
            NodeTypeIterator allTypes = ntMgr.getAllNodeTypes();
            while (allTypes.hasNext()) {
                NodeType nt = allTypes.nextNodeType();
                NodeType[] superTypes = nt.getSupertypes();
                if (Arrays.asList(superTypes).contains(base)) {
                    terms.add(getTerm(nt, session, nsMappings));
                }
            }
        } catch (NoSuchNodeTypeException e) {
            log.error("invalid nodetype" + e.getMessage() + "\n" + MESSAGE_ZEROMATCH_QUERY);
        } catch (RepositoryException e) {
            log.error("invalid nodetype" + e.getMessage() + "\n" + MESSAGE_ZEROMATCH_QUERY);
        }

        if (terms.size() == 0) {
            // exception occured
            if (facetRule.isEqual()) {
                return QueryHelper.getNoHitsQuery();
            } else {
                return new MatchAllDocsQuery();
            }
        }

        Query query;
        if (terms.size() == 1) {
            query = new TermQuery(terms.get(0));
        } else {
            BooleanQuery b = new BooleanQuery(true);
            for (final Term term : terms) {
                b.add(new TermQuery(term), Occur.SHOULD);
            }
            query = b;
        }
        if (facetRule.isEqual()) {
            return query;
        } else {
            return QueryHelper.negateQuery(query);
        }
    }

    private Term getTerm(NodeType nt, InternalHippoSession session, NamespaceMappings nsMappings) throws IllegalNameException, NamespaceException {
        String ntName = getNtName(nt.getName(), session, nsMappings);
        if (nt.isMixin()) {
            return new Term(ServicingFieldNames.HIPPO_MIXINTYPE, ntName);
        } else {
            return new Term(ServicingFieldNames.HIPPO_PRIMARYTYPE, ntName);
        }
    }

    private Query getNodeTypeQuery(String luceneFieldName, QFacetRule facetRule, InternalHippoSession session, NamespaceMappings nsMappings) throws IllegalNameException, NamespaceException {
        String termValue = facetRule.getValue();
        String name = getNtName(termValue, session, nsMappings);
        Query nodetypeQuery = new TermQuery(new Term(luceneFieldName, name));
        if (facetRule.isEqual()) {
            return nodetypeQuery;
        } else {
            return QueryHelper.negateQuery(nodetypeQuery);
        }
    }

    private String getNtName(String value, InternalHippoSession session, NamespaceMappings nsMappings) throws IllegalNameException, NamespaceException {
        Name n = session.getQName(value);
        return nsMappings.translateName(n);
    }

    public BooleanQuery getQuery() {
        return query;
    }

    private Query getNodeNameQuery(QFacetRule facetRule, Set<String> userIds, Set<String> roles, Set<String> memberShips, final NamespaceMappings nsMappings) {
        try {
            String fieldName = ServicingNameFormat.getInternalFacetName(NameConstants.JCR_NAME, nsMappings);
            String value = facetRule.getValue();
            Query nodeNameQuery;
            if (FacetAuthConstants.WILDCARD.equals(value)) {
                if (facetRule.isEqual()) {
                    return new MatchAllDocsQuery();
                } else {
                    return QueryHelper.getNoHitsQuery();
                }
            } else if (FacetAuthConstants.EXPANDER_USER.equals(value)) {
                nodeNameQuery = expandUser(fieldName, userIds);
            } else if (FacetAuthConstants.EXPANDER_ROLE.equals(value)) {
                nodeNameQuery = expandRole(fieldName, userIds);
            } else if (FacetAuthConstants.EXPANDER_GROUP.equals(value)) {
                nodeNameQuery = expandGroup(fieldName, userIds);
            } else {
                nodeNameQuery = new TermQuery(new Term(fieldName, value));
            }
            if (facetRule.isEqual()) {
                return nodeNameQuery;
            } else {
                return QueryHelper.negateQuery(nodeNameQuery);
            }
        } catch (IllegalNameException e) {
            log.error("Failed to create node name query: " + e);
            return QueryHelper.getNoHitsQuery();
        }
    }

    private Query expandUser(final String field, final Set<String> userIds) {
        if (userIds.isEmpty()) {
            return QueryHelper.getNoHitsQuery();
        }
        // optimize the single-user principal case
        if (userIds.size() == 1) {
            String userId = userIds.iterator().next();
            return new TermQuery(new Term(field, userId));
        } else {
            BooleanQuery b = new BooleanQuery(true);
            for (String userId : userIds) {
                Term term = new Term(field, userId);
                b.add(new TermQuery(term), Occur.SHOULD);
            }
            return b;
        }
    }

    private Query expandGroup(final String field, final Set<String> memberships) {
        // boolean OR query of groups
        if (memberships.isEmpty()) {
            return QueryHelper.getNoHitsQuery();
        }
        BooleanQuery b = new BooleanQuery(true);
        for (String groupName : memberships) {
            Term term = new Term(field, groupName);
            b.add(new TermQuery(term), Occur.SHOULD);
        }
        return b;
    }

    private Query expandRole(final String field, final Set<String> roles) {
        // boolean Or query of roles
        if (roles.size() == 0) {
            return QueryHelper.getNoHitsQuery();
        }
        BooleanQuery b = new BooleanQuery(true);
        for (String role : roles) {
            Term term = new Term(field, role);
            b.add(new TermQuery(term), Occur.SHOULD);
        }
        return b;
    }

    @Override
    public String toString() {
        return "authorisation query: " + query.toString();
    }
}
