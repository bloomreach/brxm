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

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.configuration.components.DynamicParameter;
import org.hippoecm.hst.configuration.components.DynamicParameterConfig;
import org.hippoecm.hst.configuration.components.JcrPathParameterConfig;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.standard.HippoAssetBean;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageSet;
import org.hippoecm.hst.core.component.HstParameterValueConversionException;
import org.hippoecm.hst.core.pagemodel.container.MetadataDecorator;
import org.hippoecm.hst.core.parameters.DefaultHstParameterValueConverter;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ParameterConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PageModelApiParameterValueConvertor extends DefaultHstParameterValueConverter {

    private final static Logger log = LoggerFactory.getLogger(DefaultHstParameterValueConverter.class);
    private JsonPointerFactory jsonPointerFactory;
    private final List<MetadataDecorator> metadataDecorators;

    public PageModelApiParameterValueConvertor(final JsonPointerFactory jsonPointerFactory, final List<MetadataDecorator> metadataDecorators) {

        this.jsonPointerFactory = jsonPointerFactory;
        this.metadataDecorators = metadataDecorators;
    }

    @Override
    public Object convert(final String parameterName, final String parameterValue,
                          ParameterConfiguration parameterConfiguration, Class<?> returnType) throws HstParameterValueConversionException {

        if (StringUtils.isBlank(parameterValue)) {
            return convert(parameterValue, returnType);
        }

        if (parameterConfiguration instanceof ComponentConfiguration) {
            final ComponentConfiguration componentConfiguration = (ComponentConfiguration) parameterConfiguration;
            final Optional<DynamicParameter> dynamicParameterOptional = componentConfiguration.getDynamicComponentParameters().stream()
                    .filter(param -> param.getName().equals(parameterName)).findFirst();
            if (dynamicParameterOptional.isPresent()) {
                final DynamicParameter dynamicParameter = dynamicParameterOptional.get();
                if (dynamicParameter.getComponentParameterConfig() != null && dynamicParameter.getComponentParameterConfig().getType() == DynamicParameterConfig.Type.JCR_PATH) {
                    final HstRequestContext requestContext = RequestContextProvider.get();
                    final String absPath;
                    if (((JcrPathParameterConfig) dynamicParameter.getComponentParameterConfig()).isRelative()) {
                        absPath = requestContext.getResolvedMount().getMount().getContentPath() + "/" + parameterValue;
                    } else {
                        absPath = parameterValue;
                    }
                    try {
                        final Object object = requestContext.getObjectBeanManager().getObject(absPath);

                        if (object instanceof HippoDocumentBean || object instanceof HippoAssetBean || object instanceof HippoGalleryImageSet) {
                            final HippoBean bean = (HippoBean) object;

                            final PageModelSerializer.SerializerContext serializerContext = PageModelSerializer.tlSerializerContext.get();
                            final String jsonPointerId = jsonPointerFactory.createJsonPointerId(object);

                            final PageModelSerializer.DecoratedPageModelEntityWrapper<HippoBean> wrapper = PageModelSerializer.wrapHippoBean(0, bean, metadataDecorators);

                            if (!serializerContext.handledPmaEntities.contains(object)) {
                                serializerContext.serializeQueue
                                        .add(new PageModelSerializer.JsonPointerWrapper(wrapper, jsonPointerId));
                            }
                            final String id = "/page/" + jsonPointerId;

                            return id;
                        } else {
                            log.info("Referenced bean is not a HippoDocumentBean, HippoAssetBean or HippoGalleryImageSet, " +
                                    "return original value");
                            return convert(parameterValue, returnType);
                        }
                    } catch (ObjectBeanManagerException e) {
                        log.info("could not create bean for path '{}', return original value", absPath, e);
                        return parameterValue;
                    }
                }
            }
        }

        return convert(parameterValue, returnType);
    }
}
