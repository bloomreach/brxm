package org.onehippo.cms7.crisp.demo.components;

import static org.onehippo.cms7.crisp.demo.Constants.RESOURCE_SPACE_DEMO_PRODUCT_CATALOG;

import java.util.HashMap;
import java.util.Map;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.onehippo.cms7.crisp.api.broker.ResourceServiceBroker;
import org.onehippo.cms7.crisp.api.query.QuerySpec;
import org.onehippo.cms7.crisp.api.query.QuerySpecBuilder;
import org.onehippo.cms7.crisp.api.resource.ResourceContainer;
import org.onehippo.cms7.crisp.demo.beans.NewsDocument;
import org.onehippo.cms7.crisp.hst.module.CrispHstServices;
import org.onehippo.cms7.essentials.components.EssentialsContentComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NewsContentComponent extends EssentialsContentComponent {

    private static Logger log = LoggerFactory.getLogger(NewsContentComponent.class);

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        super.doBeforeRender(request, response);

        NewsDocument document = (NewsDocument) request.getRequestContext().getContentBean();

        try {
            ResourceServiceBroker resourceServiceBroker = CrispHstServices.getDefaultResourceServiceBroker();
            final Map<String, Object> pathVars = new HashMap<>();
            // Note: Just as an example, let's try to find all the data by passing empty query string.
            pathVars.put("fullTextSearchTerm", "");
            final QuerySpec querySpec = QuerySpecBuilder.create().offset(0).limit(100L).build();
            ResourceContainer productCatalogs = resourceServiceBroker.findResources(RESOURCE_SPACE_DEMO_PRODUCT_CATALOG,
                    "/products/?q={fullTextSearchTerm}", pathVars, querySpec);
            request.setAttribute("productCatalogs", productCatalogs);
        } catch (Exception e) {
            log.warn("Failed to find resources from '{}{}' resource space for full text search term, '{}'.",
                    RESOURCE_SPACE_DEMO_PRODUCT_CATALOG, "/products/", document.getTitle(), e);
        }
    }
}
