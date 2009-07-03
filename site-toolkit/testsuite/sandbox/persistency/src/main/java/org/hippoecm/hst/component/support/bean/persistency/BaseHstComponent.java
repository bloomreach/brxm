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
package org.hippoecm.hst.component.support.bean.persistency;

import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.PersistableObjectBeanManagerImpl;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.persistence.ContentNodeBinder;
import org.hippoecm.hst.persistence.ContentPersistenceManager;
import org.hippoecm.hst.util.PathUtils;
import org.hippoecm.repository.api.HippoNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A base HstComponent implementation to provide some facility methods for accessing content node POJO objects,
 * {@link ObjectBeanManager}, {@link ContentPersistenceManager}, request parameters, query manager, etc.
 * <P>
 * This implementation enables developers to make use of HST Content Bean's {@link ObjectBeanManager}
 * which provides a simple object-content mapping solution.
 * To use {@link ObjectBeanManager}, you can invoke {@link #getObjectBeanManager(HstRequest)}, which retrieves
 * a JCR session from {@link HstRequestContext#getSession()} internally.
 * </P>
 * <P>
 * In most read-only cases, the {@link ObjectBeanManager} could be enough to access content objects.
 * However, if you want to persist content POJO objects back to the repository, you need one more: {@link ContentPersistenceManager}.
 * You can create an instance of <CODE>ContentPersistenceManager</CODE> by using one of <CODE>getContentPersistenceManager()</CODE> methods.
 * These methods require {@link javax.jcr.Session} because the persistency may require another session with different credentials.
 * You can get a session by {@link HstRequestContext#getSession()} or you can impersonate the session to another session
 * by {@link javax.jcr.Session#impersonate(javax.jcr.Credentials)} to persist the content object.
 * </P>
 * <P>
 * <EM>Note: if you retrieve another JCR session to persist contents by invoking {@link javax.jcr.Session#impersonate(javax.jcr.Credentials)}, 
 * then you are responsible to invoke <CODE>logout()</CODE> after persisting. The HST container will not care the 
 * impersonated session from the application side.</EM>
 * </P>
 * 
 * @version $Id$
 */
public class BaseHstComponent extends org.hippoecm.hst.component.support.bean.BaseHstComponent {
    
    public static final String DEFAULT_WRITABLE_USERNAME_PROPERTY = "writable.repository.user.name";
    public static final String DEFAULT_WRITABLE_PASSWORD_PROPERTY = "writable.repository.password";
    
    private static Logger log = LoggerFactory.getLogger(BaseHstComponent.class);
    
    /**
     * Creates a persistable JCR session with the default credentials
     * <P>
     * <EM>Note: The client should invoke <CODE>logout()</CODE> method on the session after use.</EM>
     * </P>
     * <P>
     * Internally, {@link javax.jcr.Session#impersonate(Credentials)} method will be used to create a
     * persistable JCR session. The method is invoked on the session from the session pooling repository.
     * </P>
     * @param request
     * @return
     */
    protected Session getPersistableSession(HstRequest request) throws RepositoryException {
        ContainerConfiguration config = request.getRequestContext().getContainerConfiguration();
        String username = config.getString(DEFAULT_WRITABLE_USERNAME_PROPERTY);
        String password = config.getString(DEFAULT_WRITABLE_PASSWORD_PROPERTY);
        return getPersistableSession(request, new SimpleCredentials(username, password.toCharArray()));
    }
    
    /**
     * Creates a persistable JCR session with provided credentials.
     * <P>
     * <EM>Note: The client should invoke <CODE>logout()</CODE> method on the session after use.</EM>
     * </P>
     * <P>
     * Internally, {@link javax.jcr.Session#impersonate(Credentials)} method will be used to create a
     * persistable JCR session. The method is invoked on the session from the session pooling repository.
     * </P>
     * @param request
     * @return
     */
    protected Session getPersistableSession(HstRequest request, Credentials credentials) throws RepositoryException {
        return request.getRequestContext().getSession().impersonate(credentials);
    }
    
    /**
     * Returns a <CODE>ContentPersistenceManager</CODE> instance.
     * @param session
     * @return
     */
    protected ContentPersistenceManager getContentPersistenceManager(Session session) {
        return getContentPersistenceManager(session, null);
    }
    
    /**
     * Returns a <CODE>ContentPersistenceManager</CODE> instance with custom binders map.
     * @param session
     * @param contentNodeBinders
     * @return
     */
    protected ContentPersistenceManager getContentPersistenceManager(Session session, Map<String, ContentNodeBinder> contentNodeBinders) {
        return getContentPersistenceManager(session, contentNodeBinders, false);
    }
    
    /**
     * Returns a <CODE>ContentPersistenceManager</CODE> instance with request-publishing-option after invoking
     * any <CODE>update()</CODE> method.
     * @param session
     * @param publishAfterUpdate
     * @return
     */
    protected ContentPersistenceManager getContentPersistenceManager(Session session, boolean publishAfterUpdate) {
        return getContentPersistenceManager(session, null, publishAfterUpdate);
    }
    
    /**
     * Returns a <CODE>ContentPersistenceManager</CODE> instance with custom binders map and 
     * request-publishing-option after invoking any <CODE>update()</CODE> method.
     * @param session
     * @param contentNodeBinders
     * @param publishAfterUpdate
     * @return
     */
    protected ContentPersistenceManager getContentPersistenceManager(Session session, Map<String, ContentNodeBinder> contentNodeBinders, boolean publishAfterUpdate) {
        PersistableObjectBeanManagerImpl cpm = new PersistableObjectBeanManagerImpl(session, this.objectConverter, contentNodeBinders);
        cpm.setPublishAfterUpdate(publishAfterUpdate);
        return cpm;
    }
    
    /**
     * Returns the physical node path of site content base node.
     * @param request
     * @return
     */
    protected String getSiteContentBasePhysicalPath(HstRequest request) {
        String physicalPath = null;
        
        try {
            HippoBean siteContentBaseBean = getSiteContentBaseBean(request);
            String siteContentBaseNodeUUID = siteContentBaseBean.getNode().getProperty("hippo:docbase").getString();
            HippoNode siteContentPhysicalNode = (HippoNode) request.getRequestContext().getSession().getNodeByUUID(siteContentBaseNodeUUID);
            physicalPath = siteContentPhysicalNode.getCanonicalNode().getPath();
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn("Failed to retrieve the physical node of site content base: {}", e);
            }
        }
        
        return physicalPath;
    }
    
    /**
     * Returns the relative path of a node from the site content base path
     * @param request
     * @param contentPath
     * @return
     */
    protected String getContentRelativePath(HstRequest request, String contentPath) {
        String siteContentBasePath = getSiteContentBasePath(request);
        String relPath = PathUtils.normalizePath(contentPath);
        
        if (relPath.startsWith(siteContentBasePath)) {
            relPath = relPath.substring(siteContentBasePath.length() + 1);
        }
        
        return relPath;
    }
    
}
