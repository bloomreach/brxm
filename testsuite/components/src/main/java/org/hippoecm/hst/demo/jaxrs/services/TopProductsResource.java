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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
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
@Path("/topproducts/")
public class TopProductsResource extends AbstractResource {
    
    private static Logger log = LoggerFactory.getLogger(TopProductsResource.class);
    
    @GET
    @Path("/")
    public List<ProductRepresentation> getProductResources(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo,
            @MatrixParam("max") @DefaultValue("10") int max) {
        
        List<ProductRepresentation> productRepList = new ArrayList<ProductRepresentation>();
        HstRequestContext requestContext = getRequestContext(servletRequest);
        
        try {
            HippoBean scope = null;
            try {
                // was there a sitemap item with a relative content path pointing to a bean. If so, use this as scope
                scope = getRequestContentBean(requestContext);
            } catch (ObjectBeanManagerException e) {
                // we did not find a bean for a matched sitemap item (or there was no matched sitemap item). Try the site content base bean:
                scope = getMountContentBaseBean(requestContext);
            }
            
            HstQueryManager manager = getHstQueryManager(requestContext.getSession(), requestContext);
            HstQuery hstQuery = manager.createQuery(scope, ProductBean.class, true);
            hstQuery.addOrderByDescending("demosite:price");
            hstQuery.setLimit(max);
            
            HstQueryResult result = hstQuery.execute();
            HippoBeanIterator iterator = result.getHippoBeans();

            while (iterator.hasNext()) {
                ProductBean productBean = (ProductBean) iterator.nextHippoBean();
                
                if (productBean != null) {
                    ProductRepresentation productRep = new ProductRepresentation().represent(productBean);
                    productRep.addLink(getNodeLink(requestContext, productBean));
                    productRep.addLink(getSiteLink(requestContext, productBean));
                    productRepList.add(productRep);
                }
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve top products.", e);
            } else {
                log.warn("Failed to retrieve top products. {}", e.toString());
            }
            
            throw new WebApplicationException(e);
        }
        
        return productRepList;
    }

}
