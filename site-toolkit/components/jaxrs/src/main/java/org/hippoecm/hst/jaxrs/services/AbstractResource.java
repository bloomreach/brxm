/*
 *  Copyright 2010-2018 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.ObjectBeanPersistenceException;
import org.hippoecm.hst.content.beans.manager.ObjectBeanPersistenceManager;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManagerImpl;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.content.rewriter.ContentRewriter;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.jaxrs.model.content.Link;
import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractResource {

    private static Logger log = LoggerFactory.getLogger(AbstractResource.class);

    public static final String MOUNT_ALIAS_REST = ContainerConstants.MOUNT_ALIAS_REST;
    public static final String MOUNT_ALIAS_SITE = ContainerConstants.MOUNT_ALIAS_SITE;
    public static final String MOUNT_ALIAS_GALLERY = ContainerConstants.MOUNT_ALIAS_GALLERY;
    public static final String MOUNT_ALIAS_ASSETS = ContainerConstants.MOUNT_ALIAS_ASSETS;
    public static final String HST_REST_RELATIONS_BASE_URI = "http://www.onehippo.org/cms7/hst/rest/relations";
    public static final String HST_MOUNT_REL_PREFIX = "mount:";
    
    private String restRelationsBaseUri = HST_REST_RELATIONS_BASE_URI;

    private boolean pageLinksExternal;
    
    private ContentRewriter<String> contentRewriter;

    public void setAnnotatedClasses(List<Class<? extends HippoBean>> annotatedClasses) {
        log.warn("AbstractResource#setAnnotatedClasses is deprecated and does not do anything any more.");
    }

    /**
     * @return a {@link ObjectConverter} instance. Note that this instance is shared between multiple threads.
     */
    public ObjectConverter getObjectConverter(HstRequestContext requestContext) {
        return requestContext.getContentBeansTool().getObjectConverter();
    }
    
    /**
     * @return the query manager backed by the session returned by {@link HstRequestContext#getSession()} of the
     * provided <code>requestContext</code>
     */
    public HstQueryManager getHstQueryManager(HstRequestContext requestContext) {
        return requestContext.getQueryManager();
    }

    /**
     * @return the query manager backed by the provided <code>session</code>
     */
    public HstQueryManager getHstQueryManager(Session session, HstRequestContext requestContext) {
        return requestContext.getQueryManager(session);
    }
    
    public String getRestRelationsBaseUri() {
    	return restRelationsBaseUri;
    }
    
    public void setRestRelationsBaseUri(String restRelationsBaseUri) {
    	this.restRelationsBaseUri = restRelationsBaseUri;
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

    /**
     * Creates and returns a persistence manager with the default session of the requestContext.
     * <P>
     * Note: when the operation is annotated with {@link org.hippoecm.hst.content.annotations.Persistable}, the default session of the requestContext
     *       should already be a persistable (writable) session.
     * </P>
     * @param requestContext
     * @return
     * @throws RepositoryException
     */
    protected ObjectBeanPersistenceManager getPersistenceManager(HstRequestContext requestContext) throws RepositoryException {
        return getPersistenceManager(requestContext, requestContext.getSession());
    }
    
    /**
     * Creates and returns a persistence manager with the specified session.
     * @param requestContext
     * @param persistableSession
     * @return
     * @throws RepositoryException
     */
    protected ObjectBeanPersistenceManager getPersistenceManager(HstRequestContext requestContext, Session persistableSession) throws RepositoryException {
        return new WorkflowPersistenceManagerImpl(persistableSession, getObjectConverter(requestContext));
    }

    /**
     * @return the {@link HstRequestContext} for the provided <code>servletRequest</code>
     */
    protected HstRequestContext getRequestContext(HttpServletRequest servletRequest) {
        return (HstRequestContext) servletRequest.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
    }
    
    /**
     * Returns the content HippoBean of type T for the current request. If there cannot be found a bean of type <code>beanMappingClass<code> for the relative content path of the
     * resolved sitemap item  a ObjectBeanManagerException is thrown
     * If there is no resolved sitemap item, <code>null</code> is returned. 
     * @param <T>
     * @param requestContext
     * @param beanMappingClass
     * @throws ObjectBeanManagerException when there cannot be returned a bean
     * @return a bean of type T 
     */
    protected <T extends HippoBean> T getRequestContentBean(HstRequestContext requestContext, Class<T> beanMappingClass) throws ObjectBeanManagerException {
        HippoBean bean = requestContext.getContentBean();
        if(bean == null) {
            throw new ObjectBeanManagerException("Cannot return bean of type '"+beanMappingClass+"'");
        }
        if(!beanMappingClass.isAssignableFrom(bean.getClass())) {
            log.debug("Expected bean of type '{}' but found of type '{}'. Return null.", beanMappingClass.getName(), bean.getClass().getName());
            throw new ObjectBeanManagerException("Cannot return bean of type '"+beanMappingClass+"'");
        }
        return (T)bean;
    }

    /**
     * Returns the content HippoBean for the current request. 
     * @param requestContext
     * @throws ObjectBeanManagerException when there cannot be returned a bean
     * @return the HippoBean where the relative contentpath of the sitemap item points to
     */
    protected HippoBean getRequestContentBean(HstRequestContext requestContext) throws ObjectBeanManagerException {
        return requestContext.getContentBean();
    }

    /**
     * 
     * @param requestContext
     * @throws ObjectBeanManagerException when there cannot be returned a site content base bean
     * @return HippoFolderBean the mountContentBaseBean 
     */
    public HippoFolderBean getMountContentBaseBean(HstRequestContext requestContext) throws ObjectBeanManagerException {
        return (HippoFolderBean) requestContext.getSiteContentBaseBean();
    }

    /**
     * Deletes the content node mapped to the provided {@link HippoBean}
     * @param servletRequest
     * @param hippoBean the bean which mapped data should be deleted
     * @return the path of the content node mapped to this bean before deletion
     * @throws RepositoryException
     * @throws ObjectBeanPersistenceException
     */
    protected String deleteHippoBean(HttpServletRequest servletRequest, HippoBean hippoBean) throws RepositoryException, ObjectBeanPersistenceException {
        String path = hippoBean.getPath();
        ObjectBeanPersistenceManager obpm = getPersistenceManager(getRequestContext(servletRequest));
        obpm.remove(hippoBean);
        obpm.save();
        return path;
    }

    /**
     * Looks up a child {@link HippoBean} for the provided {@link HippoBean}. If <code>relPath</code> is not
     * <code>null</code> or empty, a lookup using <code>relPath</code> is done. If <code>relPath</code> is
     * <code>null</code> or empty, a lookup using <code>primaryNodeType</code> is done; if any child beans are
     * found, the first is returned.
     * @param hippoBean
     * @param relPath             the path identifying the child bean, see also {@link HippoBean#getBean(String)}
     * @param primaryNodeType     the primary node type used to search for child beans, see also
     *                            {@link HippoBean#getChildBeans(String)}
     * @return the child bean or <code>null</code> if not found
     */
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

    /**
     * Creates an URL that can be used to specify the relation type for a link such as
     * <code>&lt;link href="http://localhost:8080/site/" rel="http://www.onehippo.org/cms7/hst/rest/relations/mount:site" title="Home Page"/&gt;</code>
     * @param iri          Internationalized Resource Identifier, e.g. <code>"http://www.onehippo.org/cms7/hst/rest/relations"</code>
     * @param simpleRel    relationship, e.g. <code>"mount:site"</code>
     * @return the combination of <code>iri</code> and <code>simpleRel</code>
     */
    protected String getQualifiedLinkRel(String iri, String simpleRel) {
    	if (iri != null) {
    		if (iri.endsWith("/")) {
    			return iri + simpleRel;
    		}
    		return iri + "/" + simpleRel;
    	}
    	return simpleRel;
    }

    /**
     * Creates an URL that can be used to specify the relation type for a link such as
     * <code>&lt;link href="http://localhost:8080/site/" rel="http://www.onehippo.org/cms7/hst/rest/relations/mount:site" title="Home Page"/&gt;</code>
     * @param simpleRel    relationship, e.g. "mount:site"
     * @return the combination of {@link #getRestRelationsBaseUri()} (which defaults to
     * <code>"http://www.onehippo.org/cms7/hst/rest/relations"</code>) and <code>simpleRel</code>
     */
    protected String getQualifiedLinkRel(String simpleRel) {
    	return getQualifiedLinkRel(getRestRelationsBaseUri(), simpleRel);
    }

    /**
     * Creates an URL that can be used to specify the relation type for a link such as
     * <code>&lt;link href="http://localhost:8080/site/" rel="http://www.onehippo.org/cms7/hst/rest/relations/mount:site" title="Home Page"/&gt;</code>
     * @param simpleRel    relationship, e.g. "mount:site"
     * @return the combination of {@link #HST_REST_RELATIONS_BASE_URI} (which is defined as
     * <code>"http://www.onehippo.org/cms7/hst/rest/relations"</code>) and <code>simpleRel</code>
     */
    protected String getHstQualifiedLinkRel(String simpleRel) {
    	return getQualifiedLinkRel(HST_REST_RELATIONS_BASE_URI,simpleRel);
    }

    /**
     * Creates an URL that can be used to specify the relation type for a link such as
     * <code>&lt;link href="http://localhost:8080/site/" rel="http://www.onehippo.org/cms7/hst/rest/relations/mount:site" title="Home Page"/&gt;</code>
     * @param mountName    mount name, e.g. "site"
     * @return the combination of {@link #HST_MOUNT_REL_PREFIX} (which is defined as
     * <code>"http://www.onehippo.org/cms7/hst/rest/relations/mount:"</code>) and <code>mountName</code>
     */
    protected String getLinkMountRelation(String mountName) {
    	return getHstQualifiedLinkRel(HST_MOUNT_REL_PREFIX+mountName);
    }

    /**
     * Creates a link to the provided {@link HippoBean}, exposed over the rest mount. If the bean is not exposed over
     * the mount, a page not found link is returned.
     * @param requestContext
     * @param hippoBean
     * @return
     */
    protected Link getNodeLink(HstRequestContext requestContext, HippoBean hippoBean) {
        return getRestLink(requestContext, hippoBean, null);
    }

    /**
     * Creates a link to the provided {@link HippoBean}, exposed over the rest mount. If the bean is not exposed over
     * the mount, a page not found link is returned.
     * @param requestContext
     * @param hippoBean
     * @param subPath subPath appended to the link created for {@link HippoBean}, can be <code>null</code>
     * @return
     */
    protected Link getRestLink(HstRequestContext requestContext, HippoBean hippoBean, String subPath) {
        return getMountLink(requestContext, hippoBean, MOUNT_ALIAS_REST, subPath);
    }

    /**
     * Creates a link to the provided {@link HippoBean}, exposed over the site mount. If the bean is not exposed over
     * the mount, a page not found link is returned.
     * @param requestContext
     * @param hippoBean
     * @return
     */
    protected Link getSiteLink(HstRequestContext requestContext, HippoBean hippoBean) {
        return getMountLink(requestContext, hippoBean, null, null);
    }

    /**
     * Creates a link to the provided {@link HippoBean}, exposed over the mount identified by the alias
     * <code>mountAliasName</code>. If no mount is found with the provided <code>mountAliasName</code>, a link is
     * created using the mount of the provided {@link HstRequestContext}. If the bean is not exposed over the mount,
     * a page not found link is returned.
     * @param requestContext
     * @param hippoBean
     * @param mountAliasName mount alias name, when <code>null</code> the mount with the alias <code>site</code> is
     *                       used to create the link
     * @param subPath        subPath appended to the link created for {@link HippoBean}, can be <code>null</code>
     * @return
     */
    protected Link getMountLink(HstRequestContext requestContext, HippoBean hippoBean, String mountAliasName, String subPath) {
        Link nodeLink = new Link();
        
        try {
            String usedMountAliasName = (mountAliasName == null ? MOUNT_ALIAS_SITE : mountAliasName);
            Mount mappedMount = requestContext.getMount(usedMountAliasName);
            nodeLink.setRel(getLinkMountRelation(usedMountAliasName));
            
            HstLink link = null;
            
            if (mappedMount != null) {
                link = requestContext.getHstLinkCreator().create(hippoBean.getNode(), mappedMount);
            } else {
                link = requestContext.getHstLinkCreator().create(hippoBean, requestContext);
            }
            
            if (link != null) {
                if (subPath != null) {
                    link.setSubPath(subPath);
                }
                
                String href = link.toUrlForm(requestContext, true);
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
            if (log.isDebugEnabled()) {
                log.warn("Failed to generate a page link. " + e.toString(), e);
            } else if (log.isWarnEnabled()) {
                log.warn("Failed to generate a page link. {}", e.toString());
            }
        }
        return nodeLink;
    }
    
    /**
     * Creates a persistable JCR session with the default credentials
     * <P>
     * <EM>Note: The client should invoke <CODE>logout()</CODE> method on the session after use.</EM>
     * </P>
     * <P>
     * Internally, {@link javax.jcr.Session#impersonate(Credentials)} method will be used to create a
     * persistable JCR session. The method is invoked on the session from the session pooling repository.
     * </P>
     * @param requestContext
     * @return
     */
    protected Session getPersistableSession(HstRequestContext requestContext) throws RepositoryException {
        Credentials credentials = requestContext.getContextCredentialsProvider().getWritableCredentials(requestContext);
        return getPersistableSession(requestContext, credentials);
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
     * @param requestContext
     * @return
     */
    protected Session getPersistableSession(HstRequestContext requestContext, Credentials credentials) throws RepositoryException {
        Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName());
        return repository.login(credentials);
    }

}
