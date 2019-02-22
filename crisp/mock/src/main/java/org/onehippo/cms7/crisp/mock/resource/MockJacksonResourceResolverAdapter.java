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
package org.onehippo.cms7.crisp.mock.resource;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceBeanMapper;
import org.onehippo.cms7.crisp.api.resource.ResourceException;
import org.onehippo.cms7.crisp.core.resource.jackson.JacksonResource;
import org.onehippo.cms7.crisp.core.resource.jackson.JacksonResourceBeanMapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Adapter class for JSON / Jackson based resource data, to be used in unit tests by mocking.
 */
public class MockJacksonResourceResolverAdapter extends AbstractMockResourceResolverAdapter {

    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper();

    private ObjectMapper objectMapper;

    @Override
    public ResourceBeanMapper getResourceBeanMapper() throws ResourceException {
        ResourceBeanMapper mapper = super.getResourceBeanMapper();

        if (mapper != null) {
            return mapper;
        }

        return new JacksonResourceBeanMapper(getObjectMapper());
    }

    public ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            return DEFAULT_OBJECT_MAPPER;
        }

        return objectMapper;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected Resource inputToResource(final InputStream inputStream) throws IOException {
        final JsonNode jsonNode = getObjectMapper().readTree(IOUtils.toString(inputStream, "UTF-8"));
        return new JacksonResource(jsonNode);
    }

}
