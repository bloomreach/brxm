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
package org.hippoecm.hst.content.beans.query;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.query.filter.IsNodeTypeFilter;
import org.hippoecm.hst.content.beans.query.filter.NodeTypeFilter;
import org.hippoecm.hst.content.beans.query.filter.PrimaryNodeTypeFilterImpl;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.LoggerFactory;


public class HstQueryManagerImpl implements HstQueryManager{

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstQueryManagerImpl.class);
    private ObjectConverter objectConverter;
    
    public HstQueryManagerImpl(ObjectConverter objectConverter){
        this.objectConverter = objectConverter;
    }
    
    /**
     * Creates a empty query, with scope
     */
    public HstQuery createQuery(HstRequestContext hstRequestContext, Node scope) throws QueryException {
        return createQuery(hstRequestContext, scope, (NodeTypeFilter)null);
    }
    
    /**
     * Creates a query, with scope  and Filter for types of filterBean. If includeSubTypes is <code>true</code>,
     * the result may also contain HippoBean's whose primarytype is a subtype of the filterBean type. 
     * 
     */
    public HstQuery createQuery(HstRequestContext hstRequestContext, Node scope, Class<? extends HippoBean> filterBean, boolean includeSubTypes) throws QueryException {
        if(!includeSubTypes) {
           return createQuery(hstRequestContext, scope, filterBean);
        }
        String primaryNodeTypeNameForBean = objectConverter.getPrimaryNodeTypeNameFor(filterBean);
        IsNodeTypeFilter isNodeTypeFilter = null;
        if(primaryNodeTypeNameForBean == null) {
          log.warn("Cannot find primaryNodeType for '{}'. Skipping filter bean", filterBean.getClass().getName());
        } else {
            isNodeTypeFilter = new IsNodeTypeFilter(primaryNodeTypeNameForBean);
        }
        return new HstQueryImpl(hstRequestContext, this.objectConverter, scope, isNodeTypeFilter);
    }
    
    /**
     * Creates a empty query, with the scope HippoBean
     */
    public HstQuery createQuery(HstRequestContext hstRequestContext, HippoBean scope) throws QueryException{
        if(scope.getNode() == null) {
            throw new QueryException("Cannot create a query for a detached HippoBean where the jcr node is null");
        }
        return createQuery(hstRequestContext, scope.getNode());
    }
    
    /**
     * Creates a query, with scope HippoBean and Filter for types of filterBean. If includeSubTypes is <code>true</code>,
     * the result may also contain HippoBean's whose primarytype is a subtype of the filterBean type. 
     * 
     */
    public HstQuery createQuery(HstRequestContext hstRequestContext, HippoBean scope, Class<? extends HippoBean> filterBean, boolean includeSubTypes) throws QueryException {
        if(scope.getNode() == null) {
            throw new QueryException("Cannot create a query for a detached HippoBean where the jcr node is null");
        }
        return createQuery(hstRequestContext, scope.getNode(), filterBean, includeSubTypes);
    }
    
    public HstQuery createQuery(HstRequestContext hstRequestContext, HippoBean scope, Class<? extends HippoBean>... filterBeans) throws QueryException{
        if(scope.getNode() == null) {
            throw new QueryException("Cannot create a query for a detached HippoBean where the jcr node is null");
        }
        return createQuery(hstRequestContext, scope.getNode(), filterBeans);
    }
    
    /**
     * Creates a query, with the scope HippoBean and with a Filter that filters to only return HippoBeans of the types that are 
     * added as variable arguments. It is not possible to retrieve subtypes of the applied filterBeans.
     */
    public HstQuery createQuery(HstRequestContext hstRequestContext, Node scope, Class<? extends HippoBean>... filterBeans) throws QueryException{
        if(scope == null) {
            throw new QueryException("Cannot create a query for a detached HippoBean where the jcr node is null");
        }
        
        List<String> primaryNodeTypes = new ArrayList<String>(); 
        for(Class<? extends HippoBean> annotatedBean : filterBeans) {
           String primaryNodeTypeNameForBean = objectConverter.getPrimaryNodeTypeNameFor(annotatedBean);
           if(primaryNodeTypeNameForBean != null) {
               primaryNodeTypes.add(primaryNodeTypeNameForBean);
           } else {
               log.warn("Cannot find primaryNodeType for '{}'. Skipping filter bean", annotatedBean.getClass().getName());
           }
        }
        NodeTypeFilter primaryNodeTypeFilter = null;
        if(primaryNodeTypes.size() > 0) {
            primaryNodeTypeFilter  = new PrimaryNodeTypeFilterImpl(primaryNodeTypes.toArray(new String[primaryNodeTypes.size()]));
        }
        return createQuery(hstRequestContext, scope, primaryNodeTypeFilter);
    }

    private HstQuery createQuery(HstRequestContext hstRequestContext, Node scope, NodeTypeFilter filter) throws QueryException {
        return new HstQueryImpl(hstRequestContext, this.objectConverter, scope, filter);
    }
    
 
}
