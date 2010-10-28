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
package org.hippoecm.hst.jaxrs.services;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.SiteMount;
import org.hippoecm.hst.content.beans.ContentNodeBinder;
import org.hippoecm.hst.content.beans.ContentNodeBindingException;
import org.hippoecm.hst.content.beans.ObjectBeanPersistenceException;
import org.hippoecm.hst.content.beans.manager.ObjectBeanPersistenceManager;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManager;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManagerImpl;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.rewriter.ContentRewriter;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.search.HstQueryManagerFactory;
import org.hippoecm.hst.jaxrs.model.content.Link;
import org.hippoecm.hst.jaxrs.model.content.NodeProperty;
import org.hippoecm.hst.jaxrs.util.AnnotatedContentBeanClassesScanner;
import org.hippoecm.hst.jaxrs.util.NodePropertyUtils;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.ObjectConverterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 *
 */
public class AbstractResource {

    private static Logger log = LoggerFactory.getLogger(AbstractResource.class);
	
    public static final String BEANS_ANNOTATED_CLASSES_CONF_PARAM = "hst-beans-annotated-classes";
    
    public static final String MOUNT_ALIAS_REST = "rest";
    public static final String MOUNT_ALIAS_SITE = "site";
    public static final String MOUNT_ALIAS_GALLERY = "gallery";
    public static final String MOUNT_ALIAS_ASSETS = "assets";
    
    public static final String MOUNT_ALIAS_REST_PROP_NAME = "hst:restMountAlias";
    public static final String MOUNT_ALIAS_SITE_PROP_NAME = "hst:siteMountAlias";
    public static final String MOUNT_ALIAS_GALLERY_PROP_NAME = "hst:galleryMountAlias";
    public static final String MOUNT_ALIAS_ASSETS_PROP_NAME = "hst:assetsMountAlias";
    
    private String annotatedClassesResourcePath;
    private List<Class<? extends HippoBean>> annotatedClasses;
    private ObjectConverter objectConverter;
    private HstQueryManager hstQueryManager;
    
    private boolean pageLinksExternal;
    
    private ContentRewriter<String> contentRewriter;
    
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
    
    public boolean isPageLinksExternal() {
        return pageLinksExternal;
    }

    public void setPageLinksExternal(boolean pageLinksExternal) {
        this.pageLinksExternal = pageLinksExternal;
    }
    
    public ContentRewriter<String> getContentRewriter() {
        return contentRewriter;
    }
    
    public void setContentRewriter(ContentRewriter<String> contentRewriter) {
        this.contentRewriter = contentRewriter;
    }
    
    protected ObjectBeanPersistenceManager getContentPersistenceManager(HstRequestContext requestContext) throws RepositoryException {
        return new WorkflowPersistenceManagerImpl(requestContext.getSession(), getObjectConverter(requestContext));
    }
    
    protected HstRequestContext getRequestContext(HttpServletRequest servletRequest) {
        return (HstRequestContext) servletRequest.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
    }
    
    protected void deleteHippoBean(HttpServletRequest servletRequest, HippoBean hippoBean) throws RepositoryException, ObjectBeanPersistenceException {
        ObjectBeanPersistenceManager obpm = getContentPersistenceManager(getRequestContext(servletRequest));
        obpm.remove(hippoBean);
        obpm.save();
    }
    
    protected HippoBean getChildBeanByRelPathOrPrimaryNodeType(HippoBean hippoBean, String relPath, String primaryNodeType) {
        if (StringUtils.isBlank(relPath)) {
            List<HippoBean> childBeans = hippoBean.getChildBeans(primaryNodeType);
            
            if (!childBeans.isEmpty()) {
                return childBeans.get(0);
            }
        } else {
            return hippoBean.getBean(relPath);
        }
        
        return null;
    }
    
    protected Link getNodeLink(HstRequestContext requestContext, HippoBean hippoBean) {
        Link nodeLink = new Link();
        
        try {
            nodeLink.setRel(MOUNT_ALIAS_REST);
            HstLink link = requestContext.getHstLinkCreator().create(hippoBean.getNode(), requestContext);
            if (link != null) {
                String href = link.toUrlForm(requestContext, isPageLinksExternal());
                nodeLink.setHref(href);
                nodeLink.setTitle(hippoBean.getName());
                
                // tries to retrieve title property if available.
                try {
                    String title = (String) PropertyUtils.getProperty(hippoBean, "title");
                    if (title != null) {
                        nodeLink.setTitle(title);
                    }
                } 
                catch (Exception ignore) {
                }
            }
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn("Failed to generate a page link. {}", e.toString());
            }
        }
        return nodeLink;
    }
    
    protected Link getSiteLink(HstRequestContext requestContext, HippoBean hippoBean) {
        Link nodeLink = new Link();
        
        try {
            String mappedMountAliasForSite = getMappedMountAliasName(requestContext, MOUNT_ALIAS_SITE);
            nodeLink.setRel(MOUNT_ALIAS_SITE);
            
            HstLink link = null;
            
            if (mappedMountAliasForSite != null) {
                link = requestContext.getHstLinkCreator().create(hippoBean.getNode(), requestContext, mappedMountAliasForSite);
            } else {
                link = requestContext.getHstLinkCreator().create(hippoBean.getNode(), requestContext);
            }
            
            if (link != null) {
                String href = link.toUrlForm(requestContext, isPageLinksExternal());
                nodeLink.setHref(href);
                nodeLink.setTitle(hippoBean.getName());
                
                // tries to retrieve title property if available.
                try {
                    String title = (String) PropertyUtils.getProperty(hippoBean, "title");
                    if (title != null) {
                        nodeLink.setTitle(title);
                    }
                } 
                catch (Exception ignore) {
                }
            }
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn("Failed to generate a page link. {}", e.toString());
            }
        }
        return nodeLink;
    }
    
    protected String getMappedMountAliasName(HstRequestContext requestContext, String mountAlias) {
        SiteMount curMount = requestContext.getResolvedSiteMount().getSiteMount();
        String mappedAlias = curMount.getAlias();
        
        if (MOUNT_ALIAS_SITE.equals(mountAlias)) {
            String propValue = curMount.getProperty(MOUNT_ALIAS_SITE_PROP_NAME);
            
            if (!StringUtils.isEmpty(propValue)) {
                return propValue;
            } else {
                SiteMount parentMount = requestContext.getResolvedSiteMount().getSiteMount().getParent();
                
                if (parentMount != null) {
                    return parentMount.getAlias();
                }
            }
        } else if (MOUNT_ALIAS_GALLERY.equals(mountAlias)) {
            String propValue = curMount.getProperty(MOUNT_ALIAS_GALLERY_PROP_NAME);
            
            if (!StringUtils.isEmpty(propValue)) {
                return propValue;
            }
        } else if (MOUNT_ALIAS_ASSETS.equals(mountAlias)) {
            String propValue = curMount.getProperty(MOUNT_ALIAS_ASSETS_PROP_NAME);
            
            if (!StringUtils.isEmpty(propValue)) {
                return propValue;
            }
        }
        return mappedAlias;
    }

    /* TODO: do we still want/need this (ate)? */
    protected void updateNodeProperties(HstRequestContext requestContext, HippoBean hippoBean, final List<NodeProperty> nodeProps) {
        try {
            WorkflowPersistenceManager wpm = (WorkflowPersistenceManager) getContentPersistenceManager(requestContext);
            wpm.update(hippoBean, new ContentNodeBinder() {
                public boolean bind(Object content, Node node) throws ContentNodeBindingException {
                    try {
                        if (nodeProps != null && !nodeProps.isEmpty()) {
                            for (NodeProperty nodeProp : nodeProps) {
                                NodePropertyUtils.setProperty(node, nodeProp);
                            }
                            return true;
                        }
                    } catch (RepositoryException e) {
                        throw new ContentNodeBindingException(e);
                    }
                    
                    return false;
                }
            });
            wpm.save();
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to save content bean.", e);
            } else {
                log.warn("Failed to save content bean. {}", e.toString());
            }
            
            throw new WebApplicationException(e);
        }
    }
}
