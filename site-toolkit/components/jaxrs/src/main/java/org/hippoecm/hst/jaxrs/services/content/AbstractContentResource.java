/*
 *  Copyright 2010-2016 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.hst.content.beans.ObjectBeanPersistenceException;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManager;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoHtml;
import org.hippoecm.hst.content.rewriter.ContentRewriter;
import org.hippoecm.hst.content.rewriter.ContentRewriterFactory;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.jaxrs.JAXRSService;
import org.hippoecm.hst.jaxrs.model.content.HippoHtmlRepresentation;
import org.hippoecm.hst.jaxrs.model.content.Link;
import org.hippoecm.hst.jaxrs.services.AbstractResource;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractContentResource extends AbstractResource {
    
    private static Logger log = LoggerFactory.getLogger(AbstractContentResource.class);
    
    protected String getRequestContentPath(HstRequestContext requestContext) {
    	return (String) requestContext.getAttribute(JAXRSService.REQUEST_CONTENT_PATH_KEY);
    }

    protected ContentRewriter<String> getResolvedContentRewriter() {
        ContentRewriter<String> contentRewriter = getContentRewriter();

        if (contentRewriter == null) {
            ContentRewriterFactory factory = HstServices.getComponentManager().getComponent(ContentRewriterFactory.class.getName());
            contentRewriter = factory.createContentRewriter();
        }

        return contentRewriter;
    }

    /**
     * Deletes the content node mapped to the child bean identified by <code>relPath</code>.
     * @param servletRequest
     * @param baseBean
     * @param relPath           the path identifying the child bean, see also {@link HippoBean#getBean(String)}
     * @return the path of the content node mapped to the child bean before deletion
     * @throws RepositoryException
     * @throws ObjectBeanPersistenceException
     * @throws IllegalArgumentException when the provided <code>relPath</code> could not be mapped to a child bean
     */
    protected String deleteContentResource(HttpServletRequest servletRequest, HippoBean baseBean, String relPath) throws RepositoryException, ObjectBeanPersistenceException {
        HippoBean child = baseBean.getBean(relPath);
        
        if (child == null) {
            throw new IllegalArgumentException("Child node not found: " + relPath);
        }
        
        return deleteHippoBean(servletRequest, child);
    }

    /**
     * Creates a {@link HippoHtmlRepresentation} for a {@link HippoHtml} child bean of the current request's content
     * bean. The child bean can be identified by providing <code>relPath</code>; if <code>relPath</code> is
     * <code>null</code> or empty, the first child bean of type <code>hippostd:html</code> is used. All links in the
     * content are rewritten as if the content was served over <code>targetMountAlias</code>; if
     * <code>targetMountAlias</code> is <code>null</code> or empty, the alias <code>site</code> is used.
     * @param servletRequest
     * @param relPath             the path identifying the child bean, see also {@link HippoBean#getBean(String)}
     * @param targetMountAlias
     * @return
     */
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
            
            ContentRewriter<String> rewriter = getResolvedContentRewriter();

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

    /**
     * Updates the content of a {@link HippoHtml} child bean of the current request's content bean. The child bean can
     * be identified by providing <code>relPath</code>; if <code>relPath</code> is <code>null</code> or empty, the
     * first child bean of type <code>hippostd:html</code> is updated. Note that saving content in a <code>live</code>
     * document variant will update the <code>preview</code> variant and the original <code>live</code> content will be
     * returned.
     * @param servletRequest
     * @param relPath             the path identifying the child bean, see also {@link HippoBean#getBean(String)}
     * @param htmlRepresentation  the {@link HippoHtmlRepresentation} containing the new content
     * @return the updated content as read from the current bean. Note that saving content in a <code>live</code>
     * document variant will update the <code>preview</code> variant and the original <code>live</code> content will be
     * returned.
     */
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

    /**
     * Creates a string for the content of a {@link HippoHtml} child bean of the current request's content
     * bean. The child bean can be identified by providing <code>relPath</code>; if <code>relPath</code> is
     * <code>null</code> or empty, the first child bean of type <code>hippostd:html</code> is used. All links in the
     * content are rewritten as if the content was served over <code>targetMountAlias</code>; if
     * <code>targetMountAlias</code> is <code>null</code> or empty, the alias <code>site</code> is used.
     * @param servletRequest
     * @param relPath             the path identifying the child bean, see also {@link HippoBean#getBean(String)}
     * @param targetMountAlias
     * @return
     */
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
        
        ContentRewriter<String> rewriter = getResolvedContentRewriter();

        if (StringUtils.isEmpty(targetMountAlias)) {
            targetMountAlias = MOUNT_ALIAS_SITE;
        }
        
        Mount targetMount = requestContext.getMount(targetMountAlias);
        
        String rewrittenHtml = rewriter.rewrite(htmlBean.getContent(), htmlBean.getNode(), requestContext, targetMount);
        return StringUtils.defaultString(rewrittenHtml);
    }

    /**
     * Updates the content of a {@link HippoHtml} child bean of the current request's content bean. The child bean can
     * be identified by providing <code>relPath</code>; if <code>relPath</code> is <code>null</code> or empty, the
     * first child bean of type <code>hippostd:html</code> is updated. Note that saving content in a <code>live</code>
     * document variant will update the <code>preview</code> variant and the original <code>live</code> content will be
     * returned.
     * @param servletRequest
     * @param relPath             the path identifying the child bean, see also {@link HippoBean#getBean(String)}
     * @param htmlContent         the <code>String</code> containing the new content
     * @return the updated content as read from the current bean. Note that saving content in a <code>live</code>
     * document variant will update the <code>preview</code> variant and the original <code>live</code> content will be
     * returned.
     */
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
