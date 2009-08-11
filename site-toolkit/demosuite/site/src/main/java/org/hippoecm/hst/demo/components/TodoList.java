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
package org.hippoecm.hst.demo.components;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.standard.HippoRequest;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TodoList extends BaseHstComponent {
    
    private static final Logger log = LoggerFactory.getLogger(Home.class);
    
    protected static final String REQUEST_ITEMS_QUERY = "//*[jcr:primaryType='" + HippoNodeType.NT_REQUEST + "']";
    
    protected long queryLimit = 0L; 
    
    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        
        try {
            List<HippoRequest> todoList = new ArrayList<HippoRequest>();

            Query query = request.getRequestContext().getSession().getWorkspace().getQueryManager().createQuery(REQUEST_ITEMS_QUERY, Query.XPATH);
            
            if (query instanceof HippoQuery) {
                if (queryLimit == 0L) {
                    String param = getParameter("queryLimit", request);
                    
                    if (param != null) {
                        queryLimit = Integer.parseInt(param);
                    } else {
                        queryLimit = 10L;
                    }
                }
                
                ((HippoQuery) query).setLimit(queryLimit);
            }
            
            QueryResult result = query.execute();
            
            for (NodeIterator nodeIt = result.getNodes(); nodeIt.hasNext(); ) {
                Node requestNode = nodeIt.nextNode();
                
                if (requestNode != null) {
                    try {
                        HippoRequest requestBean = (HippoRequest) getObjectConverter().getObject(requestNode);
                        todoList.add(requestBean);
                    }  catch (ObjectBeanManagerException e) {
                        e.printStackTrace();
                    }
                }
            }
            
            request.setAttribute("todoList", todoList);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to query request items.", e);
            } else if (log.isWarnEnabled()) {
                log.warn("Failed to query request items. {}", e.toString());
            }
        }
        
    }

}
