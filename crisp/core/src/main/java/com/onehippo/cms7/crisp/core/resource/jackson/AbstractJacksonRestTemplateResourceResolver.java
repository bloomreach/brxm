/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package com.onehippo.cms7.crisp.core.resource.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onehippo.cms7.crisp.core.resource.AbstractRestTemplateResourceResolver;

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
