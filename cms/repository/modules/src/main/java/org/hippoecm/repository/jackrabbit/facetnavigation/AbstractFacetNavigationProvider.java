/*
 *  Copyright 2008 Hippo.
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
import java.util.LinkedHashMap;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.spi.Name;
import org.hippoecm.repository.FacetRange;
import org.hippoecm.repository.FacetedNavigationEngine;
import org.hippoecm.repository.OrderBy;
import org.hippoecm.repository.FacetedNavigationEngine.Context;
import org.hippoecm.repository.FacetedNavigationEngine.Count;
import org.hippoecm.repository.FacetedNavigationEngine.Query;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.jackrabbit.DataProviderContext;
import org.hippoecm.repository.jackrabbit.HippoNodeId;
import org.hippoecm.repository.jackrabbit.HippoVirtualProvider;
import org.hippoecm.repository.jackrabbit.IFilterNodeId;
import org.hippoecm.repository.jackrabbit.KeyValue;

public abstract class AbstractFacetNavigationProvider extends HippoVirtualProvider {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    protected static final String VALID_RANGE_EXAMPLE = "hippo:date$[{name:'this week', resolution:'week', begin:-1, end:0}, {name:'last 7 days', resolution:'day', begin:-7, end:0 }]";
    
    FacetedNavigationEngine<Query, Context> facetedEngine;
    FacetedNavigationEngine.Context facetedContext;

    Name countName;
    PropDef subCountPropDef;
    Name virtualNodeName;

    
    @Override
    public void initialize(DataProviderContext stateMgr) throws RepositoryException {
        super.initialize(stateMgr);
        this.facetedEngine = stateMgr.getFacetedEngine();
        this.facetedContext = stateMgr.getFacetedContext();
        stateMgr.registerProviderProperty(countName);
    }

    @Override
    protected void initialize() throws RepositoryException {
        countName = resolveName(HippoNodeType.HIPPO_COUNT);
        subCountPropDef = lookupPropDef(resolveName(HippoNodeType.NT_ABSTRACTFACETNAVIGATION), countName);
    }

    @Override
    public NodeState populate(NodeState state) throws RepositoryException {
        throw new RepositoryException("Subclasses must implement populate(NodeState state)");
    }

    @Override
    public NodeState populate(HippoNodeId nodeId, NodeId parentId) throws RepositoryException {
        throw new RepositoryException("Cannot populate node");
    }

    protected class FacetNavigationNodeId extends HippoNodeId implements IFilterNodeId {
        private static final long serialVersionUID = 1L;
        String docbase;
        String[] availableFacets;
        /*
         * Contains the possible translations for the facets --> facet node name
         */
        String[] facetNodeNames;
        String currentFacet;
       
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
        
        FacetNavigationNodeId(HippoVirtualProvider provider, NodeId parent, Name name) {
            super(provider, parent, name);
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
	    if(state.getNodeId() instanceof IFilterNodeId) {
	        IFilterNodeId filterNodeId = (IFilterNodeId)state.getNodeId();
            if(filterNodeId.getView() != null) {
                childNodeId.view = new LinkedHashMap<Name,String>(filterNodeId.getView());
            }
            if(filterNodeId.getOrder() != null) {
                childNodeId.order = new LinkedHashMap<Name,String>(filterNodeId.getOrder());
            }
            childNodeId.singledView = filterNodeId.isSingledView();
	    } else if (state.getParentId()!=null && state.getParentId() instanceof IFilterNodeId) {
			IFilterNodeId filterNodeId = (IFilterNodeId)state.getParentId();
			if(filterNodeId.getView() != null) {
				childNodeId.view = new LinkedHashMap<Name,String>(filterNodeId.getView());
			}
			if(filterNodeId.getOrder() != null) {
				childNodeId.order = new LinkedHashMap<Name,String>(filterNodeId.getOrder());
			}
			childNodeId.singledView = filterNodeId.isSingledView();
		}
	}
    
    public class FacetNavigationEntry implements Comparable<FacetNavigationEntry> {
        String facetValue;
        Count count;
        public FacetNavigationEntry(String facetValue, Count count) {
            this.facetValue = facetValue;
            this.count = count;
        }

        public int compareTo(FacetNavigationEntry entry) {
           if(entry == null) {
               throw new NullPointerException();
           }
           if(entry.equals(this)) {
        	   return 0;
           }
           if(entry.count.count - this.count.count == 0) {
        	   return 1;
           }
           return (entry.count.count - this.count.count);
        }
        
    }
    
}
