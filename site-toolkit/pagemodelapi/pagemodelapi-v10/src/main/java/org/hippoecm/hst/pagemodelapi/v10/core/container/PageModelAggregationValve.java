/*
 *  Copyright 2018-2020 Bloomreach
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
package org.hippoecm.hst.pagemodelapi.v10.core.container;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.components.DynamicParameter;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.container.HstContainerRequest;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.container.AggregationValve;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstComponentWindow;
import org.hippoecm.hst.core.container.HstContainerConfig;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.container.ValveContext;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.pagemodel.container.MetadataDecorator;
import org.hippoecm.hst.core.pagemodel.model.MetadataContributable;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.pagemodelapi.common.content.beans.PageModelObjectMapperFactory;
import org.hippoecm.hst.pagemodelapi.v10.content.beans.jackson.LinkModel;
import org.hippoecm.hst.pagemodelapi.v10.core.model.ChannelInfoModel;
import org.hippoecm.hst.pagemodelapi.v10.core.model.ChannelModel;
import org.hippoecm.hst.pagemodelapi.v10.core.model.ComponentWindowModel;
import org.hippoecm.hst.pagemodelapi.v10.core.model.IdentifiableLinkableMetadataBaseModel;
import org.hippoecm.hst.site.request.ComponentConfigurationImpl;
import org.hippoecm.hst.util.HstRequestUtils;
import org.hippoecm.hst.util.ParametersInfoUtils;
import org.hippoecm.hst.util.QueryStringBuilder;
import org.onehippo.cms7.services.hst.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hippoecm.hst.core.container.ContainerConstants.LINK_NAME_SELF;
import static org.hippoecm.hst.pagemodelapi.v10.content.beans.jackson.LinkModel.LinkType.EXTERNAL;
import static org.hippoecm.hst.pagemodelapi.v10.content.beans.jackson.LinkModel.LinkType.INTERNAL;
import static org.hippoecm.hst.util.HstRequestUtils.getFarthestRequestHost;
import static org.hippoecm.hst.util.HstRequestUtils.getFarthestRequestScheme;
import static org.onehippo.repository.branch.BranchConstants.MASTER_BRANCH_ID;

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
     * Preview mode meta-data name.
     */
    private static final String PAGE_PREVIEW_METADATA = "preview";

    /**
     * Maximum content reference level request parameter name.
     */
    private static final String MAX_CONTENT_REFERENCE_LEVEL_PARAM_NAME = "_maxreflevel";

    /**
     * Page Model Object Jackson ObjectMapper instance for JSON (de)serialization.
     */
    private final ObjectMapper pageModelObjectMapper;

    /**
     * Custom metadata decorators.
     */
    private final List<MetadataDecorator> metadataDecorators = new ArrayList<>();
    private final static String HIDE_PARAMETER_NAME = "com.onehippo.cms7.targeting.TargetingParameterUtil.hide";

    private int defaultMaxContentReferenceLevel;

    private String apiDocPath;

    private boolean prettyPrint;
    private JsonPointerFactory jsonPointerFactory;

    public PageModelAggregationValve(final PageModelObjectMapperFactory factory, final Map<Class<?>, Class<?>> extraMixins,
                                     final JsonPointerFactory jsonPointerFactory) {
        this.jsonPointerFactory = jsonPointerFactory;
        pageModelObjectMapper = factory.createPageModelObjectMapper().registerModule(new SimpleModule().setSerializerModifier(
                new PageModelSerializerModifier(metadataDecorators, jsonPointerFactory)
        ));
        HstBeansObjectMapperDecorator.decorate(pageModelObjectMapper, extraMixins);

    }

    public void setDefaultMaxContentReferenceLevel(int defaultMaxContentReferenceLevel) {
        this.defaultMaxContentReferenceLevel = defaultMaxContentReferenceLevel;
    }

    public void setPrettyPrint(final boolean prettyPrint) {

        this.prettyPrint = prettyPrint;
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
     * Set API Document (e.g, /swagger.json) Path.
     * @param apiDocPath API Document (e.g, /swagger.json) Path
     */
    public void setApiDocPath(String apiDocPath) {
        this.apiDocPath = apiDocPath;
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
        setCurrentAggregatedPageModel(aggregatedPageModel);
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

        final AggregatedPageModel aggregatedPageModel = new AggregatedPageModel(null);
        decorateAggregatedPageModel(requestContext, aggregatedPageModel);

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

        aggregatedPageModel.setDocument(requestContext.getContentBean());

        addPreviewFlagToPageModel(aggregatedPageModel, requestContext);

        // include api version to _meta section
        aggregatedPageModel.putMetadata("version",
                requestContext.getServletRequest().getAttribute(ContainerConstants.PAGE_MODEL_API_VERSION));

        final Channel channel = requestContext.getResolvedMount().getMount().getChannel();
        if (channel == null || channel.getBranchId() == null) {
            // even though channel can be null, it means kind of that there is only a master 'branch' even though there
            // is not channel
            aggregatedPageModel.putMetadata("branch", MASTER_BRANCH_ID);
        } else {
            aggregatedPageModel.putMetadata("branch", channel.getBranchId());
        }

        addLinksToPageModel(aggregatedPageModel);
        setChannelModelToPageModel(aggregatedPageModel);

        final int sortedComponentWindowsLen = sortedComponentWindows.length;

        for (int i = 0; i < sortedComponentWindowsLen; i++) {
            final HstComponentWindow window = sortedComponentWindows[i];
            final HstRequest hstRequest = requestMap.get(window);
            final HstResponse hstResponse = responseMap.get(window);

            final ComponentWindowModel currentComponentWindowModel = aggregatedPageModel.getModel(window.getReferenceNamespace())
                    .orElseThrow(() -> new ContainerException(
                            String.format("Expected window for '%s' to be present", window.getReferenceName())));

            addComponentRenderingURLLink(hstResponse, requestContext, currentComponentWindowModel);
            addParametersInfoMetadata(pageWindowModel, window, hstRequest, currentComponentWindowModel);
            decorateComponentWindowMetadata(hstRequest, hstResponse, currentComponentWindowModel);

            for (Map.Entry<String, Object> entry : hstRequest.getModelsMap().entrySet()) {
                currentComponentWindowModel.putModel(entry.getKey(), entry.getValue());
            }
        }

        return aggregatedPageModel;
    }

    /**
     * <code>HstRequestContext</code> specific {@link AggregatedPageModel} attribute name.
     */
    private static final String AGGREGATED_PAGE_MODEL_ATTR = PageModelAggregationValve.class.getName()
            + ".aggregatedPageModel";

    /**
     * Return the current {@link AggregatedPageModel} object.
     * @return the current {@link AggregatedPageModel} object
     */
    public static AggregatedPageModel getCurrentAggregatedPageModel() {
        final HstRequestContext requestContext = RequestContextProvider.get();

        if (requestContext != null) {
            return (AggregatedPageModel) requestContext.getAttribute(AGGREGATED_PAGE_MODEL_ATTR);
        }

        return null;
    }

    /**
     * Set the current {@link AggregatedPageModel} object.
     * @param aggregatedPageModel the current {@link AggregatedPageModel} object
     */
    public static void setCurrentAggregatedPageModel(AggregatedPageModel aggregatedPageModel) {
        final HstRequestContext requestContext = RequestContextProvider.get();

        if (requestContext == null) {
            throw new IllegalStateException("HstRequestContext is not available.");
        }

        requestContext.setAttribute(AGGREGATED_PAGE_MODEL_ATTR, aggregatedPageModel);
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
        final HstRequestContext requestContext = context.getRequestContext();

        final HttpServletResponse response = requestContext.getServletResponse();
        try {

            final int maxDocumentRefLevel = NumberUtils.toInt(
                    requestContext.getServletRequest().getParameter(MAX_CONTENT_REFERENCE_LEVEL_PARAM_NAME),
                    defaultMaxContentReferenceLevel);
            PageModelSerializer.initContext(maxDocumentRefLevel);

            response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setHeader(ContainerConstants.PAGE_MODEL_API_VERSION,
                    (String)requestContext.getServletRequest().getAttribute(ContainerConstants.PAGE_MODEL_API_VERSION));


            if (prettyPrint) {
                pageModelObjectMapper.writerWithDefaultPrettyPrinter().writeValue(response.getWriter(), getCurrentAggregatedPageModel());
            } else {
                pageModelObjectMapper.writeValue(response.getWriter(), getCurrentAggregatedPageModel());
            }


        } catch (JsonGenerationException e) {
            throw new ContainerException(e.getMessage(), e);
        } catch (JsonMappingException e) {
            throw new ContainerException(e.getMessage(), e);
        } catch (IOException e) {
            log.warn("Failed to write aggregated page model in json.", e);
        } finally {
            PageModelSerializer.closeContext();
        }
    }

    @Override
    protected boolean isAggregationApiDocumentRequest(final ValveContext context) {
        if (StringUtils.isNotBlank(apiDocPath)) {
            final HstRequestContext requestContext = context.getRequestContext();
            final HstContainerURL baseURL = requestContext.getBaseURL();
            return apiDocPath.equals(baseURL.getPathInfo());
        }

        return false;
    }

    private void addComponentRenderingURLLink(final HstResponse hstResponse,
                                              final HstRequestContext requestContext,
                                              final IdentifiableLinkableMetadataBaseModel linkableModel) {
        HstURL compRenderURL = hstResponse.createComponentRenderingURL();
        final String fullyQualifiedHref;
        String pageModelApiHost = requestContext.getResolvedMount().getMount().getHstLinkUrlPrefix();
        if (pageModelApiHost != null) {
            fullyQualifiedHref = pageModelApiHost + compRenderURL.toString();
        } else {
            final HttpServletRequest request = requestContext.getServletRequest();
            fullyQualifiedHref = getFarthestRequestScheme(request) + "://" + getFarthestRequestHost(request) + compRenderURL.toString();
        }
        linkableModel.putLink(LINK_NAME_SELF, new LinkModel(fullyQualifiedHref, EXTERNAL));
    }

    /**
     * Add <code>params</code> metadata to the {@code model} from the {@code window}.
     * @param pageWindowModel
     * @param window HST Component Window instance
     * @param model the {@link MetadataContributable} model instance where the parameter map should be contributed to
     */
    private void addParametersInfoMetadata(final ComponentWindowModel pageWindowModel,
                                           HstComponentWindow window, HstRequest hstRequest, MetadataContributable model) {
        final ComponentConfiguration compConfig = (window.getComponent() != null)
                ? window.getComponent().getComponentConfiguration()
                : null;

        if (compConfig == null) {
            return;
        }

        final Object paramsInfo = ParametersInfoUtils.createParametersInfo(window.getComponent(), compConfig, hstRequest,
                new PageModelApiParameterValueConvertor(jsonPointerFactory, metadataDecorators));

        if (paramsInfo != null) {
            try {
                model.putMetadata(PARAMETERS_INFO_METADATA, paramsInfo);
            } catch (Exception e) {
                log.warn("Failed to convert ParametersInfo instance ({}) to ObjectNode.", paramsInfo, e);
            }
        }

        model.putMetadata(PARAMETERS_METADATA, getResidualParameters(compConfig));
    }

    private Map<String, String> getResidualParameters(final ComponentConfiguration compConfig) {

        final ResolvedSiteMapItem resolvedSiteMapItem = RequestContextProvider.get().getResolvedSiteMapItem();

        Map<String, String> parameters = compConfig.getParameters(resolvedSiteMapItem);

        String[] variants = getVariants(compConfig);
        List<String> paramsInfoNames = getParamsInfoNames(compConfig);

        return parameters.entrySet().stream()
            .filter(entry -> !StringUtils.startsWithAny(entry.getKey(), variants))
            .filter(entry -> !paramsInfoNames.contains(entry.getKey()))
            .filter(entry -> !HIDE_PARAMETER_NAME.equals(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private List<String> getParamsInfoNames(final ComponentConfiguration compConfig) {
        if (compConfig instanceof ComponentConfigurationImpl) {
            return ((ComponentConfigurationImpl) compConfig).getComponentConfiguration().getDynamicComponentParameters()
                .stream().map(DynamicParameter::getName).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private String[] getVariants(final ComponentConfiguration compConfig) {
        if (compConfig instanceof ComponentConfigurationImpl) {
            return ((ComponentConfigurationImpl) compConfig).getComponentConfiguration().getVariants()
                .toArray(new String[0]);
        }
        return new String[0];
    }

    private void addPreviewFlagToPageModel(final AggregatedPageModel aggregatedPageModel, final HstRequestContext requestContext) {
        if (requestContext.isChannelManagerPreviewRequest()) {
            aggregatedPageModel.putMetadata(PAGE_PREVIEW_METADATA, true);
        }
    }

    /**
     * Add links to the page model.
     * @param pageModel the aggregated page model instance
     */
    private void addLinksToPageModel(IdentifiableLinkableMetadataBaseModel pageModel) {
        final HstRequestContext requestContext = RequestContextProvider.get();
        final HstLinkCreator linkCreator = requestContext.getHstLinkCreator();
        final ResolvedSiteMapItem resolvedSiteMapItem = requestContext.getResolvedSiteMapItem();

        final Mount selfMount = requestContext.getResolvedMount().getMount();
        final HstLink selfLink = linkCreator.create(resolvedSiteMapItem.getPathInfo(), selfMount);
        String hrefSelf = selfLink.toUrlForm(requestContext, true);

        // the site link for the current Page Model API output is *always* internal and relative within the sitemap of the
        // current channel hence just take path info from resolved sitemap item

        String hrefSite = resolvedSiteMapItem.getPathInfo();

        hrefSite = hrefSite.equals(selfMount.getHomePage()) ? "/" : "/" + hrefSite;

        HttpServletRequest servletRequest = requestContext.getServletRequest();

        // now if needed include the getPathSuffix() and if present in the 'SELF' link but never in the 'site link'
        if (servletRequest instanceof HstContainerRequest) {
            String pathSuffix = ((HstContainerRequest) servletRequest).getPathSuffix();
            if (isNotBlank(pathSuffix)) {
                hrefSelf += "./" + pathSuffix;
            }
        }

        // servletRequest.getQueryString() can have encoded parts like '%3A' instead of ':' so cannot be used
        // therefore use QueryStringBuilder which is encoding aware
        final QueryStringBuilder queryStringBuilder = new QueryStringBuilder(HstRequestUtils.getURIEncoding(servletRequest));

        servletRequest.getParameterMap().entrySet().forEach(entry -> {
            final String[] values = entry.getValue();
            if (values != null) {
                Arrays.stream(values).forEach(value -> {
                    try {
                        queryStringBuilder.append(entry.getKey(), value);
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        });

        final String queryString = queryStringBuilder.toString();

        if (isNotBlank(queryString)) {
            hrefSelf += queryString;
            hrefSite += queryString;
        }

        pageModel.putLink(LINK_NAME_SELF, new LinkModel(hrefSelf, EXTERNAL));
        pageModel.putLink(ContainerConstants.LINK_NAME_SITE, new LinkModel(hrefSite, INTERNAL));

    }

    /**
     * Set channel model to the page model.
     * @param pageModel the aggregated page model instance
     */
    private void setChannelModelToPageModel(final AggregatedPageModel pageModel) {
        final ChannelModel channelModel = new ChannelModel();

        final HstRequestContext requestContext = RequestContextProvider.get();
        final Mount mount = requestContext.getResolvedMount().getMount();
        final ChannelInfo channelInfo = mount.getChannelInfo();

        if (channelInfo != null) {
            channelModel.setChannelInfoModel(new ChannelInfoModel(channelInfo));
        }

        pageModel.setChannelModel(channelModel);
    }

    /**
     * Invoke custom metadata decorators to give a chance to add more metadata for the aggregated page model.
     * @param requestContext the HST requestContext object
     * @param aggregatedPageModel MetadataContributable model
     */
    private void decorateAggregatedPageModel(final HstRequestContext requestContext,
                                             final AggregatedPageModel aggregatedPageModel) {
        if (CollectionUtils.isEmpty(metadataDecorators)) {
            return;
        }

        for (MetadataDecorator decorator : metadataDecorators) {
            decorator.decorateAggregatedPageModelMetadata(requestContext, aggregatedPageModel);
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

}
