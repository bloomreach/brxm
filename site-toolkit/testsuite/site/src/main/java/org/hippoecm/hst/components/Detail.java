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
package org.hippoecm.hst.components;

import java.util.Collections;
import java.util.List;

import javax.jcr.Session;

import org.hippoecm.hst.beans.TextPage;
import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.ObjectBeanPersistenceException;
import org.hippoecm.hst.content.beans.manager.ObjectBeanPersistenceManager;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowCallbackHandler;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManager;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.util.PathUtils;
import org.hippoecm.repository.reviewedactions.FullReviewedActionsWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Detail extends BaseHstComponent {
    
    private static Logger log = LoggerFactory.getLogger(Detail.class);
    
    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        
        HippoBean document = getContentBean(request);
        
        if (document == null) {
            return;
        }
        
        request.setAttribute("parent", document.getParentBean());
        request.setAttribute("document", document);
        
        List<HippoDocumentBean> commentList = null;
            
        try {
            String commentsFolderNodePath = getCommentsFolderPath(request);
            HippoFolderBean commentsFolderBean = (HippoFolderBean) getObjectBeanManager(request).getObject(commentsFolderNodePath);
            
            if (commentsFolderBean != null) {
                commentList = commentsFolderBean.getDocuments();
            }
            
        } catch (ObjectBeanManagerException e) {
            log.warn("Failed to retrieve comments folder for {}: {}", document.getPath(), e);
        }
        
        if (commentList == null) {
            commentList = Collections.emptyList();
        }
        
        request.setAttribute("comments", commentList);
    }

    @Override
    public void doAction(HstRequest request, HstResponse response) throws HstComponentException {
        String type = request.getParameter("type");
        
        if ("add".equals(type)) {
            String title = request.getParameter("title");
            String comment = request.getParameter("comment");
            
            if (title != null && !"".equals(title.trim()) && comment != null) {
                Session persistableSession = null;
                WorkflowPersistenceManager wpm = null;
                
                try {
                    // retrieves writable session. NOTE: this session should be logged out manually!
                    persistableSession = getPersistableSession(request);
                    
                    wpm = getWorkflowPersistenceManager(persistableSession);
                    wpm.setWorkflowCallbackHandler(new WorkflowCallbackHandler<FullReviewedActionsWorkflow>() {
                        public void processWorkflow(FullReviewedActionsWorkflow wf) throws Exception {
                            FullReviewedActionsWorkflow fraw = (FullReviewedActionsWorkflow) wf;
                            fraw.requestPublication();
                        }
                    });
                    
                    // retrieve comments folder path
                    String commentsFolderPath = getCommentsFolderPath(request);
                    // comment node name is simply a concatenation of 'comment-' and current time millis. 
                    String commentNodeName = "comment-" + System.currentTimeMillis();
                    
                    // create comment node now
                    wpm.create(commentsFolderPath, "testproject:textpage", commentNodeName, true);
    
                    // retrieve the comment content to manipulate
                    TextPage commentPage = (TextPage) wpm.getObject(commentsFolderPath + "/" + commentNodeName);
                    // update content properties
                    commentPage.setTitle(title);
                    commentPage.setBodyContent(comment);
                    
                    // update now
                    wpm.update(commentPage);
                    
                    // save the pending changes
                    wpm.save();
                } catch (Exception e) {
                    log.warn("Failed to create a comment.", e);
                    
                    if (wpm != null) {
                        try {
                            wpm.refresh();
                        } catch (ObjectBeanPersistenceException e1) {
                            log.warn("Failed to reset. {}", e);
                        }
                    }
                } finally {
                    if (persistableSession != null) {
                        persistableSession.logout();
                    }
                }
            }
        } else if ("remove".equals(type)) {
            String commentPath = request.getParameter("path");
            
            if (commentPath != null) {
                Session persistableSession = null;
                ObjectBeanPersistenceManager cpm = null;
                
                try {
                    // Retrieve writable session. NOTE: this session should be logged out manually!
                    persistableSession = getPersistableSession(request);
                    // create ContentPersistenceManager with request-publishing-option
                    cpm = getWorkflowPersistenceManager(persistableSession);
                    
                    TextPage commentPage = (TextPage) cpm.getObject(commentPath);
                    
                    cpm.remove(commentPage);
                } catch (Exception e) {
                    log.warn("Failed to create a comment.", e);
                    
                    if (cpm != null) {
                        try {
                            cpm.refresh();
                        } catch (ObjectBeanPersistenceException e1) {
                            log.warn("Failed to reset. {}", e);
                        }
                    }
                } finally {
                    if (persistableSession != null) {
                        persistableSession.logout();
                    }
                }
            }
        }
    }
    
    /**
     * Returns physical comments folder node path.
     * @param request
     * @return
     */
    protected String getCommentsFolderPath(HstRequest request) {
        HippoBean document = getContentBean(request);
        String siteContentBasePath = getSiteContentBasePath(request);
        String documentRelPath = PathUtils.normalizePath(document.getPath());
        
        if (documentRelPath.startsWith(siteContentBasePath)) {
            documentRelPath = documentRelPath.substring(siteContentBasePath.length() + 1);
        }
        
        return "/" + getSiteContentBasePath(request) + "/comments/" + documentRelPath;
    }
    
}


  
