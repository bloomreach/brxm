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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.core.channelmanager.ChannelManagerConstants;
import org.hippoecm.hst.core.channelmanager.ContentManagementLinkUtils;
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
import org.hippoecm.hst.core.pagemodel.model.NodeSpan;
import org.hippoecm.hst.core.pagemodel.model.ReferenceMetadataBaseModel;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.util.EncodingUtils;
import org.hippoecm.repository.api.HippoNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.w3c.dom.Comment;
import org.w3c.dom.Node;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
     * Self link name.
     */
    private static final String SELF_LINK_NAME = "self";

    /**
     * Site link name.
     */
    private static final String SITE_LINK_NAME = "site";

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
     * Default constructor.
     */
    public PageModelAggregationValve() {
        super();

        // TODO: Move this decorator out of HST to a separate (enterprise) add-on module.
        addMetadataDecorator(new MetadataDecorator() {

            /**
             * Container's or component's begin Node (HTML Comment) span metadata.
             */
            private static final String BEGIN_NODE_SPAN_METADATA = "beginNodeSpan";

            /**
             * Container's or component's end Node (HTML Comment) span metadata.
             */
            private static final String END_NODE_SPAN_METADATA = "endNodeSpan";

            /**
             * Content Link Type attribute name, as same as the one generated by <code>hst:cmseditlink</code> tag.
             */
            private static final String CONTENT_LINK_TYPE = "CONTENT_LINK";

            /**
             * Content UUID attribute name, as same as the one generated by <code>hst:cmseditlink</code> tag.
             */
            private static final String CONTENT_UUID = "uuid";

            /**
             * Content EDIT URL attribute name, as same as the one generated by <code>hst:cmseditlink</code> tag.
             */
            private static final String CONTENT_EDIT_URL = "url";

            @Override
            public void decorateComponentWindowMetadata(HstRequest hstRequest, HstResponse hstResponse,
                    MetadataContributable model) {
                if (model instanceof ComponentWindowModel || model instanceof ComponentContainerWindowModel) {
                    // Add preamble and epilogue comment metadata to the model.
                    // Note the preamble and epilogue comments are available only in preview mode.
                    final Comment preambleComment = getPreambleComment(hstRequest, hstResponse);
                    final Comment epilogueComment = getEpilogueComment(hstRequest, hstResponse);

                    if (preambleComment != null) {
                        model.putMetadata(BEGIN_NODE_SPAN_METADATA, new NodeSpan(preambleComment));
                    }

                    if (epilogueComment != null) {
                        model.putMetadata(END_NODE_SPAN_METADATA, new NodeSpan(epilogueComment));
                    }
                }
            }

            @Override
            public void decorateContentMetadata(final HstRequest hstRequest, final HstResponse hstResponse,
                    HippoBean contentBean, MetadataContributable model) {
                final Comment contentLinkComment = getContentLinkComment(hstRequest, hstResponse, contentBean);

                if (contentLinkComment != null) {
                    model.putMetadata(BEGIN_NODE_SPAN_METADATA, new NodeSpan(contentLinkComment));
                }
            }

            /**
             * Retrieve the preamble Comment node accumulated in {@code hstResponse} during the <code>#doBeforeRender(...)</code>
             * phases of the <code>HstComponent</code> in the parent <code>AggregationValve</code>.
             * @param hstRequest HST Request instance
             * @param hstResponse HST Response instance
             * @return the preamble Comment node accumulated in {@code hstResponse} during the <code>#doBeforeRender(...)</code>
             */
            private Comment getPreambleComment(final HstRequest hstRequest, final HstResponse hstResponse) {
                List<Node> preambles = hstResponse.getPreambleNodes();

                if (preambles == null || preambles.isEmpty()) {
                    return null;
                }

                for (Node preamble : preambles) {
                    if (preamble.getNodeType() == Node.COMMENT_NODE) {
                        return (Comment) preamble;
                    }
                }

                return null;
            }

            /**
             * Retrieve the epilogue Comment node accumulated in {@code hstResponse} during the <code>#doBeforeRender(...)</code>
             * phases of the <code>HstComponent</code> in the parent <code>AggregationValve</code>.
             * @param hstRequest HST Request instance
             * @param hstResponse HST Response instance
             * @return the epilogue Comment node accumulated in {@code hstResponse} during the <code>#doBeforeRender(...)</code>
             */
            private Comment getEpilogueComment(final HstRequest hstRequest, final HstResponse hstResponse) {
                List<Node> epilogues = hstResponse.getEpilogueNodes();

                if (epilogues == null || epilogues.isEmpty()) {
                    return null;
                }

                for (Node epilogue : epilogues) {
                    if (epilogue.getNodeType() == Node.COMMENT_NODE) {
                        return (Comment) epilogue;
                    }
                }

                return null;
            }

            /**
             * Return a Comment node for the {@contentBean} if available and it's in preview mode. Otherwise, null.
             * <p>
             * TODO: For now, generate the same comment as the deprecated hst:cmseditlink tag generates.
             *       The new comments generated by hst:manageContent tag can be delivered in the second-phase.
             * @param hstRequest HST Request instance
             * @param hstResponse HST Response instance
             * @param contentBean HST Content Bean instance
             * @return a Comment node for the {@contentBean} if available and it's in preview mode. Otherwise, null
             */
            private Comment getContentLinkComment(final HstRequest hstRequest, final HstResponse hstResponse,
                    final HippoBean contentBean) {
                if (!hstRequest.getRequestContext().isPreview()) {
                    return null;
                }

                try {
                    final String cmsBaseURL = ContentManagementLinkUtils.getCmsBaseURL();
                    final javax.jcr.Node editableNode = ContentManagementLinkUtils
                            .getCmsEditableNode((HippoNode) contentBean.getNode());

                    if (editableNode != null) {
                        final String nodeId = editableNode.getIdentifier();
                        final String nodeLocation = editableNode.getPath();
                        final String encodedPath = EncodingUtils.getEncodedPath(nodeLocation, hstRequest);
                        final String cmsEditLink = cmsBaseURL + "?path=" + encodedPath;

                        final Map<String, Object> result = new HashMap<>();
                        result.put(ChannelManagerConstants.HST_TYPE, CONTENT_LINK_TYPE);
                        result.put(CONTENT_UUID, nodeId);
                        result.put(CONTENT_EDIT_URL, cmsEditLink);

                        return hstResponse.createComment(ContentManagementLinkUtils.toJSONMap(result));
                    }
                } catch (RepositoryException e) {
                    log.warn("Failed to generate content link comment.", e);
                }

                return null;
            }
        });
    }

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
     * Add a custom {@link MetadataDecorator} instance, which is invoked to give a chance to customize
     * the {@link MetadataContributable} instances in the page model aggregation result output.
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
            final HstRequest hstRequest = requestMap.get(window);
            final HstResponse hstResponse = responseMap.get(window);

            if (window.isContainerWindow()) {
                curContainerWindowModel = new ComponentContainerWindowModel(window.getReferenceNamespace(),
                        window.getName());
                addParameterMapMetadata(window, curContainerWindowModel);
                decorateComponentWindowMetadata(hstRequest, hstResponse, curContainerWindowModel);
                pageModel.addContainerWindow(curContainerWindowModel);
            } else if (window.isContainerItemWindow()) {
                if (curContainerWindowModel == null) {
                    log.warn("Invalid container item component window location for {}.",
                            window.getReferenceNamespace());
                    continue;
                }

                final ComponentWindowModel componentWindowModel = new ComponentWindowModel(
                        window.getReferenceNamespace(), window.getName(), window.getComponentName());
                componentWindowModel.setLabel(window.getComponentInfo().getLabel());
                addParameterMapMetadata(window, componentWindowModel);
                decorateComponentWindowMetadata(hstRequest, hstResponse, componentWindowModel);

                for (Map.Entry<String, Object> entry : hstRequest.getModelsMap().entrySet()) {
                    final String name = entry.getKey();
                    final Object model = entry.getValue();
                    ReferenceMetadataBaseModel referenceModel = null;

                    if (model instanceof HippoBean) {
                        final HippoBean bean = (HippoBean) model;
                        final String contentId = getContentId(bean);
                        final String jsonPointerContentId = contentIdToJsonName(contentId);
                        HippoBeanWrapperModel beanWrapperModel = addContentModelToPageModel(pageModel, bean, contentId,
                                jsonPointerContentId);
                        decorateContentMetadata(hstRequest, hstResponse, bean, beanWrapperModel);
                        referenceModel = new ReferenceMetadataBaseModel(
                                CONTENT_JSON_POINTER_PREFIX + jsonPointerContentId);
                    }

                    componentWindowModel.putModel(name, (referenceModel != null) ? referenceModel : model);
                }

                curContainerWindowModel.addComponentWindowSet(componentWindowModel);
            } else {
                curContainerWindowModel = null;
            }

        }

        return pageModel;
    }

    /**
     * Add and return a content model to the page model's content section.
     * @param pageModel aggregated page model
     * @param bean Hippo content bean instance
     * @param contentId the content identifier of a specific Hippo content bean instance
     * @param jsonPointerContentId the identifier to be used in JSON Pointer String representation for the content {@code bean}
     * @return wrapperBeanModel added content wrapper model
     */
    private HippoBeanWrapperModel addContentModelToPageModel(final AggregatedPageModel pageModel, final HippoBean bean,
            final String contentId, final String jsonPointerContentId) {
        final HippoBeanWrapperModel wrapperBeanModel = new HippoBeanWrapperModel(contentId, bean);

        if (bean.isHippoDocumentBean() || bean.isHippoFolderBean()) {
            final HstRequestContext requestContext = RequestContextProvider.get();
            final HstLinkCreator linkCreator = requestContext.getHstLinkCreator();

            final Mount selfMount = requestContext.getResolvedMount().getMount();
            final HstLink selfLink = linkCreator.create(bean.getNode(), selfMount);
            wrapperBeanModel.putLink(SELF_LINK_NAME, selfLink.toUrlForm(requestContext, true));

            final Mount siteMount = requestContext.getMount(ContainerConstants.MOUNT_ALIAS_SITE);
            if (siteMount != null) {
                final HstLink siteLink = linkCreator.create(bean.getNode(), siteMount);
                wrapperBeanModel.putLink(SITE_LINK_NAME, siteLink.toUrlForm(requestContext, true));
            }
        }

        pageModel.putContent(jsonPointerContentId, wrapperBeanModel);

        return wrapperBeanModel;
    }

    /**
     * Get content identifier for a {@link HippoBean}.
     * @param bean Hippo content bean instance
     * @return content identifier for a {@link HippoBean}
     */
    private String getContentId(final HippoBean bean) {
        if (bean instanceof HippoDocumentBean) {
            return ((HippoDocumentBean) bean).getCanonicalHandleUUID();
        }

        return bean.getCanonicalUUID();
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
            pageModel.putLink(SELF_LINK_NAME, selfLink.toUrlForm(requestContext, true));

            final Mount siteMount = requestContext.getMount(ContainerConstants.MOUNT_ALIAS_SITE);
            if (siteMount != null) {
                final HstLink siteLink = linkCreator.create(siteMapItem, siteMount);
                pageModel.putLink(SITE_LINK_NAME, siteLink.toUrlForm(requestContext, true));
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
     * @param HippoBean content bean
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