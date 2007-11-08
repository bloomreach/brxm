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

import org.apache.jackrabbit.conversion.IllegalNameException;
import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.jackrabbit.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.Name;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanClause.Occur;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacetPropExistsQuery {
    
   
    /**
     * The logger instance for this class
     */
    private static final Logger log = LoggerFactory.getLogger(FacetsQuery.class);
    
    /**
     * The lucene query
     */
    private BooleanQuery query;
    
    public FacetPropExistsQuery(String facet, NamespaceMappings nsMappings, ServicingIndexingConfiguration indexingConfig) {
        this.query = new BooleanQuery(true);
        
        Name nodeName;
        String internalName = "";
        try {
        	// TODO Assume empty namespace for facet. Is this always true?
            nodeName = NameFactoryImpl.getInstance().create("", facet);
            if(indexingConfig.isFacet(nodeName)){
            	internalName = nsMappings.translatePropertyName(nodeName);
                Query q = new FixedScoreTermQuery(new Term(ServicingFieldNames.FACET_PROPERTIES_SET,internalName));
                this.query.add(q, Occur.MUST);
            } else {
                log.warn("Property " + nodeName.getNamespaceURI()+":"+nodeName.getLocalName()+" not allowed for facetted search. " +
                        "Add the property to the indexing configuration to be defined as FACET");
            }
            
        } catch (IllegalNameException e) {
        	log.error(e.toString());
		}
    }

    public BooleanQuery getQuery() {
        return query;
    }

}
