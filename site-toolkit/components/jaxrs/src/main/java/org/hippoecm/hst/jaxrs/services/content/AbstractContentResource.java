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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.SiteMount;
import org.hippoecm.hst.content.beans.ContentNodeBinder;
import org.hippoecm.hst.content.beans.ContentNodeBindingException;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.ObjectBeanPersistenceException;
import org.hippoecm.hst.content.beans.manager.ObjectBeanPersistenceManager;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManager;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManagerImpl;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoHtml;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.search.HstQueryManagerFactory;
import org.hippoecm.hst.jaxrs.JAXRSService;
import org.hippoecm.hst.jaxrs.model.content.HippoHtmlRepresentation;
import org.hippoecm.hst.jaxrs.model.content.Link;
import org.hippoecm.hst.jaxrs.model.content.NodeProperty;
import org.hippoecm.hst.jaxrs.util.AnnotatedContentBeanClassesScanner;
import org.hippoecm.hst.jaxrs.util.NodePropertyUtils;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.ObjectConverterUtils;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AbstractContentResource
 * @version $Id$
 */
public abstract class AbstractContentResource {
    
    private static Logger log = LoggerFactory.getLogger(AbstractContentResource.class);
    
    public static final String BEANS_ANNOTATED_CLASSES_CONF_PARAM = "hst-beans-annotated-classes";
    
    private String annotatedClassesResourcePath;
    private List<Class<? extends HippoBean>> annotatedClasses;
    private ObjectConverter objectConverter;
    private HstQueryManager hstQueryManager;
    
    private String siteAliasForPageLinks;
    private boolean pageLinksExternal;
    
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
    
    public String getSiteAliasForPageLinks() {
        return siteAliasForPageLinks;
    }

    public void setSiteAliasForPageLinks(String siteAliasForPageLinks) {
        this.siteAliasForPageLinks = siteAliasForPageLinks;
    }

    public boolean isPageLinksExternal() {
        return pageLinksExternal;
    }

    public void setPageLinksExternal(boolean pageLinksExternal) {
        this.pageLinksExternal = pageLinksExternal;
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
    
    protected HippoHtmlRepresentation getHippoHtmlRepresentation(HttpServletRequest servletRequest, String relPath) {
        HippoHtml htmlBean = null;
        
        try {
            HstRequestContext requestContext = getRequestContext(servletRequest);       
            HippoBean hippoBean = getRequestContentBean(requestContext);
            htmlBean = (HippoHtml) getChildBeanByRelPathOrPrimaryNodeType(hippoBean, relPath, "hippostd:html");
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            
            throw new WebApplicationException(e);
        }
        
        if (htmlBean == null) {
            if (log.isWarnEnabled()) {
                log.warn("HippoHtml child bean not found.");
            }
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        
        return new HippoHtmlRepresentation().represent(htmlBean);
    }
    
    protected HippoHtmlRepresentation updateHippoHtmlRepresentation(HttpServletRequest servletRequest, String relPath, HippoHtmlRepresentation htmlRepresentation) {
        HippoBean hippoBean = null;
        HippoHtml htmlBean = null;
        
        HstRequestContext requestContext = getRequestContext(servletRequest);
        
        try {
            hippoBean = getRequestContentBean(requestContext);
            htmlBean = (HippoHtml) getChildBeanByRelPathOrPrimaryNodeType(hippoBean, relPath, "hippostd:html");
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            
            throw new WebApplicationException(e);
        }
        
        if (htmlBean == null) {
            if (log.isWarnEnabled()) {
                log.warn("HippoHtml child bean not found.");
            }
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        
        try {
            WorkflowPersistenceManager wpm = (WorkflowPersistenceManager) getContentPersistenceManager(requestContext);
            final String html = htmlRepresentation.getContent();
            final String htmlRelPath = PathUtils.normalizePath(htmlBean.getPath().substring(hippoBean.getPath().length()));
            wpm.update(hippoBean, new ContentNodeBinder() {
                public boolean bind(Object content, Node node) throws ContentNodeBindingException {
                    try {
                        Node htmlNode = node.getNode(htmlRelPath);
                        htmlNode.setProperty("hippostd:content", html);
                        return true;
                    } catch (RepositoryException e) {
                        throw new ContentNodeBindingException(e);
                    }
                }
            });
            wpm.save();
            
            hippoBean = (HippoBean) wpm.getObject(hippoBean.getPath());
            htmlBean = (HippoHtml) getChildBeanByRelPathOrPrimaryNodeType(hippoBean, relPath, "hippostd:html");
            htmlRepresentation = new HippoHtmlRepresentation().represent(htmlBean);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            
            throw new WebApplicationException(e);
        }
        
        return htmlRepresentation;
    }
    
    protected String getHippoHtmlContent(HttpServletRequest servletRequest, String relPath) {
        
        HippoHtml htmlBean = null;
        
        try {
            HstRequestContext requestContext = getRequestContext(servletRequest);       
            HippoBean hippoBean = getRequestContentBean(requestContext);
            htmlBean = (HippoHtml) getChildBeanByRelPathOrPrimaryNodeType(hippoBean, relPath, "hippostd:html");
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            
            throw new WebApplicationException(e);
        }
        
        if (htmlBean == null) {
            if (log.isWarnEnabled()) {
                log.warn("HippoHtml child bean not found.");
            }
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        
        return htmlBean.getContent();
    }
    
    protected String updateHippoHtmlContent(HttpServletRequest servletRequest, String relPath, String htmlContent) {
        HippoBean hippoBean = null;
        HippoHtml htmlBean = null;
        
        HstRequestContext requestContext = getRequestContext(servletRequest);
        
        try {
            hippoBean = getRequestContentBean(requestContext);
            htmlBean = (HippoHtml) getChildBeanByRelPathOrPrimaryNodeType(hippoBean, relPath, "hippostd:html");
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            
            throw new WebApplicationException(e);
        }
        
        if (htmlBean == null) {
            if (log.isWarnEnabled()) {
                log.warn("HippoHtml child bean not found.");
            }
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        
        try {
            WorkflowPersistenceManager wpm = (WorkflowPersistenceManager) getContentPersistenceManager(requestContext);
            final String html = htmlContent;
            final String htmlRelPath = PathUtils.normalizePath(htmlBean.getPath().substring(hippoBean.getPath().length()));
            wpm.update(hippoBean, new ContentNodeBinder() {
                public boolean bind(Object content, Node node) throws ContentNodeBindingException {
                    try {
                        Node htmlNode = node.getNode(htmlRelPath);
                        htmlNode.setProperty("hippostd:content", html);
                        return true;
                    } catch (RepositoryException e) {
                        throw new ContentNodeBindingException(e);
                    }
                }
            });
            wpm.save();
            
            hippoBean = (HippoBean) wpm.getObject(hippoBean.getPath());
            htmlBean = (HippoHtml) getChildBeanByRelPathOrPrimaryNodeType(hippoBean, relPath, "hippostd:html");
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            
            throw new WebApplicationException(e);
        }
        
        return htmlBean.getContent();
    }
    
    protected Link getNodeLink(HstRequestContext requestContext, HippoBean hippoBean) {
        Link nodeLink = new Link();
        
        try {
            if (getSiteAliasForPageLinks() == null) {
                SiteMount parentMount = requestContext.getResolvedSiteMount().getSiteMount().getParent();
                if (parentMount != null) {
                    setSiteAliasForPageLinks(parentMount.getAlias());
                }
            }
            
            String siteAliasForPageLink = getSiteAliasForPageLinks();
            nodeLink.setRel(siteAliasForPageLink);
            
            HstLink link = null;
            
            if (siteAliasForPageLink != null) {
                link = requestContext.getHstLinkCreator().create(hippoBean.getNode(), requestContext, getSiteAliasForPageLinks());
            } else {
                link = requestContext.getHstLinkCreator().create(hippoBean.getNode(), requestContext);
            }
            
            String href = link.toUrlForm(requestContext, isPageLinksExternal());
            nodeLink.setHref(href);
            nodeLink.setTitle(hippoBean.getName());
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn("Failed to generate a page link. {}", e.toString());
            }
        }
        
        return nodeLink;
    }
}
