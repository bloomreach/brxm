package org.hippoecm.repository.query.lucene;

import java.util.Map;

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

public class FacetsQuery {
    
    /**
     * The logger instance for this class
     */
    private static final Logger log = LoggerFactory.getLogger(FacetsQuery.class);

    /**
     * The lucene query
     */
    private BooleanQuery query;
    
    public FacetsQuery(Map<String, String> facetsQuery, NamespaceMappings nsMappings, ServicingIndexingConfiguration indexingConfig) {
        this.query = new BooleanQuery();
        
        if(facetsQuery != null){
            ParsingNameResolver pnr = new ParsingNameResolver(nsMappings);
            for(Map.Entry<String,String> entry : facetsQuery.entrySet()) {
                
                QName nodeName;
                String internalName = "";
                try {
                    nodeName = pnr.getQName(entry.getKey());
                    if(indexingConfig.isFacet(nodeName)){
                        internalName = ServicingNameFormat.getInternalFacetName(nodeName,nsMappings);
                        Query q = new TermQuery(new Term(internalName,entry.getValue()));
                        this.query.add(q, Occur.MUST);
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
