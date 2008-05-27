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

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;

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

    public AuthorizationQuery(Set<FacetAuthPrincipal> facetAuths, Map<String, String> facetsQueryMap,
            NamespaceMappings nsMappings, ServicingIndexingConfiguration indexingConfig) {
        this.query = new BooleanQuery(true);

        Iterator<FacetAuthPrincipal> facetAuthsIt = facetAuths.iterator();
        while (facetAuthsIt.hasNext()) {
            // TODO test for facetAuthPrincipal wether 'read' is bit is set to 1 in ROLE 
            FacetAuthPrincipal facetAuthPrincipal = facetAuthsIt.next();
            Iterator<DomainRule> domainRulesIt = facetAuthPrincipal.getRules().iterator();

            while (domainRulesIt.hasNext()) {
                DomainRule domainRule = domainRulesIt.next();
                Iterator<FacetRule> facetRuleIt = domainRule.getFacetRules().iterator();

                BooleanQuery facetQuery = new BooleanQuery(true);
                while (facetRuleIt.hasNext()) {
                    FacetRule facetRule = facetRuleIt.next();

                    Query q = getFacetRuleQuery(facetRule, indexingConfig, nsMappings);
                    if (q == null) {
                        continue;
                    }
                    facetQuery.add(q, Occur.MUST);
                }
                query.add(facetQuery, Occur.SHOULD);
            }
        }
    }

    private Query getFacetRuleQuery(FacetRule facetRule, ServicingIndexingConfiguration indexingConfig,
            NamespaceMappings nsMappings) {
        switch (facetRule.getType()) {
        case PropertyType.STRING: 
            Name nodeName = facetRule.getFacetName();
            try {
                if (indexingConfig.isFacet(nodeName)) {
                    String internalFieldName = ServicingNameFormat.getInternalFacetName(nodeName, nsMappings);
                    String internalNameTerm = nsMappings.translatePropertyName(nodeName);
                    if (facetRule.getValue().equals("*")) {
                        if(facetRule.isEqual()) {
                            return new TermQuery(new Term(ServicingFieldNames.FACET_PROPERTIES_SET,internalNameTerm));
                        } else {
                            // * in combination with unequal should never return a hit (though should not be possible 
                            // to exist anyway )
                            return QueryHelper.getNoHitsQuery();
                        }
                    } else {
                        Query wq = new WildcardQuery(new Term(internalFieldName, facetRule.getValue() + "?"));
                        if(facetRule.isEqual()) {
                            return wq;
                        } else {
                            BooleanQuery b = new BooleanQuery(false);
                            Query propExists = new TermQuery(new Term(ServicingFieldNames.FACET_PROPERTIES_SET,internalNameTerm));
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
                if(facetRule.isEqual()) {
                    return new MatchAllDocsQuery();
                } else {
                    return QueryHelper.getNoHitsQuery();
                }
            } else if ("nodetype".equals(nodeNameString)) {
                if(facetRule.isEqual()) {
                    return new MatchAllDocsQuery();
                } else {
                    log.error("invalid combination of hippo:facet='nodetype' and hippo:equals='false'");
                    return QueryHelper.getNoHitsQuery();
                }
            } else if (nodeNameString.equals("jcr:primaryType")) {
                return getNodeTypeQuery(ServicingFieldNames.HIPPO_PRIMARYTYPE,facetRule,nsMappings);
                
            } else if (nodeNameString.equals("jcr:mixinTypes")) {
                return getNodeTypeQuery(ServicingFieldNames.HIPPO_MIXINTYPE,facetRule,nsMappings);
            } else {
                log.error("hippo:facet must be either 'nodetype', 'jcr:primaryType' " +
                		"or 'jcr:mixinTypes' when hippo:type = Name \n Ignoring facetrule");
            }
            break;
        }
        log.error("Incorrect FacetRule: returning a match zero nodes query");
        return QueryHelper.getNoHitsQuery();
    }

    private Query getNodeTypeQuery(String luceneFieldName, FacetRule facetRule, NamespaceMappings nsMappings) {
        try {
        String termValue = getTermValue(facetRule.getValue(), nsMappings);
        Query nodetypeQuery = new TermQuery(new Term(luceneFieldName, termValue));
        if(facetRule.isEqual()) {
            return nodetypeQuery;
        } else {
            return QueryHelper.negateQuery(nodetypeQuery);
        }
        } catch (NamespaceException e) {
            log.error(e.getMessage() +" \n returning zero node matching query");
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage() +" \n returning zero node matching query");
        }
        return QueryHelper.getNoHitsQuery();
    }

    private String getTermValue(String nameString, NamespaceMappings nsMappings) throws NamespaceException {
        Name facetNodeType = NameFactoryImpl.getInstance().create(nameString);
        return nsMappings.getPrefix(facetNodeType.getNamespaceURI()) + ":" + facetNodeType.getLocalName();
    }

    public BooleanQuery getQuery() {
        return query;
    }
    
    @Override
    public String toString(){
        return "authorisation query: " + query.toString();
    }
    
}
