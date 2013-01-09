/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.services;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.internal.MountDecorator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.jaxrs.util.AnnotatedContentBeanClassesScanner;
import org.hippoecm.hst.pagecomposer.jaxrs.cxf.CXFJaxrsHstConfigService;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ExtResponseRepresentation;
import org.hippoecm.hst.util.ObjectConverterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */

public class AbstractConfigResource {
    
    private static Logger log = LoggerFactory.getLogger(AbstractConfigResource.class);

    private ObjectConverter objectConverter;
    private HstManager hstManager;
    private MountDecorator mountDecorator;
    private List<Class<? extends HippoBean>> annotatedClasses;
    public static final String BEANS_ANNOTATED_CLASSES_CONF_PARAM = "hst-beans-annotated-classes";

    private static final String CURRENT_MOUNT_CANONICAL_CONTENT_PATH = AbstractConfigResource.class.getName() + "-CurrentMountCanonicalContentPath";

    public void setHstManager(final HstManager hstManager) {
        this.hstManager = hstManager;
    }

    public void setMountDecorator(MountDecorator mountDecorator) {
        this.mountDecorator = mountDecorator;
    }

    public HstManager getHstManager() {
        return hstManager;
    }

    protected HstRequestContext getRequestContext(HttpServletRequest servletRequest) {
        return (HstRequestContext) servletRequest.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
    }
 
    protected String getRequestConfigIdentifier(HstRequestContext requestContext) {
        return (String) requestContext.getAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER);
    }

    protected HstSite getEditingLiveSite(final HstRequestContext requestContext) {
        final Mount mount = getEditingLiveMount(requestContext);
        return mount.getHstSite();
    }

    protected HstSite getEditingPreviewSite(final HstRequestContext requestContext) {
        final Mount mount = getEditingPreviewMount(requestContext);
        return mount.getHstSite();
    }

    protected Mount getEditingLiveMount(final HstRequestContext requestContext) {
        final String hstMountIdentifier = getRequestConfigIdentifier(requestContext);
        Mount mount = requestContext.getVirtualHost().getVirtualHosts().getMountByIdentifier(hstMountIdentifier);
        if (mount == null) {
            throw new IllegalStateException("Cound not find a Mount for identifier + '"+hstMountIdentifier+"'");
        }
        return mount;
    }
    
    protected Mount getEditingPreviewMount(final HstRequestContext requestContext) {
        Mount mount =  getEditingLiveMount(requestContext);
        if(!(mount instanceof ContextualizableMount )) {
            log.warn("Mount must be an instance of ContextualizableMount. Cannot create a preview. Return mount as is");
            return mount;
        }
        Mount previewMount = mountDecorator.decorateMountAsPreview((ContextualizableMount)mount);
        return previewMount;
    }

    protected void setCurrentMountCanonicalContentPath(HttpServletRequest servletRequest, String canonicalContentPath) {
        HttpSession session = servletRequest.getSession(true);
        session.setAttribute(CURRENT_MOUNT_CANONICAL_CONTENT_PATH, canonicalContentPath);
    }

    protected String getCurrentMountCanonicalContentPath(HttpServletRequest servletRequest) {
        HttpSession session = servletRequest.getSession(true);
        Object result = session.getAttribute(CURRENT_MOUNT_CANONICAL_CONTENT_PATH);
        return result == null ? null : result.toString();
    }

    protected Node getRequestConfigNode(HstRequestContext requestContext) {
        String id = (String)requestContext.getAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER);
        if(id == null) {
            log.warn("Cannot get requestConfigNode because no attr '{}' on request. Return null", CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER);
        }
        try {
            return requestContext.getSession().getNodeByIdentifier(id);
        } catch (RepositoryException e) {
            log.warn("Cannot find requestConfigNode because could not get node with id '{}' : {}", id, e.toString());
            return null;
        }
        
    }

    protected Response ok(String msg) {
        return ok(msg, ArrayUtils.EMPTY_STRING_ARRAY);
    }

    protected Response ok(String msg, Object data) {
        ExtResponseRepresentation entity = new ExtResponseRepresentation(data);
        entity.setMessage(msg);
        entity.setSuccess(true);
        return Response.ok().entity(entity).build();
    }
    
    protected Response error(String msg) {
        return error(msg, ArrayUtils.EMPTY_STRING_ARRAY);
    }

    protected Response error(String msg, Object data) {
        ExtResponseRepresentation entity = new ExtResponseRepresentation(data);
        entity.setMessage(msg);
        entity.setSuccess(false);
        return Response.serverError().entity(entity).build();
    }


    protected Response created(String msg) {
        ExtResponseRepresentation entity = new ExtResponseRepresentation();
        entity.setMessage(msg);
        entity.setSuccess(true);
        return Response.status(Response.Status.CREATED).entity(entity).build();
    }
    
    protected  Response conflict(String msg) {
        ExtResponseRepresentation entity = new ExtResponseRepresentation();
        entity.setMessage(msg);
        entity.setSuccess(false);
        return Response.status(Response.Status.CONFLICT).entity(entity).build();
    } 

    
    protected List<Class<? extends HippoBean>> getAnnotatedClasses(HstRequestContext requestContext) {
        if (annotatedClasses == null) {
            String annoClassPathResourcePath = "";

            if (StringUtils.isBlank(annoClassPathResourcePath)) {
                annoClassPathResourcePath = requestContext.getServletContext().getInitParameter(BEANS_ANNOTATED_CLASSES_CONF_PARAM);
            }

            annotatedClasses = AnnotatedContentBeanClassesScanner.scanAnnotatedContentBeanClasses(requestContext, annoClassPathResourcePath);
        }
        return annotatedClasses;
    }

    protected ObjectConverter getObjectConverter(HstRequestContext requestContext) {
        if (objectConverter == null) {
            List<Class<? extends HippoBean>> annotatedClasses = getAnnotatedClasses(requestContext);
            objectConverter = ObjectConverterUtils.createObjectConverter(annotatedClasses);
        }
        return objectConverter;
    }

}
