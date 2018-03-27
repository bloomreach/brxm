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
package org.hippoecm.hst.pagemodelapi09.core.container;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.core.pagemodel.container.MetadataDecorator;
import org.hippoecm.hst.pagemodelapi09.content.beans.jackson.LinkModel;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.container.AggregationValve;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstComponentWindow;
import org.hippoecm.hst.core.container.HstContainerConfig;
import org.hippoecm.hst.core.container.ValveContext;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.pagemodelapi09.core.container.ContentSerializationContext.Phase;
import org.hippoecm.hst.pagemodelapi09.core.model.ComponentWindowModel;
import org.hippoecm.hst.pagemodelapi09.core.model.IdentifiableLinkableMetadataBaseModel;
import org.hippoecm.hst.core.pagemodel.model.MetadataContributable;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.ParametersInfoUtils;
import org.htmlcleaner.HtmlCleaner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Page model aggregation valve, to write a JSON model from the aggregated data for a page request.
 */
public class PageModelAggregationValve extends AggregationValve {

    private static Logger log = LoggerFactory.getLogger(PageModelAggregationValve.class);

    /**
     * Page or component parameter metadata name.
     */
    private static final String PARAMETERS_METADATA = "params";

    /**
     * Page or component parametersInfo metadata name.
     */
    private static final String PARAMETERS_INFO_METADATA = "paramsInfo";

    /**
     * Page title metadata name.
     */
    private static final String PAGE_TITLE_METADATA = "pageTitle";

    /**
     * Page definition ID (from the configuration) metadata name.
     */
    private static final String PAGE_DEFINITION_ID_METADATA = "definitionId";

    /**
     * Maximum content reference level request parameter name.
     */
    private static final String MAX_CONTENT_REFERENCE_LEVEL_PARAM_NAME = "_maxreflevel";

    /**
     * Jackson ObjectMapper instance for JSON (de)serialization.
     */
    private final ObjectMapper objectMapper;

    /**
     * Custom metadata decorators.
     */
    private final List<MetadataDecorator> metadataDecorators = new ArrayList<>();

    public PageModelAggregationValve(final ObjectMapper objectMapperInput, final Map<Class<?>, Class<?>> extraMixins,
                                     final HtmlCleaner htmlCleaner) {
        objectMapper = objectMapperInput.registerModule(new SimpleModule().setSerializerModifier(
                new HippoBeanModelsSerializerModifier(metadataDecorators, htmlCleaner)
        ));
        HstBeansObjectMapperDecorator.decorate(objectMapper, extraMixins);
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
    @SuppressWarnings("unused")
    public void addMetadataDecorator(MetadataDecorator metadataDecorator) {
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
        final AggregatedPageModel aggregatedPageModel = (AggregatedPageModel) createAggregatedPageModel(
                sortedComponentWindows, requestMap, responseMap);
        ContentSerializationContext.setCurrentAggregatedPageModel(aggregatedPageModel);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overrides <code>AggregationValve#processWindowsRender()</code> to create an {@link AggregatedPageModel}
     * from the current page request and write it as JSON output.
     */
    @Override
    protected void writeAggregatedOutput(final ValveContext context, final HstComponentWindow rootRenderingWindow)
            throws ContainerException {
        final HstRequestContext requestContext = RequestContextProvider.get();
        final AggregatedPageModel aggregatedPageModel = ContentSerializationContext.getCurrentAggregatedPageModel();

        if (aggregatedPageModel == null) {
            throw new ContainerException("Page model cannot be null! Page model might not be aggregated for some reason in #processWindowsRender() for some reason.");
        }

        final HttpServletResponse response = requestContext.getServletResponse();

        try {
            response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
            response.setCharacterEncoding("UTF-8");

            final ComponentWindowModel pageWindowModel = aggregatedPageModel.getPageWindowModel();

            ContentSerializationContext.setCurrentPhase(Phase.REFERENCING_CONTENT_IN_COMPONENT);

            if (pageWindowModel != null) {
                JsonNode pageNode = getObjectMapper().valueToTree(pageWindowModel);
                aggregatedPageModel.setPageNode(pageNode);
            }

            ContentSerializationContext.setCurrentPhase(Phase.SERIALIZING_CONTENT);

            if (aggregatedPageModel.hasAnyContent()) {
                final int maxRefLevel = NumberUtils.toInt(
                        requestContext.getServletRequest().getParameter(MAX_CONTENT_REFERENCE_LEVEL_PARAM_NAME),
                        getDefaultMaxContentReferenceLevel());
                final JsonNode contentNode = serializeContentMap(aggregatedPageModel.getContentMap(), maxRefLevel);
                aggregatedPageModel.setContentNode(contentNode);
            }

            getObjectMapper().writeValue(response.getWriter(), aggregatedPageModel);
        } catch (JsonGenerationException e) {
            throw new ContainerException(e.getMessage(), e);
        } catch (JsonMappingException e) {
            throw new ContainerException(e.getMessage(), e);
        } catch (IOException e) {
            log.warn("Failed to write aggregated page model in json.", e);
        } finally {
            HippoBeanSerializationContext.clear();
            ContentSerializationContext.clear();
        }
    }

    private int getDefaultMaxContentReferenceLevel() {
        final ContainerConfiguration config = HstServices.getComponentManager().getContainerConfiguration();
        return config.getInt("pagemodelapi09.defaultMaxContentReferenceLevel", 1);
    }

    /**
     * Return the Jackson ObjectMapper used in JSON (de)serialization.
     * @return the Jackson ObjectMapper used in JSON (de)serialization
     */
    protected ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    /**
     * Create an aggregated page model to write.
     * <p>
     * Note: if this method is overriden, it is supposed to override {@link #writeAggregatedOutput(ValveContext, HstComponentWindow)}
     * method as well because the method is reponsible for serializaing the returned object from this method.
     * @param sortedComponentWindows sorted component window array which was sorted by the parent {@link AggregationValve}
     * @param requestMap HST Request map for each {@link HstComponentWindow} instance
     * @param responseMap HST Response map for each {@link HstComponentWindow} instance
     * @return an aggregated page model to write
     * @throws ContainerException if container exception occurs
     */
    protected Object createAggregatedPageModel(final HstComponentWindow[] sortedComponentWindows,
            final Map<HstComponentWindow, HstRequest> requestMap,
            final Map<HstComponentWindow, HstResponse> responseMap) throws ContainerException {
        final HstRequestContext requestContext = RequestContextProvider.get();

        // root component (page component) is the first item in the sortedComponentWindows.
        final HstComponentWindow rootWindow = sortedComponentWindows[0];
        final String id = rootWindow.getReferenceNamespace();

        final AggregatedPageModel aggregatedPageModel = new AggregatedPageModel(id);

        final ComponentWindowModel pageWindowModel = new ComponentWindowModel(rootWindow);

        final String definitionId = rootWindow.getComponentInfo().getId();
        if (StringUtils.isNotEmpty(definitionId)) {
            pageWindowModel.putMetadata(PAGE_DEFINITION_ID_METADATA, definitionId);
        }

        final String pageTitle = requestContext.getResolvedSiteMapItem().getPageTitle();
        if (StringUtils.isNotEmpty(pageTitle)) {
            pageWindowModel.putMetadata(PAGE_TITLE_METADATA, pageTitle);
        }

        aggregatedPageModel.setPageWindowModel(pageWindowModel);
        addLinksToPageModel(aggregatedPageModel);

        final int sortedComponentWindowsLen = sortedComponentWindows.length;

        for (int i = 0; i < sortedComponentWindowsLen; i++) {
            final HstComponentWindow window = sortedComponentWindows[i];
            final HstRequest hstRequest = requestMap.get(window);
            final HstResponse hstResponse = responseMap.get(window);

            final ComponentWindowModel currentComponentWindowModel = aggregatedPageModel.getModel(window.getReferenceNamespace())
                    .orElseThrow(() -> new ContainerException(
                            String.format("Expected window for '%s' to be present", window.getReferenceName())));

            addComponentRenderingURLLink(hstResponse, currentComponentWindowModel);
            addParametersInfoMetadata(window, hstRequest, currentComponentWindowModel);
            decorateComponentWindowMetadata(hstRequest, hstResponse, currentComponentWindowModel);

            for (Map.Entry<String, Object> entry : hstRequest.getModelsMap().entrySet()) {
                final String name = entry.getKey();
                final Object model = entry.getValue();
                currentComponentWindowModel.putModel(name, model);
            }
        }

        return aggregatedPageModel;
    }

    /**
     * Adding componentRendering URL link to the linkable model.
     * @param hstResponse HstResponse
     * @param linkableModel linkable model
     */
    private void addComponentRenderingURLLink(HstResponse hstResponse,
            IdentifiableLinkableMetadataBaseModel linkableModel) {
        HstURL compRenderURL = hstResponse.createComponentRenderingURL();
        linkableModel.putLink(ContainerConstants.LINK_NAME_COMPONENT_RENDERING, new LinkModel(compRenderURL.toString()));
    }

    /**
     * Add <code>params</code> metadata to the {@code model} from the {@code window}.
     * @param window HST Component Window instance
     * @param model the {@link MetadataContributable} model instance where the parameter map should be contributed to
     */
    private void addParametersInfoMetadata(HstComponentWindow window, HstRequest hstRequest, MetadataContributable model) {
        final ComponentConfiguration compConfig = window.getComponent().getComponentConfiguration();

        if (compConfig == null) {
            return;
        }

        final Object paramsInfo = ParametersInfoUtils.createParametersInfo(window.getComponent(), compConfig, hstRequest);
        JsonNode paramsInfoNode = null;

        if (paramsInfo != null) {
            try {
                paramsInfoNode = getObjectMapper().valueToTree(paramsInfo);
                model.putMetadata(PARAMETERS_INFO_METADATA, paramsInfoNode);
            } catch (Exception e) {
                log.warn("Failed to convert ParametersInfo instance ({}) to ObjectNode.", paramsInfo, e);
            }
        }

        final ResolvedSiteMapItem resolvedSiteMapItem = RequestContextProvider.get().getResolvedSiteMapItem();
        final ObjectNode paramsNode = getObjectMapper().getNodeFactory().objectNode();

        for (String paramName : compConfig.getParameterNames()) {
            final String paramValue = compConfig.getParameter(paramName, resolvedSiteMapItem);

            if (paramValue != null) {
                paramsNode.put(paramName, paramValue);
            }
        }

        model.putMetadata(PARAMETERS_METADATA, paramsNode);
    }

    /**
     * Add links to the page model.
     * @param pageModel the aggregated page model instance
     */
    private void addLinksToPageModel(IdentifiableLinkableMetadataBaseModel pageModel) {
        final HstRequestContext requestContext = RequestContextProvider.get();
        final HstLinkCreator linkCreator = requestContext.getHstLinkCreator();
        final HstSiteMapItem siteMapItem = requestContext.getResolvedSiteMapItem().getHstSiteMapItem();

        final Mount selfMount = requestContext.getResolvedMount().getMount();
        final HstLink selfLink = linkCreator.create(siteMapItem, selfMount);
        pageModel.putLink(ContainerConstants.LINK_NAME_SELF, new LinkModel(selfLink.toUrlForm(requestContext, true)));

        final Mount siteMount = selfMount.getParent();
        if (siteMount != null) {
            final HstLink siteLink = linkCreator.create(siteMapItem, siteMount);
            pageModel.putLink(ContainerConstants.LINK_NAME_SITE, new LinkModel(siteLink.toUrlForm(requestContext, true)));
        } else {
            log.warn("Expected a 'PageModelPipeline' always to be nested below a parent site mount. This is not the " +
                    "case for '{}'. Cannot add site links", selfMount);
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
     * Serialize content model map which were accumulated from the references in models of components into a <code>JsonNode</code>
     * and return the <code>JsonNode</code>.
     * @param contentMap content model map
     * @return <code>JsonNode</code> serialized from the content model map which were accumulated from the references in models
     * of components
     */
    private JsonNode serializeContentMap(final Map<String, HippoBeanWrapperModel> contentMap, final int maxRefLevel) {
        ObjectNode contentNode = getObjectMapper().createObjectNode();

        for (Map.Entry<String, HippoBeanWrapperModel> entry : contentMap.entrySet()) {
            final String jsonPropName = entry.getKey();
            final HippoBeanWrapperModel beanModel = entry.getValue();

            try {
                appendContentItemModel(contentNode, jsonPropName, beanModel, maxRefLevel, 0);
            } catch (Exception e) {
                log.warn("Failed to append a content item: {}.", jsonPropName, e);
            }
        }

        return contentNode;
    }

    /**
     * Serialize {@code beanModel} into a <code>JsonNode</code> and append the <code>JsonNode</code> to {@code contentNode}
     * with the property name, {@code jsonPropName}.
     * @param contentNode to which the serialized <code>JsonNode</code> from {@code cbeanModel} should be appended
     * @param jsonPropName JSON property name
     * @param beanModel content item bean model
     * @param maxRefLevel maximum reference depth level
     * @param curRefLevel reference depth level
     */
    private void appendContentItemModel(ObjectNode contentNode, final String jsonPropName,
            final HippoBeanWrapperModel beanModel, final int maxRefLevel, final int curRefLevel) {
        if (curRefLevel > maxRefLevel) {
            return;
        }

        if (!contentNode.has(jsonPropName)) {
            final ObjectNode modelNode = getObjectMapper().valueToTree(beanModel);

            // We want to put all the properties of the HippoBean nested in HippoBeanWrapperModel,
            // so let's move the "bean" property object out to the parent node.
            if (modelNode.has(HippoBeanWrapperModel.HIPPO_BEAN_PROP)) {
                final ObjectNode beanNode = (ObjectNode) modelNode.remove(HippoBeanWrapperModel.HIPPO_BEAN_PROP);
                modelNode.setAll(beanNode);
            }

            contentNode.set(jsonPropName, modelNode);
        }

        final Set<HippoBeanWrapperModel> set = HippoBeanSerializationContext
                .getContentBeanModelSet(beanModel.getBean().getRepresentationId());

        if (set != null && !set.isEmpty()) {
            for (HippoBeanWrapperModel model : set) {
                appendContentItemModel(contentNode,
                        HippoBeanSerializer.representationIdToJsonPropName(model.getBean().getRepresentationId()),
                        model, maxRefLevel, curRefLevel + 1);
            }
        }
    }

}