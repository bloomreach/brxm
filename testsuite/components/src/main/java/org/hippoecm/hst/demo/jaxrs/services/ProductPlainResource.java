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
package org.hippoecm.hst.demo.jaxrs.services;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.annotations.Persistable;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.manager.workflow.BaseWorkflowCallbackHandler;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManager;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.demo.beans.ProductBean;
import org.hippoecm.hst.demo.jaxrs.model.ProductRepresentation;
import org.hippoecm.hst.demo.jaxrs.services.util.ResponseUtils;
import org.hippoecm.hst.jaxrs.services.AbstractResource;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * @version $Id$
 */
@Path("/products/")
@Api(value = "Products")
public class ProductPlainResource extends AbstractResource {
    
    private static Logger log = LoggerFactory.getLogger(ProductPlainResource.class);
    
    @GET
    @Path("/search/")
    @ApiOperation(value = "Finds products",
        response = ProductRepresentation.class,
        responseContainer = "List")
    public List<ProductRepresentation> searchProductResources(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo,
            @QueryParam("brand") String brand,
            @QueryParam("product") String productType,
            @QueryParam("color") String color,
            @QueryParam("query") String query,
            @QueryParam("begin") @DefaultValue("0") int begin,
            @QueryParam("psize") @DefaultValue("10") int pageSize,
            @MatrixParam("sitelink") @DefaultValue("true") boolean siteLink) {
        
        List<ProductRepresentation> products = new ArrayList<ProductRepresentation>();
        
        try {
            // You can use either getRequestContext(servletRequest) or RequestContextProvider.get().
            //
            //HstRequestContext requestContext = getRequestContext(servletRequest);
            HstRequestContext requestContext = RequestContextProvider.get();
            
            HstQueryManager hstQueryManager = getHstQueryManager(requestContext);
            
            // for plain jaxrs, we do not have a requestContentBean because no resolved sitemapitem
            HippoBean scope = getMountContentBaseBean(requestContext);
            
            HstQuery hstQuery = hstQueryManager.createQuery(scope, ProductBean.class, true);
            hstQuery.setOffset(begin);
            hstQuery.setLimit(pageSize);
            
            Filter filter = hstQuery.createFilter();
            
            if (!StringUtils.isEmpty(brand)) {
                filter.addEqualTo("demosite:brand", brand);
            }
            if (!StringUtils.isEmpty(productType)) {
                filter.addEqualTo("demosite:product", productType);
            }
            if (!StringUtils.isEmpty(color)) {
                filter.addEqualTo("demosite:color", color);
            }
            if (!StringUtils.isEmpty(query)) {
                filter.addContains(".", query);
            }
            
            hstQuery.setFilter(filter);
            hstQuery.addOrderByDescending("demosite:price");
            
            HstQueryResult result = hstQuery.execute();
            HippoBeanIterator iterator = result.getHippoBeans();

            while (iterator.hasNext()) {
                ProductBean productBean = (ProductBean) iterator.nextHippoBean();
                
                if (productBean != null) {
                    ProductRepresentation productRep = new ProductRepresentation().represent(productBean);
                    productRep.addLink(getNodeLink(requestContext, productBean));
                    
                    if (siteLink) {
                        productRep.addLink(getSiteLink(requestContext, productBean));
                    }
                    
                    products.add(productRep);
                }
            }
            
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to query products.", e);
            } else {
                log.warn("Failed to query products. {}", e.toString());
            }
            
            throw new WebApplicationException(e, ResponseUtils.buildServerErrorResponse(e));
        }
        
        return products;
    }
    
    @GET
    @Path("/brand/{brand}/")
    @ApiOperation(value = "Finds product of the brand", response = ProductRepresentation.class)
    public ProductRepresentation getProductResources(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo,
            @PathParam("brand") String brand,
            @MatrixParam("sitelink") @DefaultValue("true") boolean siteLink) {
        
        ProductRepresentation productRep = null;
        
        if (!StringUtils.isEmpty(brand)) {
            try {
                // You can use either getRequestContext(servletRequest) or RequestContextProvider.get().
                //
                //HstRequestContext requestContext = getRequestContext(servletRequest);
                HstRequestContext requestContext = RequestContextProvider.get();
                
                HstQueryManager hstQueryManager = getHstQueryManager(requestContext);
                
                // for plain jaxrs, we do not have a requestContentBean because no resolved sitemapitem
                HippoBean scope = getMountContentBaseBean(requestContext);
                
                HstQuery hstQuery = hstQueryManager.createQuery(scope, ProductBean.class, true);
                hstQuery.setOffset(0);
                hstQuery.setLimit(1);
                
                Filter filter = hstQuery.createFilter();
                filter.addEqualTo("demosite:brand", brand);
                
                hstQuery.setFilter(filter);
                
                HstQueryResult result = hstQuery.execute();
                HippoBeanIterator iterator = result.getHippoBeans();
    
                if (iterator.hasNext()) {
                    ProductBean productBean = (ProductBean) iterator.nextHippoBean();
                    
                    if (productBean != null) {
                        productRep = new ProductRepresentation().represent(productBean);
                        productRep.addLink(getNodeLink(requestContext, productBean));
                        
                        if (siteLink) {
                            productRep.addLink(getSiteLink(requestContext, productBean));
                        }
                    }
                }
                
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.warn("Failed to query product by tag.", e);
                } else {
                    log.warn("Failed to query product by tag. {}", e.toString());
                }
                
                throw new WebApplicationException(e, ResponseUtils.buildServerErrorResponse(e));
            }
        }
        
        if (productRep == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND.getStatusCode());
        }
        
        return productRep;
    }

    @RolesAllowed( { "admin", "author", "editor" } )
    @Persistable
    @POST
    public ProductRepresentation createProductResources(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo,
            ProductRepresentation productRepresentation,
            @MatrixParam("sitelink") @DefaultValue("true") boolean siteLink) {
        
        HstRequestContext requestContext = getRequestContext(servletRequest);
        
        try {
            WorkflowPersistenceManager wpm = (WorkflowPersistenceManager) getPersistenceManager(requestContext);
            HippoFolderBean contentBaseFolder = getMountContentBaseBean(requestContext);
            String productFolderPath = contentBaseFolder.getPath() + "/products";
            String beanPath = wpm.createAndReturn(productFolderPath, "demosite:productdocument", productRepresentation.getBrand(), true);
            ProductBean productBean = (ProductBean) wpm.getObject(beanPath);
            
            productBean.setBrand(productRepresentation.getBrand());
            productBean.setProduct(productRepresentation.getProduct());
            productBean.setColor(productRepresentation.getColor());
            productBean.setType(productRepresentation.getType());
            productBean.setPrice(productRepresentation.getPrice());
            productBean.setTags(productRepresentation.getTags());

            wpm.update(productBean);
            wpm.save();

            // Note: Retrieve bean again from the repository to return.
            productBean = (ProductBean) wpm.getObject(productBean.getPath());
            productRepresentation = new ProductRepresentation().represent(productBean);
            productRepresentation.addLink(getNodeLink(requestContext, productBean));
            if (siteLink) {
                productRepresentation.addLink(getSiteLink(requestContext, productBean));
            }
        } catch (ObjectBeanManagerException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to create product.", e);
            } else {
                log.warn("Failed to create product. {}", e.toString());
            }
            
            throw new WebApplicationException(e, ResponseUtils.buildServerErrorResponse(e));
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to create product.", e);
            } else {
                log.warn("Failed to create product. {}", e.toString());
            }
            
            throw new WebApplicationException(e, ResponseUtils.buildServerErrorResponse(e));
        }

        return productRepresentation;
    }
    
    @RolesAllowed( { "admin", "author", "editor" } )
    @Persistable
    @PUT
    @Path("/brand/{brand}/")
    public ProductRepresentation updateProductResources(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo,
            @PathParam("brand") String brand,
            @QueryParam("wfaction") String workflowAction,
            @MatrixParam("sitelink") @DefaultValue("true") boolean siteLink,
            ProductRepresentation productRepresentation) {
        
        ProductBean productBean = null;
        HstRequestContext requestContext = getRequestContext(servletRequest);
        
        try {
            HstQueryManager hstQueryMgr = getHstQueryManager(requestContext);
            HippoBean contentBaseBean = getMountContentBaseBean(requestContext);
            HstQuery hstQuery = hstQueryMgr.createQuery(contentBaseBean, ProductBean.class, true);
            Filter filter = hstQuery.createFilter();
            filter.addEqualTo("demosite:brand", brand);
            hstQuery.setFilter(filter);
            HstQueryResult result = hstQuery.execute();
            HippoBeanIterator it = result.getHippoBeans();
            
            if (it.hasNext()) {
                productBean = (ProductBean) it.nextHippoBean();
            }
            
            if (productBean == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            
            WorkflowPersistenceManager wpm = (WorkflowPersistenceManager) getPersistenceManager(requestContext);
            productBean.setProduct(productRepresentation.getProduct());
            productBean.setColor(productRepresentation.getColor());
            productBean.setType(productRepresentation.getType());
            productBean.setPrice(productRepresentation.getPrice());
            productBean.setTags(productRepresentation.getTags());

            if (StringUtils.equals("requestPublication", workflowAction)) {
                wpm.setWorkflowCallbackHandler(new BaseWorkflowCallbackHandler<DocumentWorkflow>() {
                    public void processWorkflow(DocumentWorkflow wf) throws Exception {
                        wf.requestPublication();
                    }
                });
            } else if (StringUtils.equals("publish", workflowAction)) {
                wpm.setWorkflowCallbackHandler(new BaseWorkflowCallbackHandler<DocumentWorkflow>() {
                    public void processWorkflow(DocumentWorkflow wf) throws Exception {
                        wf.publish();
                    }
                });
            } else if (StringUtils.equals("requestDepublication", workflowAction)) {
                wpm.setWorkflowCallbackHandler(new BaseWorkflowCallbackHandler<DocumentWorkflow>() {
                    public void processWorkflow(DocumentWorkflow wf) throws Exception {
                        wf.requestDepublication();
                    }
                });
            } else if (StringUtils.equals("depublish", workflowAction)) {
                wpm.setWorkflowCallbackHandler(new BaseWorkflowCallbackHandler<DocumentWorkflow>() {
                    public void processWorkflow(DocumentWorkflow wf) throws Exception {
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
            if (siteLink) {
                productRepresentation.addLink(getSiteLink(requestContext, productBean));
            }
        } catch (ObjectBeanManagerException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to update product.", e);
            } else {
                log.warn("Failed to update product. {}", e.toString());
            }
            
            throw new WebApplicationException(e, ResponseUtils.buildServerErrorResponse(e));
        } catch (QueryException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to update product.", e);
            } else {
                log.warn("Failed to update product. {}", e.toString());
            }
            
            throw new WebApplicationException(e, ResponseUtils.buildServerErrorResponse(e));
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to update product.", e);
            } else {
                log.warn("Failed to update product. {}", e.toString());
            }
            
            throw new WebApplicationException(e, ResponseUtils.buildServerErrorResponse(e));
        }

        return productRepresentation;
    }
    
    @RolesAllowed( { "admin", "author", "editor" } )
    @Persistable
    @DELETE
    @Path("/brand/{brand}/")
    public Response deleteProductResources(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo,
            @PathParam("brand") String brand,
            @MatrixParam("sitelink") @DefaultValue("true") boolean siteLink) {
        ProductBean productBean = null;
        HstRequestContext requestContext = getRequestContext(servletRequest);
        
        try {
            HstQueryManager hstQueryMgr = getHstQueryManager(requestContext);
            HippoBean contentBaseBean = getMountContentBaseBean(requestContext);
            HstQuery hstQuery = hstQueryMgr.createQuery(contentBaseBean, ProductBean.class, true);
            Filter filter = hstQuery.createFilter();
            filter.addEqualTo("demosite:brand", brand);
            hstQuery.setFilter(filter);
            HstQueryResult result = hstQuery.execute();
            HippoBeanIterator it = result.getHippoBeans();
            
            if (it.hasNext()) {
                productBean = (ProductBean) it.nextHippoBean();
            }
            
            if (productBean == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            
            WorkflowPersistenceManager wpm = (WorkflowPersistenceManager) getPersistenceManager(requestContext);
            wpm.remove(productBean);
            wpm.save();
        } catch (ObjectBeanManagerException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to remove product.", e);
            } else {
                log.warn("Failed to remove product. {}", e.toString());
            }
            
            throw new WebApplicationException(e, ResponseUtils.buildServerErrorResponse(e));
        } catch (QueryException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to remove product.", e);
            } else {
                log.warn("Failed to remove product. {}", e.toString());
            }
            
            throw new WebApplicationException(e, ResponseUtils.buildServerErrorResponse(e));
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to remove product.", e);
            } else {
                log.warn("Failed to remove product. {}", e.toString());
            }
            
            throw new WebApplicationException(e, ResponseUtils.buildServerErrorResponse(e));
        }
        
        return Response.ok().build();
    }
}
