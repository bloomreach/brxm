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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowCallbackHandler;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManager;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.demo.beans.ProductBean;
import org.hippoecm.hst.demo.jaxrs.model.ProductRepresentation;
import org.hippoecm.hst.demo.jaxrs.services.util.ResponseUtils;
import org.hippoecm.hst.jaxrs.services.content.AbstractContentResource;
import org.hippoecm.repository.reviewedactions.FullReviewedActionsWorkflow;
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
            ProductBean productBean = getRequestContentBean(requestContext, ProductBean.class);
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
            
            throw new WebApplicationException(e, ResponseUtils.buildServerErrorResponse(e));
        }
    }
    
    @RolesAllowed( { "admin", "author", "editor" } )
    @PUT
    @Path("/")
    public ProductRepresentation updateProductResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo,
            @QueryParam("wfaction") String workflowAction,
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
            
            throw new WebApplicationException(e, ResponseUtils.buildServerErrorResponse(e));
        }
        
        try {
            WorkflowPersistenceManager wpm = (WorkflowPersistenceManager) getContentPersistenceManager(requestContext);
            // Note: Need to retrieve bean again by persistableSession because WorkflowPersistenceManager#update() uses its underlying JCR node to save.
            productBean = (ProductBean) wpm.getObject(productBean.getPath());
            productBean.setProduct(productRepresentation.getProduct());
            productBean.setColor(productRepresentation.getColor());
            productBean.setType(productRepresentation.getType());
            productBean.setPrice(productRepresentation.getPrice());
            productBean.setTags(productRepresentation.getTags());
            
            if (StringUtils.equals("requestPublication", workflowAction)) {
                wpm.setWorkflowCallbackHandler(new WorkflowCallbackHandler<FullReviewedActionsWorkflow>() {
                    public void processWorkflow(FullReviewedActionsWorkflow wf) throws Exception {
                        wf.requestPublication();
                    }
                });
            } else if (StringUtils.equals("publish", workflowAction)) {
                wpm.setWorkflowCallbackHandler(new WorkflowCallbackHandler<FullReviewedActionsWorkflow>() {
                    public void processWorkflow(FullReviewedActionsWorkflow wf) throws Exception {
                        wf.publish();
                    }
                });
            } else if (StringUtils.equals("requestDepublication", workflowAction)) {
                wpm.setWorkflowCallbackHandler(new WorkflowCallbackHandler<FullReviewedActionsWorkflow>() {
                    public void processWorkflow(FullReviewedActionsWorkflow wf) throws Exception {
                        wf.requestDepublication();
                    }
                });
            } else if (StringUtils.equals("depublish", workflowAction)) {
                wpm.setWorkflowCallbackHandler(new WorkflowCallbackHandler<FullReviewedActionsWorkflow>() {
                    public void processWorkflow(FullReviewedActionsWorkflow wf) throws Exception {
                        wf.depublish();
                    }
                });
            }

            wpm.update(productBean);
            wpm.save();

            // Note: Retrieve bean again from the repository to return.
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
            
            throw new WebApplicationException(e, ResponseUtils.buildServerErrorResponse(e));
        }
        
        return productRepresentation;
    }
    
    @RolesAllowed( { "admin", "author", "editor" } )
    @DELETE
    @Path("/")
    public Response deleteProductResources(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo) {
        
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
            
            throw new WebApplicationException(e, ResponseUtils.buildServerErrorResponse(e));
        }
        
        try {
            WorkflowPersistenceManager wpm = (WorkflowPersistenceManager) getContentPersistenceManager(requestContext);
            productBean = (ProductBean) wpm.getObject(productBean.getPath());
            wpm.remove(productBean);
            wpm.save();
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            
            throw new WebApplicationException(e, ResponseUtils.buildServerErrorResponse(e));
        }
        
        return Response.ok().build();
    }
}
