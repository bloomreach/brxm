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

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.beans.TextPage;
import org.hippoecm.hst.component.support.bean.persistency.BaseHstComponent;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.persistence.ContentPersistenceException;
import org.hippoecm.hst.persistence.ContentPersistenceManager;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Detail extends BaseHstComponent {
    
    private static Logger log = LoggerFactory.getLogger(Detail.class);
    
    private final static String DEFAULT_WRITABLE_USERNAME_PROPERTY = "writable.repository.user.name";
    private final static String DEFAULT_WRITABLE_PASSWORD_PROPERTY = "writable.repository.password";
    
    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        
        HippoBean document = getContentBean(request);
        
        if (document == null) {
            return;
        }
        
        request.setAttribute("parent", document.getParentBean());
        request.setAttribute("document", document);
        
        
        // retrieves comments folder bean for this document
        String siteContentBasePath = getSiteContentBasePath(request);
        String documentRelPath = PathUtils.normalizePath(document.getPath());
        if (documentRelPath.startsWith(siteContentBasePath)) {
            documentRelPath = documentRelPath.substring(siteContentBasePath.length() + 1);
        }
        String commentsFolderNodePath = "/" + siteContentBasePath + "/comments/" + documentRelPath;
        
        try {
            HippoFolderBean commentsFolderBean = (HippoFolderBean) getObjectBeanManager(request).getObject(commentsFolderNodePath);
            if (commentsFolderBean != null) {
                request.setAttribute("commentsFolder", commentsFolderBean);
            }
        } catch (ObjectBeanManagerException e) {
            if (log.isWarnEnabled()) {
                log.warn("Failed to retrieve comments folder for {}: {}", commentsFolderNodePath, e);
            }
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
                    // Retrieve writable session. NOTE: this session should be logged out manually!
                    persistableSession = getPersistableSession(request);
                    boolean requestPublishingAfterUpdate = true;
                    // create ContentPersistenceManager with request-publishing-option
                    cpm = getContentPersistenceManager(persistableSession, requestPublishingAfterUpdate);
                    
                    // retrieve the content news bean and its relative path
                    HippoBean document = getContentBean(request);
                    String siteContentBasePath = getSiteContentBasePath(request);
                    String documentRelPath = PathUtils.normalizePath(document.getPath());
                    if (documentRelPath.startsWith(siteContentBasePath)) {
                        documentRelPath = documentRelPath.substring(siteContentBasePath.length() + 1);
                    }
                    // retrieve the physical path of the site content base node.
                    String siteContentBasePhysicalPath = getSiteContentBasePhysicalPath(request);
                    
                    // calculates comments folder node path for this news document.
                    String documentCommentsFolderNodePath = siteContentBasePhysicalPath + "/comments/" + documentRelPath;
                    // comment node name is simply a concatenation of 'comment-' and current time millis. 
                    String commentNodeName = "comment-" + System.currentTimeMillis();
                    // calculates comment node path
                    String commentNodeAbsPath = documentCommentsFolderNodePath + "/" + commentNodeName;
                    
                    // create comment node now
                    cpm.create(documentCommentsFolderNodePath, "testproject:textpage", commentNodeName, true);
    
                    // retrieve the comment content to manipulate
                    TextPage commentPage = (TextPage) cpm.getObject(commentNodeAbsPath);
                    // update content properties
                    commentPage.setTitle(title);
                    commentPage.setBodyContent(comment);
                    
                    // update now
                    cpm.update(commentPage);
                    
                    // save the pending changes
                    cpm.save();
                } catch (Exception e) {
                    if (log.isDebugEnabled()) {
                        log.warn("Failed to create a comment.", e);
                    } else if (log.isWarnEnabled()) {
                        log.warn("Failed to create a comment. {}", e);
                    }
                    
                    if (cpm != null) {
                        try {
                            cpm.reset();
                        } catch (ContentPersistenceException e1) {
                            if (log.isWarnEnabled()) log.warn("Failed to reset. {}", e);
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
                    if (log.isDebugEnabled()) {
                        log.warn("Failed to create a comment.", e);
                    } else if (log.isWarnEnabled()) {
                        log.warn("Failed to create a comment. {}", e);
                    }
                    
                    if (cpm != null) {
                        try {
                            cpm.reset();
                        } catch (ContentPersistenceException e1) {
                            if (log.isWarnEnabled()) log.warn("Failed to reset. {}", e);
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
    
    protected Session getPersistableSession(HstRequest request) {
        Session persistableSession = null;
        
        HstRequestContext requestContext = request.getRequestContext();
        ContainerConfiguration config = requestContext.getContainerConfiguration();
        
        String username = config.getString(DEFAULT_WRITABLE_USERNAME_PROPERTY);
        String password = config.getString(DEFAULT_WRITABLE_PASSWORD_PROPERTY);
        
        if (username == null || password == null) {
            if (log.isWarnEnabled()) {
                log.warn("Cannot retrieve a writable user for '{}'", DEFAULT_WRITABLE_USERNAME_PROPERTY);
            }
        } else {
            try {
                persistableSession = requestContext.getSession().impersonate(new SimpleCredentials(username, password.toCharArray()));
            } catch (RepositoryException e) {
                if (log.isWarnEnabled()) {
                    log.warn("Cannot impersonate a session to user '{}'", username);
                }
            }
        }
        
        return persistableSession;
    }
}


  
