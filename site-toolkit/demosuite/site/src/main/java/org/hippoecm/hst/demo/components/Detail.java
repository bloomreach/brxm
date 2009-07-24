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

import java.util.Calendar;
import java.util.List;

import javax.jcr.Session;

import org.hippoecm.hst.component.support.bean.persistency.BasePersistenceHstComponent;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.demo.beans.BaseBean;
import org.hippoecm.hst.demo.beans.CommentBean;
import org.hippoecm.hst.persistence.ContentPersistenceException;
import org.hippoecm.hst.persistence.workflow.WorkflowCallbackHandler;
import org.hippoecm.hst.persistence.workflow.WorkflowPersistenceManager;
import org.hippoecm.hst.utils.BeanUtils;
import org.hippoecm.repository.reviewedactions.FullReviewedActionsWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Detail extends BasePersistenceHstComponent {

    public static final Logger log = LoggerFactory.getLogger(Detail.class);

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {

        super.doBeforeRender(request, response);
        HippoBean crBean = this.getContentBean(request);

        if (crBean == null || !(crBean instanceof BaseBean)) {
            return;
        }
        request.setAttribute("document", crBean);
        
        try {
            HstQuery commentQuery = BeanUtils.createIncomingBeansQuery((BaseBean)crBean, "demosite:commentlink/@hippo:docbase", request, this , CommentBean.class, false);
            commentQuery.addOrderByDescending("demosite:date");
            commentQuery.setLimit(15);
            List<CommentBean> comments = BeanUtils.getIncomingBeans(commentQuery, CommentBean.class);
            request.setAttribute("comments", comments);
        } catch (QueryException e) {
            e.printStackTrace();
        }
        
    }

    @Override
    public void doAction(HstRequest request, HstResponse response) throws HstComponentException {
        String type = request.getParameter("type");

        if ("add".equals(type)) {
            String title = request.getParameter("title");
            String comment = request.getParameter("comment");
            HippoBean commentTo = this.getContentBean(request);
            if( !(commentTo instanceof HippoDocumentBean)) {
                log.warn("Cannot comment on non documents");
                return;
            }
            String commentToUuidOfHandle = ((HippoDocumentBean)commentTo).getCanonicalHandleUUID();
            if (title != null && !"".equals(title.trim()) && comment != null) {
                Session persistableSession = null;
                WorkflowPersistenceManager cpm = null;

                try {
                    // retrieves writable session. NOTE: this session should be logged out manually!
                    persistableSession = getPersistableSession(request);
                    cpm = getWorkflowPersistenceManager(persistableSession);
                    cpm.setWorkflowCallbackHandler(new WorkflowCallbackHandler<FullReviewedActionsWorkflow>() {
                        public void processWorkflow(FullReviewedActionsWorkflow wf) throws Exception {
                            FullReviewedActionsWorkflow fraw = (FullReviewedActionsWorkflow) wf;
                            fraw.requestPublication();
                        }
                    });

                    // it is not important where we store comments. WE just use some time path below our project content
                    String siteContentBasePath = getSiteContentBasePath(request);
                    Calendar currentDate = Calendar.getInstance();
                    
                    String commentsFolderPath = "/"+siteContentBasePath + "/comment/" + currentDate.get(Calendar.YEAR) + "/"
                            + currentDate.get(Calendar.MONTH) + "/" + currentDate.get(Calendar.DAY_OF_MONTH);
                    // comment node name is simply a concatenation of 'comment-' and current time millis. 
                    String commentNodeName = "comment-" + System.currentTimeMillis();

                    // create comment node now
                    cpm.create(commentsFolderPath, "demosite:comment", commentNodeName, true);

                    // retrieve the comment content to manipulate
                    CommentBean commentBean = (CommentBean) cpm.getObject(commentsFolderPath + "/" + commentNodeName);
                    // update content properties
                    if(commentBean == null) {
                        throw new HstComponentException("Failed to add Comment");
                    }
                    commentBean.setTitle(title);

                    commentBean.setHtml(comment);
                    
                    commentBean.setDate(currentDate);
                    
                    commentBean.setCommentTo(commentToUuidOfHandle);
                    
                    // update now
                    cpm.update(commentBean);

                    
                } catch (Exception e) {
                    log.warn("Failed to create a comment: ", e);

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
        } else if ("remove".equals(type)) {
        }
    }
}