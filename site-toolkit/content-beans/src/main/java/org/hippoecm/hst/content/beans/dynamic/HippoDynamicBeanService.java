/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.dynamic;

import java.util.Set;

import javax.jcr.RepositoryException;

import org.hippoecm.hst.content.beans.builder.AbstractBeanBuilderService;
import org.hippoecm.hst.content.beans.builder.BeanBuilderServiceParameters;
import org.hippoecm.hst.content.beans.builder.HippoContentBean;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.util.ObjectConverterUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.cms7.services.contenttype.ContentTypeService;
import org.onehippo.cms7.services.contenttype.ContentTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class HippoDynamicBeanService extends AbstractBeanBuilderService implements DynamicBeanService {
    private static final Logger log = LoggerFactory.getLogger(HippoDynamicBeanService.class);
    private static final String CONTENT_TYPES_VERSION_CACHE_KEY = "CONTENT_TYPES_VERSION_CACHE_KEY";
    private static final String HIPPO_DOCUMENT_CLASS_PATH = "org.hippoecm.hst.content.beans.standard.HippoDocument";
    private static final String HIPPO_COMPOUND_CLASS_PATH = "org.hippoecm.hst.content.beans.standard.HippoCompound";

    private ObjectConverter objectConverter;
    private final Cache<String, Long> contentTypesVersionCache = CacheBuilder.newBuilder().build();

    class DynamicBeanBuilderServiceParameters implements BeanBuilderServiceParameters {
        private DynamicBeanBuilder builder;

        public DynamicBeanBuilderServiceParameters(final DynamicBeanBuilder builder) {
            this.builder = builder;
        }

        public DynamicBeanBuilder getBeanlessBeanBuilder() {
            return builder;
        }

        public void setBeanlessBeanBuilder(DynamicBeanBuilder builder) {
            this.builder = builder;
        }
    }

    public void setObjectConverter(ObjectConverter objectConverter) {
        this.objectConverter = objectConverter;
    }

    @Override
    public void invalidateDynamicBeans() {
        final ContentTypeService contentTypeService = HippoServiceRegistry.getService(ContentTypeService.class);
        try {
            final ContentTypes contentTypes = contentTypeService.getContentTypes();
            final Long currentContentTypesVersion = contentTypesVersionCache
                    .getIfPresent(CONTENT_TYPES_VERSION_CACHE_KEY);

            if (currentContentTypesVersion != null && contentTypes.version() == currentContentTypesVersion) {
                // if there isn't any change in version number, no need to invalidate document types
                return;
            }

            synchronized (contentTypes) {
                if (currentContentTypesVersion == null) {
                    // if there isn't any record in the contentTypesVersionCache that means
                    // generating the beanless beans will be handled in ObjectCoverterImpl
                    contentTypesVersionCache.put(CONTENT_TYPES_VERSION_CACHE_KEY, 1L);
                    refreshDynamicBeans(contentTypes);
                } else if (contentTypes.version() > currentContentTypesVersion) {
                    refreshDynamicBeans(contentTypes);
                }
            }
        } catch (RepositoryException e) {
            log.error("Error on contentTypes : {}, {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unknown error on ContentTypeService : {}, {}", e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends HippoBean> createDynamicCompoundBean(String namespace, ContentType contentType) {
        final HippoContentBean contentBean = new HippoContentBean(namespace, contentType);

        final Set<String> superTypes = contentBean.getSuperTypes();
        if (superTypes.size() != 1 || !superTypes.iterator().next().equals(HippoNodeType.NT_COMPOUND)) {
            return null;
        }

        Class<? extends HippoBean> parentBean = null;
        try {
            parentBean = (Class<? extends HippoBean>) Class.forName(HIPPO_COMPOUND_CLASS_PATH);
        } catch (ClassNotFoundException e) {
            log.error("Problem while creating hippo compound bean.");
        }

        return generateBean(parentBean, contentBean);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends HippoBean> createDynamicDocumentBean(Class<? extends HippoBean> parentBean, String namespace, ContentType contentType) {
        final HippoContentBean contentBean = new HippoContentBean(namespace, contentType);

        if (parentBean == null) {
            try {
                parentBean = (Class<? extends HippoBean>) Class.forName(HIPPO_DOCUMENT_CLASS_PATH);
            } catch (ClassNotFoundException e) {
                log.error("Problem while creating hippo document bean.");
            }
        }

        return generateBean(parentBean, contentBean);
    }

    /**
     * Removes bean definitions from {@link ObjectConverter}.
     */
    private void refreshDynamicBeans(final ContentTypes contentTypes) {
        log.info("Invalidating document types for version number {}", contentTypes.version());
        contentTypesVersionCache.put(CONTENT_TYPES_VERSION_CACHE_KEY, contentTypes.version());

        contentTypes.getTypesByPrefix().entrySet().stream().forEach(contentTypeSet -> {
            contentTypeSet.getValue()
                .stream()
                .filter(ContentType::isCompoundType)
                .forEach(contentType -> {
                    String namespace = contentTypeSet.getKey();
                    // TODO exclude internal compound types
                    ObjectConverterUtils.invalidateDynamicBean(contentTypeSet.getKey(), contentType, objectConverter);
                    createDynamicCompoundBean(namespace, contentType);
                });
            contentTypeSet.getValue()
                .stream()
                .filter(ContentType::isDocumentType)
                .forEach(contentType -> {
                    // TODO exclude internal document types
                    ObjectConverterUtils.invalidateDynamicBean(contentTypeSet.getKey(), contentType, objectConverter);
                });
        });
    }

    private Class<? extends HippoBean> generateBean(Class<? extends HippoBean> parentBean, HippoContentBean hippoContentBean) {
        final DynamicBeanBuilder builder = new DynamicBeanBuilder(DynamicBeanUtils.createJavaClassName(hippoContentBean.getName()),
                parentBean);
        final BeanBuilderServiceParameters builderParameters = new DynamicBeanBuilderServiceParameters(builder);
        super.generateMethodsByProperties(hippoContentBean, builderParameters);
        super.generateMethodsByNodes(hippoContentBean, builderParameters);
        return builder.create();
    }

    @Override
    public boolean hasChange(String name, boolean multiple, BeanBuilderServiceParameters builderParameters) {
        return true;
    }

    @Override
    public void addBeanMethodString(String name, boolean multiple, BeanBuilderServiceParameters builderParameters) {
        final DynamicBeanBuilderServiceParameters parameters = (DynamicBeanBuilderServiceParameters) builderParameters;
        final String methodName = DynamicBeanUtils.createMethodName(name);
        parameters.getBeanlessBeanBuilder().addBeanMethodString(methodName, name, multiple);
    }

    @Override
    public void addBeanMethodCalendar(String name, boolean multiple, BeanBuilderServiceParameters builderParameters) {
        final DynamicBeanBuilderServiceParameters parameters = (DynamicBeanBuilderServiceParameters) builderParameters;
        final String methodName = DynamicBeanUtils.createMethodName(name);
        parameters.getBeanlessBeanBuilder().addBeanMethodCalendar(methodName, name, multiple);
    }

    @Override
    public void addBeanMethodBoolean(String name, boolean multiple, BeanBuilderServiceParameters builderParameters) {
        final DynamicBeanBuilderServiceParameters parameters = (DynamicBeanBuilderServiceParameters) builderParameters;
        final String methodName = DynamicBeanUtils.createMethodName(name);
        parameters.getBeanlessBeanBuilder().addBeanMethodBoolean(methodName, name, multiple);
    }

    @Override
    public void addBeanMethodLong(String name, boolean multiple, BeanBuilderServiceParameters builderParameters) {
        final DynamicBeanBuilderServiceParameters parameters = (DynamicBeanBuilderServiceParameters) builderParameters;
        final String methodName = DynamicBeanUtils.createMethodName(name);
        parameters.getBeanlessBeanBuilder().addBeanMethodLong(methodName, name, multiple);
    }

    @Override
    public void addBeanMethodDouble(String name, boolean multiple, BeanBuilderServiceParameters builderParameters) {
        final DynamicBeanBuilderServiceParameters parameters = (DynamicBeanBuilderServiceParameters) builderParameters;
        final String methodName = DynamicBeanUtils.createMethodName(name);
        parameters.getBeanlessBeanBuilder().addBeanMethodDouble(methodName, name, multiple);
    }

    @Override
    public void addBeanMethodDocbase(String name, boolean multiple, BeanBuilderServiceParameters builderParameters) {
        final DynamicBeanBuilderServiceParameters parameters = (DynamicBeanBuilderServiceParameters) builderParameters;
        final String methodName = DynamicBeanUtils.createMethodName(name);
        parameters.getBeanlessBeanBuilder().addBeanMethodDocbase(methodName, name, multiple);
    }

    @Override
    public void addDefaultPropertyType(String name, String type, BeanBuilderServiceParameters builderParameters) {
        log.warn("Failed to create getter for property: {} of type: {}", name, type);
    }

    @Override
    public void addBeanMethodHippoHtml(String name, boolean multiple, BeanBuilderServiceParameters builderParameters) {
        final DynamicBeanBuilderServiceParameters parameters = (DynamicBeanBuilderServiceParameters) builderParameters;
        final String methodName = DynamicBeanUtils.createMethodName(name);
        parameters.getBeanlessBeanBuilder().addBeanMethodHippoHtml(methodName, name, multiple);
    }

    @Override
    public void addBeanMethodImageLink(String name, boolean multiple, BeanBuilderServiceParameters builderParameters) {
        final DynamicBeanBuilderServiceParameters parameters = (DynamicBeanBuilderServiceParameters) builderParameters;
        final String methodName = DynamicBeanUtils.createMethodName(name);
        parameters.getBeanlessBeanBuilder().addBeanMethodImageLink(methodName, name, multiple);
    }

    @Override
    public void addBeanMethodHippoMirror(String name, boolean multiple, BeanBuilderServiceParameters builderParameters) {
        final DynamicBeanBuilderServiceParameters parameters = (DynamicBeanBuilderServiceParameters) builderParameters;
        final String methodName = DynamicBeanUtils.createMethodName(name);
        parameters.getBeanlessBeanBuilder().addBeanMethodHippoMirror(methodName, name, multiple);
    }

    @Override
    public void addBeanMethodHippoImage(String name, boolean multiple, BeanBuilderServiceParameters builderParameters) {
        final DynamicBeanBuilderServiceParameters parameters = (DynamicBeanBuilderServiceParameters) builderParameters;
        final String methodName = DynamicBeanUtils.createMethodName(name);
        parameters.getBeanlessBeanBuilder().addBeanMethodHippoImage(methodName, name, multiple);
    }

    @Override
    public void addBeanMethodHippoResource(String name, boolean multiple, BeanBuilderServiceParameters builderParameters) {
        final DynamicBeanBuilderServiceParameters parameters = (DynamicBeanBuilderServiceParameters) builderParameters;
        final String methodName = DynamicBeanUtils.createMethodName(name);
        parameters.getBeanlessBeanBuilder().addBeanMethodHippoResource(methodName, name, multiple);
    }

    @Override
    public void addDefaultNodeType(String name, boolean multiple, String prefix, String type, BeanBuilderServiceParameters builderParameters) {
        DynamicBeanBuilderServiceParameters parameters = (DynamicBeanBuilderServiceParameters) builderParameters;
        final String className = DynamicBeanUtils.createClassName(type);
        final String methodName = DynamicBeanUtils.createMethodName(name);
        parameters.getBeanlessBeanBuilder().addBeanMethodInternalType(className, methodName, name, multiple);
    }

}
