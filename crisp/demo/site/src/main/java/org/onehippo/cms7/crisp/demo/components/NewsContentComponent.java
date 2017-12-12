/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.demo.components;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.onehippo.cms7.crisp.api.broker.ResourceServiceBroker;
import org.onehippo.cms7.crisp.api.exchange.ExchangeHint;
import org.onehippo.cms7.crisp.api.exchange.ExchangeHintBuilder;
import org.onehippo.cms7.crisp.api.resource.Binary;
import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceBeanMapper;
import org.onehippo.cms7.crisp.demo.beans.NewsDocument;
import org.onehippo.cms7.crisp.demo.model.Product;
import org.onehippo.cms7.crisp.hst.module.CrispHstServices;
import org.onehippo.cms7.essentials.components.EssentialsContentComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cms7.crisp.demo.Constants.RESOURCE_SPACE_DEMO_PRODUCT_CATALOG;
import static org.onehippo.cms7.crisp.demo.Constants.RESOURCE_SPACE_DEMO_PRODUCT_CATALOG_XML;

public class NewsContentComponent extends EssentialsContentComponent {

    private static Logger log = LoggerFactory.getLogger(NewsContentComponent.class);

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        super.doBeforeRender(request, response);

        NewsDocument document = (NewsDocument) request.getRequestContext().getContentBean();

        ResourceServiceBroker resourceServiceBroker = CrispHstServices.getDefaultResourceServiceBroker();

        log.warn("\n\n===============================================================================\n"
                + "  [INFO] JSON based Resource Retrieval DEMO\n"
                + "===============================================================================\n\n");
        Resource productCatalogs = findProductCatalogs(document);

        if (productCatalogs != null) {
            request.setAttribute("productCatalogs", productCatalogs);

            if (productCatalogs.getChildCount() > 0) {
                Resource firstProductResource = productCatalogs.getChildren(0, 1).get(0);
                ResourceBeanMapper resourceBeanMapper = resourceServiceBroker.getResourceBeanMapper(RESOURCE_SPACE_DEMO_PRODUCT_CATALOG);
                Product firstProduct = resourceBeanMapper.map(firstProductResource, Product.class);
                log.debug("==> First product: [{}] {} - {}", firstProduct.getSku(), firstProduct.getName(),
                        firstProduct.getDescription());
            }
        }

        log.warn("\n\n===============================================================================\n"
                + "  [INFO] XML based Resource Retrieval DEMO\n"
                + "===============================================================================\n\n");
        Resource productCatalogsXml = findProductCatalogsXml(document);

        if (productCatalogsXml != null) {
            request.setAttribute("productCatalogsXml", productCatalogsXml);

            if (productCatalogsXml.getChildCount() > 0) {
                Resource products = (Resource) productCatalogsXml.getValue("products");
                Resource firstProductResource = products.getChildren(0, 1).get(0);
                ResourceBeanMapper resourceBeanMapper = resourceServiceBroker.getResourceBeanMapper(RESOURCE_SPACE_DEMO_PRODUCT_CATALOG_XML);
                Product firstProduct = resourceBeanMapper.map(firstProductResource, Product.class);
                log.debug("==> First product (XML): [{}] {} - {}", firstProduct.getSku(), firstProduct.getName(),
                        firstProduct.getDescription());
            }
        }

    }

    private Resource findProductCatalogs(final NewsDocument document) {
        Resource productCatalogs = null;

        try {
            ResourceServiceBroker resourceServiceBroker = CrispHstServices.getDefaultResourceServiceBroker();
            final Map<String, Object> pathVars = new HashMap<>();
            // Note: Just as an example, let's try to find all the data by passing empty query string.
            pathVars.put("fullTextSearchTerm", "");
            productCatalogs = resourceServiceBroker.findResources(RESOURCE_SPACE_DEMO_PRODUCT_CATALOG,
                    "/products/?q={fullTextSearchTerm}", pathVars, createExampleExchangeHintFromParameter());
        } catch (Exception e) {
            log.warn("Failed to find resources from '{}{}' resource space for full text search term, '{}'.",
                    RESOURCE_SPACE_DEMO_PRODUCT_CATALOG, "/products/", document.getTitle(), e);
        }

        return productCatalogs;
    }

    @Override
    public void doBeforeServeResource(final HstRequest request, final HstResponse response) {
        final String resourceId = request.getResourceID();
        final String sku = request.getParameter("sku");

        if ("downloadImage".equals(resourceId)) {
            try {
                ResourceServiceBroker resourceServiceBroker = CrispHstServices.getDefaultResourceServiceBroker();
                final Map<String, Object> pathVars = new HashMap<>();
                pathVars.put("sku", sku);
                Binary binary = resourceServiceBroker.resolveBinary(RESOURCE_SPACE_DEMO_PRODUCT_CATALOG,
                        "/products/{sku}/image/download", pathVars, createExampleExchangeHintFromParameter());
                request.setAttribute("binary", binary);
            } catch (Exception e) {
                log.warn("Failed to find binary.", e);
            }
        }

        response.setServeResourcePath("/WEB-INF/jsp/downloadjpeg.jsp");
    }

    private Resource findProductCatalogsXml(final NewsDocument document) {
        Resource productCatalogs = null;

        try {
            ResourceServiceBroker resourceServiceBroker = CrispHstServices.getDefaultResourceServiceBroker();
            final Map<String, Object> pathVars = new HashMap<>();
            // Note: Just as an example, let's try to find all the data by passing empty query string.
            pathVars.put("fullTextSearchTerm", "");
            productCatalogs = resourceServiceBroker.findResources(RESOURCE_SPACE_DEMO_PRODUCT_CATALOG_XML,
                    "/products.xml?q={fullTextSearchTerm}", pathVars, createExampleExchangeHintFromParameter());
        } catch (Exception e) {
            log.warn("Failed to find resources from '{}{}' resource space for full text search term, '{}'.",
                    RESOURCE_SPACE_DEMO_PRODUCT_CATALOG, "/products.xml", document.getTitle(), e);
        }

        return productCatalogs;
    }

    /**
     * Create a demo-purpose-only <code>ExchangeHint</code> instance that can be passed *optionally*.
     * @return
     */
    private ExchangeHint createExampleExchangeHintFromParameter() {
        final String methodName = RequestContextProvider.get().getServletRequest().getParameter("_method");
        if (!StringUtils.equalsIgnoreCase("POST", methodName)) {
            return null;
        }
        return ExchangeHintBuilder.create().methodName("POST").request("{}").build();
    }
}
