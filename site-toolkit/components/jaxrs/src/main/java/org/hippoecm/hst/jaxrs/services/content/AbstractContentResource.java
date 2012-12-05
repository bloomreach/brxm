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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.content.beans.ContentNodeBinder;
import org.hippoecm.hst.content.beans.ContentNodeBindingException;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.ObjectBeanPersistenceException;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManager;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoHtml;
import org.hippoecm.hst.content.rewriter.ContentRewriter;
import org.hippoecm.hst.content.rewriter.impl.SimpleContentRewriter;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.jaxrs.JAXRSService;
import org.hippoecm.hst.jaxrs.model.content.HippoHtmlRepresentation;
import org.hippoecm.hst.jaxrs.model.content.Link;
import org.hippoecm.hst.jaxrs.services.AbstractResource;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AbstractContentResource
 * @version $Id$
 */
public abstract class AbstractContentResource extends AbstractResource {
    
    private static Logger log = LoggerFactory.getLogger(AbstractContentResource.class);
    
    protected String getRequestContentPath(HstRequestContext requestContext) {
    	return (String) requestContext.getAttribute(JAXRSService.REQUEST_CONTENT_PATH_KEY);
    }

    protected String deleteContentResource(HttpServletRequest servletRequest, HippoBean baseBean, String relPath) throws RepositoryException, ObjectBeanPersistenceException {
        HippoBean child = baseBean.getBean(relPath);
        
        if (child == null) {
            throw new IllegalArgumentException("Child node not found: " + relPath);
        }
        
        return deleteHippoBean(servletRequest, child);
    }
    
    protected HippoHtmlRepresentation getHippoHtmlRepresentation(HttpServletRequest servletRequest, String relPath, String targetMountAlias) {
        HstRequestContext requestContext = getRequestContext(servletRequest);
        HippoBean hippoBean = null;
        HippoHtml htmlBean = null;
        
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
            log.warn("HippoHtml child bean not found.");
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        
        try {
            HippoHtmlRepresentation htmlRep = new HippoHtmlRepresentation().represent(htmlBean);
            Link parentLink = getNodeLink(requestContext, hippoBean);
            parentLink.setRel(getHstQualifiedLinkRel("parent"));
            htmlRep.addLink(parentLink);
            
            ContentRewriter<String> rewriter = getContentRewriter();
            if (rewriter == null) {
                rewriter = new SimpleContentRewriter();
            }
            
            if (StringUtils.isEmpty(targetMountAlias)) {
                targetMountAlias = MOUNT_ALIAS_SITE;
            }
            
            Mount targetMount = requestContext.getMount(targetMountAlias);
            
            String rewrittenHtml = rewriter.rewrite(htmlBean.getContent(), htmlBean.getNode(), requestContext, targetMount);
            htmlRep.setContent(StringUtils.defaultString(rewrittenHtml));
            
            return htmlRep;
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            
            throw new WebApplicationException(e);
        }
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
            log.warn("HippoHtml child bean not found.");
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        
        try {
            WorkflowPersistenceManager wpm = (WorkflowPersistenceManager) getPersistenceManager(requestContext);
            final String html = htmlRepresentation.getContent();
            final String htmlRelPath = PathUtils.normalizePath(htmlBean.getPath().substring(hippoBean.getPath().length()));
            wpm.update(hippoBean, new ContentNodeBinder() {
                public boolean bind(Object content, Node node) throws ContentNodeBindingException {
                    try {
                        Node htmlNode = node.getNode(htmlRelPath);
                        htmlNode.setProperty("hippostd:content", StringUtils.defaultString(html));
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
    
    protected String getHippoHtmlContent(HttpServletRequest servletRequest, String relPath, String targetMountAlias) {
        
        HstRequestContext requestContext = getRequestContext(servletRequest);
        HippoHtml htmlBean = null;
        
        try {
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
            log.warn("HippoHtml child bean not found.");
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        
        ContentRewriter<String> rewriter = getContentRewriter();
        if (rewriter == null) {
            rewriter = new SimpleContentRewriter();
        }
        
        if (StringUtils.isEmpty(targetMountAlias)) {
            targetMountAlias = MOUNT_ALIAS_SITE;
        }
        
        Mount targetMount = requestContext.getMount(targetMountAlias);
        
        String rewrittenHtml = rewriter.rewrite(htmlBean.getContent(), htmlBean.getNode(), requestContext, targetMount);
        return StringUtils.defaultString(rewrittenHtml);
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
            log.warn("HippoHtml child bean not found.");
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        
        try {
            WorkflowPersistenceManager wpm = (WorkflowPersistenceManager) getPersistenceManager(requestContext);
            final String html = htmlContent;
            final String htmlRelPath = PathUtils.normalizePath(htmlBean.getPath().substring(hippoBean.getPath().length()));
            wpm.update(hippoBean, new ContentNodeBinder() {
                public boolean bind(Object content, Node node) throws ContentNodeBindingException {
                    try {
                        Node htmlNode = node.getNode(htmlRelPath);
                        htmlNode.setProperty("hippostd:content", StringUtils.defaultString(html));
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
}
