/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.hst.jaxrs.services.content;

import java.util.Collections;
import java.util.List;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;

import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.ObjectBeanPersistenceException;
import org.hippoecm.hst.content.beans.manager.ObjectBeanPersistenceManager;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManagerImpl;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.search.HstQueryManagerFactory;
import org.hippoecm.hst.jaxrs.JAXRSService;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.ObjectConverterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
public abstract class AbstractNodeResource {

    private static Logger log = LoggerFactory.getLogger(AbstractNodeResource.class);
    
    private List<Class<? extends HippoBean>> annotatedClasses;
    private ObjectConverter objectConverter;
    private HstQueryManager hstQueryManager;
    
    public List<Class<? extends HippoBean>> getAnnotatedClasses() {
        if (annotatedClasses == null) {
            return Collections.emptyList();
        }
        return annotatedClasses;
    }
    
    public void setAnnotatedClasses(List<Class<? extends HippoBean>> annotatedClasses) {
        this.annotatedClasses = annotatedClasses;
    }
    
    public ObjectConverter getObjectConverter() {
        if (objectConverter == null) {
            List<Class<? extends HippoBean>> annotatedClasses = getAnnotatedClasses();
            objectConverter = ObjectConverterUtils.createObjectConverter(annotatedClasses);
        }
        return objectConverter;
    }
    
    public void setObjectConverter(ObjectConverter objectConverter) {
    	this.objectConverter = objectConverter;
    }
    
    public HstQueryManager getHstQueryManager() {
        if (hstQueryManager == null) {
            ComponentManager compManager = HstServices.getComponentManager();
            if (compManager != null) {
                HstQueryManagerFactory hstQueryManagerFactory = (HstQueryManagerFactory) compManager.getComponent(HstQueryManagerFactory.class.getName());
                hstQueryManager = hstQueryManagerFactory.createQueryManager(getObjectConverter());
            }
        }
        return hstQueryManager;
    }
    
    public void setHstQueryManager(HstQueryManager hstQueryManager) {
    	this.hstQueryManager = hstQueryManager;
    }
    
    protected ObjectBeanPersistenceManager getContentPersistenceManager(Session jcrSession) throws LoginException, RepositoryException {
        return new WorkflowPersistenceManagerImpl(jcrSession, getObjectConverter());
    }
    
    protected HstRequestContext getRequestContext(HttpServletRequest servletRequest) {
        return (HstRequestContext) servletRequest.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
    }
    
    protected String getRequestContentPath(HstRequestContext requestContext) {
    	return (String)requestContext.getAttribute(JAXRSService.REQUEST_CONTENT_PATH_KEY);
    }
    
    protected Node getRequestContentNode(HstRequestContext requestContext) {
    	return (Node)requestContext.getAttribute(JAXRSService.REQUEST_CONTENT_NODE_KEY);
    }
    
    protected HippoBean getRequestContentAsHippoBean(HstRequestContext requestContext) throws WebApplicationException {
        Node requestContentNode = getRequestContentNode(requestContext);
        
        if (requestContentNode == null) {
            if (log.isWarnEnabled()) {
                log.warn("Request content node is not found.");
            }
            throw new WebApplicationException(new IllegalStateException("Request content node is not found."));
        }
        
        try {
            return (HippoBean) getObjectConverter().getObject(requestContentNode);
        } catch (ObjectBeanManagerException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to convert content node.", e);
            } else {
                log.warn("Failed to convert content node. {}", e.toString());
            }
            throw new WebApplicationException(e);
        }
    }
    
    protected HippoFolderBean getRequestContentAsHippoFolderBean(HstRequestContext requestContext) throws WebApplicationException {
        HippoBean hippoBean = getRequestContentAsHippoBean(requestContext);
        
        if (!hippoBean.isHippoFolderBean()) {
            if (log.isWarnEnabled()) {
                log.warn("The content bean is not a folder bean.");
            }
            throw new WebApplicationException(new IllegalArgumentException("The content bean is not a folder bean."));
        }
        
        return (HippoFolderBean) hippoBean;
    }
    
    protected HippoDocumentBean getRequestContentAsHippoDocumentBean(HstRequestContext requestContext) throws WebApplicationException {
        HippoBean hippoBean = getRequestContentAsHippoBean(requestContext);
        
        if (!hippoBean.isHippoDocumentBean()) {
            if (log.isWarnEnabled()) {
                log.warn("The content bean is not a document bean.");
            }
            throw new WebApplicationException(new IllegalArgumentException("The content bean is not a document bean."));
        }
        
        return (HippoDocumentBean) hippoBean;
    }
    
    protected void createContentNode(HttpServletRequest servletRequest, HippoFolderBean baseFolderBean, String nodeTypeName, String name) throws WebApplicationException {
        ObjectBeanPersistenceManager obpm = null;
        
        try {
            obpm = getContentPersistenceManager(getRequestContext(servletRequest).getSession());
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to create workflow persistence manager.", e);
            } else {
                log.warn("Failed to create workflow persistence manager. {}", e.toString());
            }
            throw new WebApplicationException(e);
        }
        
        try {
            obpm.create(baseFolderBean.getPath(), nodeTypeName, name);
        } catch (ObjectBeanPersistenceException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to create node.", e);
            } else {
                log.warn("Failed to create node. {}", e.toString());
            }
            throw new WebApplicationException(e);
        }
    }
    
    protected void deleteContentNode(HttpServletRequest servletRequest, HippoFolderBean baseFolderBean, String relPath) {
        ObjectBeanPersistenceManager obpm = null;
        
        try {
            obpm = getContentPersistenceManager(getRequestContext(servletRequest).getSession());
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to create workflow persistence manager.", e);
            } else {
                log.warn("Failed to create workflow persistence manager. {}", e.toString());
            }
            throw new WebApplicationException(e);
        }
        
        HippoBean child = baseFolderBean.getBean(relPath);
        
        if (child == null) {
            if (log.isWarnEnabled()) {
                log.warn("Child node not found: " + relPath);
            }
            throw new WebApplicationException(new IllegalArgumentException("Child node not found: " + relPath));
        }
        
        try {
            obpm.remove(child);
        } catch (ObjectBeanPersistenceException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to create folder.", e);
            } else {
                log.warn("Failed to create folder. {}", e.toString());
            }
            throw new WebApplicationException(e);
        }
    }
}
