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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.hippoecm.hst.jaxrs.model.content.HippoResourceRepresentation;

/**
 * @version $Id$
 */
@Path("/hippogallery:exampleImageSet/")
public class ExampleImageSetContentResource extends BaseImageSetContentResource {
    
    @GET
    @Path("/thumbnail/")
    public HippoResourceRepresentation getThumbnailImageResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo) {
        return super.getChildResource(servletRequest, servletResponse, uriInfo, "hippogallery:thumbnail");
    }
    
    @GET
    @Path("/thumbnail/content/")
    public Response getThumbnailImageResourceContent(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo) {
        return super.getChildResourceContent(servletRequest, servletResponse, uriInfo, "hippogallery:thumbnail");
    }
    
    @PUT
    @Path("/thumbnail/content/")
    public void updateThumbnailImageResourceContent(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo,
            @MatrixParam("mimetype") @DefaultValue("application/octet-stream") String mimeType,
            InputStream thumbnailResourceStream) {
        super.updateChildResourceContent(servletRequest, servletResponse, uriInfo, "hippogallery:thumbnail", mimeType, thumbnailResourceStream);
    }
    
    @GET
    @Path("/picture/")
    public HippoResourceRepresentation getPictureImageResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo) {
        return super.getChildResource(servletRequest, servletResponse, uriInfo, "hippogallery:picture");
    }
    
    @GET
    @Path("/picture/content/")
    public Response getPictureImageResourceContent(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo) {
        return super.getChildResourceContent(servletRequest, servletResponse, uriInfo, "hippogallery:picture");
    }
    
    @PUT
    @Path("/picture/content/")
    public void updatePictureImageResourceContent(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo,
            @MatrixParam("mimetype") @DefaultValue("application/octet-stream") String mimeType,
            InputStream pictureResourceStream) {
        super.updateChildResourceContent(servletRequest, servletResponse, uriInfo, "hippogallery:picture", mimeType, pictureResourceStream);
    }
    
}
