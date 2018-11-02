/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

import org.onehippo.cms7.crisp.api.exchange.ExchangeHint;
import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceBeanMapper;
import org.onehippo.cms7.crisp.api.resource.ResourceException;
import org.onehippo.cms7.crisp.core.resource.AbstractRestTemplateResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class AbstractJacksonRestTemplateResourceResolver extends AbstractRestTemplateResourceResolver {

    private static Logger log = LoggerFactory.getLogger(AbstractJacksonRestTemplateResourceResolver.class);

    private ObjectMapper objectMapper;
    private ResourceBeanMapper resourceBeanMapper;

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

    @Override
    public ResourceBeanMapper getResourceBeanMapper() throws ResourceException {
        if (resourceBeanMapper == null) {
            resourceBeanMapper = new JacksonResourceBeanMapper(getObjectMapper());
        }

        return resourceBeanMapper;
    }

    /**
     * Extract response data from the given {@code responseException} and set those to {@code exchangeHint}.
     * @param responseException a {@link RestClientResponseException} thrown by a {@link ResponseErrorHandler}.
     * @param exchangeHint exchange hint to hold any available response data on error
     */
    @Override
    protected void extractResponseDataToExchangeHint(final RestClientResponseException responseException,
            final ExchangeHint exchangeHint) {
        if (exchangeHint == null) {
            return;
        }

        try {
            exchangeHint.setResponseStatusCode(responseException.getRawStatusCode());

            final HttpHeaders responseHeaders = responseException.getResponseHeaders();
            exchangeHint.setResponseHeaders(responseHeaders);

            final String responseBody = responseException.getResponseBodyAsString();

            if (responseBody == null) {
                return;
            }

            exchangeHint.setResponseBody(responseBody);

            final MediaType contentType = responseHeaders.getContentType();

            if (contentType == null || !MediaType.APPLICATION_JSON.includes(contentType)) {
                return;
            }

            final String bodyText = responseException.getResponseBodyAsString();
            JsonNode jsonNode = getObjectMapper().readTree(bodyText);
            Resource errorInfoResource = new JacksonResource(jsonNode);
            exchangeHint.setResponseBody(errorInfoResource);
        } catch (Exception e) {
            log.warn("Failed to extract response data from response exception.", e);
        }
    }
}
