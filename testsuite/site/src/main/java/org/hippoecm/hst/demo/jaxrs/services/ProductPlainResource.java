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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.demo.beans.ProductBean;
import org.hippoecm.hst.demo.jaxrs.model.ProductRepresentation;
import org.hippoecm.hst.jaxrs.services.AbstractResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
@Path("/products/")
public class ProductPlainResource extends AbstractResource {
    
    private static Logger log = LoggerFactory.getLogger(ProductPlainResource.class);
    
    @GET
    @Path("/{productType}/")
    public List<ProductRepresentation> getProductResources(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo,
            @PathParam("productType") String productType,
            @MatrixParam("sitelink") @DefaultValue("true") boolean siteLink) {
        
        List<ProductRepresentation> products = new ArrayList<ProductRepresentation>();
        
        try {
            // You can use either getRequestContext(servletRequest) or RequestContextProvider.get().
            //
            //HstRequestContext requestContext = getRequestContext(servletRequest);
            HstRequestContext requestContext = RequestContextProvider.get();
            
            HstQueryManager hstQueryManager = getHstQueryManager(requestContext.getSession(), requestContext);
           
            // for plain jaxrs, we do not have a requestContentBean because no resolved sitemapitem
            HippoBean scope  = getMountContentBaseBean(requestContext);
           
            
            HstQuery hstQuery = hstQueryManager.createQuery(scope, ProductBean.class, true);
            Filter filter = hstQuery.createFilter();
            filter.addEqualTo("demosite:product", productType);
            hstQuery.setFilter(filter);
            hstQuery.addOrderByDescending("demosite:price");
            hstQuery.setLimit(10);
            
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
                log.warn("Failed to query products by tag.", e);
            } else {
                log.warn("Failed to query products by tag. {}", e.toString());
            }
            
            throw new WebApplicationException(e);
        }
        
        return products;
    }
}
