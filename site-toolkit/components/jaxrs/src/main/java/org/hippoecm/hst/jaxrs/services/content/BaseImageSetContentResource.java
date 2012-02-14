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

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
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
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.hippoecm.hst.cache.HstCache;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageBean;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageSetBean;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.jaxrs.model.content.HippoGalleryImageRepresentation;
import org.hippoecm.hst.jaxrs.model.content.HippoGalleryImageSetRepresentation;
import org.hippoecm.hst.jaxrs.model.content.Link;
import org.hippoecm.hst.util.PathUtils;
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
    public HippoGalleryImageSetRepresentation getImageSetResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo) {
        try {
            HstRequestContext requestContext = getRequestContext(servletRequest);
            HippoGalleryImageSetBean imageSetBean = getRequestContentBean(requestContext, HippoGalleryImageSetBean.class);
            HippoGalleryImageSetRepresentation imageRep = new HippoGalleryImageSetRepresentation().represent(imageSetBean);
            imageRep.addLink(getMountLink(requestContext, imageSetBean, MOUNT_ALIAS_GALLERY, null));
            imageRep.addLink(getSiteLink(requestContext, imageSetBean));
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
    @Path("/image/{imageName}/")
    public HippoGalleryImageRepresentation getImageResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo,
            @PathParam("imageName") String imageName,
            @MatrixParam("subpath") String subPath) {
        try {
            HstRequestContext requestContext = getRequestContext(servletRequest);       
            HippoGalleryImageSetBean imageSetBean = getRequestContentBean(requestContext, HippoGalleryImageSetBean.class);
            HippoGalleryImageBean childImageBean = imageSetBean.getBean(imageName, HippoGalleryImageBean.class);
            
            if (childImageBean == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            
            HippoGalleryImageRepresentation childImageRep = new HippoGalleryImageRepresentation().represent(childImageBean);
            
            if (subPath == null) {
                subPath = "image/" + imageName;
            }
            
            childImageRep.addLink(getMountLink(requestContext, imageSetBean, MOUNT_ALIAS_GALLERY, subPath));
            Link parentLink = getMountLink(requestContext, imageSetBean, MOUNT_ALIAS_GALLERY, null);
            parentLink.setRel(getHstQualifiedLinkRel("parent"));
            childImageRep.addLink(parentLink);

            return childImageRep;
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
    @Path("/image/{imageName}/content")
    public Response getImageResourceContent(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo,
            @PathParam("imageName") String imageName) {
        try {
            HstRequestContext requestContext = getRequestContext(servletRequest);       
            HippoGalleryImageSetBean imageSetBean = getRequestContentBean(requestContext, HippoGalleryImageSetBean.class);
            HippoGalleryImageBean childImageBean = imageSetBean.getBean(imageName, HippoGalleryImageBean.class);
            
            if (childImageBean == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            
            final String mimeType = childImageBean.getMimeType();
            
            if (!childImageBean.getNode().hasProperty("jcr:data")) {
                return Response.ok(ArrayUtils.EMPTY_BYTE_ARRAY, MediaType.valueOf(mimeType)).build();
            }
            
            final Binary binary = childImageBean.getNode().getProperty("jcr:data").getBinary();
            
            StreamingOutput output = new StreamingOutput() {
                public void write(OutputStream output) throws IOException, WebApplicationException {
                    InputStream dataInputStream = null;
                    try {
                        dataInputStream = binary.getStream();
                        IOUtils.copy(dataInputStream, output);
                    } catch (RepositoryException e) {
                        throw new WebApplicationException(e);
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
    @Path("/image/{imageName}/content")
    public String updateImageResourceContent(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo,
            @PathParam("imageName") String imageName,
            @MatrixParam("mimetype") @DefaultValue("application/octet-stream") String mimeType,
            InputStream childResourceContentStream) {
        try {
            HstRequestContext requestContext = getRequestContext(servletRequest);       
            HippoGalleryImageSetBean imageSetBean = getRequestContentBean(requestContext, HippoGalleryImageSetBean.class);
            HippoGalleryImageBean childImageBean = imageSetBean.getBean(imageName, HippoGalleryImageBean.class);
            
            if (childImageBean == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            
            try {
                Node childResourceNode = childImageBean.getNode();
                childResourceNode.setProperty("jcr:mimeType", mimeType);
                childResourceNode.setProperty("jcr:data", childResourceNode.getSession().getValueFactory().createBinary(childResourceContentStream));
                childResourceNode.save();
                
                if (binariesCache != null) {
                    HstLink hstLink = requestContext.getHstLinkCreator().create(childImageBean, requestContext);
                    String relativeContentPath = 
                        StringUtils.removeStart(hstLink.getPath(), PathUtils.normalizePath(requestContext.getContainerConfiguration().getString("binaries.prefix.path", "/binaries")));
                    binariesCache.remove(relativeContentPath);
                }
                
                return childResourceNode.getPath();
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
    @Path("/image/{imageName}/content")
    @Consumes("multipart/form-data")
    public String updateImageResourceContentByAttachments(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo,
            @PathParam("imageName") String imageName,
            @MatrixParam("mimetype") @DefaultValue("application/octet-stream") String mimeType,
            List<Attachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return "";
        }
        
        Attachment attachment = attachments.get(0);
        String attachmentMimeType = attachment.getContentType().toString();
        
        if (attachmentMimeType == null) {
            attachmentMimeType = mimeType;
        }
        
        InputStream attachmentStream = null;
        
        try {
            attachmentStream = attachment.getDataHandler().getInputStream();
            return updateImageResourceContent(servletRequest, servletResponse, uriInfo, imageName, attachmentMimeType, attachmentStream);
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
