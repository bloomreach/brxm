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
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.hippoecm.hst.component.support.bean.persistency.BasePersistenceHstComponent;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.standard.HippoRequest;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.persistence.ContentPersistenceException;
import org.hippoecm.hst.persistence.workflow.WorkflowCallbackHandler;
import org.hippoecm.hst.persistence.workflow.WorkflowPersistenceManager;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoQuery;
import org.hippoecm.repository.reviewedactions.FullRequestWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TodoList extends BasePersistenceHstComponent {
    
    private static final Logger log = LoggerFactory.getLogger(Home.class);
    
    protected static final String DEFAULT_TODO_ITEMS_QUERY = "//*[jcr:primaryType='" + HippoNodeType.NT_REQUEST + "']";
    
    protected static final long DEFAULT_QUERY_LIMIT = 10;
    
    @Override
    public void doAction(HstRequest request, HstResponse response) throws HstComponentException {
        
        Session persistableSession = null;
        WorkflowPersistenceManager cpm = null;

        try {
            final String requestPath = request.getParameter("requestPath");
            final String requestType = request.getParameter("requestType");
            final String documentAction = request.getParameter("documentAction");
            
            if (requestPath == null || "".equals(requestPath)) {
                return;
            }
            
            if (!"publish".equals(requestType)) {
                return;
            }
            
            // retrieves writable session. NOTE: this session should be logged out manually!
            persistableSession = getPersistableSession(request);

            cpm = getWorkflowPersistenceManager(persistableSession);
            cpm.setWorkflowCallbackHandler(new WorkflowCallbackHandler<FullRequestWorkflow>() {
                public void processWorkflow(FullRequestWorkflow wf) throws Exception {
                    FullRequestWorkflow fraw = (FullRequestWorkflow) wf;
                    
                    if ("Accept".equals(documentAction)) {
                        fraw.acceptRequest();
                    } else if ("Reject".equals(documentAction)) {
                        fraw.cancelRequest();
                    }
                }
            });
            
            HippoRequest requestBean = (HippoRequest) cpm.getObject(requestPath);
            cpm.update(requestBean);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to process action.", e);
            } else if (log.isWarnEnabled()) {
                log.warn("Failed to process action. {}", e.toString());
            }
            
            if (cpm != null) {
                try {
                    cpm.refresh();
                } catch (ContentPersistenceException e1) {
                    log.warn("Failed to refresh: ", e);
                }
            }
        } finally {
            if (persistableSession != null) {
                persistableSession.logout();
            }
        }
    }
    
    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        
        try {
            String todoItemsQuery = DEFAULT_TODO_ITEMS_QUERY;
            long queryLimit = DEFAULT_QUERY_LIMIT;
            
            String param = getParameter("todoItemsQuery", request);
            
            if (param != null) {
                todoItemsQuery = param;
            }
            
            param = getParameter("queryLimit", request);
            
            if (param != null) {
                queryLimit = Long.parseLong(param);
            }
            
            List<HippoRequest> todoList = new ArrayList<HippoRequest>();
            
            Query query = request.getRequestContext().getSession().getWorkspace().getQueryManager().createQuery(todoItemsQuery, Query.XPATH);
            
            if (query instanceof HippoQuery) {
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
                        if (log.isDebugEnabled()) {
                            log.warn("Exception occurred during object converting.", e);
                        } else if (log.isWarnEnabled()) {
                            log.warn("Exception occurred during object converting. {}", e.toString());
                        }
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
