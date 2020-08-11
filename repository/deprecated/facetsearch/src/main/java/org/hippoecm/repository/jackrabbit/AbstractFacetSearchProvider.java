/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.jackrabbit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.hippoecm.repository.FacetKeyValue;
import org.hippoecm.repository.FacetedNavigationEngine;
import org.hippoecm.repository.FacetedNavigationEngine.Context;
import org.hippoecm.repository.FacetedNavigationEngine.Count;
import org.hippoecm.repository.FacetedNavigationEngine.Query;
import org.hippoecm.repository.HitsRequested;
import org.hippoecm.repository.KeyValue;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.dataprovider.DataProviderContext;
import org.hippoecm.repository.dataprovider.HippoNodeId;
import org.hippoecm.repository.dataprovider.HippoVirtualProvider;
import org.hippoecm.repository.dataprovider.StateProviderContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * deprecated since 2.26.00
 */
@Deprecated
public abstract class AbstractFacetSearchProvider extends HippoVirtualProvider {

    private final Logger log = LoggerFactory.getLogger(HippoVirtualProvider.class);

    class FacetSearchNodeId extends HippoNodeId {
        private static final long serialVersionUID = 1L;
        String queryname;
        String docbase;
        String[] facets;
        String[] search;
        long count;

        FacetSearchNodeId(HippoVirtualProvider provider, NodeId parent, StateProviderContext context, Name name) {
            super(provider, parent, context, name);
        }
    }

    private static Pattern facetPropertyPattern;
    static {
        facetPropertyPattern = Pattern.compile("^@([^=]+)='(.+)'$");
    }

    protected FacetSubSearchProvider subSearchProvider = null;
    protected FacetResultSetProvider subNodesProvider = null;

    FacetedNavigationEngine<Query, Context> facetedEngine;
    FacetedNavigationEngine.Context facetedContext;

    Name querynameName;
    Name docbaseName;
    Name facetsName;
    Name searchName;
    Name countName;

    QPropertyDefinition querynamePropDef;
    QPropertyDefinition docbasePropDef;
    QPropertyDefinition facetsPropDef;
    QPropertyDefinition searchPropDef;
    QPropertyDefinition countPropDef;

    Name virtualNodeName;

    protected AbstractFacetSearchProvider() {
        super();
    }

    @Override
    public void initialize(DataProviderContext stateMgr) throws RepositoryException {
        super.initialize(stateMgr);
        this.facetedEngine = stateMgr.getFacetedEngine();
        this.facetedContext = stateMgr.getFacetedContext();
        stateMgr.registerProviderProperty(countName);
    }

    @Override
    protected void initialize() throws RepositoryException {
        querynameName = resolveName(HippoNodeType.HIPPO_QUERYNAME);
        docbaseName = resolveName(HippoNodeType.HIPPO_DOCBASE);
        facetsName = resolveName(HippoNodeType.HIPPO_FACETS);
        searchName = resolveName(HippoNodeType.HIPPO_SEARCH);
        countName = resolveName(HippoNodeType.HIPPO_COUNT);

        querynamePropDef = lookupPropDef(resolveName(HippoNodeType.NT_FACETBASESEARCH), querynameName);
        docbasePropDef = lookupPropDef(resolveName(HippoNodeType.NT_FACETSEARCH), docbaseName);
        facetsPropDef = lookupPropDef(resolveName(HippoNodeType.NT_FACETSEARCH), facetsName);
        searchPropDef = lookupPropDef(resolveName(HippoNodeType.NT_FACETSUBSEARCH), searchName);
        countPropDef = lookupPropDef(resolveName(HippoNodeType.NT_FACETBASESEARCH), countName);
    }

    @Override
    public NodeState populate(StateProviderContext context, NodeState state) throws RepositoryException {
        NodeId nodeId = state.getNodeId();
        String queryname;
        String docbase;
        String[] facets;
        String[] search = null;
        long count = 0;
        if (nodeId instanceof FacetSearchNodeId) {
            FacetSearchNodeId facetSearchNodeId = (FacetSearchNodeId) nodeId;
            queryname = facetSearchNodeId.queryname;
            docbase = facetSearchNodeId.docbase;
            facets = facetSearchNodeId.facets;
            search = facetSearchNodeId.search;
            count = facetSearchNodeId.count;
        } else {
            String[] property = getProperty(nodeId, querynameName, null);
            queryname = (property != null && property.length > 0 ? property[0] : null);
            property = getProperty(nodeId, docbaseName);
            docbase = (property != null && property.length > 0 ? property[0] : null);
            facets = getProperty(nodeId, facetsName);
            search = getProperty(nodeId, searchName, null);
        }

        if (facets != null && facets.length > 0) {
            Map<String, Map<String, org.hippoecm.repository.FacetedNavigationEngine.Count>> facetSearchResultMap;
            facetSearchResultMap = new TreeMap<String, Map<String, FacetedNavigationEngine.Count>>();
            Map<String, FacetedNavigationEngine.Count> facetSearchResult;
            facetSearchResult = new TreeMap<String, FacetedNavigationEngine.Count>();
            facetSearchResultMap.put(resolvePath(facets[0]).toString(), facetSearchResult);

            List<KeyValue<String, String>> currentFacetQuery = new ArrayList<KeyValue<String, String>>();
            String resolvedFacet = null;
            for (int i = 0; search != null && i < search.length; i++) {
                Matcher matcher = facetPropertyPattern.matcher(search[i]);
                if (matcher.matches() && matcher.groupCount() == 2) {
                    try {
                        resolvedFacet = resolvePath(matcher.group(1)).toString();
                        currentFacetQuery.add(new FacetKeyValue(resolvedFacet, matcher.group(2)));
                    } catch (IllegalNameException ex) {
                        log.error("intermediate facet search broken", ex);
                        return state;
                    } catch (NamespaceException ex) {
                        log.error("intermediate facet search broken", ex);
                        return state;
                    }
                }
            }
            FacetedNavigationEngine.Query initialQuery;
            initialQuery = (docbase != null ? facetedEngine.parse(docbase) : null);

            HitsRequested hitsRequested = new HitsRequested();
            hitsRequested.setResultRequested(false);

            FacetedNavigationEngine.Result facetedResult;
            long t1 = 0, t2;
            if (log.isDebugEnabled()) {
                t1 = System.currentTimeMillis();
            }
            facetedResult = facetedEngine.view(queryname, initialQuery, facetedContext, currentFacetQuery, (context != null ? context.getParameterQuery(facetedEngine) : null),
                    facetSearchResultMap, null, hitsRequested);
            if (log.isDebugEnabled()) {
                t2 = System.currentTimeMillis();
                log.debug("facetsearch turnaround=" + (t2 - t1));
            }
            count = facetedResult.length();

            PropertyState propState = createNew(countName, state.getNodeId());
            propState.setType(PropertyType.LONG);
            propState.setValues(new InternalValue[] { InternalValue.create(count) });
            propState.setMultiValued(false);
            state.addPropertyName(countName);

            // facetSearchResult logicals default order is the natural descending order of the count. Therefore, we need to create sort the facetSearchResult first.
            FacetSearchEntry[] facetSearchEntry = new FacetSearchEntry[facetSearchResult.size()];
            int i = 0;
            for (Map.Entry<String, FacetedNavigationEngine.Count> entry : facetSearchResult.entrySet()) {
                facetSearchEntry[i] = new FacetSearchEntry(entry.getKey(), entry.getValue());
                i++;
            }
            // sort according count
            Arrays.sort(facetSearchEntry);
            
            for(FacetSearchEntry entry : facetSearchEntry) {
                if ("".equals(entry.facetValue)) {
                    continue;
                }
                String[] newFacets = new String[Math.max(0, facets.length - 1)];
                if (facets.length > 1) {
                    System.arraycopy(facets, 1, newFacets, 0, facets.length - 1);
                }
                String[] newSearch = new String[search != null ? search.length + 1 : 1];
                if (search != null && search.length > 0) {
                    System.arraycopy(search, 0, newSearch, 0, search.length);
                }
                
                String facetValue = entry.facetValue;
                
                if (facets[0].indexOf("#") == -1) {
                    newSearch[newSearch.length - 1] = "@" + facets[0] + "='" + facetValue + "'";
                } else {
                    newSearch[newSearch.length - 1] = "@" + facets[0].substring(0, facets[0].indexOf("#")) + "='"
                            + facetValue + "'" + facets[0].substring(facets[0].indexOf("#"));
                }
                try {
                    Name childName = resolveName(NodeNameCodec.encode(facetValue, true));
                    FacetSearchNodeId childNodeId = new FacetSearchNodeId(subSearchProvider, state.getNodeId(), context, childName);
                    state.addChildNodeEntry(childName, childNodeId);
                    childNodeId.queryname = queryname;
                    childNodeId.docbase = docbase;
                    childNodeId.facets = newFacets;
                    childNodeId.search = newSearch;
                    childNodeId.count = entry.count.count;
                } catch (RepositoryException ex) {
                    log.warn("cannot add virtual child in facet search: " + ex.getMessage());
                }
            }
            
        }

        FacetResultSetProvider.FacetResultSetNodeId childNodeId;
        Name resultSetChildName = resolveName(HippoNodeType.HIPPO_RESULTSET);
        childNodeId = subNodesProvider.new FacetResultSetNodeId(state.getNodeId(), context, resultSetChildName, queryname,
                docbase, search);
        state.addChildNodeEntry(resultSetChildName, childNodeId);

        return state;
    }

    private InternalValue[] createInternalValue(String[] strings) {
        InternalValue[] values = new InternalValue[strings.length];
        for(int i=0; i<strings.length; i++) {
            values[i] = InternalValue.create(strings[i]);
        }
        return values;
    }

    @Override
    public NodeState populate(StateProviderContext context, HippoNodeId nodeId, NodeId parentId) throws RepositoryException {
        FacetSearchNodeId searchNodeId = (FacetSearchNodeId) nodeId;
        NodeState state = createNew(nodeId, virtualNodeName, parentId);
        state.setNodeTypeName(resolveName(HippoNodeType.NT_FACETSUBSEARCH));

        PropertyState propState = createNew(querynameName, nodeId);
        propState.setType(PropertyType.STRING);
        propState.setValues(new InternalValue[] { InternalValue.create(searchNodeId.queryname) });
        propState.setMultiValued(false);
        state.addPropertyName(querynameName);

        propState = createNew(docbaseName, nodeId);
        propState.setType(PropertyType.STRING);
        propState.setValues(new InternalValue[] { InternalValue.create(searchNodeId.docbase) });
        propState.setMultiValued(false);
        state.addPropertyName(docbaseName);

        propState = createNew(facetsName, nodeId);
        propState.setType(PropertyType.STRING);
        propState.setValues(createInternalValue(searchNodeId.facets));
        propState.setMultiValued(true);
        state.addPropertyName(facetsName);

        propState = createNew(searchName, nodeId);
        propState.setType(PropertyType.STRING);
        propState.setValues(createInternalValue(searchNodeId.search));
        propState.setMultiValued(true);
        state.addPropertyName(searchName);

        propState = createNew(countName, nodeId);
        propState.setType(PropertyType.LONG);
        propState.setValues(new InternalValue[] { InternalValue.create(searchNodeId.count) });
        propState.setMultiValued(false);
        state.addPropertyName(countName);

        return populate(context, state);
    }

    public class FacetSearchEntry implements Comparable<FacetSearchEntry> {
        protected String facetValue;
        protected Count count;
        public FacetSearchEntry(String facetValue, Count count) {
            this.facetValue = facetValue;
            this.count = count;
        }

        /**
         * This compareTo returns 0 only when count & facetValue are equal. 
         */
        @Override
        public int compareTo(FacetSearchEntry entry) {
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
            if (!(obj instanceof FacetSearchEntry)) {
                return false;
            }
            FacetSearchEntry other = (FacetSearchEntry) obj;
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
            return "FacetSearchEntry [facetValue=" + facetValue + ", count=" + count + "]";
        }
        
    }
}
