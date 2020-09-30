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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.hippoecm.hst.component.pagination.Page;
import org.hippoecm.hst.component.pagination.Pagination;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.PageModelEntity;
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
import org.hippoecm.hst.pagemodelapi.v10.content.beans.jackson.LinkModel.LinkType;
import org.hippoecm.hst.pagemodelapi.v10.core.model.ComponentWindowModel;
import org.hippoecm.hst.pagemodelapi.v10.core.model.IdentifiableLinkableMetadataBaseModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ResolvableSerializer;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static org.hippoecm.hst.core.container.ContainerConstants.LINK_NAME_SELF;
import static org.hippoecm.hst.core.container.ContainerConstants.LINK_NAME_SITE;

public class PageModelSerializer extends JsonSerializer<Object> implements ResolvableSerializer {

    static ThreadLocal<SerializerContext> tlSerializerContext = new ThreadLocal<>();

    private static Logger log = LoggerFactory.getLogger(PageModelSerializer.class);

    private static final String PAGINATION_QUERY_PAGE_PARAM = "%s:page";
    private static final String PAGINATION_QUERY_LIMIT_PARAM = "%s:limit";

    static class SerializerContext {
        private boolean firstEntity = true;
        final ArrayDeque<JsonPointerWrapper> serializeQueue = new ArrayDeque<>();
        // the current page model entity that is being serialized
        private Object serializingPageModelEntity;
        // hash set to keep track of already serialized objects (same pointers)
        private final Set<String> serializedPointers = new HashSet<>();
        final Set<Object> handledPmaEntities = new HashSet<>();
        private final Map<Object, String> objectJsonPointerMap = new HashMap<>();
        private final int maxDocumentRefLevel;

        public SerializerContext(final int maxDocumentRefLevel) {
            this.maxDocumentRefLevel = maxDocumentRefLevel;
        }
    }

    static void initContext(final int maxDocumentRefLevel) {
        tlSerializerContext.set(new SerializerContext(maxDocumentRefLevel));
    }

    static void closeContext() {
        tlSerializerContext.remove();
    }

    private final JsonSerializer<Object> delegatee;
    private final JsonPointerFactory jsonPointerFactory;
    private final List<MetadataDecorator> metadataDecorators;

    public PageModelSerializer(final JsonSerializer<Object> delegatee, final JsonPointerFactory jsonPointerFactory,
                               final List<MetadataDecorator> metadataDecorators) {
        this.delegatee = delegatee;
        this.jsonPointerFactory = jsonPointerFactory;
        this.metadataDecorators = metadataDecorators;
    }

    @Override
    public void resolve(final SerializerProvider provider) throws JsonMappingException {
        if (delegatee instanceof ResolvableSerializer) {
            ((ResolvableSerializer) delegatee).resolve(provider);
        }
    }

    @Override
    public void serialize(final Object object, final JsonGenerator gen, final SerializerProvider serializerProvider) throws IOException {
        final SerializerContext serializerContext = getSerializerContext();

        if (object instanceof AggregatedPageModel.RootReference) {
            serializeRootRef((AggregatedPageModel.RootReference) object, gen, serializerContext);
        }  else {
            final Optional<DecoratedPageModelEntityWrapper<?>> nonSerializedWrappedEntity = getNonSerializedWrappedEntity(object, serializerContext);

            if (!(object instanceof PageModelEntity)
                    || serializerContext.serializingPageModelEntity == object
                    || nonSerializedWrappedEntity.isPresent()) {
                // If it's a DecoratedPageModelEntityWrapper for the current object then set it to serialized
                nonSerializedWrappedEntity.ifPresent(e -> e.setSerialized(true));
                // Not a PMA entity
                // or entity == object
                // or already wrapped in DecoratedPageModelEntityWrapper
                // , so no delegate to normal serialization (since if needed, flattening already done)
                delegatee.serialize(object, gen, serializerProvider);
            } else if (serializerContext.serializingPageModelEntity != null) {
                serializePageModelEntityIfNeeded(object, gen, serializerContext);
            } else {
                serializeQueuedObjects(gen, serializerContext);
            }
        }
    }

    private Optional<DecoratedPageModelEntityWrapper<?>> getNonSerializedWrappedEntity(Object object, SerializerContext serializerContext) {
        if (serializerContext.serializingPageModelEntity instanceof DecoratedPageModelEntityWrapper) {
            final DecoratedPageModelEntityWrapper<?> wrapper = (DecoratedPageModelEntityWrapper<?>) serializerContext.serializingPageModelEntity;
            if (wrapper.getData() == object && !wrapper.isSerialized()) {
                return Optional.of(wrapper);
            }
        }
        return Optional.empty();
    }

    private PageModelSerializer.SerializerContext getSerializerContext() {
        final SerializerContext serializerContext = tlSerializerContext.get();
        if (serializerContext == null) {
            throw new IllegalStateException("PageModelSerializer not in the correct state for current thread, first " +
                    "invoke #initContext to setup the required thread local");
        }
        return serializerContext;
    }

    private void serializeRootRef(final AggregatedPageModel.RootReference ref, final JsonGenerator gen, final SerializerContext serializerContext) throws IOException {
        final String jsonPointerId = jsonPointerFactory.createJsonPointerId(ref.getObject());
        serializeBeanReference(gen, jsonPointerId);

        if (ref.getObject() instanceof HippoBean) {
            HippoBean hippoBean = (HippoBean) ref.getObject();

            DecoratedPageModelEntityWrapper<HippoBean> wrapper = wrapHippoBean(0, hippoBean, metadataDecorators);

            //DecoratedPageModelEntityWrapper wrapper = new DecoratedPageModelEntityWrapper(hippoBean, getHippoBeanType(hippoBean), 0);

            final JsonPointerWrapper value = new JsonPointerWrapper(wrapper, jsonPointerId);
            serializerContext.serializeQueue.add(value);
        } else {
            final JsonPointerWrapper value = new JsonPointerWrapper(ref.getObject(), jsonPointerId);
            serializerContext.serializeQueue.add(value);
        }
    }

    private void serializeQueuedObjects(final JsonGenerator gen, final SerializerContext serializerContext) throws IOException {
        while (!serializerContext.serializeQueue.isEmpty()) {
            final JsonPointerWrapper pop = serializerContext.serializeQueue.removeFirst();
            final String jsonPointer = pop.getJsonPointer();
            if (!serializerContext.serializedPointers.contains(jsonPointer)) {
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

    private void serializePageModelEntityIfNeeded(final Object object, final JsonGenerator gen, final SerializerContext serializerContext) throws IOException {
        int nextDepth = serializePageModelEntity(object, gen, serializerContext);
        if (nextDepth >= 0) {
            // make sure same object returns same jsonPointerId : handy if the same model object is used multiple
            // times in the PMA
            final String jsonPointerId = serializerContext.objectJsonPointerMap
                    .computeIfAbsent(object, obj -> jsonPointerFactory.createJsonPointerId(object));

            serializeBeanReference(gen, jsonPointerId);

            if (!serializerContext.handledPmaEntities.contains(object)) {
                serializerContext.handledPmaEntities.add(object);
                final JsonPointerWrapper jsonPointerWrapper = getJsonPointerWrapper(object, nextDepth, jsonPointerId, serializerContext);
                serializerContext.serializeQueue.add(jsonPointerWrapper);
            }
        }
    }

    private int serializePageModelEntity(final Object object, final JsonGenerator gen, final SerializerContext serializerContext) throws IOException {
        // we are currently serializing a page model entity and during that serialization, we now encounter a
        // new nested page model entity which must be postponed and therefore added to serializeQueue unless it
        // is a document at a deeper than max level depth
        final Object pageModelEntity = serializerContext.serializingPageModelEntity;
        int nextDepth = 0;
        if (pageModelEntity instanceof DecoratedPageModelEntityWrapper) {
            nextDepth = ((DecoratedPageModelEntityWrapper<?>) pageModelEntity).getCurrentDepth();
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
                } else {
                    gen.writeStartObject();
                    // just serialize only the ID of the document bean and return : the bean does not get
                    // serialized since deeper than max reference
                    gen.writeStringField("uuid", ((HippoBean) object).getRepresentationId());
                    gen.writeEndObject();
                }
                return -1;
            }
        }
        return nextDepth;
    }

    private PageModelSerializer.JsonPointerWrapper getJsonPointerWrapper(final Object object, final int nextDepth,
            final String jsonPointerId, final SerializerContext serializerContext) {
        if (object instanceof CommonMenu) {
            final DecoratedPageModelEntityWrapper<CommonMenu> decoratedPageModelEntityWrapper = wrapCommonMenu(nextDepth, (CommonMenu) object);
            return new JsonPointerWrapper(decoratedPageModelEntityWrapper, jsonPointerId);
        }
        if (object instanceof HippoDocumentBean || object instanceof HippoFolderBean) {
            final DecoratedPageModelEntityWrapper<HippoBean> decoratedPageModelEntityWrapper = wrapHippoBean(nextDepth, (HippoBean) object, metadataDecorators);
            return new JsonPointerWrapper(decoratedPageModelEntityWrapper, jsonPointerId);
        }
        if (object instanceof Pagination) {
            final DecoratedPaginationEntityWrapper paginationEntityWrapper = new DecoratedPaginationEntityWrapper((Pagination<HippoBean>) object, serializerContext);
            return new JsonPointerWrapper(paginationEntityWrapper, jsonPointerId);
        }
        // no extra wrapping needed other than json pointer inclusion
        return new JsonPointerWrapper(object, jsonPointerId);
    }

    static PageModelSerializer.DecoratedPageModelEntityWrapper<HippoBean> wrapHippoBean(final int nextDepth, final HippoBean hippoBean,
                                                                                        final List<MetadataDecorator> metadataDecorators) {
        final HstRequestContext requestContext = RequestContextProvider.get();
        final DecoratedPageModelEntityWrapper<HippoBean> wrapper
                = new DecoratedPageModelEntityWrapper<>(hippoBean, getHippoBeanType(hippoBean), nextDepth);
        if (hippoBean instanceof HippoAssetBean || hippoBean instanceof HippoGalleryImageSet) {
            log.trace("Skip adding links to hippo asset or gallery document since only links to the " +
                    "resources in them are useful");
        } else {
            log.trace("Add links to document or folder bean");
            addLinksToContent(requestContext, wrapper);
        }

        for (MetadataDecorator metadataDecorator : metadataDecorators) {
            log.trace("Decorate '{}' with metadataDecorator '{}'", hippoBean.getPath(), metadataDecorator);
            metadataDecorator.decorateContentMetadata(requestContext,
                    wrapper.getData(), wrapper);
        }
        return wrapper;
    }

    private PageModelSerializer.DecoratedPageModelEntityWrapper<CommonMenu> wrapCommonMenu(final int nextDepth, final CommonMenu commonMenu) {
        final HstRequestContext requestContext = RequestContextProvider.get();
        final DecoratedPageModelEntityWrapper<CommonMenu> wrapper
                = new DecoratedPageModelEntityWrapper<>(commonMenu, "menu", nextDepth);
        for (MetadataDecorator metadataDecorator : metadataDecorators) {
            log.trace("Decorate menu '{}' with metadataDecorator '{}'", commonMenu.getName(), metadataDecorator);
            metadataDecorator.decorateCommonMenuMetadata(requestContext,
                    wrapper.getData(), wrapper);
        }
        return wrapper;
    }

    @JsonPropertyOrder({ "type","offset", "items", "total", "first", "previous", "current", "next", "last", "pages", "size", "enabled"})
    private static class DecoratedPaginationEntityWrapper {

        private final static String PAGINATION_TYPE = "pagination";

        private final Pagination<HippoBean> pagination;
        private final String componentNamespace;
        private final String siteLink;
        private final String selfLink;

        public DecoratedPaginationEntityWrapper(final Pagination<HippoBean> pagination, final SerializerContext serializerContext) {
            this.pagination = pagination;
            this.componentNamespace = ((ComponentWindowModel) serializerContext.serializingPageModelEntity).getId();

            final AggregatedPageModel aggregatedPageModel = PageModelAggregationValve.getCurrentAggregatedPageModel();
            this.siteLink = aggregatedPageModel.getLink(LINK_NAME_SITE).getHref();
            this.selfLink = aggregatedPageModel.getLink(LINK_NAME_SELF).getHref();
        }

        private DecoratedPageEntityWrapper decoratePage(final Page page) {
            if (page == null) {
                return null;
            }

            final DecoratedPageEntityWrapper pageEntityWrapper = new DecoratedPageEntityWrapper(page);
            pageEntityWrapper.putLink(LINK_NAME_SITE,new LinkModel(addOrReplaceQueryParams(siteLink, page.getNumber()), LinkType.INTERNAL));
            pageEntityWrapper.putLink(LINK_NAME_SELF,new LinkModel(addOrReplaceQueryParams(selfLink, page.getNumber()), LinkType.EXTERNAL));

            return pageEntityWrapper;
        }

        /**
         * Adds or replaces the query parameters
         * 
         * @param uri external or internal uri path
         * @param pageNumber number of the pagination page
         * @return
         */
        private String addOrReplaceQueryParams(final String uri, final int pageNumber) {
            final UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(uri);
            uriBuilder.replaceQueryParam(String.format(PAGINATION_QUERY_PAGE_PARAM, componentNamespace), pageNumber);
            uriBuilder.replaceQueryParam(String.format(PAGINATION_QUERY_LIMIT_PARAM, componentNamespace), pagination.getLimit());
            return uriBuilder.build().toUriString();
        }

        @JsonProperty
        public String getType() {
            return PAGINATION_TYPE;
        }

        @JsonProperty
        public List<HippoBean> getItems() {
            return pagination.getItems();
        }

        @JsonProperty
        public DecoratedPageEntityWrapper getFirst() {
            return decoratePage(pagination.getFirst());
        }

        @JsonProperty
        public DecoratedPageEntityWrapper getPrevious() {
            return decoratePage(pagination.getPrevious());
        }

        @JsonProperty
        public DecoratedPageEntityWrapper getCurrent() {
            return decoratePage(pagination.getCurrent());
        }

        @JsonProperty
        public DecoratedPageEntityWrapper getNext() {
            return decoratePage(pagination.getNext());
        }

        @JsonProperty
        public DecoratedPageEntityWrapper getLast() {
            return decoratePage(pagination.getLast());
        }

        @JsonProperty
        public int getSize() {
            return pagination.getSize();
        }

        @JsonProperty
        public long getTotal() {
            return pagination.getTotal();
        }

        @JsonProperty
        public boolean isEnabled() {
            return pagination.isEnabled();
        }

        @JsonProperty
        public int getOffset() {
            return pagination.getOffset();
        }

        @JsonProperty
        public List<DecoratedPageEntityWrapper> getPages() {
            return pagination.getPages().stream().map(this::decoratePage).collect(Collectors.toList());
        }
    }

    @JsonPropertyOrder({ "number", "links" })
    private static class DecoratedPageEntityWrapper extends IdentifiableLinkableMetadataBaseModel {

        private final Page page;

        public DecoratedPageEntityWrapper(final Page page) {
            super(null);
            this.page = page;
        }

        @JsonProperty("number")
        public int getNumber() {
            return page.getNumber();
        }

        @JsonIgnore
        @Override
        public Map<String, Object> getMetadataMap() {
            return null;
        }
    }


    private void serializeBeanReference(final JsonGenerator gen,
                                        final String jsonPointerId) throws IOException {
        // JSON Pointer Reference Property name.
        final String ref = "$ref";
        gen.writeStartObject();
        gen.writeStringField(ref,
                "/page/" + jsonPointerId);
        gen.writeEndObject();
    }


    static class JsonPointerWrapper {

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
    static class DecoratedPageModelEntityWrapper<T> extends IdentifiableLinkableMetadataBaseModel {

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
    private static void addLinksToContent(final HstRequestContext requestContext, final DecoratedPageModelEntityWrapper<HippoBean> contentBeanModel) {
        final HippoBean bean = contentBeanModel.getData();

        final HstLink selfLink = requestContext.getHstLinkCreator().create(bean.getNode(), requestContext);
        if (selfLink == null) {
            return;
        }

        contentBeanModel.putLink(LINK_NAME_SITE, LinkModel.convert(selfLink, requestContext));

    }

    private static String getHippoBeanType(final HippoBean bean) {
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
