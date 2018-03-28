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
package org.hippoecm.hst.pagemodelapi.v09.core.container;

import java.io.IOException;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.core.pagemodel.container.MetadataDecorator;
import org.hippoecm.hst.pagemodelapi.v09.content.beans.jackson.jackson.LinkModel;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.pagemodel.model.MetadataContributable;
import org.hippoecm.hst.core.request.HstRequestContext;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import static org.hippoecm.hst.core.container.ContainerConstants.LINK_NAME_SITE;

class HippoBeanSerializer extends JsonSerializer<HippoBean> {

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
        final ContentSerializationContext.Phase curPhase = ContentSerializationContext.getCurrentPhase();

        if (curPhase == null) {
            beanSerializer.serialize(bean, gen, provider);
            return;
        }

        boolean inContentSection = bean.isHippoFolderBean();

        if (!inContentSection) {
            if (bean.isHippoDocumentBean() && ((HippoDocumentBean) bean).getCanonicalHandleUUID() != null) {
                inContentSection = true;
            }
        }

        if (!inContentSection) {
            beanSerializer.serialize(bean, gen, provider);
            return;
        }

        final HstRequestContext requestContext = RequestContextProvider.get();

        final String representationId = bean.getRepresentationId();
        final String jsonRepresentationId = representationIdToJsonPropName(representationId);

        if (curPhase == ContentSerializationContext.Phase.REFERENCING_CONTENT_IN_COMPONENT) {
            serializeBeanReference(bean, gen, jsonRepresentationId);
            final HippoBeanWrapperModel wrapperBeanModel = new HippoBeanWrapperModel(representationId, bean);
            decorateContentMetadata(requestContext, bean, wrapperBeanModel);
            addLinksToContent(requestContext, wrapperBeanModel);
            AggregatedPageModel aggregatedPageModel = ContentSerializationContext.getCurrentAggregatedPageModel();
            aggregatedPageModel.putContent(jsonRepresentationId, wrapperBeanModel);
        } else if (curPhase == ContentSerializationContext.Phase.SERIALIZING_CONTENT) {
            try {
                ContentSerializationContext.setCurrentPhase(ContentSerializationContext.Phase.REFERENCING_CONTENT_IN_CONTENT);
                HippoBeanSerializationContext.beginTopLevelContentBean(representationId);
                beanSerializer.serialize(bean, gen, provider);
            } finally {
                HippoBeanSerializationContext.endTopLevelContentBean();
                ContentSerializationContext.setCurrentPhase(ContentSerializationContext.Phase.SERIALIZING_CONTENT);
            }
        } else if (curPhase == ContentSerializationContext.Phase.REFERENCING_CONTENT_IN_CONTENT) {
            serializeBeanReference(bean, gen, jsonRepresentationId);
            final HippoBeanWrapperModel wrapperBeanModel = new HippoBeanWrapperModel(representationId, bean);
            decorateContentMetadata(requestContext, bean, wrapperBeanModel);
            addLinksToContent(requestContext, wrapperBeanModel);
            HippoBeanSerializationContext.addSerializableContentBeanModel(wrapperBeanModel);
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
        final HstLink selfLink = requestContext.getHstLinkCreator().create(hippoBean.getNode(), selfMount);
        if (selfLink == null) {
            return;
        }

        contentBeanModel.putLink(LINK_NAME_SITE, LinkModel.convert(selfLink, requestContext));

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