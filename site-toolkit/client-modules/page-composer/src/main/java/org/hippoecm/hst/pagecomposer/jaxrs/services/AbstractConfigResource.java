/*
 *  Copyright 2010-2014 Hippo B.V. (http://www.onehippo.com)
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

import java.util.concurrent.Callable;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.ArrayUtils;
import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.cxf.CXFJaxrsHstConfigService;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ExtResponseRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractConfigResource {
    
    private static Logger log = LoggerFactory.getLogger(AbstractConfigResource.class);

    protected HstRequestContext getRequestContext(HttpServletRequest servletRequest) {
        return (HstRequestContext) servletRequest.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
    }
 
    protected String getRequestConfigIdentifier(HstRequestContext requestContext) {
        return (String) requestContext.getAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER);
    }

    protected HstSite getEditingLiveSite(final HstRequestContext requestContext) {
        final Mount mount = getEditingMount(requestContext);
        return mount.getHstSite();
    }

    protected HstSite getEditingPreviewSite(final HstRequestContext requestContext) {
        final Mount liveMount = getEditingMount(requestContext);
        assertIsContextualizableMount(liveMount);
        return ((ContextualizableMount)liveMount).getPreviewHstSite();
    }

    protected String getEditingPreviewChannelPath(final HstRequestContext requestContext) {
        final Mount liveMount = getEditingMount(requestContext);
        assertIsContextualizableMount(liveMount);
        final String previewChannelPath =  liveMount.getChannelPath()+ "-preview";
        return previewChannelPath;
    }
    /**
     * @return the preview {@link Channel} and <code>null</code> if there is no preview channel available
     */
    protected Channel getEditingPreviewChannel(final HstRequestContext requestContext) {
        final String previewChannelPath =  getEditingPreviewChannelPath(requestContext);
        return requestContext.getVirtualHost().getVirtualHosts().getChannelByJcrPath(previewChannelPath);
    }

    protected boolean hasPreviewConfiguration(final Mount mount) {
        assertIsContextualizableMount(mount);
        return  ((ContextualizableMount)mount).getPreviewHstSite().hasPreviewConfiguration();
    }

    protected Mount getEditingMount(final HstRequestContext requestContext) {
        HttpSession session = requestContext.getServletRequest().getSession(true);
        final String renderingMountId = (String)session.getAttribute(ContainerConstants.CMS_REQUEST_RENDERING_MOUNT_ID);
        if (renderingMountId == null) {
            throw new IllegalStateException("Cound not find rendering mount id '"+renderingMountId+"' on request session.");
        }
        Mount mount = requestContext.getVirtualHost().getVirtualHosts().getMountByIdentifier(renderingMountId);
        if (mount == null) {
            throw new IllegalStateException("Cound not find a Mount for identifier + '"+renderingMountId+"'");
        }
        return mount;
    }

    protected Node getRequestConfigNode(final HstRequestContext requestContext, final String expectedNodeType) {
        String id = getRequestConfigIdentifier(requestContext);
        if(id == null) {
            log.warn("Cannot get requestConfigNode because no attr '{}' on request. Return null", CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER);
        }
        try {
            Node configNode = requestContext.getSession().getNodeByIdentifier(id);
            if (configNode.isNodeType(expectedNodeType)) {
                return configNode;
            } else {
                log.warn("Expected node was of type '' but actual node is of type '{}'. Return null.", expectedNodeType, configNode.getPrimaryNodeType().getName());
                return null;
            }
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

    protected ObjectConverter getObjectConverter(HstRequestContext requestContext) {
        return requestContext.getContentBeansTool().getObjectConverter();
    }

    protected void assertIsContextualizableMount(final Mount liveMount) throws IllegalStateException{
        if (!(liveMount instanceof  ContextualizableMount)) {
            throw new IllegalStateException("Expected a mount of type "+ContextualizableMount.class.getName()+"" +
                    " but found '"+liveMount.toString()+"' which is of type " + liveMount.getClass().getName());
        }
    }

    protected Response tryExecute(Callable<Response> callable) {
        try {
            return callable.call();
        } catch (IllegalStateException | IllegalArgumentException | ItemNotFoundException e) {
            return logAndReturnClientError(e);
        } catch (Exception e) {
            return logAndReturnServerError(e);
        }
    }

    protected Response logAndReturnServerError(Exception e) {
        if (log.isDebugEnabled()) {
            log.warn(e.toString(), e);
        } else {
            log.warn(e.toString());
        }
        return error(e.getMessage());
    }

    protected Response logAndReturnClientError(Exception e) {
        log.info(e.toString());
        final ExtResponseRepresentation entity = new ExtResponseRepresentation();
        entity.setSuccess(false);
        entity.setMessage(e.getMessage());
        return Response.status(Response.Status.BAD_REQUEST).entity(entity).build();
    }


    protected CanonicalInfo getCanonicalInfo(final Object o) throws IllegalStateException {
        if (o instanceof CanonicalInfo) {
            return (CanonicalInfo)o;
        }
        throw new IllegalStateException("HstSiteMenuItemConfiguration not instanceof CanonicalInfo");
    }
}
