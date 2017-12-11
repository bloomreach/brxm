/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.core.resource.jackson;

import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceBeanMapper;
import org.onehippo.cms7.crisp.api.resource.ResourceException;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Mapper to convert a {@link JacksonResource} object to a bean.
 */
public class JacksonResourceBeanMapper implements ResourceBeanMapper {

    private final ObjectMapper objectMapper;

    public JacksonResourceBeanMapper(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> T map(Resource resource, Class<T> beanType) throws ResourceException {
        if (!(resource instanceof JacksonResource)) {
            throw new ResourceException("Cannot convert resource because it's not a JacksonResource.");
        }

        return objectMapper.convertValue(((JacksonResource) resource).getJsonNode(), beanType);
    }

}
