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

import org.hippoecm.hst.content.beans.standard.HippoFacetNavigationBean;
import org.hippoecm.hst.content.beans.standard.HippoResultSetBean;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.jaxrs.model.content.HippoFacetNavigationRepresentation;
import org.hippoecm.hst.jaxrs.model.content.HippoFacetResultSetRepresentation;
import org.hippoecm.hst.jaxrs.model.content.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
@Path("/hippofacnav:facetnavigation/")
public class HippoFacetNavigationContentResource extends HippoFolderContentResource {
    
    private static Logger log = LoggerFactory.getLogger(HippoFacetNavigationContentResource.class);
    
    @GET
    @Path("/")
    public HippoFacetNavigationRepresentation getFacetNavigationResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo) {
        try {
            HstRequestContext requestContext = getRequestContext(servletRequest);
            HippoFacetNavigationBean facetNavBean = getRequestContentBean(requestContext, HippoFacetNavigationBean.class);
            HippoFacetNavigationRepresentation facetNavRep = new HippoFacetNavigationRepresentation().represent(facetNavBean);
            facetNavRep.addLink(getNodeLink(requestContext, facetNavBean));
            facetNavRep.addLink(getSiteLink(requestContext, facetNavBean));
            return facetNavRep;
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
    @Path("/facetresult/")
    public HippoFacetResultSetRepresentation getFacetResultSetResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo) {
        HstRequestContext requestContext = getRequestContext(servletRequest);
        
        try {
            HippoFacetNavigationBean hippoFacetNavBean = (HippoFacetNavigationBean) getRequestContentBean(requestContext);
            HippoResultSetBean resultSetBean = hippoFacetNavBean.getResultSet();
            
            if (resultSetBean != null) {
                HippoFacetResultSetRepresentation resultSetRep = new HippoFacetResultSetRepresentation().represent(resultSetBean);
                resultSetRep.addLink(getNodeLink(requestContext, resultSetBean));
                resultSetRep.addLink(getSiteLink(requestContext, resultSetBean));
                Link parentLink = getNodeLink(requestContext, hippoFacetNavBean);
                parentLink.setRel(getHstQualifiedLinkRel("parent"));
                resultSetRep.addLink(parentLink);
                return resultSetRep;
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            throw new WebApplicationException(e);
        }
        
        return null;
    }
    
    @GET
    @Path("/root/")
    public HippoFacetNavigationRepresentation getRootFacetNavigationResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo) {
        HstRequestContext requestContext = getRequestContext(servletRequest);
        
        try {
            HippoFacetNavigationBean hippoFacetNavBean = (HippoFacetNavigationBean) getRequestContentBean(requestContext);
            HippoFacetNavigationBean rootFacetNavBean = hippoFacetNavBean.getRootFacetNavigationBean();
            
            if (rootFacetNavBean != null) {
                HippoFacetNavigationRepresentation rootFacetNavRep = new HippoFacetNavigationRepresentation().represent(rootFacetNavBean);
                rootFacetNavRep.addLink(getNodeLink(requestContext, rootFacetNavBean));
                rootFacetNavRep.addLink(getSiteLink(requestContext, rootFacetNavBean));
                return rootFacetNavRep;
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            throw new WebApplicationException(e);
        }
        
        return null;
    }
    
}
