package org.hippoecm.repository.jackrabbit.facetnavigation;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacetNavigationProvider extends AbstractFacetNavigationProvider {

    private final Logger log = LoggerFactory.getLogger(FacetNavigationProvider.class);
    
	protected FacetsAvailableNavigationProvider facetsAvailableNavigationProvider = null;
	
    Name docbaseName;
    Name facetsName;
    Name facetNodeNamesName;
	@Override
	protected void initialize() throws RepositoryException {
		super.initialize();
		facetsAvailableNavigationProvider = (FacetsAvailableNavigationProvider) lookup(FacetsAvailableNavigationProvider.class.getName());
		docbaseName = resolveName(HippoNodeType.HIPPO_DOCBASE);
        facetsName = resolveName(HippoNodeType.HIPPO_FACETS);
        facetNodeNamesName = resolveName(HippoNodeType.HIPPO_FACETNODENAMES);
		virtualNodeName = resolveName(HippoNodeType.NT_FACETSAVAILABLENAVIGATION);
		register(resolveName(HippoNodeType.NT_FACETNAVIGATION), virtualNodeName);
	}
	
	@Override
	public NodeState populate(NodeState state) throws RepositoryException {
		NodeId nodeId = state.getNodeId();
		
        String[] property = getProperty(nodeId, docbaseName);
        String docbase = (property != null && property.length > 0 ? property[0] : null);
        String[] facets = getProperty(nodeId, facetsName);
        String[] facetNodeNames = getProperty(nodeId, facetNodeNamesName);
        if (facets != null && facets.length > 0) {
            boolean mapFacets = false;
            if(facetNodeNames != null) {
                mapFacets = true;
                if(facets.length != facetNodeNames.length) {
                    log.error("When using multivalued property '{}', it must have equal number of values" +
                    		"as for property '{}'", HippoNodeType.HIPPO_FACETNODENAMES, HippoNodeType.HIPPO_FACETS);
                    return state;
                }
            }
            int i = 0;
        	for(String facet : facets){
        		try {
        		    String nodeName = facet;
        		    if(mapFacets && facetNodeNames[i] != null && !"".equals(facetNodeNames[i])) {
        		        nodeName = facetNodeNames[i];
        		    }
	        		Name childName = resolveName(NodeNameCodec.encode(nodeName));
	        		FacetNavigationNodeId childNodeId = new FacetNavigationNodeId(facetsAvailableNavigationProvider,state.getNodeId(), childName);
	        		childNodeId.availableFacets = facets;
                    childNodeId.facetNodeNames = facetNodeNames;
	        		childNodeId.currentFacet = facet;
	        		childNodeId.docbase = docbase;
	        		inheritParentFilters(childNodeId, state);
	        		state.addChildNodeEntry(childName, childNodeId);
	        		i++;
        		} catch (IllegalNameException e){
        			log.error("Skipping illegal name as facet : " + facet + " because : " +  e.getMessage());
        		}catch (NamespaceException e) {
        			log.error("Skipping illegal name as facet : " + facet + " because : " +  e.getMessage());
        		}
        	}
        }
	    
	    return state;
	}

}
