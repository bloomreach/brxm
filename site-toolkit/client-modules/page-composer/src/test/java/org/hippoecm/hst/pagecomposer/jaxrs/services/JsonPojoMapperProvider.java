/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.hst.pagecomposer.jaxrs.services;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

@Provider
public class JsonPojoMapperProvider implements ContextResolver<ObjectMapper> {
    private final ObjectMapper mapper;

    public JsonPojoMapperProvider() {
        this.mapper = new ObjectMapper();
        final SimpleModule simpleModule = new SimpleModule(MultivaluedMap.class.getName());
        simpleModule.addAbstractTypeMapping(MultivaluedMap.class, MultivaluedHashMap.class);
        mapper.registerModule(simpleModule);
    }

    @Override
    public ObjectMapper getContext(final Class<?> type) {
        return mapper;
    }
}
