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

import java.util.List;

import org.hippoecm.hst.content.beans.standard.HippoBean;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanSerializerBuilder;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.impl.ObjectIdWriter;
import com.fasterxml.jackson.databind.ser.std.StringSerializer;

class HippoBeanModelsSerializerModifier extends BeanSerializerModifier {

    private final List<MetadataDecorator> metadataDecorators;

    public HippoBeanModelsSerializerModifier(final List<MetadataDecorator> metadataDecorators) {
        this.metadataDecorators = metadataDecorators;
    }

    @SuppressWarnings("unchecked")
    @Override
    public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc,
            JsonSerializer<?> serializer) {
        final Class<?> beanClazz = beanDesc.getBeanClass();

        if (beanClazz != null) {
            // Customize JsonSerializer for HippoBean type as we need to accumulate HippoBeans and add JSON Pointer
            // references in the first phase, and later serialize the HippoBeans in content section in the end.
            if (HippoBean.class.isAssignableFrom(beanClazz)) {
                return new HippoBeanSerializer((JsonSerializer<Object>) serializer, metadataDecorators);
            }
        }

        return serializer;
    }

    @Override
    public BeanSerializerBuilder updateBuilder(SerializationConfig config, BeanDescription beanDesc,
            BeanSerializerBuilder builder) {
        final Class<?> beanClazz = beanDesc.getBeanClass();

        if (beanClazz != null) {
            // Customize BeanSerializerBuilder in order to set the Serializer of the ObjectIdWriter
            // as there seems to be a bug(?) in Jackson serialization when (a) there's a custom JsonSerializer for a type
            // and (b) there's an ObjectIdGenerator annotation for the type to avoid circular reference errors.
            if (HippoBean.class.isAssignableFrom(beanClazz)) {
                final ObjectIdWriter objectIdWriter = ObjectIdWriter
                        .construct(beanDesc.getType(), PropertyName.construct("id"), new HippoBeanIdGenerator(), false)
                        .withSerializer(new StringSerializer());
                builder.setObjectIdWriter(objectIdWriter);
            }
        }

        return builder;
    }

    private static class HippoBeanIdGenerator extends ObjectIdGenerator<String> {

        private static final long serialVersionUID = 1L;

        private final Class<?> scope = HippoBean.class;

        @Override
        public final Class<?> getScope() {
            return scope;
        }

        @Override
        public String generateId(Object forPojo) {
            return ((HippoBean) forPojo).getRepresentationId();
        }

        @Override
        public ObjectIdGenerator<String> forScope(Class<?> scope) {
            return this;
        }

        @Override
        public ObjectIdGenerator<String> newForSerialization(Object context) {
            return this;
        }

        @Override
        public IdKey key(Object key) {
            if (key == null) {
                return null;
            }

            return new IdKey(getClass(), scope, key);
        }

        @Override
        public boolean canUseFor(ObjectIdGenerator<?> gen) {
            return (gen instanceof HippoBeanIdGenerator);
        }
    }
}
