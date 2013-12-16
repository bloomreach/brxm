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
package org.hippoecm.hst.demo.components;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.ObjectBeanPersistenceException;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowCallbackHandler;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManager;
import org.hippoecm.hst.content.beans.standard.HippoRequestBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.repository.api.HippoQuery;
import org.hippoecm.repository.reviewedactions.FullRequestWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TodoList extends BaseHstComponent {

    private static final Logger log = LoggerFactory.getLogger(Home.class);

    protected static final String DEFAULT_TODO_ITEMS_QUERY = "//element(*, hippostdpubwf:request)";

    protected static final long DEFAULT_QUERY_LIMIT = 10;

    @Override
    public void doAction(HstRequest request, HstResponse response) throws HstComponentException {

        Session persistableSession = null;
        WorkflowPersistenceManager wpm = null;

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

            wpm = getWorkflowPersistenceManager(persistableSession);
            wpm.setWorkflowCallbackHandler(new WorkflowCallbackHandler<FullRequestWorkflow>() {
                public void processWorkflow(FullRequestWorkflow wf) throws Exception {
                    FullRequestWorkflow fraw = (FullRequestWorkflow) wf;

                    if ("Accept".equals(documentAction)) {
                        fraw.acceptRequest();
                    } else if ("Reject".equals(documentAction)) {
                        fraw.cancelRequest();
                    }
                }
            });

            HippoRequestBean requestBean = (HippoRequestBean) wpm.getObject(requestPath);
            wpm.update(requestBean);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to process action.", e);
            } else if (log.isWarnEnabled()) {
                log.warn("Failed to process action. {}", e.getMessage());
            }

            if (wpm != null) {
                try {
                    wpm.refresh();
                } catch (ObjectBeanPersistenceException e1) {
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
        super.doBeforeRender(request, response);
        try {


            String todoItemsQuery = DEFAULT_TODO_ITEMS_QUERY;
            long queryLimit = DEFAULT_QUERY_LIMIT;

            String param = getComponentParameter("todoItemsQuery");

            if (param != null) {
                todoItemsQuery = param;
            }

            param = getComponentParameter("queryLimit");

            if (param != null) {
                queryLimit = Long.parseLong(param);
            }

            List<HippoRequestBean> todoList = new ArrayList<HippoRequestBean>();

            Query query = request.getRequestContext().getSession().getWorkspace().getQueryManager().createQuery(todoItemsQuery, Query.XPATH);

            if (query instanceof HippoQuery) {
                ((HippoQuery) query).setLimit(queryLimit);
            }

            QueryResult result = query.execute();

            for (NodeIterator nodeIt = result.getNodes(); nodeIt.hasNext();) {
                Node requestNode = nodeIt.nextNode();

                if (requestNode != null) {
                    try {
                        HippoRequestBean requestBean = (HippoRequestBean) RequestContextProvider.get().getContentBeansTool().getObjectConverter().getObject(requestNode);
                        todoList.add(requestBean);
                    } catch (ObjectBeanManagerException e) {
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
