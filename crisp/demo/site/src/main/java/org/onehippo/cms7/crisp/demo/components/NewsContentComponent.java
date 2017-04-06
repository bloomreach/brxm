package org.onehippo.cms7.crisp.demo.components;

import static org.onehippo.cms7.crisp.demo.Constants.RESOURCE_SPACE_PRODUCT_CATALOG;

import java.util.HashMap;
import java.util.Map;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.onehippo.cms7.crisp.api.broker.ResourceServiceBroker;
import org.onehippo.cms7.crisp.api.resource.ResourceContainer;
import org.onehippo.cms7.crisp.demo.beans.NewsDocument;
import org.onehippo.cms7.crisp.hst.module.CrispServices;
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
            ResourceServiceBroker resourceServiceBroker = CrispServices.getDefaultResourceServiceBroker();
            final Map<String, Object> variables = new HashMap<>();
            variables.put("fullTextSearchTerm", document.getTitle());
            ResourceContainer productCatalogs = resourceServiceBroker.findResources(RESOURCE_SPACE_PRODUCT_CATALOG,
                    "/products?q={fullTextSearchTerm}", variables);
            request.setAttribute("productCatalogs", productCatalogs);
        } catch (Exception e) {
            log.warn("Failed to find resources from '{}{}' resource space for full text search term, '{}'.",
                    RESOURCE_SPACE_PRODUCT_CATALOG, "/products", document.getTitle(), e);
        }
    }
}
