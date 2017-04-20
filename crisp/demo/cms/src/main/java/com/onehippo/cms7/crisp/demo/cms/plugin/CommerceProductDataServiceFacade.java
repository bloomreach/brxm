package com.onehippo.cms7.crisp.demo.cms.plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.forge.exdocpicker.api.ExternalDocumentCollection;
import org.onehippo.forge.exdocpicker.api.ExternalDocumentServiceContext;
import org.onehippo.forge.exdocpicker.api.ExternalDocumentServiceFacade;
import org.onehippo.forge.exdocpicker.impl.SimpleExternalDocumentCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.onehippo.cms7.crisp.api.broker.ResourceServiceBroker;
import com.onehippo.cms7.crisp.api.resource.Resource;
import com.onehippo.cms7.crisp.api.resource.ResourceContainer;

public class CommerceProductDataServiceFacade implements ExternalDocumentServiceFacade<Resource> {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(CommerceProductDataServiceFacade.class);

    @Override
    public ExternalDocumentCollection<Resource> searchExternalDocuments(ExternalDocumentServiceContext context,
            String queryString) {
        ExternalDocumentCollection<Resource> collection = new SimpleExternalDocumentCollection<>();

        try {
            ResourceContainer resourceContainer = findAllProductResources(queryString);

            if (resourceContainer.isAnyChildContained()) {
                for (Resource resource : resourceContainer.getChildren()) {
                    collection.add(resource);
                }
            }
        } catch (Exception e) {
            log.error("Failed to find resources.", e);
        }

        return collection;
    }

    @Override
    public ExternalDocumentCollection<Resource> getFieldExternalDocuments(ExternalDocumentServiceContext context) {
        ExternalDocumentCollection<Resource> collection = new SimpleExternalDocumentCollection<>();

        try {
            String fieldName = context.getPluginConfig().getString("related.products.field.name");
            String[] skuValues = JcrUtils.getMultipleStringProperty(context.getContextModel().getNode(), fieldName,
                    null);

            if (skuValues != null) {
                ResourceContainer resourceContainer = findAllProductResources(null);

                for (String skuValue : skuValues) {
                    Resource resource = findResourceBySKU(resourceContainer, skuValue);

                    if (resource != null) {
                        collection.add(resource);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to read the existing product resource metadata.", e);
        }

        return collection;
    }

    @Override
    public void setFieldExternalDocuments(ExternalDocumentServiceContext context,
            ExternalDocumentCollection<Resource> exdocs) {
        try {
            List<String> skuValues = new ArrayList<>();

            for (Iterator<? extends Resource> it = exdocs.iterator(); it.hasNext();) {
                Resource resource = it.next();
                skuValues.add((String) resource.getValueMap().get("SKU"));
            }

            String fieldName = context.getPluginConfig().getString("related.products.field.name");
            context.getContextModel().getNode().setProperty(fieldName, skuValues.toArray(new String[skuValues.size()]));
        } catch (Exception e) {
            log.error("Failed to save related product SKU data.", e);
        }
    }

    @Override
    public String getDocumentTitle(ExternalDocumentServiceContext context, Resource doc, Locale preferredLocale) {
        return (String) doc.getValueMap().get("name");
    }

    @Override
    public String getDocumentDescription(ExternalDocumentServiceContext context, Resource doc, Locale preferredLocale) {
        Resource extendedData = (Resource) doc.getValueMap().get("extendedData");
        return (String) extendedData.getValueMap().get("description");
    }

    @Override
    public String getDocumentIconLink(ExternalDocumentServiceContext context, Resource doc, Locale preferredLocale) {
        return "/cms/skin/images/icons/domain-48.png";
    }

    private ResourceContainer findAllProductResources(final String queryString) {
        ResourceServiceBroker broker = HippoServiceRegistry.getService(ResourceServiceBroker.class);
        Map<String, Object> variables = new HashMap<>();
        variables.put("queryString", StringUtils.isNotBlank(queryString) ? queryString : "");
        return broker.findResources("demoProductCatalogs", "/products/?q={queryString}", variables);
    }

    /*
     * WARNING: This method implementation is intended only for demonstration purpose about how cms code can use
     *          ResourceServiceBroker. So, this kind of implementation shouldn't be used in production.
     */
    private Resource findResourceBySKU(ResourceContainer resourceContainer, final String sku) {
        for (Resource resource : resourceContainer.getChildren()) {
            if (StringUtils.equals(sku, (String) resource.getValueMap().get("SKU"))) {
                return resource;
            }
        }

        return null;
    }
}
