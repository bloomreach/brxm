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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.hippoecm.hst.content.beans.standard.HippoFacetChildNavigationBean;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.jaxrs.model.content.HippoFacetChildNavigationRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
@Path("/hippofacnav:facetsubnavigation/")
public class HippoFacetSubNavigationContentResource extends HippoFacetNavigationContentResource {
    
    private static Logger log = LoggerFactory.getLogger(HippoFacetSubNavigationContentResource.class);
    
    @GET
    @Path("/")
    public HippoFacetChildNavigationRepresentation getFacetNavigationResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo) {
        try {
            HstRequestContext requestContext = getRequestContext(servletRequest);
            HippoFacetChildNavigationBean facetChildNavBean = getRequestContentBean(requestContext, HippoFacetChildNavigationBean.class);
            HippoFacetChildNavigationRepresentation facetChildNavRep = new HippoFacetChildNavigationRepresentation().represent(facetChildNavBean);
            facetChildNavRep.addLink(getNodeLink(requestContext, facetChildNavBean));
            facetChildNavRep.addLink(getSiteLink(requestContext, facetChildNavBean));
            return facetChildNavRep;
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
