/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.pagemodel.container;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.container.AggregationValve;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstComponentWindow;
import org.hippoecm.hst.core.container.HstContainerConfig;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.pagemodel.model.AggregatedPageModel;
import org.hippoecm.hst.core.pagemodel.model.ComponentContainerWindowModel;
import org.hippoecm.hst.core.pagemodel.model.ComponentWindowModel;
import org.hippoecm.hst.core.pagemodel.model.HippoBeanWrapperModel;
import org.hippoecm.hst.core.pagemodel.model.IdentifiableLinkableMetadataBaseModel;
import org.hippoecm.hst.core.pagemodel.model.MetadataContributable;
import org.hippoecm.hst.core.pagemodel.model.ReferenceMetadataBaseModel;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

/**
 * Page model aggregation valve, to write a JSON model from the aggregated data for a page request.
 */
public class PageModelAggregationValve extends AggregationValve {

    private static Logger log = LoggerFactory.getLogger(PageModelAggregationValve.class);

    /**
     * Page definition ID metadata name.
     */
    private static final String DEFINITION_ID_METADATA = "definitionId";

    /**
     * Content JSON Pointer prefix.
     */
    private static final String CONTENT_JSON_POINTER_PREFIX = "/content/";

    /**
     * JSON property name prefix for a UUID-based identifier.
     */
    private static final String CONTENT_ID_JSON_NAME_PREFIX = "u";

    /**
     * Page or component parameter map metadata name.
     */
    private static final String PARAMETERNS_METADATA = "params";

    /**
     * Jackson ObjectMapper instance for JSON (de)serialization.
     */
    private ObjectMapper objectMapper;

    /**
     * Custom metadata decorators.
     */
    private List<MetadataDecorator> metadataDecorators;

    /**
     * Return the Jackson ObjectMapper used in JSON (de)serialization.
     * @return the Jackson ObjectMapper used in JSON (de)serialization
     */
    public ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
        }

        return objectMapper;
    }

    /**
     * Set Jackson ObjectMapper used in JSON (de)serialization.
     * @param objectMapper Jackson ObjectMapper used in JSON (de)serialization
     */
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Return custom {@link MetadataDecorator} instance list, each of which is invoked to give a chance to customize
     * the {@link MetadataContributable} instances in the page model aggregation result output.
     * @return custom {@link MetadataDecorator} instance list
     */
    public List<MetadataDecorator> getMetadataDecorators() {
        if (metadataDecorators == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(metadataDecorators);
    }

    /**
     * Set custom {@link MetadataDecorator} instance list, each of which is invoked to give a chance to customize
     * the {@link MetadataContributable} instances in the page model aggregation result output.
     * @param metadataDecorators custom {@link MetadataDecorator} instance list
     */
    public void setMetadataDecorators(List<MetadataDecorator> metadataDecorators) {
        this.metadataDecorators = metadataDecorators;
    }

    /**
     * <p>
     *     Add a custom {@link MetadataDecorator} instance, which is invoked to give a chance to customize
     *     the {@link MetadataContributable} instances in the page model aggregation result output.
     * </p>
     * <p>
     *     Downstream projects like enterprise modules can inject extra meta data decorators
     * </p>
     * @param metadataDecorator custom {@link MetadataDecorator} instance
     */
    public void addMetadataDecorator(MetadataDecorator metadataDecorator) {
        if (metadataDecorators == null) {
            metadataDecorators = new ArrayList<>();
        }
        metadataDecorators.add(metadataDecorator);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overrides <code>AggregationValve#processWindowsRender()</code> to create an {@link AggregatedPageModel}
     * from the current page request and write it as JSON output.
     */
    @Override
    protected void processWindowsRender(final HstContainerConfig requestContainerConfig,
            final HstComponentWindow[] sortedComponentWindows, final Map<HstComponentWindow, HstRequest> requestMap,
            final Map<HstComponentWindow, HstResponse> responseMap) throws ContainerException {

        final HstRequestContext requestContext = RequestContextProvider.get();
        final HttpServletResponse response = requestContext.getServletResponse();

        PrintWriter writer = null;

        try {
            response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
            response.setCharacterEncoding("UTF-8");
            AggregatedPageModel pageModel = createAggregatedPageModel(sortedComponentWindows, requestMap, responseMap);
            writer = response.getWriter();
            writeAggregatedPageModel(writer, pageModel);
        } catch (IOException e) {
            log.warn("Failed to write aggregated page model in json.", e);
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    /**
     * Write {@link AggregatedPageModel} object to the {@code writer}.
     * @param writer output writer
     * @param pageModel aggregated page model
     * @throws ContainerException if container exception occurs
     * @throws IOException if IO exception occurs
     */
    private void writeAggregatedPageModel(final Writer writer, final AggregatedPageModel pageModel)
            throws ContainerException, IOException {
        try {
            getObjectMapper().writeValue(writer, pageModel);
        } catch (JsonGenerationException e) {
            throw new ContainerException(e.getMessage(), e);
        } catch (JsonMappingException e) {
            throw new ContainerException(e.getMessage(), e);
        }
    }

    /**
     * Create an {@link AggregatedPageModel} instance to write.
     * @param sortedComponentWindows sorted component window array which was sorted by the parent {@link AggregationValve}
     * @param requestMap HST Request map for each {@link HstComponentWindow} instance
     * @param responseMap HST Response map for each {@link HstComponentWindow} instance
     * @return an {@link AggregatedPageModel} instance to write
     * @throws ContainerException if container exception occurs
     */
    protected AggregatedPageModel createAggregatedPageModel(final HstComponentWindow[] sortedComponentWindows,
            final Map<HstComponentWindow, HstRequest> requestMap,
            final Map<HstComponentWindow, HstResponse> responseMap) throws ContainerException {

        // root component (page component) is the first item in the sortedComponentWindows.
        final HstComponentWindow rootWindow = sortedComponentWindows[0];
        final String id = rootWindow.getReferenceNamespace();
        final String definitionId = rootWindow.getComponentInfo().getId();
        final AggregatedPageModel pageModel = new AggregatedPageModel(id);
        pageModel.putMetadata(DEFINITION_ID_METADATA, definitionId);
        addParameterMapMetadata(rootWindow, pageModel);
        addLinksToPageModel(pageModel);

        final int sortedComponentWindowsLen = sortedComponentWindows.length;
        ComponentContainerWindowModel curContainerWindowModel = null;

        // As sortedComponentWindows is sorted by parent-child order, we can assume all the container item component
        // window appears after a container component window.

        for (int i = 1; i < sortedComponentWindowsLen; i++) {
            final HstComponentWindow window = sortedComponentWindows[i];
            ComponentWindowModel componentWindowModel = null;
            final HstRequest hstRequest = requestMap.get(window);
            final HstResponse hstResponse = responseMap.get(window);

            if (window.getComponentInfo().isContainer()) {
                curContainerWindowModel = new ComponentContainerWindowModel(window.getReferenceNamespace(),
                        window.getName());
                addParameterMapMetadata(window, curContainerWindowModel);
                decorateComponentWindowMetadata(hstRequest, hstResponse, curContainerWindowModel);
                pageModel.addContainerWindow(curContainerWindowModel);
            } else if (window.getComponentInfo().isContainerItem()) {
                if (curContainerWindowModel == null) {
                    log.warn("Invalid container item component window location for {}.",
                            window.getReferenceNamespace());
                    continue;
                }

                componentWindowModel = new ComponentWindowModel(
                        window.getReferenceNamespace(), window.getName(), window.getComponentName());
                componentWindowModel.setLabel(window.getComponentInfo().getLabel());
                addParameterMapMetadata(window, componentWindowModel);
                decorateComponentWindowMetadata(hstRequest, hstResponse, componentWindowModel);
                curContainerWindowModel.addComponentWindowSet(componentWindowModel);
            } else {
                curContainerWindowModel = null;
            }

            for (Map.Entry<String, Object> entry : hstRequest.getModelsMap().entrySet()) {
                final String name = entry.getKey();
                final Object model = entry.getValue();
                ReferenceMetadataBaseModel referenceModel = null;

                if (model instanceof HippoBean) {
                    final HippoBean bean = (HippoBean) model;
                    final String contentId = bean.getCanonicalUUID();
                    final String jsonPointerContentId = contentIdToJsonName(contentId);

                    final HippoBeanWrapperModel wrapperBeanModel = new HippoBeanWrapperModel(contentId, bean);
                    pageModel.putContent(jsonPointerContentId, wrapperBeanModel);

                    decorateContentMetadata(hstRequest, hstResponse, bean, wrapperBeanModel);

                    referenceModel = new ReferenceMetadataBaseModel(
                            CONTENT_JSON_POINTER_PREFIX + jsonPointerContentId);
                }

                if (componentWindowModel != null) {
                    componentWindowModel.putModel(name, (referenceModel != null) ? referenceModel : model);
                }
            }
        }

        return pageModel;
    }

    /**
     * Convert content identifier (e.g, UUID) to a safe JSON property/variable name.
     * @param uuid content identifier
     * @return a safe JSON property/variable name converted from the {@code uuid}
     */
    private String contentIdToJsonName(final String uuid) {
        return new StringBuilder(uuid.length()).append(CONTENT_ID_JSON_NAME_PREFIX).append(uuid.replaceAll("-", ""))
                .toString();
    }

    /**
     * Add <code>params</code> metadata to the {@code model} from the {@code window}.
     * @param window HST Component Window instance
     * @param model the {@link MetadataContributable} model instance where the parameter map should be contributed to
     */
    private void addParameterMapMetadata(HstComponentWindow window, MetadataContributable model) {
        final ComponentConfiguration compConfig = window.getComponent().getComponentConfiguration();

        if (compConfig == null) {
            return;
        }

        final ResolvedSiteMapItem resolvedSiteMapItem = RequestContextProvider.get().getResolvedSiteMapItem();
        final Map<String, String> params = new LinkedHashMap<>();

        for (String paramName : compConfig.getParameterNames()) {
            String paramValue = compConfig.getParameter(paramName, resolvedSiteMapItem);
            params.put(paramName, paramValue);
        }

        model.putMetadata(PARAMETERNS_METADATA, params);
    }

    /**
     * Add links to the page model.
     * @param pageModel the aggregated page model instance
     */
    private void addLinksToPageModel(IdentifiableLinkableMetadataBaseModel pageModel) {
        final HstRequestContext requestContext = RequestContextProvider.get();
        final HstLinkCreator linkCreator = requestContext.getHstLinkCreator();
        final HstSiteMapItem siteMapItem = requestContext.getResolvedSiteMapItem().getHstSiteMapItem();

        if (siteMapItem != null) {
            final Mount selfMount = requestContext.getResolvedMount().getMount();
            final HstLink selfLink = linkCreator.create(siteMapItem, selfMount);
            pageModel.putLink(ContainerConstants.LINK_NAME_SELF, selfLink.toUrlForm(requestContext, true));

            final Mount siteMount = requestContext.getMount(ContainerConstants.MOUNT_ALIAS_SITE);
            if (siteMount != null) {
                final HstLink siteLink = linkCreator.create(siteMapItem, siteMount);
                pageModel.putLink(ContainerConstants.LINK_NAME_SITE, siteLink.toUrlForm(requestContext, true));
            }
        }
    }

    /**
     * Invoke custom metadata decorators to give a chance to add more metadata for the component window.
     * @param hstRequest HstRequest object
     * @param hstResponse HstResponse object
     * @param model MetadataContributable model
     */
    private void decorateComponentWindowMetadata(final HstRequest hstRequest, final HstResponse hstResponse,
            MetadataContributable model) {
        if (CollectionUtils.isEmpty(metadataDecorators)) {
            return;
        }

        for (MetadataDecorator decorator : metadataDecorators) {
            decorator.decorateComponentWindowMetadata(hstRequest, hstResponse, model);
        }
    }

    /**
     * Invoke custom metadata decorators to give a chance to add more metadata for the content bean.
     * @param hstRequest HstRequest object
     * @param hstResponse HstResponse object
     * @param contentBean content bean
     * @param model MetadataContributable model
     */
    private void decorateContentMetadata(final HstRequest hstRequest, final HstResponse hstResponse,
            final HippoBean contentBean, MetadataContributable model) {
        if (CollectionUtils.isEmpty(metadataDecorators)) {
            return;
        }

        for (MetadataDecorator decorator : metadataDecorators) {
            decorator.decorateContentMetadata(hstRequest, hstResponse, contentBean, model);
        }
    }
}