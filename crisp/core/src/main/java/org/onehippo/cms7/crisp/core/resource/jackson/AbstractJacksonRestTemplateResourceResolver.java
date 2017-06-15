/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package org.onehippo.cms7.crisp.core.resource.jackson;

import org.onehippo.cms7.crisp.core.resource.AbstractRestTemplateResourceResolver;

import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class AbstractJacksonRestTemplateResourceResolver extends AbstractRestTemplateResourceResolver {

    private ObjectMapper objectMapper;

    public AbstractJacksonRestTemplateResourceResolver() {
        super();
    }

    public ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
        }

        return objectMapper;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
}
