package org.hippoecm.repository.jackrabbit.facetnavigation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.hippoecm.repository.FacetedNavigationEngine;
import org.hippoecm.repository.FacetedNavigationEngine.HitsRequested;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.jackrabbit.FacetKeyValue;
import org.hippoecm.repository.jackrabbit.FacetResultSetProvider;
import org.hippoecm.repository.jackrabbit.HippoNodeId;
import org.hippoecm.repository.jackrabbit.KeyValue;

public class FacetSubNavigationProvider extends AbstractFacetNavigationProvider {

	@SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    
    protected FacetsAvailableNavigationProvider facetsAvailableNavigationProvider = null;
    protected FacetResultSetProvider subNodesProvider = null;
    

    @Override
    public void initialize() throws RepositoryException {
        super.initialize();
        facetsAvailableNavigationProvider = (FacetsAvailableNavigationProvider) lookup(FacetsAvailableNavigationProvider.class.getName());
        subNodesProvider  = (FacetResultSetProvider) lookup(FacetResultSetProvider.class.getName());
        virtualNodeName = resolveName(HippoNodeType.NT_FACETSUBNAVIGATION);
        register(null, virtualNodeName);
    }

 
    @Override
    public NodeState populate(NodeState state) throws RepositoryException {
    	NodeId nodeId = state.getNodeId();
    	if (nodeId instanceof FacetNavigationNodeId) {
    		FacetNavigationNodeId facetNavigationNodeId = (FacetNavigationNodeId)nodeId;
    		List<KeyValue<String, String>> currentSearch = facetNavigationNodeId.currentSearch;
    		String[] availableFacets = facetNavigationNodeId.availableFacets;
    		String[] facetNodeNames = facetNavigationNodeId.facetNodeNames;
    	    String docbase = facetNavigationNodeId.docbase;
    	    Map<Name,String> inheritedFilter = facetNavigationNodeId.view;
    	    List<KeyValue<String, String>> usedFacetValueCombis = facetNavigationNodeId.usedFacetValueCombis;
    	 
            FacetedNavigationEngine.Query initialQuery;
            initialQuery = (docbase != null ? facetedEngine.parse(docbase) : null);

            HitsRequested hitsRequested = new HitsRequested();
            hitsRequested.setResultRequested(false);
            hitsRequested.setCountOnlyForFacetExists(true);
            
            FacetedNavigationEngine.Result facetedResult;
            
            facetedResult = facetedEngine.view(null, initialQuery, facetedContext, currentSearch, null, inheritedFilter,
                    hitsRequested);
            
            int count = facetedResult.length();
            
            PropertyState propState = createNew(countName, state.getNodeId());
            propState.setType(PropertyType.LONG);
            propState.setDefinitionId(subCountPropDef.getId());
            propState.setValues(new InternalValue[] { InternalValue.create(count) });
            propState.setMultiValued(false);
            state.addPropertyName(countName);
            
            if(facetNavigationNodeId.stopSubNavigation) {
                // we are done with this facet - value combination
                return state;
            }
            int i = 0;
        	for(String facet : availableFacets){ 
        	    
        	    String nodeName = facet;
        	    if(facetNodeNames != null && facetNodeNames[i] != null && !"".equals(facetNodeNames[i])) {
                    nodeName = facetNodeNames[i];
                }
        	    i++;
        	    
        		Name childName = resolveName(NodeNameCodec.encode(nodeName));
        		FacetNavigationNodeId childNodeId = new FacetNavigationNodeId(facetsAvailableNavigationProvider,state.getNodeId(), childName);
            	  for(String value : facetNavigationNodeId.ancestorAndSelfUsedLuceneTerms) {
            		    KeyValue<String,String> facetValueCombi = new FacetKeyValue(facet, value);
                		if(usedFacetValueCombis.indexOf(facetValueCombi) > -1) {
                           /*
                            * the exact facet value combination is already populated before in the tree. We populate the key-value
                            * combi one more time, to have a consistent faceted navigation view, but, after that, we do not populate the 
                            * childs anymore, also to avoid recursion
                            */
                		   childNodeId.stopSubNavigation = true;
                           break;  
                        }
            	  }
        		childNodeId.availableFacets = availableFacets;
                childNodeId.facetNodeNames = facetNodeNames;
        		childNodeId.currentFacet = facet;
        		childNodeId.ancestorAndSelfUsedLuceneTerms = facetNavigationNodeId.ancestorAndSelfUsedLuceneTerms;
        		childNodeId.docbase = docbase;
        		childNodeId.currentSearch = currentSearch;
        		childNodeId.view = facetNavigationNodeId.view;
				childNodeId.order = facetNavigationNodeId.order;
				childNodeId.singledView = facetNavigationNodeId.singledView;
				childNodeId.usedFacetValueCombis = new ArrayList<KeyValue<String,String>>(usedFacetValueCombis);
        		state.addChildNodeEntry(childName, childNodeId);
        		
        	}
          
            // add child node resultset:
            Name resultSetChildName = resolveName(HippoNodeType.HIPPO_RESULTSET);
            FacetResultSetProvider.FacetResultSetNodeId childNodeId;
            childNodeId = subNodesProvider.new FacetResultSetNodeId(state.getNodeId(), resultSetChildName, null,
                    docbase, currentSearch, count);
            state.addChildNodeEntry(resultSetChildName, childNodeId);
    	}
        return state;
    }
    
    @Override
    public NodeState populate(HippoNodeId nodeId, NodeId parentId) throws RepositoryException {
        NodeState state = createNew(nodeId, virtualNodeName, parentId);
        state.setDefinitionId(lookupNodeDef(getNodeState(parentId), resolveName(HippoNodeType.NT_FACETSUBNAVIGATION),
                nodeId.name).getId());
        state.setNodeTypeName(resolveName(HippoNodeType.NT_FACETSUBNAVIGATION));

        return populate(state);
    }
}
