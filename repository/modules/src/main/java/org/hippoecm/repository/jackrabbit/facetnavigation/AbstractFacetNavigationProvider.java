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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.hippoecm.repository.FacetRange;
import org.hippoecm.repository.FacetedNavigationEngine;
import org.hippoecm.repository.KeyValue;
import org.hippoecm.repository.OrderBy;
import org.hippoecm.repository.FacetedNavigationEngine.Context;
import org.hippoecm.repository.FacetedNavigationEngine.Count;
import org.hippoecm.repository.FacetedNavigationEngine.Query;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.dataprovider.DataProviderContext;
import org.hippoecm.repository.dataprovider.HippoNodeId;
import org.hippoecm.repository.dataprovider.HippoVirtualProvider;
import org.hippoecm.repository.dataprovider.IFilterNodeId;
import org.hippoecm.repository.dataprovider.StateProviderContext;
import org.hippoecm.repository.jackrabbit.FacetResultSetProvider;

public abstract class AbstractFacetNavigationProvider extends HippoVirtualProvider {

    protected static final String VALID_RANGE_EXAMPLE = "hippo:date$[{name:'this week', resolution:'week', begin:-1, end:0}, {name:'last 7 days', resolution:'day', begin:-7, end:0 }]";
    protected static final String VALID_NODENAME_EXAMPLE = "date${sortby:'facetvalue', sortorder:'descending'}";
    
    FacetedNavigationEngine<Query, Context> facetedEngine;
    FacetedNavigationEngine.Context facetedContext;

    Name countName;
    Name virtualNodeName;
    Name resultSetChildName;
    QPropertyDefinition subCountPropDef;
    QPropertyDefinition countPropDef;

    FacetResultSetProvider subNodesProvider = null;

    
    @Override
    public void initialize(DataProviderContext stateMgr) throws RepositoryException {
        super.initialize(stateMgr);
        this.facetedEngine = stateMgr.getFacetedEngine();
        this.facetedContext = stateMgr.getFacetedContext();
        subNodesProvider = (FacetResultSetProvider) lookup(FacetResultSetProvider.class.getName());
        stateMgr.registerProviderProperty(countName);
    }

    @Override
    protected void initialize() throws RepositoryException {
        countName = resolveName(HippoNodeType.HIPPO_COUNT);
        resultSetChildName = resolveName(HippoNodeType.HIPPO_RESULTSET);
        subCountPropDef = lookupPropDef(resolveName(FacNavNodeType.NT_ABSTRACTFACETNAVIGATION), countName);
        countPropDef = lookupPropDef(resolveName(FacNavNodeType.NT_FACETNAVIGATION), countName);
    }

    @Override
    public NodeState populate(StateProviderContext context, NodeState state) throws RepositoryException {
        throw new RepositoryException("Subclasses must implement populate(NodeState state)");
    }

    @Override
    public NodeState populate(StateProviderContext context, HippoNodeId nodeId, NodeId parentId) throws RepositoryException {
        throw new RepositoryException("Cannot populate node");
    }

    protected class FacetNavigationNodeId extends HippoNodeId implements IFilterNodeId {
        private static final long serialVersionUID = 1L;
        String docbase;
        String[] availableFacets;
        
        String facetedFiltersString; 
        
        /*
         * Contains the possible translations for the facets --> facet node name
         */
        FacetNodeViews facetNodeViews;

        FacetNodeView currentFacetNodeView;
        
        //String currentFacet;
       
        // the count property of this node
        int count;
        
        /*
         * when true, the FacetSubNavigationProvider will stop populating adding child nodeId's to this NodeId
         */
        boolean stopSubNavigation = false;
        
        /*
         * contains all the used key-value combinations for the current node and its ancestors
         */
        String[] ancestorAndSelfUsedCombinations;
        
        /*
         * usedFacetValueCombis is the same as currentSearch, only the current search has as keys the string value
         * of org.apache.jackrabbit.spi.Path whereas the usedFacetValueCombis as keys has the orginal facet name
         */
        List<KeyValue<String, String>> usedFacetValueCombis = new ArrayList<KeyValue<String, String>>();
        List<KeyValue<String, String>> currentSearch = new ArrayList<KeyValue<String, String>>();
        List<FacetRange> currentRanges = new ArrayList<FacetRange>();
        
        /*
         * the filter info to propagate
         */
        boolean singledView;
        LinkedHashMap<Name,String> view;
        LinkedHashMap<Name,String> order;
        
        // the list of properties to order the resultset on
        List<OrderBy> orderByList;
        // the limt of the resultset: default is 1000
        int limit = 1000;
        // whether to skip the resultset for facets available
        boolean skipResultSetForFacetsAvailable;
        
        FacetNavigationNodeId(HippoVirtualProvider provider, NodeId parent, StateProviderContext context, Name name) {
            super(provider, parent, context, name);
        }

        public LinkedHashMap<Name, String> getOrder() {
            if(this.order != null) {
                return new LinkedHashMap<Name, String>(this.order);
            } 
            return null;
        }

        public LinkedHashMap<Name, String> getView() {
            if(this.view != null) {
                return new LinkedHashMap<Name, String>(this.view);
            }
            return null;
        }

        public boolean isSingledView() {
            return this.singledView;
        }
    }
    
    public void inheritParentFilters(FacetNavigationNodeId childNodeId, NodeState state) {
        ParentFilters parentFilters = new ParentFilters(state);
        childNodeId.view = parentFilters.view;
        childNodeId.order = parentFilters.order;
        childNodeId.singledView = parentFilters.singledView;
    }
    
    public class ParentFilters {
        boolean singledView;
        LinkedHashMap<Name,String> view;
        LinkedHashMap<Name,String> order;
        
        public ParentFilters(NodeState state) {
            if(state.getNodeId() instanceof IFilterNodeId) {
                IFilterNodeId filterNodeId = (IFilterNodeId)state.getNodeId();
                if(filterNodeId.getView() != null) {
                    view = new LinkedHashMap<Name,String>(filterNodeId.getView());
                }
                if(filterNodeId.getOrder() != null) {
                    order = new LinkedHashMap<Name,String>(filterNodeId.getOrder());
                }
                singledView = filterNodeId.isSingledView();
            } else if (state.getParentId()!=null && state.getParentId() instanceof IFilterNodeId) {
                IFilterNodeId filterNodeId = (IFilterNodeId)state.getParentId();
                if(filterNodeId.getView() != null) {
                    view = new LinkedHashMap<Name,String>(filterNodeId.getView());
                }
                if(filterNodeId.getOrder() != null) {
                    order = new LinkedHashMap<Name,String>(filterNodeId.getOrder());
                }
                singledView = filterNodeId.isSingledView();
            }
        }  
    }
    
    public class FacetNavigationEntry implements Comparable<FacetNavigationEntry> {
        String facetValue;
        Count count;
      
        public FacetNavigationEntry(String facetValue, Count count) {
            this.facetValue = facetValue;
            this.count = count;
        }

        /**
         * This compareTo returns 0 only when count & facetValue are equal. 
         */
        @Override
        public int compareTo(FacetNavigationEntry entry) {
            if(entry == null) {
                throw new NullPointerException();
            }
            if(entry == this) {
                return 0;
            }

            // count will never be negative and never in the range of MAX integer hence
            // below will never fail
            int compare = entry.count.count - this.count.count;
            if(compare != 0) {
                return compare;
            }
            // now, if facetValue's are equal, we just return 0 : this is inline with the equals
            return facetValue.compareTo(entry.facetValue);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 17;
            result = prime * result + ((count == null) ? 0 : count.hashCode());
            result = prime * result + ((facetValue == null) ? 0 : facetValue.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof FacetNavigationEntry)) {
                return false;
            }
            FacetNavigationEntry other = (FacetNavigationEntry) obj;
            if (count == null) {
                if (other.count != null) {
                    return false;
                }
            } else if (!count.equals(other.count)) {
                return false;
            }
            if (facetValue == null) {
                if (other.facetValue != null) {
                    return false;
                }
            } else if (!facetValue.equals(other.facetValue)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "FacetNavigationEntry [facetValue=" + facetValue + ", count=" + count + "]";
        }
    }
    
    /*
     * Comparator which compares on the count of a FacetNavigationEntry
     */
    static final FacetNavigationEntryComparator<FacetNavigationEntry> DESCENDING_COUNT_COMPARATOR = new FacetNavigationEntryComparator<FacetNavigationEntry>(false);
    static final FacetNavigationEntryComparator<FacetNavigationEntry> ASCENDING_COUNT_COMPARATOR = new FacetNavigationEntryComparator<FacetNavigationEntry>(true);
    
    /*
     * Comparator which compares on the FacetNavigationEntry facetValue: if both values are parseable as double, they are compare as double.
     * Otherwise, String comparison is used
     */
    static final FacetNavigationEntryComparator<FacetNavigationEntry> DESCENDING_FACETVALUE_COMPARATOR = new FacetNavigationEntryComparator<FacetNavigationEntry>(false);
    static final FacetNavigationEntryComparator<FacetNavigationEntry> ASCENDING_FACETVALUE_COMPARATOR = new FacetNavigationEntryComparator<FacetNavigationEntry>(true);
    
    
    static class FacetNavigationEntryComparator<T extends FacetNavigationEntry> implements Comparator<FacetNavigationEntry> , Serializable{

        private static final long serialVersionUID = 1L;
        private boolean ascending;
        
        public FacetNavigationEntryComparator(boolean ascending) {
          this.ascending = ascending;
        }

        public int compare(FacetNavigationEntry o1, FacetNavigationEntry o2) {
            
            // if o1 or o2 is null, an NPE is correctly thrown
            if(o2.equals(o1)) {
                return 0;
            }
            
            if (this == DESCENDING_COUNT_COMPARATOR || this == ASCENDING_COUNT_COMPARATOR) {

                int compare = o2.count.count - o1.count.count;
                if(ascending) {
                    compare = -compare;
                }
                return compare;
                
            } else if (this == DESCENDING_FACETVALUE_COMPARATOR || this == ASCENDING_FACETVALUE_COMPARATOR){
                String value1 = o1.facetValue;
                String value2 = o2.facetValue;
                if(value1 == null || value2 == null) {
                    int compare = 1;
                    if(!ascending) {
                        compare = -compare;
                    }
                    return value1 == null ? compare : -compare;
                }
                try {
                    Double double1 = Double.parseDouble(value1);
                    Double double2 = Double.parseDouble(value2);
                    int compare = double1.compareTo(double2);
                    if(!ascending) {
                        compare = -compare;
                    }
                    return compare;
                } catch(NumberFormatException e) {
                    //  not both are of type double
                }
                // simple string comparison
                int compare = value1.compareTo(value2);
                if(!ascending) {
                    compare = -compare;
                }
                return compare;
            }
            return 0;
        }
        
    }

    public static FacetNavigationEntryComparator<FacetNavigationEntry> getComparator(String sortby, String sortorder) throws IllegalArgumentException{
        if(sortby == null) {
            return null;
        }
        if(sortby.equals("facetvalue")) {
            if(sortorder == null || "ascending".equals(sortorder)) {
                return ASCENDING_FACETVALUE_COMPARATOR;
            } 
            if("descending".equals(sortorder)) {
                return DESCENDING_FACETVALUE_COMPARATOR;
            }
            throw new IllegalArgumentException("Unsupported sortorder configured: '"+sortorder+"'. Only 'descending' or 'ascending' is supported");
        } else if (sortby.equals("count")) {
            if(sortorder == null || "descending".equals(sortorder)) {
                return DESCENDING_COUNT_COMPARATOR;
            } 
            if("ascending".equals(sortorder)) {
                return ASCENDING_COUNT_COMPARATOR;
            }
            throw new IllegalArgumentException("Unsupported sortorder configured: '"+sortorder+"'. Only 'descending' or 'ascending' is supported");
        } else if (sortby.equals("config")) {
            // sorting by config, we return null, as we need to sort by hand
            return null;
        } else {
            throw new IllegalArgumentException("Only supported sortby values are 'facetvalue', 'count' or 'config' but configured one is: '"+sortby+"'");
        }
        
    }
    
    public String getStats(long time, NodeState state, StateProviderContext context) {
       
        NodeId anc = state.getParentId();
        NodeId child = state.getNodeId();
        String path = "";
        while(anc != null) {
            NodeState ancState = getNodeState(anc, context);
            if (ancState != null) {
                ChildNodeEntry e =  ancState.getChildNodeEntry(child);
                if(e == null) {
                    // this happens sometimes for some reason but is harmless. We just put _ignore_ with uFFFF around it instead of the real name
                    path = "/" + '\uFFFF' +"_ignore_"+'\uFFFF' + path;
                } else {
                    path = "/" + e.getName().getLocalName() + path;
                }
                child = ancState.getNodeId();
                anc = ancState.getParentId();
            } else {
                anc = null;
            }
        }
        return time + " ms for " + path;
    }
}
