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

import javax.jcr.Session;

import org.hippoecm.hst.beans.TextPage;
import org.hippoecm.hst.component.support.bean.persistency.BaseHstComponent;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.persistence.ContentPersistenceException;
import org.hippoecm.hst.persistence.ContentPersistenceManager;
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
        
        try {
            String commentsFolderNodePath = "/" + getSiteContentBasePath(request) + "/comments/" + getContentRelativePath(request, document.getPath());
            HippoFolderBean commentsFolderBean = (HippoFolderBean) getObjectBeanManager(request).getObject(commentsFolderNodePath);
            request.setAttribute("commentsFolder", commentsFolderBean);
        } catch (ObjectBeanManagerException e) {
            log.warn("Failed to retrieve comments folder for {}: {}", document.getPath(), e);
        }
    }

    @Override
    public void doAction(HstRequest request, HstResponse response) throws HstComponentException {
        String type = request.getParameter("type");
        
        if ("add".equals(type)) {
            String title = request.getParameter("title");
            String comment = request.getParameter("comment");
            
            if (title != null && !"".equals(title.trim()) && comment != null) {
                Session persistableSession = null;
                ContentPersistenceManager cpm = null;
                
                try {
                    // retrieves writable session. NOTE: this session should be logged out manually!
                    persistableSession = getPersistableSession(request);
                    
                    boolean requestPublishingAfterUpdate = true;
                    // create ContentPersistenceManager with request-publishing-option
                    cpm = getContentPersistenceManager(persistableSession, requestPublishingAfterUpdate);
                    
                    // retrieve comments folder path
                    String commentsFolderPath = getCommentsFolderPath(request);
                    // comment node name is simply a concatenation of 'comment-' and current time millis. 
                    String commentNodeName = "comment-" + System.currentTimeMillis();
                    
                    // create comment node now
                    cpm.create(commentsFolderPath, "testproject:textpage", commentNodeName, true);
    
                    // retrieve the comment content to manipulate
                    TextPage commentPage = (TextPage) cpm.getObject(commentsFolderPath + "/" + commentNodeName);
                    // update content properties
                    commentPage.setTitle(title);
                    commentPage.setBodyContent(comment);
                    
                    // update now
                    cpm.update(commentPage);
                    
                    // save the pending changes
                    cpm.save();
                } catch (Exception e) {
                    log.warn("Failed to create a comment. {}", e);
                    
                    if (cpm != null) {
                        try {
                            cpm.refresh();
                        } catch (ContentPersistenceException e1) {
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
                ContentPersistenceManager cpm = null;
                
                try {
                    // Retrieve writable session. NOTE: this session should be logged out manually!
                    persistableSession = getPersistableSession(request);
                    boolean requestPublishingAfterUpdate = true;
                    // create ContentPersistenceManager with request-publishing-option
                    cpm = getContentPersistenceManager(persistableSession, requestPublishingAfterUpdate);
                    
                    TextPage commentPage = (TextPage) cpm.getObject(commentPath);
                    
                    cpm.remove(commentPage);
                } catch (Exception e) {
                    log.warn("Failed to create a comment. {}", e);
                    
                    if (cpm != null) {
                        try {
                            cpm.refresh();
                        } catch (ContentPersistenceException e1) {
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
        String documentRelPath = getContentRelativePath(request, document.getPath());
        // retrieve the physical path of the site content base node.
        String siteContentBasePhysicalPath = getSiteContentBasePhysicalPath(request);
        // calculates comments folder node path for this news document.
        return siteContentBasePhysicalPath + "/comments/" + documentRelPath;
    }
    
}


  
