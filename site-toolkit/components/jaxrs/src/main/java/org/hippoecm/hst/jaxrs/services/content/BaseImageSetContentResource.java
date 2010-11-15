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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.hippoecm.hst.cache.HstCache;
import org.hippoecm.hst.content.beans.standard.HippoImageBean;
import org.hippoecm.hst.content.beans.standard.HippoResourceBean;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.jaxrs.model.content.HippoImageRepresentation;
import org.hippoecm.hst.jaxrs.model.content.HippoResourceRepresentation;
import org.hippoecm.hst.jaxrs.model.content.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
@Path("/{galleryResourceType}/")
public class BaseImageSetContentResource extends AbstractContentResource {
    
    private static Logger log = LoggerFactory.getLogger(BaseImageSetContentResource.class);
    
    private HstCache binariesCache;
    
    public HstCache getBinariesCache() {
        return binariesCache;
    }

    public void setBinariesCache(HstCache binariesCache) {
        this.binariesCache = binariesCache;
    }

    @GET
    @Path("/")
    public HippoImageRepresentation getImageResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo) {
        try {
            HstRequestContext requestContext = getRequestContext(servletRequest);
            HippoImageBean imageBean = (HippoImageBean) getRequestContentBean(requestContext);
            HippoImageRepresentation imageRep = new HippoImageRepresentation().represent(imageBean);
            imageRep.addLink(getMountLink(requestContext, imageBean, MOUNT_ALIAS_GALLERY, null));
            imageRep.addLink(getSiteLink(requestContext, imageBean));
            return imageRep;
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            
            throw new WebApplicationException(e);
        }
    }
    
    @GET
    @Path("/resource/{childResourceName}/")
    public HippoResourceRepresentation getChildResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo,
            @PathParam("childResourceName") String childResourceName,
            @MatrixParam("subpath") String subPath) {
        try {
            HstRequestContext requestContext = getRequestContext(servletRequest);       
            HippoImageBean imageBean = (HippoImageBean) getRequestContentBean(requestContext);
            HippoResourceBean childResourceBean = (HippoResourceBean) imageBean.getBean(childResourceName);
            
            if (childResourceBean == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            
            HippoResourceRepresentation childResourceRep = new HippoResourceRepresentation().represent(childResourceBean);
            
            if (subPath == null) {
                subPath = "resource/" + childResourceName;
            }
            
            childResourceRep.addLink(getMountLink(requestContext, imageBean, MOUNT_ALIAS_GALLERY, subPath));
            Link ownerLink = getMountLink(requestContext, imageBean, MOUNT_ALIAS_GALLERY, null);
            ownerLink.setRel("owner");
            childResourceRep.addLink(ownerLink);

            return childResourceRep;
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            
            throw new WebApplicationException(e);
        }
    }
    
    @GET
    @Path("/resource/{childResourceName}/content")
    public Response getChildResourceContent(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo,
            @PathParam("childResourceName") String childResourceName) {
        try {
            HstRequestContext requestContext = getRequestContext(servletRequest);       
            HippoImageBean imageBean = (HippoImageBean) getRequestContentBean(requestContext);
            HippoResourceBean childResourceBean = (HippoResourceBean) imageBean.getBean(childResourceName);
            
            if (childResourceBean == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            
            final String mimeType = childResourceBean.getMimeType();
            
            if (!childResourceBean.getNode().hasProperty("jcr:data")) {
                return Response.ok(new byte[0], MediaType.valueOf(mimeType)).build();
            }
            
            final InputStream dataInputStream = childResourceBean.getNode().getProperty("jcr:data").getStream();
            
            StreamingOutput output = new StreamingOutput() {
                public void write(OutputStream output) throws IOException, WebApplicationException {
                    try {
                        IOUtils.copy(dataInputStream, output);
                    } finally {
                        IOUtils.closeQuietly(dataInputStream);
                        IOUtils.closeQuietly(output);
                    }
                }
            };
            
            return Response.ok(output, MediaType.valueOf(mimeType)).build();
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            
            throw new WebApplicationException(e);
        }
    }
    
    @PUT
    @Path("/resource/{childResourceName}/content")
    public void updateChildResourceContent(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo,
            @PathParam("childResourceName") String childResourceName,
            @MatrixParam("mimetype") @DefaultValue("application/octet-stream") String mimeType,
            InputStream childResourceContentStream) {
        try {
            HstRequestContext requestContext = getRequestContext(servletRequest);       
            HippoImageBean imageBean = (HippoImageBean) getRequestContentBean(requestContext);
            HippoResourceBean childResourceBean = (HippoResourceBean) imageBean.getBean(childResourceName);
            
            if (childResourceBean == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            
            try {
                Node childResourceNode = childResourceBean.getNode();
                Property mimeTypeProp = childResourceNode.getProperty("jcr:mimeType");
                mimeTypeProp.setValue(mimeType);
                Property dataProp = childResourceNode.getProperty("jcr:data");
                dataProp.setValue(childResourceContentStream);
                dataProp.save();
                childResourceNode.save();
                
                if (binariesCache != null) {
                    HstLink hstLink = requestContext.getHstLinkCreator().create(imageBean, requestContext);
                    String contentPath = hstLink.getMount().getMountPoint() + "/" + hstLink.getPath();
                    binariesCache.remove(contentPath);
                }
            } finally {
                IOUtils.closeQuietly(childResourceContentStream);
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            
            throw new WebApplicationException(e);
        }
    }
    
    @POST
    @Path("/resource/{childResourceName}/content")
    @Consumes("multipart/form-data")
    public void updateChildResourceContentByAttachments(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo,
            @PathParam("childResourceName") String childResourceName,
            @MatrixParam("mimetype") @DefaultValue("application/octet-stream") String mimeType,
            List<Attachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }
        
        Attachment attachment = attachments.get(0);
        String attachmentMimeType = attachment.getContentType().toString();
        
        if (attachmentMimeType == null) {
            attachmentMimeType = mimeType;
        }
        
        InputStream attachmentStream = null;
        
        try {
            attachmentStream = attachment.getDataHandler().getInputStream();
            updateChildResourceContent(servletRequest, servletResponse, uriInfo, childResourceName, attachmentMimeType, attachmentStream);
        } catch (IOException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content stream.", e);
            } else {
                log.warn("Failed to retrieve content stream. {}", e.toString());
            }
            
            throw new WebApplicationException(e);
        } finally {
            IOUtils.closeQuietly(attachmentStream);
        }
    }
    
}
