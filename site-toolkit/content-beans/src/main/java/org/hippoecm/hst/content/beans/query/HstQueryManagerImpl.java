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
import org.hippoecm.hst.content.beans.query.filter.NodeTypeFilter;
import org.hippoecm.hst.content.beans.query.filter.PrimaryNodeTypeFilterImpl;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.request.HstRequestContext;


public class HstQueryManagerImpl implements HstQueryManager{

    private ObjectConverter objectConverter;
    
    public HstQueryManagerImpl(ObjectConverter objectConverter){
        this.objectConverter = objectConverter;
    }
    
    public HstQuery createQuery(HstRequestContext hstRequestContext, Node scope) throws QueryException {
        return createQuery(hstRequestContext, scope, null);
    }
    
    public HstQuery createQuery(HstRequestContext hstRequestContext, HippoBean scope) throws QueryException{
        if(scope.getNode() == null) {
            throw new QueryException("Cannot create a query for a detached HippoBean where the jcr node is null");
        }
        return createQuery(hstRequestContext, scope.getNode());
    }
    
    public HstQuery createQuery(HstRequestContext hstRequestContext, HippoBean scope, Class<? extends HippoBean>... filterBeans) throws QueryException{
        if(scope.getNode() == null) {
            throw new QueryException("Cannot create a query for a detached HippoBean where the jcr node is null");
        }
        
        List<String> primaryNodeTypes = new ArrayList<String>(); 
        for(Class<? extends HippoBean> annotatedBean : filterBeans) {
           String primaryNodeTypeNameForBean = objectConverter.getPrimaryNodeTypeNameFor(annotatedBean);
           if(primaryNodeTypeNameForBean != null) {
               primaryNodeTypes.add(primaryNodeTypeNameForBean);
           }
        }
        NodeTypeFilter primaryNodeTypeFilter = null;
        if(primaryNodeTypes.size() > 0) {
            primaryNodeTypeFilter  = new PrimaryNodeTypeFilterImpl(primaryNodeTypes.toArray(new String[primaryNodeTypes.size()]));
        }
        return createQuery(hstRequestContext, scope.getNode(), primaryNodeTypeFilter);
    }

    private HstQuery createQuery(HstRequestContext hstRequestContext, Node scope, NodeTypeFilter filter) throws QueryException {
        return new HstQueryImpl(hstRequestContext, this.objectConverter, scope, filter);
    }
    
 
}
