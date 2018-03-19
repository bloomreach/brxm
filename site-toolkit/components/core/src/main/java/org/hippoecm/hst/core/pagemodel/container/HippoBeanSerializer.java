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
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections.CollectionUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.pagemodel.container.ContentSerializationContext.Phase;
import org.hippoecm.hst.core.pagemodel.model.LinkModel;
import org.hippoecm.hst.core.pagemodel.model.MetadataContributable;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import static org.hippoecm.hst.core.container.ContainerConstants.LINK_NAME_SITE;

class HippoBeanSerializer extends JsonSerializer<HippoBean> {

    private static Logger log = LoggerFactory.getLogger(HippoBeanSerializer.class);

    /**
     * Page Model API processing pipeline name.
     */
    private static final String PAGE_MODEL_PIPELINE_NAME = "PageModelPipeline";

    /**
     * Content JSON Pointer prefix.
     */
    private static final String CONTENT_JSON_POINTER_PREFIX = "/content/";

    /**
     * JSON property name prefix for a UUID-based identifier.
     */
    private static final String CONTENT_ID_JSON_NAME_PREFIX = "u";

    /**
     * JSON Pointer Reference Property name.
     */
    private static final String CONTENT_JSON_POINTER_REFERENCE_PROP = "$ref";

    /**
     * Convert content representation identifier (e.g, handle ID or node ID) to a safe JSON property/variable name.
     * @param uuid content identifier
     * @return a safe JSON property/variable name converted from the handle ID or node ID
     */
    static String representationIdToJsonPropName(final String uuid) {
        return new StringBuilder(uuid.length()).append(CONTENT_ID_JSON_NAME_PREFIX).append(uuid.replaceAll("-", ""))
                .toString();
    }

    private final JsonSerializer<Object> beanSerializer;

    private final List<MetadataDecorator> metadataDecorators;

    public HippoBeanSerializer(JsonSerializer<Object> beanSerializer,
            final List<MetadataDecorator> metadataDecorators) {
        this.beanSerializer = beanSerializer;
        this.metadataDecorators = metadataDecorators;
    }

    @Override
    public void serialize(final HippoBean bean, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (!bean.isHippoDocumentBean() || ((HippoDocumentBean) bean).getCanonicalHandleUUID() == null) {
            beanSerializer.serialize(bean, gen, provider);
            return;
        }

        final HstRequestContext requestContext = RequestContextProvider.get();
        Phase curPhase = ContentSerializationContext.getCurrentPhase();

        final String representationId = bean.getRepresentationId();
        final String jsonRepresentationId = representationIdToJsonPropName(representationId);

        if (curPhase == Phase.REFERENCING_CONTENT_IN_COMPONENT) {
            serializeBeanReference(bean, gen, jsonRepresentationId);

            final HippoBeanWrapperModel wrapperBeanModel = new HippoBeanWrapperModel(representationId, bean);
            decorateContentMetadata(requestContext, bean, wrapperBeanModel);
            addLinksToContent(requestContext, wrapperBeanModel);
            AggregatedPageModel aggregatedPageModel = ContentSerializationContext.getCurrentAggregatedPageModel();
            aggregatedPageModel.putContent(jsonRepresentationId, wrapperBeanModel);
        } else {
            if (curPhase == Phase.SERIALIZING_CONTENT) {
                try {
                    ContentSerializationContext.setCurrentPhase(Phase.REFERENCING_CONTENT_IN_CONTENT);
                    HippoBeanSerializationContext.beginTopLevelContentBean(representationId);
                    beanSerializer.serialize(bean, gen, provider);
                } finally {
                    HippoBeanSerializationContext.endTopLevelContentBean();
                    ContentSerializationContext.setCurrentPhase(Phase.SERIALIZING_CONTENT);
                }
            } else if (curPhase == Phase.REFERENCING_CONTENT_IN_CONTENT) {
                serializeBeanReference(bean, gen, jsonRepresentationId);
                final HippoBeanWrapperModel wrapperBeanModel = new HippoBeanWrapperModel(representationId, bean);
                decorateContentMetadata(requestContext, bean, wrapperBeanModel);
                addLinksToContent(requestContext, wrapperBeanModel);
                HippoBeanSerializationContext.pushContentBeanModel(
                        HippoBeanSerializationContext.getCurrentTopLevelContentBeanRepresentationId(), wrapperBeanModel);
            }
        }
    }

    /**
     * Invoke custom metadata decorators to give a chance to add more metadata for the content bean.
     * @param requestContext HstRequestContext object
     * @param contentBean content bean
     * @param model MetadataContributable model
     */
    private void decorateContentMetadata(final HstRequestContext requestContext, final HippoBean contentBean,
            MetadataContributable model) {
        if (CollectionUtils.isEmpty(metadataDecorators)) {
            return;
        }

        for (MetadataDecorator decorator : metadataDecorators) {
            decorator.decorateContentMetadata(requestContext, contentBean, model);
        }
    }

    /**
     * Add links to content bean model.
     * @param contentBeanModel content bean model
     */
    private void addLinksToContent(final HstRequestContext requestContext, final HippoBeanWrapperModel contentBeanModel) {
        final HippoBean hippoBean = contentBeanModel.getBean();

        if (!hippoBean.isHippoDocumentBean() && !hippoBean.isHippoFolderBean()) {
            return;
        }

        final Mount selfMount = requestContext.getResolvedMount().getMount();

        // admittedly a bit of a dirty check to check on PageModelPipeline. Can this be improved?
        if (PAGE_MODEL_PIPELINE_NAME.equals(selfMount.getNamedPipeline())) {
            final Mount siteMount = selfMount.getParent();

            if (siteMount == null) {
                log.warn("Expected a 'PageModelPipeline' always to be nested below a parent site mount. This is not the " +
                        "case for '{}'. Cannot add site links", selfMount);
                return;
            }

            // since the selfLink could be resolved, the site link also must be possible to resolve
            final HstLink siteLink = requestContext.getHstLinkCreator().create(hippoBean.getNode(), siteMount);

            if (siteLink != null && !siteLink.isNotFound()) {
                String linkType = null;
                final HstSiteMapItem siteMapItem = siteLink.getHstSiteMapItem();

                if (siteMapItem != null) {
                    if (siteMapItem.isContainerResource()) {
                        linkType = "resource";
                    } else {
                        final String linkApplicationId = siteMapItem.getApplicationId();
                        // although this is the resolved sitemap item for the PAGE_MODEL_PIPELINE_NAME, it should resolve
                        // to exactly the same hst sitemap item configuration node as the parent mount, hence we can compare
                        // the application id
                        final String currentApplicationId = requestContext.getResolvedSiteMapItem().getHstSiteMapItem().getApplicationId();
                        linkType = (Objects.equals(linkApplicationId, currentApplicationId)) ? "internal" : "external";
                    }
                }

                contentBeanModel.putLink(LINK_NAME_SITE, new LinkModel(siteLink.toUrlForm(requestContext, false), linkType));
            }
        }
    }

    /**
     * Serialize content bean reference.
     * @param bean content bean
     * @param gen JsonGenerator
     * @param jsonPointerRepresentationId JSON Pointer representation ID
     * @throws IOException if IO exception occurs
     */
    private void serializeBeanReference(final HippoBean bean, final JsonGenerator gen,
            final String jsonPointerRepresentationId) throws IOException {
        gen.writeStartObject(bean);
        gen.writeStringField(CONTENT_JSON_POINTER_REFERENCE_PROP,
                CONTENT_JSON_POINTER_PREFIX + jsonPointerRepresentationId);
        gen.writeEndObject();
    }
}