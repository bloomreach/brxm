package org.hippoecm.repository.query.lucene;

import java.util.Map;

import javax.jcr.NamespaceException;

import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.jackrabbit.name.NameException;
import org.apache.jackrabbit.name.NameFormat;
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
        this.query = new BooleanQuery();
        
        ParsingNameResolver pnr = new ParsingNameResolver(nsMappings);
        QName nodeName;
        String internalName = "";
        try {
            nodeName = pnr.getQName(facet);
            if(indexingConfig.isFacet(nodeName)){
                internalName = NameFormat.format(nodeName,nsMappings);
                Query q = new TermQuery(new Term(ServicingFieldNames.FACET_PROPERTIES_SET,internalName));
                this.query.add(q, Occur.MUST);
            } else {
                log.warn("Property " + nodeName.getNamespaceURI()+":"+nodeName.getLocalName()+" not allowed for facetted search. " +
                        "Add the property to the indexing configuration to be defined as FACET");
            }
            
        } catch (NoPrefixDeclaredException e) {
            log.error(e.toString());
        } catch (NameException e) {
            log.error(e.toString());
        } catch (NamespaceException e) {
            log.error(e.toString());
        }
    }

    public BooleanQuery getQuery() {
        return query;
    }

}
