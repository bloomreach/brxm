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

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.beans.TextPage;
import org.hippoecm.hst.component.support.bean.persistency.BaseHstComponent;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.persistence.ContentNodeBinder;
import org.hippoecm.hst.persistence.ContentPersistenceBindingException;
import org.hippoecm.hst.persistence.ContentPersistenceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Detail extends BaseHstComponent {
    
    private static Logger log = LoggerFactory.getLogger(Detail.class);
    
    private final static String DEFAULT_WRITABLE_USERNAME_PROPERTY = "writable.repository.user.name";
    private final static String DEFAULT_WRITABLE_PASSWORD_PROPERTY = "writable.repository.password";
    
    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        
        HippoBean  n = this.getContentBean(request);
        
        if (n == null) {
            return;
        }
        
        request.setAttribute("parent", n.getParentBean());
        request.setAttribute("document",n);
    }

    @Override
    public void doAction(HstRequest request, HstResponse response) throws HstComponentException {
        String title = request.getParameter("title");
        String comment = request.getParameter("comment");
        
        if (title != null && !"".equals(title.trim()) && comment != null) {
            try {
                Session persistableSession = getPersistableSession(request);
                boolean requestPublishingAfterUpdate = true;
                ContentPersistenceManager cpm = getContentPersistenceManager(persistableSession, requestPublishingAfterUpdate);
                
                HippoBean document = getContentBean(request);
                List<HippoFolderBean> commentsFolderList = document.getChildBeansByName("comments");
                
                if (commentsFolderList.isEmpty()) {
                    cpm.create(document.getPath(), "hippostd:folder", "comments");
                }
                
                cpm.create(document.getPath() + "/comments", "testproject:textpage", title);

                TextPage commentPage = (TextPage) cpm.getObject(document.getPath() + "/comments" + "/" + title);
                commentPage.setTitle(title);
                commentPage.setSummary(comment);
                
                cpm.update(commentPage, new ContentNodeBinder() {
                    public void bind(Object content, Node node) throws ContentPersistenceBindingException {
                        try {
                            TextPage commentPage = (TextPage) content;
                            node.setProperty("testproject:title", commentPage.getTitle());
                            node.setProperty("testproject:summary", commentPage.getSummary());
                        } catch (Exception e) {
                            throw new ContentPersistenceBindingException(e);
                        }
                    }
                });
                
                cpm.save();
                
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.warn("Failed to create a comment.", e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Failed to create a comment. {}", e);
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


  
