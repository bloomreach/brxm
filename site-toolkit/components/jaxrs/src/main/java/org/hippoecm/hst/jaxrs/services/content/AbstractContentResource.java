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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
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
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.ObjectConverterUtils;

/**
 * @version $Id$
 */
public abstract class AbstractContentResource {
    
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
    
    protected ObjectBeanPersistenceManager getContentPersistenceManager(Session jcrSession) throws RepositoryException {
        return new WorkflowPersistenceManagerImpl(jcrSession, getObjectConverter());
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
        
        return (HippoBean) getObjectConverter().getObject(requestContentNode);
    }
    
}
