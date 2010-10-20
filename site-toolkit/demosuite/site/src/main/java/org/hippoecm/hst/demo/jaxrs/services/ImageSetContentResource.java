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
package org.hippoecm.hst.demo.jaxrs.services;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.hippoecm.hst.content.beans.standard.HippoImageBean;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.demo.jaxrs.model.HippoResourceRepresentation;
import org.hippoecm.hst.demo.jaxrs.model.ImageSetRepresentation;
import org.hippoecm.hst.jaxrs.services.content.AbstractContentResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
@Path("/hippogallery:exampleImageSet/")
public class ImageSetContentResource extends AbstractContentResource {
    
    private static Logger log = LoggerFactory.getLogger(ImageSetContentResource.class);
    
    @GET
    @Path("/")
    public ImageSetRepresentation getImageSetResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo) {
        try {
            HstRequestContext requestContext = getRequestContext(servletRequest);       
            HippoImageBean imageBean = (HippoImageBean) getRequestContentBean(requestContext);
            return new ImageSetRepresentation().represent(imageBean);
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
    @Path("/thumbnail/")
    public HippoResourceRepresentation getImageSetResourceThumbnail(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo) {
        try {
            HstRequestContext requestContext = getRequestContext(servletRequest);       
            HippoImageBean imageBean = (HippoImageBean) getRequestContentBean(requestContext);
            return new HippoResourceRepresentation().represent(imageBean.getThumbnail());
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
    @Path("/picture/")
    public HippoResourceRepresentation getImageSetResourcePicture(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo) {
        try {
            HstRequestContext requestContext = getRequestContext(servletRequest);       
            HippoImageBean imageBean = (HippoImageBean) getRequestContentBean(requestContext);
            return new HippoResourceRepresentation().represent(imageBean.getPicture());
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            
            throw new WebApplicationException(e);
        }
    }
   
}
