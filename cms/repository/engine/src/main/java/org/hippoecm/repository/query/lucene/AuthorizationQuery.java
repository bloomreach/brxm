/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.query.lucene;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.hippoecm.repository.security.domain.DomainRule;
import org.hippoecm.repository.security.domain.FacetRule;
import org.hippoecm.repository.security.principals.FacetAuthPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthorizationQuery {

    /**
     * The logger instance for this class
     */
    private static final Logger log = LoggerFactory.getLogger(FacetsQuery.class);

    /**
     * The lucene query
     */
    private BooleanQuery query;
    private NodeTypeManager ntMgr;
    private NamespaceMappings nsMappings;
    private ServicingIndexingConfiguration indexingConfig;
    private Set<FacetAuthPrincipal> facetAuths;
    private SessionImpl session;
    private final static String MESSAGE_ZEROMATCH_QUERY = "returning a match zero nodes query";

    public AuthorizationQuery(Set<FacetAuthPrincipal> facetAuths, NamespaceMappings nsMappings,
            ServicingIndexingConfiguration indexingConfig, NodeTypeManager ntMgr, Session session)
            throws RepositoryException {

        this.facetAuths = facetAuths;
        this.nsMappings = nsMappings;
        this.indexingConfig = indexingConfig;
        this.ntMgr = ntMgr;
        if (!(session instanceof SessionImpl)) {
            throw new RepositoryException("Session is not an instance of o.a.j.core.SessionImpl");
        }
        this.session = (SessionImpl) session;
        this.query = initQuery();

    }

    private BooleanQuery initQuery() {

        BooleanQuery authQuery = new BooleanQuery(true);
        Iterator<FacetAuthPrincipal> facetAuthsIt = facetAuths.iterator();
        while (facetAuthsIt.hasNext()) {
            // TODO test for facetAuthPrincipal wether 'read' is bit is set to 1 in ROLE 
            FacetAuthPrincipal facetAuthPrincipal = facetAuthsIt.next();
            Iterator<DomainRule> domainRulesIt = facetAuthPrincipal.getRules().iterator();

            while (domainRulesIt.hasNext()) {
                DomainRule domainRule = domainRulesIt.next();
                Iterator<FacetRule> facetRuleIt = domainRule.getFacetRules().iterator();
                BooleanQuery facetQuery = new BooleanQuery(true);
                boolean hasMatchAllDocsQuery = false;
                while (facetRuleIt.hasNext()) {
                    FacetRule facetRule = facetRuleIt.next();
                    Query q = getFacetRuleQuery(facetRule);
                    if (q == null) {
                        continue;
                    }
                    if (q instanceof MatchAllDocsQuery) {
                        hasMatchAllDocsQuery = true;
                    }
                    facetQuery.add(q, Occur.MUST);
                }
                if (domainRule.getFacetRules().size() == 1 && hasMatchAllDocsQuery) {
                    /*
                     * we have a domain rule, with only one facetrule, a MatchAllDocsQuery: this means
                     * visibility for every node. Return instantly with an empty boolean query
                     */
                    log.debug("found domain rule with only one facetrule which is a "
                            + "MatchAllDocsQuery: return empty booleanQuery because user is allowed to see all");
                    return new BooleanQuery(true);
                }
                authQuery.add(facetQuery, Occur.SHOULD);
            }
        }
        return authQuery;
    }

    private Query getFacetRuleQuery(FacetRule facetRule) {
        switch (facetRule.getType()) {
        case PropertyType.STRING:
            Name nodeName = facetRule.getFacetName();
            try {
                if (indexingConfig.isFacet(nodeName)) {
                    String internalFieldName = ServicingNameFormat.getInternalFacetName(nodeName, nsMappings);
                    String internalNameTerm = nsMappings.translatePropertyName(nodeName);
                    if (facetRule.getValue().equals("*")) {
                        if (facetRule.isEqual()) {
                            return new TermQuery(new Term(ServicingFieldNames.FACET_PROPERTIES_SET, internalNameTerm));
                        } else {
                            // * in combination with unequal should never return a hit (though should not be possible 
                            // to exist anyway )
                            return QueryHelper.getNoHitsQuery();
                        }
                    } else {
                        Query wq = new WildcardQuery(new Term(internalFieldName, facetRule.getValue() + "?"));
                        if (facetRule.isEqual()) {
                            return wq;
                        } else {
                            BooleanQuery b = new BooleanQuery(false);
                            Query propExists = new TermQuery(new Term(ServicingFieldNames.FACET_PROPERTIES_SET,
                                    internalNameTerm));
                            b.add(propExists, Occur.MUST);
                            b.add(wq, Occur.MUST_NOT);
                            return b;
                        }
                    }
                } else {
                    log.warn("Property " + nodeName.getNamespaceURI() + ":" + nodeName.getLocalName()
                            + " not allowed for facetted search. "
                            + "Add the property to the indexing configuration to be defined as FACET");
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
            } else if (nodeNameString.equals("jcr:primaryType")) {
                return getNodeTypeQuery(ServicingFieldNames.HIPPO_PRIMARYTYPE, facetRule);

            } else if (nodeNameString.equals("jcr:mixinTypes")) {
                return getNodeTypeQuery(ServicingFieldNames.HIPPO_MIXINTYPE, facetRule);
            } else {
                log.error("hippo:facet must be either 'nodetype', 'jcr:primaryType' "
                        + "or 'jcr:mixinTypes' when hippo:type = Name \n Ignoring facetrule");
            }
            break;
        }
        log.error("Incorrect FacetRule: returning a match zero nodes query");
        return QueryHelper.getNoHitsQuery();
    }

    private Query getNodeTypeDescendantQuery(FacetRule facetRule) {
        List<Term> terms = new ArrayList<Term>();
        try {
            Name baseName = NameFactoryImpl.getInstance().create(facetRule.getValue());
            NodeType base = ntMgr.getNodeType(session.getJCRName(baseName));
            if (base.isMixin()) {
                // search for nodes where jcr:mixinTypes is set to this mixin
                Term t = new Term(ServicingFieldNames.HIPPO_MIXINTYPE, getTermValue(facetRule.getValue()));
                terms.add(t);
            } else {
                // search for nodes where jcr:primaryType is set to this type
                Term t = new Term(ServicingFieldNames.HIPPO_PRIMARYTYPE, getTermValue(facetRule.getValue()));
                terms.add(t);
            }

            // now search for all node types that are derived from base
            NodeTypeIterator allTypes = ntMgr.getAllNodeTypes();
            while (allTypes.hasNext()) {
                NodeType nt = allTypes.nextNodeType();
                NodeType[] superTypes = nt.getSupertypes();
                if (Arrays.asList(superTypes).contains(base)) {
                    Name n = session.getQName(nt.getName());
                    String ntName = nsMappings.translatePropertyName(n);
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
            return new TermQuery((Term) terms.get(0));
        } else {
            BooleanQuery b = new BooleanQuery();
            for (Iterator<Term> it = terms.iterator(); it.hasNext();) {
                b.add(new TermQuery(it.next()), Occur.SHOULD);
            }
            return b;
        }

    }

    private Query getNodeTypeQuery(String luceneFieldName, FacetRule facetRule) {
        try {
            String termValue = getTermValue(facetRule.getValue());
            Query nodetypeQuery = new TermQuery(new Term(luceneFieldName, termValue));
            if (facetRule.isEqual()) {
                return nodetypeQuery;
            } else {
                return QueryHelper.negateQuery(nodetypeQuery);
            }
        } catch (NamespaceException e) {
            log.error(e.getMessage() + "\n" + MESSAGE_ZEROMATCH_QUERY);
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage() + "\n" + MESSAGE_ZEROMATCH_QUERY);
        }
        return QueryHelper.getNoHitsQuery();
    }

    private String getTermValue(String nameString) throws NamespaceException {
        Name facetNodeType = NameFactoryImpl.getInstance().create(nameString);
        return nsMappings.getPrefix(facetNodeType.getNamespaceURI()) + ":" + facetNodeType.getLocalName();
    }

    public BooleanQuery getQuery() {
        return query;
    }

    @Override
    public String toString() {
        return "authorisation query: " + query.toString();
    }

}
