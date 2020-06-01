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
package org.hippoecm.hst.jaxrs.services.content;

import java.io.InputStream;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageBean;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageSetBean;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.jaxrs.model.content.HippoGalleryImageRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
@Path("/hippogallery:imageset/")
public class ImageSetContentResource extends BaseImageSetContentResource {
    
    private static Logger log = LoggerFactory.getLogger(ImageSetContentResource.class);
    
    @GET
    @Path("/thumbnail/")
    public HippoGalleryImageRepresentation getThumbnailImageResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo) {
        return super.getImageResource(servletRequest, servletResponse, uriInfo, "hippogallery:thumbnail", "thumbnail/");
    }
    
    @GET
    @Path("/thumbnail/content/")
    public Response getThumbnailImageResourceContent(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo) {
        return super.getImageResourceContent(servletRequest, servletResponse, uriInfo, "hippogallery:thumbnail");
    }
    
    @PUT
    @Path("/thumbnail/content/")
    public String updateThumbnailImageResourceContent(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo,
            @MatrixParam("mimetype") @DefaultValue("application/octet-stream") String mimeType,
            InputStream thumbnailResourceStream) {
        return super.updateImageResourceContent(servletRequest, servletResponse, uriInfo, "hippogallery:thumbnail", mimeType, thumbnailResourceStream);
    }
    
    @POST
    @Path("/thumbnail/content/")
    @Consumes("multipart/form-data")
    public String updateThumbnailImageResourceContentByAttachments(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo,
            @MatrixParam("mimetype") @DefaultValue("application/octet-stream") String mimeType,
            List<Attachment> attachments) {
        return super.updateImageResourceContentByAttachments(servletRequest, servletResponse, uriInfo, "hippogallery:thumbnail", mimeType, attachments);
    }
    
    @GET
    @Path("/original/")
    public HippoGalleryImageRepresentation getOriginalImageResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo) {
        return super.getImageResource(servletRequest, servletResponse, uriInfo, "hippogallery:original", "original/");
    }
    
    @GET
    @Path("/original/content/")
    public Response getOriginalImageResourceContent(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo) {
        return super.getImageResourceContent(servletRequest, servletResponse, uriInfo, "hippogallery:original");
    }
    
    @PUT
    @Path("/original/content/")
    public String updateOriginalImageResourceContent(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo,
            @MatrixParam("mimetype") @DefaultValue("application/octet-stream") String mimeType,
            @MatrixParam("createthumbnail") @DefaultValue("true") String createThumbnail,
            @MatrixParam("thumbnailsize") @DefaultValue("60") String thumbnailSize,
            InputStream originalResourceStream) {
        String path = super.updateImageResourceContent(servletRequest, servletResponse, uriInfo, "hippogallery:original", mimeType, originalResourceStream);
        
        if (BooleanUtils.toBoolean(createThumbnail)) {
            createThumbnailFromOriginal(servletRequest, servletResponse, uriInfo, NumberUtils.toInt(thumbnailSize, 60));
        }
        
        return path;
    }
    
    @POST
    @Path("/original/content/")
    @Consumes("multipart/form-data")
    public String updateOriginalImageResourceContentByAttachments(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo,
            @MatrixParam("mimetype") @DefaultValue("application/octet-stream") String mimeType,
            @MatrixParam("createthumbnail") @DefaultValue("true") String createThumbnail,
            @MatrixParam("thumbnailsize") @DefaultValue("60") String thumbnailSize,
            List<Attachment> attachments) {
        String path = super.updateImageResourceContentByAttachments(servletRequest, servletResponse, uriInfo, "hippogallery:original", mimeType, attachments);
        
        if (BooleanUtils.toBoolean(createThumbnail)) {
            createThumbnailFromOriginal(servletRequest, servletResponse, uriInfo, NumberUtils.toInt(thumbnailSize, 60));
        }
        
        return path;
    }
    
    private void createThumbnailFromOriginal(HttpServletRequest servletRequest, HttpServletResponse servletResponse, UriInfo uriInfo, int thumbnailSize) {
        InputStream originalImageInputStream = null;
        InputStream thumbnailImageInputStream = null;
        
        try {
            HstRequestContext requestContext = getRequestContext(servletRequest);       
            HippoGalleryImageSetBean imageSetBean = getRequestContentBean(requestContext, HippoGalleryImageSetBean.class);
            HippoGalleryImageBean originalImageBean = imageSetBean.getBean("hippogallery:original", HippoGalleryImageBean.class);
            String originalImageMimeType = originalImageBean.getMimeType();
            originalImageInputStream = originalImageBean.getNode().getProperty("jcr:data").getBinary().getStream();
            thumbnailImageInputStream = HippoGalleryImageThumbnailCreator.createThumbnail(originalImageInputStream, thumbnailSize, originalImageMimeType);
            IOUtils.closeQuietly(originalImageInputStream);
            originalImageInputStream = null;
            updateThumbnailImageResourceContent(servletRequest, servletResponse, uriInfo, originalImageMimeType, thumbnailImageInputStream);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to generate thumbnail.", e);
            } else {
                log.warn("Failed to generate thumbnail. {}", e.toString());
            }
            throw new WebApplicationException(e);
        } finally {
            if (thumbnailImageInputStream != null) {
                IOUtils.closeQuietly(thumbnailImageInputStream);
            }
            if (originalImageInputStream != null) {
                IOUtils.closeQuietly(originalImageInputStream);
            }
        }
    }
}
