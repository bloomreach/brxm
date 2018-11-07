/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.HstRequestContext;
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
import static org.onehippo.cms7.crisp.demo.Constants.RESOURCE_SPACE_DEMO_PRODUCT_CATALOG_XML_BEAN;

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
        Resource productCatalogs = findProductCatalogs(request, document);

        if (productCatalogs != null) {
            request.setAttribute("productCatalogs", productCatalogs);

            if (productCatalogs.getChildCount() > 0) {
                Resource firstProductResource = productCatalogs.getChildren(0, 1).get(0);
                ResourceBeanMapper resourceBeanMapper = resourceServiceBroker
                        .getResourceBeanMapper(RESOURCE_SPACE_DEMO_PRODUCT_CATALOG);
                Product firstProduct = resourceBeanMapper.map(firstProductResource, Product.class);
                log.debug("==> First product: [{}] {} - {}", firstProduct.getSku(), firstProduct.getName(),
                        firstProduct.getDescription());
            }
        }

        log.warn("\n\n===============================================================================\n"
                + "  [INFO] XML based Resource Retrieval DEMO\n"
                + "===============================================================================\n\n");
        Resource productCatalogsXml = findProductCatalogsXml(request, document);

        if (productCatalogsXml != null) {
            request.setAttribute("productCatalogsXml", productCatalogsXml);

            if (productCatalogsXml.getChildCount() > 0) {
                Resource products = (Resource) productCatalogsXml.getValue("products");
                Resource firstProductResource = products.getChildren(0, 1).get(0);
                ResourceBeanMapper resourceBeanMapper = resourceServiceBroker
                        .getResourceBeanMapper(RESOURCE_SPACE_DEMO_PRODUCT_CATALOG_XML_BEAN);
                Product firstProduct = resourceBeanMapper.map(firstProductResource, Product.class);
                log.debug("==> First product (XML): [{}] {} - {}", firstProduct.getSku(), firstProduct.getName(),
                        firstProduct.getDescription());
            }
        }

        if (BooleanUtils.toBoolean(getPublicRequestParameter(request, "_testput"))) {
            testPutMethodOnEndpoint();
        }

        if (BooleanUtils.toBoolean(getPublicRequestParameter(request, "_testdelete"))) {
            testDeleteMethodOnEndpoint();
        }
    }

    private Resource findProductCatalogs(final HstRequest request, final NewsDocument document) {
        Resource productCatalogs = null;
        final ExchangeHint exchangeHint = createExampleExchangeHintFromParameter("application/json", "{}");

        try {
            ResourceServiceBroker resourceServiceBroker = CrispHstServices.getDefaultResourceServiceBroker();
            final Map<String, Object> pathVars = new HashMap<>();
            final String queryTerm = StringUtils.defaultString(getAnyParameter(request, "q"));
            pathVars.put("fullTextSearchTerm", queryTerm);
            productCatalogs = resourceServiceBroker.findResources(RESOURCE_SPACE_DEMO_PRODUCT_CATALOG,
                    "/products/?q={fullTextSearchTerm}", pathVars, exchangeHint);
            log.info("REST Client info in exchangeHint. statusCode: {}, headers: {}",
                    exchangeHint.getResponseStatusCode(), exchangeHint.getResponseHeaders());
        } catch (Exception e) {
            final Resource errorInfoResource = (Resource) exchangeHint.getResponseBody();
            log.warn(
                    "REST Client error. See response data in exchangeHint. statusCode: {}, headers: {}, body's status: {}, body's message: {}",
                    exchangeHint.getResponseStatusCode(), exchangeHint.getResponseHeaders(),
                    (errorInfoResource != null) ? errorInfoResource.getValue("status") : null,
                    (errorInfoResource != null) ? errorInfoResource.getValue("message") : null);
            log.warn("Failed to find resources from '{}{}' resource space for full text search term, '{}'. {}",
                    RESOURCE_SPACE_DEMO_PRODUCT_CATALOG, "/products/", document.getTitle(), e.toString());
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
                        "/products/{sku}/image/download", pathVars, createExampleExchangeHintFromParameter(null, null));
                request.setAttribute("binary", binary);
            } catch (Exception e) {
                log.warn("Failed to find binary.", e);
            }
        }

        response.setServeResourcePath("/WEB-INF/jsp/downloadjpeg.jsp");
    }

    private Resource findProductCatalogsXml(final HstRequest request, final NewsDocument document) {
        Resource productCatalogs = null;
        final ExchangeHint exchangeHint = createExampleExchangeHintFromParameter("application/xml", null);

        try {
            ResourceServiceBroker resourceServiceBroker = CrispHstServices.getDefaultResourceServiceBroker();
            final Map<String, Object> pathVars = new HashMap<>();
            final String queryTerm = StringUtils.defaultString(getAnyParameter(request, "q"));
            pathVars.put("fullTextSearchTerm", queryTerm);
            productCatalogs = resourceServiceBroker.findResources(RESOURCE_SPACE_DEMO_PRODUCT_CATALOG_XML_BEAN,
                    "/products.xml?q={fullTextSearchTerm}", pathVars, exchangeHint);
            log.info("REST Client info in exchangeHint. statusCode: {}, headers: {}",
                    exchangeHint.getResponseStatusCode(), exchangeHint.getResponseHeaders());
        } catch (Exception e) {
            final Resource errorInfoResource = (Resource) exchangeHint.getResponseBody();
            log.warn(
                    "REST Client error. See response data in exchangeHint. statusCode: {}, headers: {}, body's status: {}, body's message: {}",
                    exchangeHint.getResponseStatusCode(), exchangeHint.getResponseHeaders(),
                    (errorInfoResource != null) ? errorInfoResource.getValue("status") : null,
                    (errorInfoResource != null) ? errorInfoResource.getValue("message") : null);
            log.warn("Failed to find resources from '{}{}' resource space for full text search term, '{}'. {}",
                    RESOURCE_SPACE_DEMO_PRODUCT_CATALOG_XML_BEAN, "/products.xml", document.getTitle(), e.toString());
        }

        return productCatalogs;
    }

    /**
     * Create a demo-purpose-only <code>ExchangeHint</code> instance that can be passed *optionally*.
     * @param contentType
     * @param requestBody
     * @return
     */
    private ExchangeHint createExampleExchangeHintFromParameter(final String contentType, final String requestBody) {
        final HstRequestContext requestContext = RequestContextProvider.get();
        final ExchangeHintBuilder builder = ExchangeHintBuilder.create();

        final String noCacheParam = requestContext.getServletRequest().getParameter("_nocache");

        if (StringUtils.isNotEmpty(noCacheParam)) {
            builder.noCache(BooleanUtils.toBoolean(noCacheParam));
        }

        final String methodName = StringUtils.upperCase(requestContext.getServletRequest().getParameter("_method"));

        if (StringUtils.isNotEmpty(methodName)) {
            builder.methodName(methodName);
        }

        if (("POST".equals(methodName) || "PUT".equals(methodName)) && StringUtils.isNotEmpty(contentType)) {
            builder.requestHeader("Content-Type", contentType);
        }

        if (StringUtils.isNotEmpty(requestBody)) {
            builder.requestBody(requestBody);
        }

        return builder.build();
    }

    private void testPutMethodOnEndpoint() {
        Binary data = null;

        final String jsonBody = "{\"description\":\"O!Play HDP-R3, MPEG1, MPEG2, MPEG4, VC-1, H.264, 479 g, 10 W, HDMI\",\"name\":\"Asus O!PLAY HDP-R3\",\"extendedData\":{\"title\":\"Asus O!PLAY HDP-R3\",\"type\":\"Link\",\"uri\":\"Incentro-HIC-Site/-/products/6384114\",\"description\":\"O!Play HDP-R3, MPEG1, MPEG2, MPEG4, VC-1, H.264, 479 g, 10 W, HDMI\"},\"SKU\":\"6384114\"}";

        try {
            ResourceServiceBroker resourceServiceBroker = CrispHstServices.getDefaultResourceServiceBroker();
            final Map<String, Object> pathVars = new HashMap<>();
            pathVars.put("sku", "6384114");
            final ExchangeHint exchangeHint = ExchangeHintBuilder.create().methodName("PUT")
                    .requestHeader("Content-Type", "application/json").requestBody(jsonBody).build();
            data = resourceServiceBroker.resolveBinary(RESOURCE_SPACE_DEMO_PRODUCT_CATALOG, "/products/{sku}", pathVars,
                    exchangeHint);
            log.warn("[INFO] testPutMethodOnEndpoint gets data: {}", IOUtils.toString(data.getInputStream(), "UTF-8"));
        } catch (Exception e) {
            log.warn("Failed to put resource from '{}{}' resource space.", RESOURCE_SPACE_DEMO_PRODUCT_CATALOG,
                    "/products/6384114", e);
        } finally {
            if (data != null) {
                data.dispose();
            }
        }
    }

    private void testDeleteMethodOnEndpoint() {
        Binary data = null;

        try {
            ResourceServiceBroker resourceServiceBroker = CrispHstServices.getDefaultResourceServiceBroker();
            final Map<String, Object> pathVars = new HashMap<>();
            pathVars.put("sku", "6384114");
            final ExchangeHint exchangeHint = ExchangeHintBuilder.create().methodName("DELETE")
                    .requestHeader("Content-Type", "application/json").build();
            data = resourceServiceBroker.resolveBinary(RESOURCE_SPACE_DEMO_PRODUCT_CATALOG, "/products/{sku}", pathVars,
                    exchangeHint);
            log.warn("[INFO] testDeleteMethodOnEndpoint gets data: {}", IOUtils.toString(data.getInputStream(), "UTF-8"));
        } catch (Exception e) {
            log.warn("Failed to delete resource from '{}{}' resource space.", RESOURCE_SPACE_DEMO_PRODUCT_CATALOG,
                    "/products/6384114", e);
        } finally {
            if (data != null) {
                data.dispose();
            }
        }
    }
}
