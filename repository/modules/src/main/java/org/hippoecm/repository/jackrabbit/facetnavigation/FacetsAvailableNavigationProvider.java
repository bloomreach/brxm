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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.hippoecm.repository.FacetKeyValue;
import org.hippoecm.repository.FacetRange;
import org.hippoecm.repository.FacetedNavigationEngine;
import org.hippoecm.repository.HitsRequested;
import org.hippoecm.repository.KeyValue;
import org.hippoecm.repository.ParsedFacet;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.dataprovider.HippoNodeId;
import org.hippoecm.repository.dataprovider.StateProviderContext;
import org.hippoecm.repository.jackrabbit.FacetResultSetProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacetsAvailableNavigationProvider extends AbstractFacetNavigationProvider {

    private final Logger log = LoggerFactory.getLogger(FacetsAvailableNavigationProvider.class);

    protected FacetSubNavigationProvider facetsSubNavigationProvider = null;

    @Override
    protected void initialize() throws RepositoryException {
        super.initialize();
        facetsSubNavigationProvider = (FacetSubNavigationProvider) lookup(FacetSubNavigationProvider.class.getName());
        virtualNodeName = resolveName(FacNavNodeType.NT_FACETSAVAILABLENAVIGATION);
        register(null, virtualNodeName);
    }

    @Override
    public NodeState populate(StateProviderContext context, NodeState state) throws RepositoryException {
        long startTime = System.currentTimeMillis();
        NodeId nodeId = state.getNodeId();
        if (nodeId instanceof FacetNavigationNodeId) {
            FacetNavigationNodeId facetNavigationNodeId = (FacetNavigationNodeId) nodeId;
            List<KeyValue<String, String>> currentSearch = facetNavigationNodeId.currentSearch;
            List<FacetRange> currentRanges = facetNavigationNodeId.currentRanges;
            FacetNodeView currentFacetNodeView = facetNavigationNodeId.currentFacetNodeView;
            String[] availableFacets = facetNavigationNodeId.availableFacets;
            String docbase = facetNavigationNodeId.docbase;
            String facetedFiltersString = facetNavigationNodeId.facetedFiltersString;
            String[] ancestorAndSelfUsedCombinations = facetNavigationNodeId.ancestorAndSelfUsedCombinations;

            Map<Name, String> inheritedFilter = facetNavigationNodeId.view;

            ParsedFacet parsedFacet;
            try {
                parsedFacet = ParsedFacet.getInstance(currentFacetNodeView.facet, null, this);
            } catch (Exception e) {
                log.warn("Malformed facet range configuration '{}'. Valid format is {}", currentFacetNodeView.facet,
                        ParsedFacet.VALID_RANGE_EXAMPLE);
                return state;
            }

            if (parsedFacet.getNamespacedProperty() == null) {
                return state;
            }
            Map<String, Map<String, FacetedNavigationEngine.Count>> facetSearchResultMap;
            facetSearchResultMap = new HashMap<String, Map<String, FacetedNavigationEngine.Count>>();

            Map<String, FacetedNavigationEngine.Count> facetSearchResult;
            facetSearchResult = new HashMap<String, FacetedNavigationEngine.Count>();

            if (parsedFacet.getRangeConfig() != null) {
                // include the rangeConfig
                facetSearchResultMap.put(parsedFacet.getNamespacedProperty() + "$" + parsedFacet.getRangeConfig(),
                        facetSearchResult);
            } else {
                // normal resolvedFacet
                facetSearchResultMap.put(parsedFacet.getNamespacedProperty(), facetSearchResult);
            }

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
                return state;
            }

            HitsRequested hitsRequested = new HitsRequested();
            hitsRequested.setResultRequested(false);
            hitsRequested.setFixedDrillPath(false);

            FacetedNavigationEngine.Result facetedResult = null;
            try {
                Map<String, String> filters = null;
                if(inheritedFilter != null) {
                    filters = new HashMap<String,String>();
                    for(Entry<Name, String> entry : inheritedFilter.entrySet()) {
                        filters.put(entry.getKey().toString(), entry.getValue());
                    }
                }
                
                long start = 0;
                if(log.isDebugEnabled()) {
                    start   = System.currentTimeMillis();
                }

                facetedResult = facetedEngine.view(null, initialQuery, facetedContext, currentSearch, currentRanges, (context != null ? context.getParameterQuery(facetedEngine) : null),
                    facetSearchResultMap, filters, hitsRequested);

                if(log.isDebugEnabled()) {
                    log.debug("Creating facetResult took '{}' ms for '{}' number of unique facet values.", (System.currentTimeMillis() - start),  facetSearchResult.size());
                }
                
            } catch (IllegalArgumentException e) {
                log.warn("Cannot get the faceted result: '"+e.getMessage()+"'");
                return state;
            }
            
            int count = facetedResult.length();

            PropertyState propState = createNew(countName, state.getNodeId());
            propState.setType(PropertyType.LONG);
            propState.setValues(new InternalValue[] { InternalValue.create(count) });
            propState.setMultiValued(false);
            state.addPropertyName(countName);

            // add child node subnavigation:

            // facetSearchResult logicals default order is the natural descending order of the count. Therefore, we need to create sort the facetSearchResult first.
            FacetNavigationEntry[] facetNavigationEntries = new FacetNavigationEntry[facetSearchResult.size()];
            int i = 0;
            for (Map.Entry<String, FacetedNavigationEngine.Count> entry : facetSearchResult.entrySet()) {
                facetNavigationEntries[i] = new FacetNavigationEntry(entry.getKey(), entry.getValue());
                i++;
            }
            
            if(facetNavigationNodeId.currentFacetNodeView != null && facetNavigationNodeId.currentFacetNodeView.comparator != null) {
                Arrays.sort(facetNavigationEntries, facetNavigationNodeId.currentFacetNodeView.comparator);
            } else if (parsedFacet.getFacetRanges() != null ) {
                // special case: we need to order by the configured ranges if there are ranges configured:
                List<FacetRange> ranges = parsedFacet.getFacetRanges();
                if (facetNavigationNodeId.currentFacetNodeView.sortorder != null
                        && "descending".equals(facetNavigationNodeId.currentFacetNodeView.sortorder)) {
                    
                    // first copy the list entries:
                    List<FacetRange> listToReverse = new ArrayList<FacetRange>(ranges);
                    
                    // reverse order
                    Collections.reverse(listToReverse);
                    ranges = listToReverse;
                }
                
                // fill the facet navigation entry array again, ordered by config
                List<FacetNavigationEntry> entryList = new ArrayList<FacetNavigationEntry>();
                for(FacetRange range : ranges) {
                    if(facetSearchResult.containsKey(range.getName())) {
                        entryList.add(new FacetNavigationEntry(range.getName(), facetSearchResult.get(range.getName())));
                    }
                }
                
                facetNavigationEntries = entryList.toArray(new FacetNavigationEntry[entryList.size()]);
            }
            else {
                // default sorting is on count
                Arrays.sort(facetNavigationEntries);
            }
            

            int number = 0;
            for (FacetNavigationEntry entry : facetNavigationEntries) {
                if ("".equals(entry.facetValue)) {
                    continue;
                }
                // if the currentFacetNodeView has a configured limit of the number of facet values, we stop when we are at the configured limit
                if (facetNavigationNodeId.currentFacetNodeView != null && number >= facetNavigationNodeId.currentFacetNodeView.limit) {
                    log.debug("Stop populating facetvalues because we reached the configured limit of '{}'", String.valueOf(facetNavigationNodeId.currentFacetNodeView.limit));
                    break;
                }
                number++;

                List<KeyValue<String, String>> newSearch = new ArrayList<KeyValue<String, String>>(currentSearch);
                List<FacetRange> newRanges = new ArrayList<FacetRange>(currentRanges);
                if (parsedFacet.getRangeConfig() != null) {
                    for (FacetRange range : parsedFacet.getFacetRanges()) {
                        if (range.getName().equals(entry.facetValue)) {
                            newRanges.add(range);
                        }
                    }
                } else {
                    newSearch.add(new FacetKeyValue(parsedFacet.getNamespacedProperty(), entry.facetValue));
                }

                List<KeyValue<String, String>> usedFacetValueCombis = new ArrayList<KeyValue<String, String>>(
                        facetNavigationNodeId.usedFacetValueCombis);
                KeyValue<String, String> facetValueCombi = new FacetKeyValue(currentFacetNodeView.facet, entry.facetValue);

                boolean stopSubNavigation = facetNavigationNodeId.stopSubNavigation;
                if (!usedFacetValueCombis.contains(facetValueCombi)) {
                    usedFacetValueCombis.add(facetValueCombi);
                    /*
                     * perhaps stopNavigation was already true, but, since it is a new unique facetvalue combi, we need
                     * to populate it further: this happens when a facet has multiple property values
                     */
                    stopSubNavigation = false;
                }
                try {
                    // use forceSimpleName = true in encode because value may contain ":" but this is not related to a namespace prefix
                    Name childName = resolveName(NodeNameCodec.encode(entry.facetValue, true));
                    FacetNavigationNodeId childNodeId = new FacetNavigationNodeId(facetsSubNavigationProvider, state.getNodeId(), context, childName);
                    state.addChildNodeEntry(childName, childNodeId);
                    childNodeId.docbase = docbase;
                    childNodeId.availableFacets = availableFacets;
                    childNodeId.facetedFiltersString = facetNavigationNodeId.facetedFiltersString;
                    childNodeId.skipResultSetForFacetsAvailable = facetNavigationNodeId.skipResultSetForFacetsAvailable;
                    childNodeId.currentSearch = newSearch;
                    childNodeId.currentRanges = newRanges;
                    childNodeId.count = entry.count.count;
                    childNodeId.currentFacetNodeView = currentFacetNodeView;

                    String[] newAncestorAndSelfUsedCombinations = new String[ancestorAndSelfUsedCombinations != null ? ancestorAndSelfUsedCombinations.length + 1
                            : 1];
                    if (ancestorAndSelfUsedCombinations != null && ancestorAndSelfUsedCombinations.length > 0) {
                        System.arraycopy(ancestorAndSelfUsedCombinations, 0, newAncestorAndSelfUsedCombinations, 0,
                                ancestorAndSelfUsedCombinations.length);
                    }
                    newAncestorAndSelfUsedCombinations[newAncestorAndSelfUsedCombinations.length - 1] = entry.facetValue;

                    childNodeId.ancestorAndSelfUsedCombinations = newAncestorAndSelfUsedCombinations;
                    childNodeId.usedFacetValueCombis = usedFacetValueCombis;
                    childNodeId.facetNodeViews = facetNavigationNodeId.facetNodeViews;
                    childNodeId.stopSubNavigation = stopSubNavigation;
                    childNodeId.view = facetNavigationNodeId.view;
                    childNodeId.order = facetNavigationNodeId.order;
                    childNodeId.singledView = facetNavigationNodeId.singledView;
                    childNodeId.limit = facetNavigationNodeId.limit;
                    childNodeId.orderByList = facetNavigationNodeId.orderByList;

                } catch (RepositoryException ex) {
                    log.warn("cannot add virtual child in facet search: " + ex.getMessage());
                }
            }

            if (!facetNavigationNodeId.skipResultSetForFacetsAvailable) {
                // add child node resultset only if facetNavigationNodeId.skipResultSetForFacetsAvailable is not true
                // we add to the search now the current facet with no value: this will make sure that 
                // the result set nodes at least contain the facet
    
                List<KeyValue<String, String>> resultSetSearch = new ArrayList<KeyValue<String, String>>(currentSearch);
                // we add here the 'facet' without value, to make sure we only get results having at least the current facet as property
                resultSetSearch.add(new FacetKeyValue(parsedFacet.getNamespacedProperty(), null));
    
                FacetResultSetProvider.FacetResultSetNodeId childNodeId;
                childNodeId = subNodesProvider.new FacetResultSetNodeId(state.getNodeId(), context, resultSetChildName, null,
                        docbase, resultSetSearch, currentRanges, facetedFiltersString);
                childNodeId.setLimit(facetNavigationNodeId.limit);
                childNodeId.setOrderByList(facetNavigationNodeId.orderByList);
                state.addChildNodeEntry(resultSetChildName, childNodeId);
            }
        }
        
        if(log.isDebugEnabled()) {
            log.debug(getStats(System.currentTimeMillis() - startTime, state, context));
        }
        
        return state;
    }

    @Override
    public NodeState populate(StateProviderContext context, HippoNodeId nodeId, NodeId parentId) throws RepositoryException {
        NodeState state = createNew(nodeId, virtualNodeName, parentId);
        state.setNodeTypeName(resolveName(FacNavNodeType.NT_FACETSAVAILABLENAVIGATION));

        return populate(context, state);
    }
}
