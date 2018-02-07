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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
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
import org.hippoecm.hst.core.pagemodel.model.MetadataBaseModel;
import org.hippoecm.hst.core.pagemodel.model.NodeSpan;
import org.hippoecm.hst.core.pagemodel.model.ReferenceMetadataBaseModel;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
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
    private static final String CONTENT_JSON_POINTER_PREFIX = "#/content/";

    /**
     * JSON property name prefix for a UUID-based identifier.
     */
    private static final String CONTENT_ID_JSON_NAME_PREFIX = "u";

    /**
     * Page or component parameter map metadata name.
     */
    private static final String PARAMETERNS_METADATA = "params";

    /**
     * Container's or component's begin Node (HTML Comment) span metadata.
     */
    private static final String BEGIN_NODE_SPAN_METADATA = "beginNodeSpan";

    /**
     * Container's or component's end Node (HTML Comment) span metadata.
     */
    private static final String END_NODE_SPAN_METADATA = "endNodeSpan";

    private ObjectMapper objectMapper;

    public ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
        }

        return objectMapper;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
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
     * @param writer
     * @param pageModel
     * @throws ContainerException
     * @throws IOException
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
     * @param sortedComponentWindows
     * @param requestMap
     * @param responseMap
     * @return
     * @throws ContainerException
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
                addPreambleEpilogueMetadata(hstResponse, curContainerWindowModel);
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
                addPreambleEpilogueMetadata(hstResponse, componentWindowModel);

                for (Map.Entry<String, Object> entry : hstRequest.getModelsMap().entrySet()) {
                    final String name = entry.getKey();
                    final Object model = entry.getValue();
                    ReferenceMetadataBaseModel referenceModel = null;

                    if (model instanceof HippoBean) {
                        final HippoBean bean = (HippoBean) model;
                        final String contentId = getContentId(bean);
                        final String jsonPointerContentId = contentIdToJsonName(contentId);
                        addContentModelToPageModel(pageModel, bean, contentId, jsonPointerContentId);
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
     * Add a content model to the page model's content section.
     * @param pageModel
     * @param bean
     * @param contentId
     * @param jsonPointerContentId
     */
    private void addContentModelToPageModel(final AggregatedPageModel pageModel, final HippoBean bean,
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
    }

    /**
     * Get content identifier for a {@link HippoBean}.
     * @param bean
     * @return
     */
    private String getContentId(final HippoBean bean) {
        if (bean instanceof HippoDocumentBean) {
            return ((HippoDocumentBean) bean).getCanonicalHandleUUID();
        }

        return bean.getCanonicalUUID();
    }

    /**
     * Convert content identifier (e.g, UUID) to a safe JSON property/variable name.
     * @param uuid
     * @return
     */
    private String contentIdToJsonName(final String uuid) {
        return new StringBuilder(uuid.length()).append(CONTENT_ID_JSON_NAME_PREFIX).append(uuid.replaceAll("-", ""))
                .toString();
    }

    /**
     * Add <code>params</code> metadata to the {@code model} from the {@code window}.
     * @param window
     * @param model
     */
    private void addParameterMapMetadata(HstComponentWindow window, MetadataBaseModel model) {
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
     * @param pageModel
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
     * Add preamble and epilogue comment metadata to the {@code model}.
     * @param hstResponse
     * @param model
     */
    private void addPreambleEpilogueMetadata(HstResponse hstResponse, MetadataBaseModel model) {
        final Comment preambleComment = getPreambleComment(hstResponse);
        final Comment epilogueComment = getEpilogueComment(hstResponse);

        if (preambleComment != null) {
            model.putMetadata(BEGIN_NODE_SPAN_METADATA, new NodeSpan(preambleComment));
        }

        if (epilogueComment != null) {
            model.putMetadata(END_NODE_SPAN_METADATA, new NodeSpan(epilogueComment));
        }
    }

    /**
     * Get the preamble commend node from the {@code response}.
     * @param response
     * @return
     */
    private Comment getPreambleComment(final HstResponse response) {
        List<Node> preambles = response.getPreambleNodes();

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
     * Get the epilogue commend node from the {@code response}.
     * @param response
     * @return
     */
    private Comment getEpilogueComment(final HstResponse response) {
        List<Node> epilogues = response.getEpilogueNodes();

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
}