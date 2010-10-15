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

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.ObjectBeanPersistenceException;
import org.hippoecm.hst.content.beans.manager.ObjectBeanPersistenceManager;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManagerImpl;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.search.HstQueryManagerFactory;
import org.hippoecm.hst.jaxrs.JAXRSService;
import org.hippoecm.hst.jaxrs.util.AnnotatedContentBeanClassesScanner;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.ObjectConverterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
public abstract class AbstractContentResource {
    
    private static Logger log = LoggerFactory.getLogger(AbstractContentResource.class);
    
    public static final String BEANS_ANNOTATED_CLASSES_CONF_PARAM = "hst-beans-annotated-classes";
    
    private String annotatedClassesResourcePath;
    private List<Class<? extends HippoBean>> annotatedClasses;
    private ObjectConverter objectConverter;
    private HstQueryManager hstQueryManager;
    
    public String getAnnotatedClassesResourcePath() {
        return annotatedClassesResourcePath;
    }
    
    public void setAnnotatedClassesResourcePath(String annotatedClassesResourcePath) {
        this.annotatedClassesResourcePath = annotatedClassesResourcePath;
    }
    
    public List<Class<? extends HippoBean>> getAnnotatedClasses(HstRequestContext requestContext) {
        if (annotatedClasses == null) {
            String annoClassPathResourcePath = getAnnotatedClassesResourcePath();
            
            if (StringUtils.isBlank(annoClassPathResourcePath)) {
                annoClassPathResourcePath = requestContext.getServletContext().getInitParameter(BEANS_ANNOTATED_CLASSES_CONF_PARAM);
            }
            
            annotatedClasses = AnnotatedContentBeanClassesScanner.scanAnnotatedContentBeanClasses(requestContext, annoClassPathResourcePath);
        }
        
        return annotatedClasses;
    }
    
    public void setAnnotatedClasses(List<Class<? extends HippoBean>> annotatedClasses) {
        this.annotatedClasses = annotatedClasses;
    }
    
    public ObjectConverter getObjectConverter(HstRequestContext requestContext) {
        if (objectConverter == null) {
            List<Class<? extends HippoBean>> annotatedClasses = getAnnotatedClasses(requestContext);
            objectConverter = ObjectConverterUtils.createObjectConverter(annotatedClasses);
        }
        return objectConverter;
    }
    
    public void setObjectConverter(ObjectConverter objectConverter) {
    	this.objectConverter = objectConverter;
    }
    
    public HstQueryManager getHstQueryManager(HstRequestContext requestContext) {
        if (hstQueryManager == null) {
            ComponentManager compManager = HstServices.getComponentManager();
            if (compManager != null) {
                HstQueryManagerFactory hstQueryManagerFactory = (HstQueryManagerFactory) compManager.getComponent(HstQueryManagerFactory.class.getName());
                hstQueryManager = hstQueryManagerFactory.createQueryManager(getObjectConverter(requestContext));
            }
        }
        return hstQueryManager;
    }
    
    public void setHstQueryManager(HstQueryManager hstQueryManager) {
    	this.hstQueryManager = hstQueryManager;
    }
    
    protected ObjectBeanPersistenceManager getContentPersistenceManager(HstRequestContext requestContext) throws RepositoryException {
        return new WorkflowPersistenceManagerImpl(requestContext.getSession(), getObjectConverter(requestContext));
    }
    
    protected HstRequestContext getRequestContext(HttpServletRequest servletRequest) {
        return (HstRequestContext) servletRequest.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
    }
    
    protected String getRequestContentPath(HstRequestContext requestContext) {
    	return (String) requestContext.getAttribute(JAXRSService.REQUEST_CONTENT_PATH_KEY);
    }
    
    protected Node getRequestContentNode(HstRequestContext requestContext) {
    	return (Node) requestContext.getAttribute(JAXRSService.REQUEST_CONTENT_NODE_KEY);
    }
    
    protected HippoBean getRequestContentBean(HstRequestContext requestContext) throws ObjectBeanManagerException {
        Node requestContentNode = getRequestContentNode(requestContext);
        
        if (requestContentNode == null) {
            throw new ObjectBeanManagerException("Invalid request content node: null");
        }
        
        return (HippoBean) getObjectConverter(requestContext).getObject(requestContentNode);
    }
    
    protected void deleteContentResource(HttpServletRequest servletRequest, HippoBean baseBean, String relPath) throws RepositoryException, ObjectBeanPersistenceException {
        HippoBean child = baseBean.getBean(relPath);
        
        if (child == null) {
            throw new IllegalArgumentException("Child node not found: " + relPath);
        }
        
        deleteContentBean(servletRequest, child);
    }
    
    protected void deleteContentBean(HttpServletRequest servletRequest, HippoBean hippoBean) throws RepositoryException, ObjectBeanPersistenceException {
        ObjectBeanPersistenceManager obpm = getContentPersistenceManager(getRequestContext(servletRequest));
        obpm.remove(hippoBean);
        obpm.save();
    }
}
