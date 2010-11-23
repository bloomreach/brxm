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

import javax.annotation.security.RolesAllowed;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.hippoecm.hst.content.beans.ContentNodeBinder;
import org.hippoecm.hst.content.beans.ContentNodeBindingException;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManager;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.demo.beans.ProductBean;
import org.hippoecm.hst.demo.jaxrs.model.ProductRepresentation;
import org.hippoecm.hst.jaxrs.services.content.AbstractContentResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
@Path("/demosite:productdocument/")
public class ProductContentResource extends AbstractContentResource {
    
    private static Logger log = LoggerFactory.getLogger(ProductContentResource.class);
    
    @GET
    @Path("/")
    public ProductRepresentation getProductResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo) {
        try {
            HstRequestContext requestContext = getRequestContext(servletRequest);       
            ProductBean productBean = (ProductBean) getRequestContentBean(requestContext);
            ProductRepresentation productRep = new ProductRepresentation().represent(productBean);
            productRep.addLink(getNodeLink(requestContext, productBean));
            productRep.addLink(getSiteLink(requestContext, productBean));
            return productRep;
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            
            throw new WebApplicationException(e);
        }
    }
    
    @RolesAllowed( { "admin", "author", "editor" } )
    @PUT
    @Path("/")
    public ProductRepresentation updateProductResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo,
            ProductRepresentation productRepresentation) {
        ProductBean productBean = null;
        HstRequestContext requestContext = getRequestContext(servletRequest);
        
        try {
            productBean = (ProductBean) getRequestContentBean(requestContext);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            
            throw new WebApplicationException(e);
        }
        
        try {
            WorkflowPersistenceManager wpm = (WorkflowPersistenceManager) getContentPersistenceManager(requestContext);
            final ProductRepresentation productRepresentationInput = productRepresentation;
            
            wpm.update(productBean, new ContentNodeBinder() {
                public boolean bind(Object content, Node node) throws ContentNodeBindingException {
                    try {
                        node.setProperty("demosite:brand", productRepresentationInput.getBrand());
                        node.setProperty("demosite:color", productRepresentationInput.getColor());
                        node.setProperty("demosite:product", productRepresentationInput.getProduct());
                        node.setProperty("demosite:price", productRepresentationInput.getPrice());
                        node.setProperty("hippostd:tags", productRepresentationInput.getTags());
                        return true;
                    } catch (RepositoryException e) {
                        throw new ContentNodeBindingException(e);
                    }
                }
            });
            wpm.save();
            
            productBean = (ProductBean) wpm.getObject(productBean.getPath());
            productRepresentation = new ProductRepresentation().represent(productBean);
            productRepresentation.addLink(getNodeLink(requestContext, productBean));
            productRepresentation.addLink(getSiteLink(requestContext, productBean));
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            
            throw new WebApplicationException(e);
        }
        
        return productRepresentation;
    }
}
