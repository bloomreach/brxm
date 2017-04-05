package org.onehippo.cms7.crisp.demo.components;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.onehippo.cms7.crisp.api.broker.ResourceServiceBroker;
import org.onehippo.cms7.crisp.api.resource.ResourceContainable;
import org.onehippo.cms7.crisp.demo.Constants;
import org.onehippo.cms7.crisp.demo.beans.NewsDocument;
import org.onehippo.cms7.crisp.hst.module.CrispServices;
import org.onehippo.cms7.essentials.components.EssentialsContentComponent;

public class NewsContentComponent extends EssentialsContentComponent {

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        super.doBeforeRender(request, response);

        NewsDocument document = (NewsDocument) request.getRequestContext().getContentBean();
        ResourceServiceBroker resourceServiceBroker = CrispServices.getDefaultResourceServiceBroker();
        ResourceContainable productCatalogs = resourceServiceBroker.findResources(Constants.PRODUCT_CATALOG_RESOURCE_SPACE,
                null, null, document.getTitle());
        request.setAttribute("productCatalogs", productCatalogs);
    }
}
