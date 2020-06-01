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

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

import org.hippoecm.hst.content.PageModelEntity;
import org.hippoecm.hst.core.pagemodel.container.MetadataDecorator;

class PageModelSerializerModifier extends BeanSerializerModifier {

    private final List<MetadataDecorator> metadataDecorators;
    private JsonPointerFactory jsonPointerFactory;

    public PageModelSerializerModifier(final List<MetadataDecorator> metadataDecorators,
                                       final JsonPointerFactory jsonPointerFactory) {
        this.metadataDecorators = metadataDecorators;
        this.jsonPointerFactory = jsonPointerFactory;
    }

    @SuppressWarnings("unchecked")
    @Override
    public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc,
                                              JsonSerializer<?> serializer) {

        final Class<?> beanClazz = beanDesc.getBeanClass();
        if (PageModelEntity.class.isAssignableFrom(beanClazz) || AggregatedPageModel.RootReference.class.isAssignableFrom(beanClazz)) {
            return new PageModelSerializer((JsonSerializer<Object>) serializer, jsonPointerFactory, metadataDecorators);
        }
        return serializer;
    }

}
