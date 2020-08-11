/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.jackrabbit.facetnavigation;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import org.apache.jackrabbit.core.id.NodeId;

import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.hippoecm.repository.FacetKeyValue;
import org.hippoecm.repository.FacetRange;
import org.hippoecm.repository.HitsRequested;
import org.hippoecm.repository.KeyValue;
import org.hippoecm.repository.ParsedFacet;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.dataprovider.HippoNodeId;
import org.hippoecm.repository.dataprovider.StateProviderContext;
import org.hippoecm.repository.jackrabbit.FacetResultSetProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacetSubNavigationProvider extends AbstractFacetNavigationProvider {

    private final Logger log = LoggerFactory.getLogger(FacetSubNavigationProvider.class);
    
    protected FacetsAvailableNavigationProvider facetsAvailableNavigationProvider = null;

    Name leafName;
    QPropertyDefinition leafPropDef;
    
    @Override
    protected void initialize() throws RepositoryException {
        super.initialize();
        facetsAvailableNavigationProvider = (FacetsAvailableNavigationProvider) lookup(FacetsAvailableNavigationProvider.class.getName());
        virtualNodeName = resolveName(FacNavNodeType.NT_FACETSUBNAVIGATION);
        register(null, virtualNodeName);
        leafName = resolveName(FacNavNodeType.HIPPOFACNAV_LEAF);
        leafPropDef = lookupPropDef(resolveName(FacNavNodeType.NT_FACETSUBNAVIGATION), leafName);
    }
 
    @Override
    public NodeState populate(StateProviderContext context, NodeState state) throws RepositoryException {
        long startTime = System.currentTimeMillis();
        NodeId nodeId = state.getNodeId();
        if (nodeId instanceof FacetNavigationNodeId) {
            FacetNavigationNodeId facetNavigationNodeId = (FacetNavigationNodeId)nodeId;
            List<KeyValue<String, String>> currentSearch = facetNavigationNodeId.currentSearch;
            List<FacetRange> currentRanges = facetNavigationNodeId.currentRanges;
            String[] availableFacets = facetNavigationNodeId.availableFacets;
            
            String docbase = facetNavigationNodeId.docbase;
            List<KeyValue<String, String>> usedFacetValueCombis = facetNavigationNodeId.usedFacetValueCombis;

            HitsRequested hitsRequested = new HitsRequested();
            hitsRequested.setResultRequested(false);
            hitsRequested.setFixedDrillPath(false);
          
            PropertyState propState = createNew(countName, state.getNodeId());
            propState.setType(PropertyType.LONG);
            propState.setValues(new InternalValue[] { InternalValue.create(facetNavigationNodeId.count) });
            propState.setMultiValued(false);
            state.addPropertyName(countName);
            
            if(facetNavigationNodeId.stopSubNavigation || facetNavigationNodeId.count == 0) {
                // we are done with this facet - value combination: set the property hippofacnav:leaf = true
                PropertyState leafState = createNew(leafName, state.getNodeId());
                leafState.setType(PropertyType.BOOLEAN);
                leafState.setValues(new InternalValue[] { InternalValue.create(true) });
                leafState.setMultiValued(false);
                state.addPropertyName(leafName);
                return state;
            }
           
            FacetNodeViews newFacetNodeViews = facetNavigationNodeId.facetNodeViews;
            
            for(FacetNodeView facetNodeView: facetNavigationNodeId.facetNodeViews) {
                if(!facetNodeView.visible || facetNodeView.disabled) {
                    // the current facet node view is not yet visible (first another facet might have to be chosen before it becomes visible)
                    // for example, month might only be shown when year is already picked, or it is disabled at all
                    continue;
                }
                
                newFacetNodeViews = newFacetNodeViews.getFacetNodeViews(facetNodeView);
                
                ParsedFacet parsedFacet;
                try {
                    parsedFacet = ParsedFacet.getInstance(facetNodeView.facet, facetNodeView.displayName, this);
                } catch (Exception e) {
                    log.warn("Malformed facet range configuration '{}'. Valid format is "+VALID_RANGE_EXAMPLE,
                            facetNodeView.facet);
                    return state;
                }
                
                Name childName = resolveName(NodeNameCodec.encode(parsedFacet.getDisplayFacetName()));
                FacetNavigationNodeId childNodeId = new FacetNavigationNodeId(facetsAvailableNavigationProvider,state.getNodeId(), context, childName);
                for(String value : facetNavigationNodeId.ancestorAndSelfUsedCombinations) {
                    KeyValue<String,String> facetValueCombi = new FacetKeyValue(facetNodeView.facet, value);
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
                childNodeId.facetedFiltersString = facetNavigationNodeId.facetedFiltersString;
                childNodeId.facetNodeViews = newFacetNodeViews;
                childNodeId.currentFacetNodeView = facetNodeView;
                childNodeId.ancestorAndSelfUsedCombinations = facetNavigationNodeId.ancestorAndSelfUsedCombinations;
                childNodeId.skipResultSetForFacetsAvailable = facetNavigationNodeId.skipResultSetForFacetsAvailable;
                childNodeId.docbase = docbase;
                childNodeId.currentSearch = currentSearch;
                childNodeId.currentRanges = currentRanges;
                childNodeId.view = facetNavigationNodeId.view;
                childNodeId.order = facetNavigationNodeId.order;
                childNodeId.singledView = facetNavigationNodeId.singledView;
                childNodeId.limit = facetNavigationNodeId.limit;
                childNodeId.orderByList = facetNavigationNodeId.orderByList;
                
                childNodeId.usedFacetValueCombis = new ArrayList<KeyValue<String,String>>(usedFacetValueCombis);
                state.addChildNodeEntry(childName, childNodeId);

            }
            
            // add child node resultset:
            FacetResultSetProvider.FacetResultSetNodeId childNodeId;
            childNodeId = subNodesProvider.new FacetResultSetNodeId(state.getNodeId(), context, resultSetChildName, null,
                    docbase, currentSearch, currentRanges, facetNavigationNodeId.facetedFiltersString);
            childNodeId.setLimit(facetNavigationNodeId.limit);
            childNodeId.setOrderByList(facetNavigationNodeId.orderByList);
            
            state.addChildNodeEntry(resultSetChildName, childNodeId);
        }
         
        if(log.isDebugEnabled()) {
            log.debug(getStats(System.currentTimeMillis() - startTime, state, context));
        }
        
        return state;
    }
    
    @Override
    public NodeState populate(StateProviderContext context, HippoNodeId nodeId, NodeId parentId) throws RepositoryException {
        NodeState state = createNew(nodeId, virtualNodeName, parentId);
        state.setNodeTypeName(resolveName(FacNavNodeType.NT_FACETSUBNAVIGATION));

        return populate(context, state);
    }
}
