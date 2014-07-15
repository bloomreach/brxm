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
package org.hippoecm.hst.content.beans.query;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;

import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.query.filter.IsNodeTypeFilter;
import org.hippoecm.hst.content.beans.query.filter.NodeTypeFilter;
import org.hippoecm.hst.content.beans.query.filter.PrimaryNodeTypeFilterImpl;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.repository.util.DateTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HstQueryManagerImpl implements HstQueryManager {

    private static final Logger log = LoggerFactory.getLogger(HstQueryManagerImpl.class);

    private final ObjectConverter objectConverter;
    private final Session session;
    private final DateTools.Resolution defaultResolution;

    /**
     * @deprecated since 2.24.13 / 2.16.01. Use {@link #HstQueryManagerImpl(Session, ObjectConverter, DateTools.Resolution)}
     * instead
     */
    @Deprecated
    public HstQueryManagerImpl(final Session session, final ObjectConverter objectConverter) {
        this(session, objectConverter, DateTools.Resolution.MILLISECOND);
        log.warn("Using deprecated HstQueryManagerImpl constructor. No Filter.Resolution is specified. Use default" +
                " Filter.Resolution.EXPENSIVE_PRECISE");
    }

    public HstQueryManagerImpl(final Session session,
                               final ObjectConverter objectConverter,
                               final DateTools.Resolution resolution) {
        this.session = session;
        this.objectConverter = objectConverter;
        defaultResolution = resolution;
    }


    public HstQuery createQuery(final Node scope) throws QueryException {
        return createQuery(scope, (NodeTypeFilter)null);
    }
    
    /**
     * Creates a query, with scope  and Filter for types of filterBean. If includeSubTypes is <code>true</code>,
     * the result may also contain HippoBean's whose primarytype is a subtype of the filterBean type. 
     * 
     */
    @SuppressWarnings("unchecked")
    public HstQuery createQuery(final Node scope,
                                final Class<? extends HippoBean> filterBean,
                                final boolean includeSubTypes) throws QueryException {
        if(!includeSubTypes) {
           return createQuery(scope, filterBean);
        }
        String primaryNodeTypeNameForBean = objectConverter.getPrimaryNodeTypeNameFor(filterBean);
        IsNodeTypeFilter isNodeTypeFilter = null;
        if(primaryNodeTypeNameForBean == null) {
          throw new QueryException("Cannot find primaryNodeType for '"+filterBean.getName()+"'.");
        } else {
            isNodeTypeFilter = new IsNodeTypeFilter(primaryNodeTypeNameForBean);
        }
        return new HstQueryImpl(session, this.objectConverter, scope, isNodeTypeFilter);
    }
    
   
    /**
     * Creates a empty query, with the scope HippoBean
     */
    public HstQuery createQuery(final HippoBean scope) throws QueryException{
        if(scope == null || scope.getNode() == null) {
            return createQuery((Node)null);
        }
        return createQuery(scope.getNode());
    }
    
 
    
    /**
     * Creates a query, with scope HippoBean and Filter for types of filterBean. If includeSubTypes is <code>true</code>,
     * the result may also contain HippoBean's whose primarytype is a subtype of the filterBean type. 
     * 
     */
    public HstQuery createQuery(final HippoBean scope,
                                final Class<? extends HippoBean> filterBean,
                                final boolean includeSubTypes) throws QueryException {
        if(scope.getNode() == null) {
            return createQuery((Node)null, filterBean, includeSubTypes);
        }
        return createQuery(scope.getNode(), filterBean, includeSubTypes);
    }

    public HstQuery createQuery(final HippoBean scope,
                                final Class<? extends HippoBean>... filterBeans) throws QueryException{
        return createQuery(scope, false, filterBeans);
    }

    public HstQuery createQuery(final HippoBean scope,
                                final boolean includeSubTypes,
                                final Class<? extends HippoBean>... filterBeans) throws QueryException{
        if(scope.getNode() == null) {
            return createQuery((Node)null, includeSubTypes, filterBeans);
        }
        return createQuery(scope.getNode(), includeSubTypes,  filterBeans);
    }

    /**
     * Creates a query, with the scope HippoBean and with a Filter that filters to only return HippoBeans of the types that are 
     * added as variable arguments. It is not possible to retrieve subtypes of the applied filterBeans.
     */
    public HstQuery createQuery(final Node scope, final Class<? extends HippoBean>... filterBeans) throws QueryException{
        return createQuery(scope, false, filterBeans);
    }

    public HstQuery createQuery(final Node scope,
                                final boolean includeSubTypes,
                                final Class<? extends HippoBean>... filterBeans) throws QueryException {
        Set<String> primaryNodeTypes = new LinkedHashSet<>();
        for(Class<? extends HippoBean> annotatedBean : filterBeans) {
            String primaryNodeTypeNameForBean = objectConverter.getPrimaryNodeTypeNameFor(annotatedBean);
            if(primaryNodeTypeNameForBean != null) {
                if (includeSubTypes) {
                    try {
                        Set<String> subNodeTypes = getSubTypes(primaryNodeTypeNameForBean);
                        primaryNodeTypes.addAll(subNodeTypes);
                    } catch (RepositoryException e) {
                        throw new QueryException("RepositoryException while getting sub types",e);
                    }
                }
                primaryNodeTypes.add(primaryNodeTypeNameForBean);
            } else {
                throw new QueryException("Cannot find primaryNodeType for '"+annotatedBean.getClass().getName()+"'.");
            }
        }
        NodeTypeFilter primaryNodeTypeFilter = null;
        if(primaryNodeTypes.size() > 0) {
            primaryNodeTypeFilter  = new PrimaryNodeTypeFilterImpl(primaryNodeTypes.toArray(new String[primaryNodeTypes.size()]));
        }
        return createQuery(scope, primaryNodeTypeFilter);
    }


    public HstQuery createQuery(final Node scope,
                                final String nodeType,
                                final boolean includeSubTypes) throws QueryException {
        if (nodeType == null) {
            throw new IllegalArgumentException("The node type for query must not be null!");
        }
        
        if (!includeSubTypes) {
            return createQuery(scope, nodeType);
        }
        
        IsNodeTypeFilter isNodeTypeFilter = new IsNodeTypeFilter(nodeType);
        HstQueryImpl query = new HstQueryImpl(session, this.objectConverter, scope, isNodeTypeFilter);
        query.setDefaultResolution(defaultResolution);
        return query;
    }

    public HstQuery createQuery(final HippoBean scope,
                                final String... primaryNodeTypes) throws QueryException {
        if (scope.getNode() == null) {
            return createQuery((Node) null, primaryNodeTypes);
        }

        return createQuery(scope.getNode(), primaryNodeTypes);
    }

    public HstQuery createQuery(final HippoBean scope,
                                final boolean includeSubTypes,
                                final String... primaryNodeTypes) throws QueryException {
        if (scope.getNode() == null) {
            return createQuery((Node) null, includeSubTypes, primaryNodeTypes);
        }

        return createQuery(scope.getNode(), includeSubTypes, primaryNodeTypes);
    }
    
    public HstQuery createQuery(final Node scope,
                                final String ... primaryNodeTypes) throws QueryException {
        return createQuery(scope, false, primaryNodeTypes);
    }

    public HstQuery createQuery(final Node scope,
                                final boolean includeSubTypes,
                                final String ... primaryNodeTypes) throws QueryException {
        if (primaryNodeTypes == null) {
            throw new IllegalArgumentException("Primary node types for query must not be null!");
        }
        Set<String> primaryNodeTypesSet = new LinkedHashSet<>();
        for (String primaryNodeType : primaryNodeTypes) {
            primaryNodeTypesSet.add(primaryNodeType);
            if (includeSubTypes) {
                try {
                    Set<String> subNodeTypes = getSubTypes(primaryNodeType);
                    primaryNodeTypesSet.addAll(subNodeTypes);
                } catch (RepositoryException e) {
                    throw new QueryException("RepositoryException while getting sub types",e);
                }
            }
        }
        NodeTypeFilter primaryNodeTypeFilter  = new PrimaryNodeTypeFilterImpl(primaryNodeTypesSet.toArray(new String[primaryNodeTypesSet.size()]));
        return createQuery(scope, primaryNodeTypeFilter);
    }

    
    private HstQuery createQuery(Node scope, NodeTypeFilter filter) throws QueryException {
        HstQueryImpl query  = new HstQueryImpl(session, this.objectConverter, scope, filter);
        query.setDefaultResolution(defaultResolution);
        return query;
    }


    private Set<String> getSubTypes(final String primaryNodeType) throws RepositoryException {
        Set<String> subTypes = new LinkedHashSet<>();
        NodeTypeManager ntMgr = session.getWorkspace().getNodeTypeManager();
        NodeTypeIterator allTypes = ntMgr.getAllNodeTypes();
        while (allTypes.hasNext()) {
            NodeType nt = allTypes.nextNodeType();
            if (nt.isNodeType(primaryNodeType)) {
                subTypes.add(nt.getName());
            }
        }
        return subTypes;
    }
    
}
