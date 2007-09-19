package org.hippoecm.repository.query.lucene;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.NamespaceException;

import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.jackrabbit.name.NameException;
import org.apache.jackrabbit.name.NoPrefixDeclaredException;
import org.apache.jackrabbit.name.ParsingNameResolver;
import org.apache.jackrabbit.name.QName;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
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
    public AuthorizationQuery(Map<String, String[]> authorizationQuery, NamespaceMappings nsMappings, ServicingIndexingConfiguration indexingConfig) {
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
    public AuthorizationQuery(Map<String, String[]> authorizationQuery, 
                              Map<String, String> facetsQueryMap, 
                              NamespaceMappings nsMappings, 
                              ServicingIndexingConfiguration indexingConfig,
                              boolean facetsORed) {
        this.query = new BooleanQuery();
        
        if(authorizationQuery!=null){
            ParsingNameResolver pnr = new ParsingNameResolver(nsMappings);   
            for(Map.Entry<String,String[]> entry : authorizationQuery.entrySet()) {
                QName nodeName;
                String internalName = "";
                try {
                    nodeName = pnr.getQName(entry.getKey());
                    if(indexingConfig.isFacet(nodeName)){
                        internalName = ServicingNameFormat.getInternalFacetName(nodeName,nsMappings);
                        String[] facetValues = entry.getValue();
                        BooleanQuery orQuery = new BooleanQuery();
                        Set tmpContainsSet = new HashSet();
                        for(int i = 0; i < facetValues.length ; i++ ) {
                            if(facetsQueryMap.containsKey(entry.getKey()) && facetsQueryMap.get(entry.getKey()).equals(facetValues[i])){
                                // the facetsQueryMap already accounts for the part in the authorization for this facet. Disregard this part
                                // for performance
                                orQuery = null;
                            }
                            // add to tmp set to check wether already added. Multiplicity slows queries down
                            if(orQuery!=null && tmpContainsSet.add(facetValues[i])) {
                                Query q = new TermQuery(new Term(internalName, facetValues[i] ));
                                orQuery.add(q, Occur.SHOULD);
                            }
                        }
                        if(orQuery!=null && facetsORed) {
                            this.query.add(orQuery, Occur.SHOULD);
                        } else if (orQuery!=null) {
                            this.query.add(orQuery, Occur.MUST);  
                        }
                        
                    } else {
                        log.warn("Property " + nodeName.getNamespaceURI()+":"+nodeName.getLocalName()+" not allowed for facetted search. " +
                                "Add the property to the indexing configuration to be defined as FACET");
                    }
                    
                } catch (NoPrefixDeclaredException e) {
                    e.printStackTrace();
                } catch (NameException e) {
                    e.printStackTrace();
                } catch (NamespaceException e) {
                    e.printStackTrace();
                }
                
              }
        }
    }

    public BooleanQuery getQuery() {
        return query;
    }

}
