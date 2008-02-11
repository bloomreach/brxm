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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanClause.Occur;
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

    /**
     *
     * @param authorizationQuery The facets + value[] combination the logged in user is allowed to see
     * @param nsMappings nameSpace mappings to find the lucene field names
     * @param indexingConfig the index configuration
     */
    public AuthorizationQuery(Map<Name, String[]> authorizationQuery, NamespaceMappings nsMappings,
            ServicingIndexingConfiguration indexingConfig) {
        this(authorizationQuery, null, nsMappings, indexingConfig, true);
    }

    /**
     * This is the authorization query constructor. For efficient queries, the requested facetsQueryMap is added, to
     * be able to remove redundant searches.
     *
     * @param authorizationQuery The facets + value[] combination the logged in user is allowed to see
     * @param facetsQueryMap The currently requested facetQueryMap. This map is used to optimize the lucene query
     * @param nsMappings nameSpace mappings to find the lucene field names
     * @param indexingConfig the index configuration
     * @param facetsORed Wether different facet fields are OR-ed or AND-ed. Most efficient is OR-ed
     */
    public AuthorizationQuery(Map<Name, String[]> authorizationQuery, Map<String, String> facetsQueryMap,
            NamespaceMappings nsMappings, ServicingIndexingConfiguration indexingConfig, boolean facetsORed) {
        this.query = new BooleanQuery(true);

        if (authorizationQuery != null && authorizationQuery.size() != 0) {

            for (Map.Entry<Name, String[]> entry : authorizationQuery.entrySet()) {
                String internalName = "";
                try {
                    if (indexingConfig.isFacet(entry.getKey())) {
                        internalName = ServicingNameFormat.getInternalFacetName(entry.getKey(), nsMappings);
                        String[] facetValues = entry.getValue();
                        BooleanQuery orQuery = new BooleanQuery(true);
                        Set tmpContainsSet = new HashSet();
                        for (int i = 0; i < facetValues.length; i++) {
//                            if (facetsQueryMap.containsKey(entry.getKey())
//                                    && facetsQueryMap.get(entry.getKey()).equals(facetValues[i])) {
//                                // the facetsQueryMap already accounts for the part in the authorization for this facet. Disregard this part
//                                // for performance
//                                orQuery = null;
//                            }
                            // add to tmp set to check wether already added. Multiplicity slows queries down
                            if (orQuery != null && tmpContainsSet.add(facetValues[i])) {
                                Query q = new FixedScoreTermQuery(new Term(internalName, facetValues[i]));
                                orQuery.add(q, Occur.SHOULD);
                            }
                        }
                        if (orQuery != null && facetsORed) {
                            this.query.add(orQuery, Occur.SHOULD);
                        } else if (orQuery != null) {
                            this.query.add(orQuery, Occur.MUST);
                        }

                    } else {
                        log.warn("Property " + entry.getKey().getNamespaceURI() + ":" + entry.getKey().getLocalName()
                                + " not allowed for facetted search. "
                                + "Add the property to the indexing configuration to be defined as FACET");
                    }

                } catch (IllegalNameException e) {
                    log.error(e.toString());
                }
            }
        } else {
            // TODO: Fix this hack. It uses "null" for the authorizationQuery to allow everything for admin users
            if (authorizationQuery != null) {
                this.query.add(new MatchAllDocsQuery(), BooleanClause.Occur.MUST_NOT);
            }
        }
    }

    public BooleanQuery getQuery() {
        return query;
    }

}
