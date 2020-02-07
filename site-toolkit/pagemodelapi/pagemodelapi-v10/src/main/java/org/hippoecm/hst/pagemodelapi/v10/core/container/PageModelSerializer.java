/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.standard.HippoAssetBean;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageSet;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.pagemodel.container.MetadataDecorator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.sitemenu.CommonMenu;
import org.hippoecm.hst.pagemodelapi.v10.content.beans.jackson.LinkModel;
import org.hippoecm.hst.pagemodelapi.v10.core.model.ComponentWindowModel;
import org.hippoecm.hst.pagemodelapi.v10.core.model.IdentifiableLinkableMetadataBaseModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static org.hippoecm.hst.core.container.ContainerConstants.LINK_NAME_SITE;

public class PageModelSerializer extends JsonSerializer<Object> {

    private static ThreadLocal<SerializerContext> tlSerializerContext = new ThreadLocal<>();

    private static Logger log = LoggerFactory.getLogger(PageModelSerializer.class);

    static class SerializerContext {
        private boolean firstEntity = true;
        private ArrayDeque<JsonPointerWrapper> serializeQueue = new ArrayDeque<>();
        // the current page model entity that is being serialized
        private Object serializingPageModelEntity;
        // hash set to keep track of already serialized objects (same pointers)
        Set<String> serializedPointers = new HashSet<>();
        Set<Object> handledPmaEntities = new HashSet<>();
        Map<Object, String> objectJsonPointerMap = new HashMap<>();
        private int maxDocumentRefLevel;

        public SerializerContext(final int maxDocumentRefLevel) {
            this.maxDocumentRefLevel = maxDocumentRefLevel;
        }
    }

    static void initContext(final int maxDocumentRefLevel) {
        tlSerializerContext.set(new SerializerContext(maxDocumentRefLevel));
    }

    static void closeContext() {
        tlSerializerContext.set(null);
    }

    private JsonSerializer<Object> delegatee;
    private JsonPointerFactory jsonPointerFactory;
    private final List<MetadataDecorator> metadataDecorators;

    public PageModelSerializer(final JsonSerializer<Object> delegatee, final JsonPointerFactory jsonPointerFactory,
                               final List<MetadataDecorator> metadataDecorators) {
        this.delegatee = delegatee;
        this.jsonPointerFactory = jsonPointerFactory;
        this.metadataDecorators = metadataDecorators;
    }

    final Class[] KNOWN_PMA_ENTITIES = new Class[]{ComponentWindowModel.class, HippoDocumentBean.class,
            HippoFolderBean.class, CommonMenu.class};

    @Override
    public void serialize(final Object object, final JsonGenerator gen, final SerializerProvider serializerProvider) throws IOException {
        SerializerContext serializerContext = tlSerializerContext.get();

        if (serializerContext == null) {
            throw new IllegalStateException("PageModelSerializer not in the correct state for current thread, first " +
                    "invoke #initContext to setup the required thread local");
        }
        if (object.getClass().equals(AggregatedPageModel.RootReference.class)) {
            AggregatedPageModel.RootReference ref = (AggregatedPageModel.RootReference) object;
            Object pageRoot = ref.getObject();
            String jsonPointerId = jsonPointerFactory.createJsonPointerId();
            JsonPointerWrapper value = new JsonPointerWrapper(pageRoot, jsonPointerId);
            serializeBeanReference(gen, jsonPointerId);
            serializerContext.serializeQueue.add(value);
            return;
        }

        if (serializerContext.serializingPageModelEntity instanceof DecoratedPageModelEntityWrapper) {
            final DecoratedPageModelEntityWrapper entity = (DecoratedPageModelEntityWrapper) serializerContext.serializingPageModelEntity;
            if (entity.getData() == object && !entity.isSerialized()) {
                // although a PMA entity, we now really need to serialize it

                // Now to avoid potential recursion for a PMA entity referencing itself (same java instance via some chain),
                // we now mark the DecoratedPageModelEntityWrapper as serialized!
                entity.setSerialized(true);
                delegatee.serialize(object, gen, serializerProvider);
                return;
            }
        }

        Optional<Class> pmaEntity = Arrays.stream(KNOWN_PMA_ENTITIES).filter(pmaClass -> pmaClass.isAssignableFrom(object.getClass())).findFirst();

        if (!pmaEntity.isPresent()) {
            // Not a PMA entity or already wrapped in DecoratedPageModelEntityWrapper, so no 'flattened' serialization
            delegatee.serialize(object, gen, serializerProvider);
            return;
        }

        if (serializerContext.serializingPageModelEntity != null) {

            if (serializerContext.serializingPageModelEntity == object) {
                delegatee.serialize(object, gen, serializerProvider);
            } else {
                // we are currently serializing a page model entity and during that serialization, we now encounter a
                // new nested page model entity which must be postponed and therefor added to serializeQueue unless it
                // is a document at a deeper than max level depth

                int nextDepth = 0;
                if (serializerContext.serializingPageModelEntity instanceof DecoratedPageModelEntityWrapper) {
                    nextDepth = ((DecoratedPageModelEntityWrapper) serializerContext.serializingPageModelEntity).getCurrentDepth();
                    if (object instanceof HippoDocumentBean || object instanceof HippoFolderBean) {
                        nextDepth++;
                    }
                    if (nextDepth > serializerContext.maxDocumentRefLevel) {
                        if (serializerContext.handledPmaEntities.contains(object)) {
                            // even though deeper reference than max depth, the referenced doc has been already handled,
                            // so just include the reference
                            String jsonPointerId = serializerContext.objectJsonPointerMap
                                    .computeIfAbsent(object, obj -> jsonPointerFactory.createJsonPointerId(object));

                            serializeBeanReference(gen, jsonPointerId);
                            return;
                        } else {
                            gen.writeStartObject();
                            // just serialize only the ID of the document bean and return : the bean does not get
                            // serialized since deeper than max reference
                            gen.writeStringField("uuid", ((HippoBean) object).getRepresentationId());
                            gen.writeEndObject();
                        }
                        return;
                    }
                }

                // make sure same object returns same jsonPointerId : handy if the same model object is used multiple
                // times in the PMA
                String jsonPointerId = serializerContext.objectJsonPointerMap
                        .computeIfAbsent(object, obj -> jsonPointerFactory.createJsonPointerId(object));

                serializeBeanReference(gen, jsonPointerId);

                if (serializerContext.handledPmaEntities.contains(object)) {
                    // already handled pma entity, so only the $ref is enough for this object
                    return;
                }

                serializerContext.handledPmaEntities.add(object);


                if (object instanceof CommonMenu) {

                    HstRequestContext requestContext = RequestContextProvider.get();
                    final DecoratedPageModelEntityWrapper<CommonMenu> decoratedPageModelEntityWrapper
                            = new DecoratedPageModelEntityWrapper(object, "menu", nextDepth);
                    for (MetadataDecorator metadataDecorator : metadataDecorators) {
                        log.trace("Decorate menu '{}' with metadataDecorator '{}'", ((CommonMenu)object).getName(), metadataDecorator);
                        metadataDecorator.decorateCommonMenuMetadata(requestContext,
                                decoratedPageModelEntityWrapper.getData(), decoratedPageModelEntityWrapper);
                    }
                    serializerContext.serializeQueue.add(new JsonPointerWrapper(decoratedPageModelEntityWrapper, jsonPointerId));

                } else if (object instanceof HippoDocumentBean || object instanceof HippoFolderBean) {

                    HstRequestContext requestContext = RequestContextProvider.get();
                    final DecoratedPageModelEntityWrapper<HippoBean> decoratedPageModelEntityWrapper
                            = new DecoratedPageModelEntityWrapper(object, getHippoBeanType((HippoBean) object), nextDepth);
                    if (object instanceof HippoAssetBean || object instanceof HippoGalleryImageSet) {
                        log.trace("Skip adding links to hippo asset or gallery document since only links to the " +
                                "resources in them are useful");
                    } else {
                        log.trace("Add links to document or folder bean");
                        addLinksToContent(requestContext, decoratedPageModelEntityWrapper);
                    }

                    for (MetadataDecorator metadataDecorator : metadataDecorators) {
                        log.trace("Decorate '{}' with metadataDecorator '{}'", ((HippoBean)object).getPath(), metadataDecorator);
                        metadataDecorator.decorateContentMetadata(requestContext,
                                decoratedPageModelEntityWrapper.getData(), decoratedPageModelEntityWrapper);
                    }

                    serializerContext.serializeQueue.add(new JsonPointerWrapper(decoratedPageModelEntityWrapper, jsonPointerId));
                } else {
                    // no extra wrapping needed other than json pointer inclusion
                    serializerContext.serializeQueue.add(new JsonPointerWrapper(object, jsonPointerId));
                }

                return;
            }
        } else {
            while (!serializerContext.serializeQueue.isEmpty()) {
                JsonPointerWrapper pop = serializerContext.serializeQueue.removeFirst();

                String jsonPointer = pop.getJsonPointer();
                if (serializerContext.serializedPointers.contains(jsonPointer)) {
                    // already serialized, avoid doubles
                    continue;
                }

                if (serializerContext.firstEntity) {
                    gen.writeStartObject();
                    serializerContext.firstEntity = false;
                }
                serializerContext.serializedPointers.add(jsonPointer);
                gen.writeFieldName(jsonPointer);
                serializerContext.serializingPageModelEntity = pop.getObject();
                gen.writeObject(pop.getObject());
                serializerContext.serializingPageModelEntity = null;

            }

        }

    }


    private void serializeBeanReference(final JsonGenerator gen,
                                        final String jsonPointerId) throws IOException {
        gen.writeStartObject();
        gen.writeStringField(CONTENT_JSON_POINTER_REFERENCE_PROP,
                "/page/" + jsonPointerId);
        gen.writeEndObject();
    }


    /**
     * JSON Pointer Reference Property name.
     */
    private static final String CONTENT_JSON_POINTER_REFERENCE_PROP = "$ref";


    private static class JsonPointerWrapper {

        private final Object object;
        private final String jsonPointer;

        public JsonPointerWrapper(final Object object, final String jsonPointer) {

            this.object = object;
            this.jsonPointer = jsonPointer;
        }

        @JsonIgnore
        public Object getObject() {
            return object;
        }

        @JsonIgnore
        public String getJsonPointer() {
            return jsonPointer;
        }
    }

    @JsonPropertyOrder({"id", "type", "links", "meta", "data"})
    private class DecoratedPageModelEntityWrapper<T> extends IdentifiableLinkableMetadataBaseModel {

        private final T data;
        private final String type;
        // in case of a HippoDocumentBean serialization, keeps track of the depth : if deeper than max depth, we should
        // not serialize a hippo document
        private int currentDepth;
        private boolean serialized;

        public DecoratedPageModelEntityWrapper(final T data, final String type, final int currentDepth) {
            super(null);
            this.data = data;
            this.type = type;
            this.currentDepth = currentDepth;
        }

        public T getData() {
            return data;
        }

        @JsonInclude(NON_NULL)
        public String getType() {
            return type;
        }

        @JsonIgnore
        public boolean isSerialized() {
            return serialized;
        }

        public void setSerialized(final boolean serialized) {
            this.serialized = serialized;
        }

        @JsonIgnore
        public int getCurrentDepth() {
            return currentDepth;
        }
    }


    /**
     * Add links to content bean model.
     *
     * @param contentBeanModel content bean model
     */
    private void addLinksToContent(final HstRequestContext requestContext, final DecoratedPageModelEntityWrapper<HippoBean> contentBeanModel) {
        final HippoBean bean = contentBeanModel.getData();

        final HstLink selfLink = requestContext.getHstLinkCreator().create(bean.getNode(), requestContext);
        if (selfLink == null) {
            return;
        }

        contentBeanModel.putLink(LINK_NAME_SITE, LinkModel.convert(selfLink, requestContext));

    }

    private String getHippoBeanType(final HippoBean bean) {
        if (bean instanceof HippoGalleryImageSet) {
            return "imageset";
        }
        if (bean instanceof HippoAssetBean) {
            return "asset";
        }
        if (bean instanceof HippoFolderBean) {
            return "folder";
        }
        return "document";
    }

}