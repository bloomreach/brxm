/*
 *  Copyright 2010-2015 Hippo B.V. (http://www.onehippo.com)
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.InvalidItemStateException;
import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.hippoecm.repository.FacetFilters;
import org.hippoecm.repository.FacetedNavigationEngine;
import org.hippoecm.repository.HitsRequested;
import org.hippoecm.repository.KeyValue;
import org.hippoecm.repository.OrderBy;
import org.hippoecm.repository.ParsedFacet;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.dataprovider.StateProviderContext;
import org.hippoecm.repository.jackrabbit.FacetResultSetProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacetNavigationProvider extends AbstractFacetNavigationProvider {

    private final Logger log = LoggerFactory.getLogger(FacetNavigationProvider.class);
    
    protected FacetsAvailableNavigationProvider facetsAvailableNavigationProvider = null;

    Name docbaseName;
    Name facetsName;
    Name facetNodeNamesName;

    Name facetLimit;
    Name facetSortBy;
    Name facetSortOrder;
    Name facetFilters;
    Name skipResultSetFacedNavigationRoot;
    Name skipResultSetFacetsAvailableName;
    
    @Override
    protected void initialize() throws RepositoryException {
        super.initialize();
        facetsAvailableNavigationProvider = (FacetsAvailableNavigationProvider) lookup(FacetsAvailableNavigationProvider.class.getName());
        docbaseName = resolveName(HippoNodeType.HIPPO_DOCBASE);
        facetsName = resolveName(FacNavNodeType.HIPPOFACNAV_FACETS);
        facetNodeNamesName = resolveName(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES);

        facetLimit = resolveName(FacNavNodeType.HIPPOFACNAV_FACETLIMIT);
        facetSortBy = resolveName(FacNavNodeType.HIPPOFACNAV_FACETSORTBY);
        facetSortOrder = resolveName(FacNavNodeType.HIPPOFACNAV_FACETSORTORDER);
        
        facetFilters = resolveName(FacNavNodeType.HIPPOFACNAV_FILTERS);
        skipResultSetFacedNavigationRoot = resolveName(FacNavNodeType.HIPPOFACNAV_SKIP_RESULTSET_FOR_FACET_NAVIGATION_ROOT);
        skipResultSetFacetsAvailableName = resolveName(FacNavNodeType.HIPPOFACNAV_SKIP_RESULTSET_FOR_FACETS_AVAILABLE);
        
        virtualNodeName = resolveName(FacNavNodeType.NT_FACETSAVAILABLENAVIGATION);
        register(resolveName(FacNavNodeType.NT_FACETNAVIGATION), virtualNodeName);
    }

    @Override
    public NodeState populate(StateProviderContext context, NodeState state) throws RepositoryException {
        long start = System.currentTimeMillis();
        try {
            return doPopulate(context, state);
        } finally {
            log.debug("Populating root faceted navigation node took '{}' ms.", (System.currentTimeMillis() - start));
        }
    }

    public NodeState doPopulate(StateProviderContext context, NodeState state) throws RepositoryException {
        NodeId nodeId = state.getNodeId();

        String[] property = getProperty(nodeId, docbaseName);
        String docbase = (property != null && property.length > 0 ? property[0] : null);
        int limit = -1;
        try {
            limit = getPropertyAsInt(nodeId, facetLimit);
            if(limit < 0 ) {
                log.warn("Skipping negative limit '{}'. Using default", limit);
            }
        } catch (NumberFormatException e) {
            // no limit configured, ignore
        } catch (InvalidItemStateException e) {
            // no limit configured, ignore
        }

        String[] facets = getProperty(nodeId, facetsName, null);
        String[] facetNodeNames = getProperty(nodeId, facetNodeNamesName, null);
        String[] sortbys = getProperty(nodeId, facetSortBy, null);
        String[] sortorders = getProperty(nodeId, facetSortOrder, null);
        String[] filters = getProperty(nodeId, facetFilters, null);
        
        // check whether to skip resultset for root faceted navigation node
        boolean skipResultSetForFacetNavigationRoot = getPropertyAsBoolean(nodeId, skipResultSetFacedNavigationRoot);
                
        // check whether to skip resultset for facets available nodes
        boolean skipResultSetForFacetsAvailable = getPropertyAsBoolean(nodeId, skipResultSetFacetsAvailableName);
        
        String facetedFiltersString = null;
        if(filters != null) {
            try { 
                FacetFilters facetedFilters = new FacetFilters(filters, this); 
                facetedFiltersString = facetedFilters.toString();
            } catch (IllegalArgumentException e) {
                log.warn("Invalid filter found. Return state : {}", e.getMessage());
                // we always need to populate the count
                populateCount(state, 0);
                return state;
            }
        }

        List<OrderBy> orderByList = null;
        if(sortbys != null) {
            orderByList = new ArrayList<OrderBy>();
            if(sortorders != null && sortorders.length != sortbys.length) {
                log.warn("When using multivalued '{}', and '{}', then both should have equal number of values (or delete property "+FacNavNodeType.HIPPOFACNAV_FACETSORTORDER+" at all)", FacNavNodeType.HIPPOFACNAV_FACETSORTBY, FacNavNodeType.HIPPOFACNAV_FACETSORTORDER);
                // we always need to populate the count
                populateCount(state, 0);
                return state;
            }
            for(int i = 0; i < sortbys.length; i++) {
                try {
                    if (sortbys[i].equals("")) {
                        log.warn("Skipping illegal name \"\" as sortby for facet node {}", nodeId);
                        continue;
                    }

                    Name propertyName = resolveName(NodeNameCodec.encode(sortbys[i]));
                    if(sortorders != null && "descending".equals(sortorders[i])) {
                        orderByList.add(new OrderBy(propertyName.toString(), true));
                    } else {
                        // default orderby is ascending
                        orderByList.add(new OrderBy(propertyName.toString()));
                    }
                } catch (IllegalNameException|NamespaceException e) {
                    log.warn("Skipping illegal name \"{}\" as sortby for facet node {} because: ", sortbys[i], nodeId, e.getMessage());
                }
            }
        }
        
        try {
            FacetNodeViews facetNodeViews = new FacetNodeViews(facets, facetNodeNames);
            for(FacetNodeView facetNodeView : facetNodeViews) {
                FacetNodeViews newFacetNodeViews = facetNodeViews;
                if(!facetNodeView.visible || facetNodeView.disabled) {
                    // the current facet node view is not yet visible (first another facet might have to be chosen before it becomes visible)
                    // for example, month might only be shown when year is already picked, or it is disabled at all
                    continue;
                }
                
                newFacetNodeViews = newFacetNodeViews.getFacetNodeViews(facetNodeView);
                
                try {
                    ParsedFacet parsedFacet;
                    try {
                        parsedFacet = ParsedFacet.getInstance(facetNodeView.facet, facetNodeView.displayName , this);
                    } catch (Exception e) {
                        log.warn("Malformed facet range configuration '"+facetNodeView.facet+"'. Valid format is "+VALID_RANGE_EXAMPLE,
                                        e);
                        // we always need to populate the count
                        populateCount(state, 0);
                        return state;
                    }
                    
                    Name childName = resolveName(NodeNameCodec.encode(parsedFacet.getDisplayFacetName()));
                    FacetNavigationNodeId childNodeId = new FacetNavigationNodeId(facetsAvailableNavigationProvider,state.getNodeId(), context, childName);
                    childNodeId.availableFacets = facets;
                    childNodeId.facetedFiltersString  = facetedFiltersString;
                    childNodeId.facetNodeViews = newFacetNodeViews;
                    childNodeId.currentFacetNodeView = facetNodeView;
                    childNodeId.docbase = docbase;
                    childNodeId.skipResultSetForFacetsAvailable = skipResultSetForFacetsAvailable;
                    if(limit > -1) {
                        childNodeId.limit = limit;
                    }
                    childNodeId.orderByList = orderByList;
                    inheritParentFilters(childNodeId, state);
                    state.addChildNodeEntry(childName, childNodeId);
                    
                } catch (IllegalNameException|NamespaceException e) {
                    log.warn("Skipping illegal name as facet : " + facetNodeView.facet + " because : " +  e.getMessage());
                }
            }
        } catch (IllegalArgumentException e) {
            log.warn("Incorrect faceted navigation configuration: '{}'. Return state", e.getMessage());
            // we always need to populate the count
            populateCount(state, 0);
            return state;
        }
        
        // Add count
        StringBuilder initialQueryString = new StringBuilder();
        if(docbase != null) {
            initialQueryString.append(docbase);
        }
        if(facetedFiltersString != null) {
            initialQueryString.append(FacetedNavigationEngine.Query.DOCBASE_FILTER_DELIMITER).append(facetedFiltersString);
        }
        FacetedNavigationEngine.Query initialQuery;
        try {
            initialQuery = (docbase != null ? facetedEngine.parse(initialQueryString.toString()) : null);
        } catch (IllegalArgumentException e) {
            log.warn("Return state. Error parsing initial query:  '{}'", e.getMessage());
            // we always need to populate the count
            populateCount(state, 0);
            return state;
        }

        HitsRequested hitsRequested = new HitsRequested();
        hitsRequested.setResultRequested(false);
        hitsRequested.setFixedDrillPath(false);

        FacetedNavigationEngine.Result facetedResult = null;
        try {
            Map<String, String> inheritedFilterMap = null;
            
            // get from public void inheritParentFilters(FacetNavigationNodeId childNodeId, NodeState state) {
            ParentFilters parentFilters = new ParentFilters(state);
            
            if(parentFilters.view != null) {
                inheritedFilterMap = new HashMap<String,String>();
                for(Entry<Name, String> entry : parentFilters.view.entrySet()) {
                    inheritedFilterMap.put(entry.getKey().toString(), entry.getValue());
                }
            }
            
            facetedResult = facetedEngine.view(null, initialQuery, facetedContext, new ArrayList<KeyValue<String, String>>(), null, (context != null ? context.getParameterQuery(facetedEngine) : null),
                null, inheritedFilterMap , hitsRequested);


        } catch (IllegalArgumentException e) {
            log.warn("Cannot get the faceted result: '"+e.getMessage()+"'");
            // we always need to populate the count
            populateCount(state, 0);
            return state;
        }
        
        int count = facetedResult.length();
        
        populateCount(state, count);
        
        // Add resultset if skipResultSetForFacetNavigationRoot is not true
        if (!skipResultSetForFacetNavigationRoot) {
            FacetResultSetProvider.FacetResultSetNodeId childNodeId = subNodesProvider.new FacetResultSetNodeId(state.getNodeId(), context, resultSetChildName, null,
                    docbase, new ArrayList<KeyValue<String, String>>(), null, facetedFiltersString);
            if(limit > -1) {
                childNodeId.setLimit(limit);
            }
            childNodeId.setOrderByList(orderByList);
            state.addChildNodeEntry(resultSetChildName, childNodeId);
        }
        return state;
    }

    /*
     * returns the value of the property is 'true' or boolean true
     * When property is missing, false is returned
     */
    private boolean getPropertyAsBoolean(NodeId nodeId, Name propName) throws InvalidItemStateException, RepositoryException {
        String[] skips = getProperty(nodeId, propName, null);
        String skip = (skips != null && skips.length > 0 ? skips[0] : "false");
        return Boolean.parseBoolean(skip);
    }

    private void populateCount(NodeState state, int count) throws RepositoryException {
        PropertyState propState = createNew(countName, state.getNodeId());
        propState.setType(PropertyType.LONG);
        propState.setValues(new InternalValue[] { InternalValue.create(count) });
        propState.setMultiValued(false);
        state.addPropertyName(countName);   
        return;
    }
    
    protected final int getPropertyAsInt(NodeId nodeId, Name propName) throws NumberFormatException, InvalidItemStateException, RepositoryException {
        PropertyState propState = getPropertyState(new PropertyId(nodeId, propName));
        if(propState == null) {
            if(log.isDebugEnabled()) {
                log.debug("possible expected property state " + propName + " in " + nodeId + " not found");
            }
            throw new NumberFormatException("Cannot get value for property '"+propName+"' as integer because there is no such property");
        }
        
        if(propState.getType() == PropertyType.DOUBLE) {
            InternalValue[] values = propState.getValues();
            if(values.length != 1) {
                throw new NumberFormatException("Cannot parse value for multivalued property '"+propName+"' to an integer");
            }
            return (int)values[0].getDouble();
        } else if(propState.getType() == PropertyType.LONG) {
            InternalValue[] values = propState.getValues();
            if(values.length != 1) {
                throw new NumberFormatException("Cannot parse value for multivalued property '"+propName+"' to an integer");
            }
            return (int)values[0].getLong();
        } else if(propState.getType() == PropertyType.STRING) {
            InternalValue[] values = propState.getValues();
            if(values.length != 1) {
                throw new NumberFormatException("Cannot parse value for multivalued property '"+propName+"' to an integer");
            }
            return Integer.parseInt(values[0].getString());
        }
        throw new NumberFormatException("Cannot parse value for property '"+propName+"' to an integer");
    }

}
