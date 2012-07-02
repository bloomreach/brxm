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
package org.hippoecm.repository.query.lucene;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.security.auth.Subject;

import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.jackrabbit.core.security.SystemPrincipal;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.hippoecm.repository.security.FacetAuthConstants;
import org.hippoecm.repository.security.domain.DomainRule;
import org.hippoecm.repository.security.domain.FacetRule;
import org.hippoecm.repository.security.principals.FacetAuthPrincipal;
import org.hippoecm.repository.security.principals.GroupPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthorizationQuery {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    /**
     * The logger instance for this class
     */
    private static final Logger log = LoggerFactory.getLogger(AuthorizationQuery.class);

    private static final String MESSAGE_ZEROMATCH_QUERY = "returning a match zero nodes query";

    /**
     * The lucene query
     */
    private final BooleanQuery query;

    private final NodeTypeManager ntMgr;
    private final NamespaceMappings nsMappings;
    private final ServicingIndexingConfiguration indexingConfig;
    private final Set<String> memberships = new HashSet<String>();
    private final SessionImpl session;

    public AuthorizationQuery(Subject subject, NamespaceMappings nsMappings, ServicingIndexingConfiguration indexingConfig,
                              NodeTypeManager ntMgr, Session session) throws RepositoryException {
        // set the max clauses for booleans higher than the default 1024.
        BooleanQuery.setMaxClauseCount(Integer.MAX_VALUE);
        this.nsMappings = nsMappings;
        this.indexingConfig = indexingConfig;
        this.ntMgr = ntMgr;
        if (!(session instanceof SessionImpl)) {
            throw new RepositoryException("Session is not an instance of o.a.j.core.SessionImpl");
        }
        this.session = (SessionImpl) session;

        if (!subject.getPrincipals(SystemPrincipal.class).isEmpty()) {
            this.query = new BooleanQuery(true);
            this.query.add(new MatchAllDocsQuery(), Occur.MUST);
        } else {
            for (GroupPrincipal groupPrincipal : subject.getPrincipals(GroupPrincipal.class)) {
                memberships.add(groupPrincipal.getName());
            }
            log.debug("----START CREATION AUTHORIZATION QUERY---------");
            this.query = initQuery(subject.getPrincipals(FacetAuthPrincipal.class));
            log.info("AUTHORIZATION Query: " + query);
            log.debug("----END CREATION AUTHORIZATION QUERY-----------");
        }
    }

    private BooleanQuery initQuery(Set<FacetAuthPrincipal> facetAuths) {
        BooleanQuery authQuery = new BooleanQuery(true);
        Iterator<FacetAuthPrincipal> facetAuthsIt = facetAuths.iterator();
        while (facetAuthsIt.hasNext()) {
            FacetAuthPrincipal facetAuthPrincipal = facetAuthsIt.next();
            if (!facetAuthPrincipal.getPrivileges().contains("jcr:read")) {
                continue;
            }
            Iterator<DomainRule> domainRulesIt = facetAuthPrincipal.getRules().iterator();
            while (domainRulesIt.hasNext()) {
                DomainRule domainRule = domainRulesIt.next();
                Iterator<FacetRule> facetRuleIt = domainRule.getFacetRules().iterator();
                BooleanQuery facetQuery = new BooleanQuery(true);
                while (facetRuleIt.hasNext()) {
                    FacetRule facetRule = facetRuleIt.next();
                    Query q = getFacetRuleQuery(facetRule, facetAuthPrincipal.getRoles());
                    if (q == null) {
                        continue;
                    }
                    log.debug("Adding to FacetQuery: FacetRuleQuery = {}", q);
                    log.debug("FacetRuleQuery has {} clauses.", (q instanceof BooleanQuery) ? ((BooleanQuery) q).getClauses().length : 1);
                    facetQuery.add(q, Occur.MUST);
                }
                log.debug("Adding to Authorization query: FacetQuery = {}", facetQuery);
                log.debug("FacetQuery has {} clauses.", facetQuery.getClauses().length);
                authQuery.add(facetQuery, Occur.SHOULD);
            }
        }

        log.debug("Authorization query is : " + authQuery);
        log.debug("Authorization query has {} clauses", authQuery.getClauses().length);

        return authQuery;
    }

    private Query getFacetRuleQuery(FacetRule facetRule, Set<String> roles) {
        switch (facetRule.getType()) {
            case PropertyType.STRING:
                Name nodeName = facetRule.getFacetName();
                try {
                    if (indexingConfig.isFacet(nodeName)) {
                        String internalFieldName = ServicingNameFormat.getInternalFacetName(nodeName, nsMappings);
                        String internalNameTerm = nsMappings.translateName(nodeName);
                        if (facetRule.getValue().equals(FacetAuthConstants.WILDCARD)) {
                            if (facetRule.isEqual()) {
                                return new TermQuery(new Term(ServicingFieldNames.FACET_PROPERTIES_SET, internalNameTerm));
                            } else {
                                // When the rule is : facet != * , the authorization is allowed on all nodes not having the property
                                return QueryHelper.negateQuery(new TermQuery(new Term(ServicingFieldNames.FACET_PROPERTIES_SET, internalNameTerm)));
                            }
                        } else if (facetRule.getValue().equals(FacetAuthConstants.EXPANDER_USER)) {
                            if (facetRule.isEqual()) {
                                return new TermQuery(new Term(internalFieldName, session.getUserID()));
                            } else {
                                return QueryHelper.negateQuery(new TermQuery(new Term(internalFieldName, session.getUserID())));
                            }
                        } else if (facetRule.getValue().equals(FacetAuthConstants.EXPANDER_ROLE)) {
                            // boolean Or query of roles
                            if (roles.size() == 0) {
                                return QueryHelper.getNoHitsQuery();
                            }
                            BooleanQuery b = new BooleanQuery(true);
                            for (String role : roles) {
                                Term term = new Term(internalFieldName, role);
                                b.add(new TermQuery(term), Occur.SHOULD);
                            }
                            if (facetRule.isEqual()) {
                                return b;
                            } else {
                                return QueryHelper.negateQuery(b);
                            }
                        } else if (facetRule.getValue().equals(FacetAuthConstants.EXPANDER_GROUP)) {
                            // boolean OR query of groups
                            if (memberships.isEmpty()) {
                                return QueryHelper.getNoHitsQuery();
                            }
                            BooleanQuery b = new BooleanQuery(true);
                            for (String groupName : memberships) {
                                Term term = new Term(internalFieldName, groupName);
                                b.add(new TermQuery(term), Occur.SHOULD);
                            }
                            if (facetRule.isEqual()) {
                                return b;
                            } else {
                                return QueryHelper.negateQuery(b);
                            }
                        } else {
                            Query tq = new TermQuery(new Term(internalFieldName, facetRule.getValue()));
                            if (!facetRule.isFilter()) {
                                Query propExists = new TermQuery(new Term(ServicingFieldNames.FACET_PROPERTIES_SET,
                                                                          internalNameTerm));
                                BooleanQuery bq = new BooleanQuery(true);
                                bq.add(propExists, Occur.MUST);
                                if (facetRule.isEqual()) {
                                    bq.add(tq, Occur.MUST);
                                } else {
                                    bq.add(tq, Occur.MUST_NOT);
                                }
                                return bq;
                            } else {
                                if (facetRule.isEqual()) {
                                    return tq;
                                } else {
                                    return QueryHelper.negateQuery(tq);
                                }
                            }
                        }
                    } else {
                        log.warn("Property " + nodeName.getNamespaceURI() + ":" + nodeName.getLocalName() + " not allowed for facetted search. " + "Add the property to the indexing configuration to be defined as FACET");
                    }
                } catch (IllegalNameException e) {
                    log.error(e.toString());
                }
                break;
            case PropertyType.NAME:
                String nodeNameString = facetRule.getFacet();
                if (facetRule.getValue().equals("*")) {
                    if (facetRule.isEqual()) {
                        return new MatchAllDocsQuery();
                    } else {
                        return QueryHelper.getNoHitsQuery();
                    }
                } else if ("nodetype".equalsIgnoreCase(nodeNameString)) {
                    Query q = getNodeTypeDescendantQuery(facetRule);
                    if (facetRule.isEqual()) {
                        return q;
                    } else {
                        return QueryHelper.negateQuery(q);
                    }
                } else if ("nodename".equalsIgnoreCase(nodeNameString)) {
                    return getNodeNameQuery(facetRule);
                } else if ("jcr:primaryType".equals(nodeNameString)) {
                    return getNodeTypeQuery(ServicingFieldNames.HIPPO_PRIMARYTYPE, facetRule);
                } else if ("jcr:mixinTypes".equals(nodeNameString)) {
                    return getNodeTypeQuery(ServicingFieldNames.HIPPO_MIXINTYPE, facetRule);
                } else {
                    log.error("Ignoring facetrule with facet '" + nodeNameString + "'. hippo:facet must be either 'nodetype', 'jcr:primaryType' " + "or 'jcr:mixinTypes' when hipposys:type = Name.");
                }
                break;
        }
        log.error("Incorrect FacetRule: returning a match zero nodes query");
        return QueryHelper.getNoHitsQuery();
    }

    private Query getNodeTypeDescendantQuery(FacetRule facetRule) {
        List<Term> terms = new ArrayList<Term>();
        try {
            NodeType base = ntMgr.getNodeType(facetRule.getValue());
            if (base.isMixin()) {
                // search for nodes where jcr:mixinTypes is set to this mixin
                Term t = new Term(ServicingFieldNames.HIPPO_MIXINTYPE, facetRule.getValue());
                terms.add(t);
            } else {
                // search for nodes where jcr:primaryType is set to this type
                Term t = new Term(ServicingFieldNames.HIPPO_PRIMARYTYPE, facetRule.getValue());
                terms.add(t);
            }

            // now search for all node types that are derived from base
            NodeTypeIterator allTypes = ntMgr.getAllNodeTypes();
            while (allTypes.hasNext()) {
                NodeType nt = allTypes.nextNodeType();
                NodeType[] superTypes = nt.getSupertypes();
                if (Arrays.asList(superTypes).contains(base)) {
                    Name n = session.getQName(nt.getName());
                    String ntName = nsMappings.translateName(n);
                    Term t;
                    if (nt.isMixin()) {
                        // search on jcr:mixinTypes
                        t = new Term(ServicingFieldNames.HIPPO_MIXINTYPE, ntName);
                    } else {
                        // search on jcr:primaryType
                        t = new Term(ServicingFieldNames.HIPPO_PRIMARYTYPE, ntName);
                    }
                    terms.add(t);
                }
            }
        } catch (NoSuchNodeTypeException e) {
            log.error("invalid nodetype" + e.getMessage() + "\n" + MESSAGE_ZEROMATCH_QUERY);
        } catch (RepositoryException e) {
            log.error("invalid nodetype" + e.getMessage() + "\n" + MESSAGE_ZEROMATCH_QUERY);
        }
        if (terms.size() == 0) {
            // exception occured
            return QueryHelper.getNoHitsQuery();
        } else if (terms.size() == 1) {
            return new TermQuery(terms.get(0));
        } else {
            BooleanQuery b = new BooleanQuery();
            for (Iterator<Term> it = terms.iterator(); it.hasNext();) {
                b.add(new TermQuery(it.next()), Occur.SHOULD);
            }
            return b;
        }
    }

    private Query getNodeTypeQuery(String luceneFieldName, FacetRule facetRule) {
        String termValue = facetRule.getValue();
        Query nodetypeQuery = new TermQuery(new Term(luceneFieldName, termValue));
        if (facetRule.isEqual()) {
            return nodetypeQuery;
        } else {
            return QueryHelper.negateQuery(nodetypeQuery);
        }
    }

    private Query getNodeNameQuery(FacetRule facetRule) {
        Query nodeNameQuery = null;
        nodeNameQuery = new TermQuery(new Term(ServicingFieldNames.HIPPO_SORTABLE_NODENAME, facetRule.getValue()));
        if (facetRule.isEqual()) {
            return nodeNameQuery;
        } else {
            return QueryHelper.negateQuery(nodeNameQuery);
        }
    }

    public BooleanQuery getQuery() {
        return query;
    }

    @Override
    public String toString() {
        return "authorisation query: " + query.toString();
    }
}
