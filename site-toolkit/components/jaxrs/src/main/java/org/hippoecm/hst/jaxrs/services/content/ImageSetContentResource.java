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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.hippoecm.hst.jaxrs.model.content.HippoGalleryImageRepresentation;

/**
 * @version $Id$
 */
@Path("/hippogallery:imageset/")
public class ImageSetContentResource extends BaseImageSetContentResource {
    
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
            InputStream originalResourceStream) {
        return super.updateImageResourceContent(servletRequest, servletResponse, uriInfo, "hippogallery:original", mimeType, originalResourceStream);
    }
    
    @POST
    @Path("/original/content/")
    @Consumes("multipart/form-data")
    public String updateOriginalImageResourceContentByAttachments(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo,
            @MatrixParam("mimetype") @DefaultValue("application/octet-stream") String mimeType,
            List<Attachment> attachments) {
        return super.updateImageResourceContentByAttachments(servletRequest, servletResponse, uriInfo, "hippogallery:original", mimeType, attachments);
    }
}
