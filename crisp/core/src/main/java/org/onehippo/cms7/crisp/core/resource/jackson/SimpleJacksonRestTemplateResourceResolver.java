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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.onehippo.cms7.crisp.api.exchange.ExchangeHint;
import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceException;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

public class SimpleJacksonRestTemplateResourceResolver extends AbstractJacksonRestTemplateResourceResolver {

    private static ThreadLocal<Map<Resource, Object>> tlResourceResultCache = new ThreadLocal<Map<Resource, Object>>() {
        @Override
        protected Map<Resource, Object> initialValue() {
            return new HashMap<>();
        }
    };

    public SimpleJacksonRestTemplateResourceResolver() {
        super();
    }

    @Override
    public Resource resolve(String absPath, Map<String, Object> pathVariables, ExchangeHint exchangeHint) throws ResourceException {
        try {
            final HttpMethod httpMethod = (exchangeHint != null) ? HttpMethod.resolve(exchangeHint.getMethodName()) : HttpMethod.GET;
            final Object requestObject = getRequestEntityObject(exchangeHint);

            RestTemplate restTemplate = getRestTemplate();
            ResponseEntity<String> result;

            if (HttpMethod.POST.equals(httpMethod)) {
                result = restTemplate.postForEntity(getBaseResourceURI(absPath), requestObject, String.class, pathVariables);
            } else {
                result = restTemplate.getForEntity(getBaseResourceURI(absPath), String.class, pathVariables);
            }

            if (isSuccessfulResponse(result)) {
                final String bodyText = result.getBody();
                JsonNode jsonNode = getObjectMapper().readTree(bodyText);
                Resource resource = new JacksonResource(jsonNode);

                if (isCacheEnabled()) {
                    tlResourceResultCache.get().put(resource, bodyText);
                }

                return resource;
            } else {
                throw new ResourceException("Unexpected response status: " + result.getStatusCode());
            }
        } catch (JsonProcessingException e) {
            throw new ResourceException("JSON processing error.", e);
        } catch (RestClientException e) {
            throw new ResourceException("REST client invocation error.", e);
        } catch (IOException e) {
            throw new ResourceException("IO error.", e);
        } catch (Exception e) {
            throw new ResourceException("Unknown error.", e);
        }
    }

    @Override
    public Resource findResources(String baseAbsPath, Map<String, Object> pathVariables, ExchangeHint exchangeHint)
            throws ResourceException {
        try {
            final HttpMethod httpMethod = (exchangeHint != null) ? HttpMethod.resolve(exchangeHint.getMethodName()) : HttpMethod.GET;
            final Object requestObject = getRequestEntityObject(exchangeHint);

            RestTemplate restTemplate = getRestTemplate();
            ResponseEntity<String> result;

            if (HttpMethod.POST.equals(httpMethod)) {
                result = restTemplate.postForEntity(getBaseResourceURI(baseAbsPath), requestObject, String.class,
                    pathVariables);
            } else {
                result = restTemplate.getForEntity(getBaseResourceURI(baseAbsPath), String.class,
                        pathVariables);
            }

            if (isSuccessfulResponse(result)) {
                final String bodyText = result.getBody();
                JsonNode jsonNode = getObjectMapper().readTree(bodyText);
                Resource rootResource = new JacksonResource(jsonNode);

                if (isCacheEnabled()) {
                    tlResourceResultCache.get().put(rootResource, bodyText);
                }

                return rootResource;
            } else {
                throw new ResourceException("Unexpected response status: " + result.getStatusCode());
            }
        } catch (JsonProcessingException e) {
            throw new ResourceException("JSON processing error.", e);
        } catch (RestClientException e) {
            throw new ResourceException("REST client invocation error.", e);
        } catch (IOException e) {
            throw new ResourceException("IO error.", e);
        } catch (Exception e) {
            throw new ResourceException("Unknown error.", e);
        }
    }

    @Override
    public boolean isCacheable(Resource resource) {
        return (isCacheEnabled() && resource instanceof JacksonResource);
    }

    @Override
    public Object toCacheData(Resource resource) throws IOException {
        if (!isCacheEnabled() || !(resource instanceof JacksonResource)) {
            return null;
        }

        Object body = tlResourceResultCache.get().remove(resource);

        if (body != null) {
            return body;
        }

        return ((JacksonResource) resource).toJsonString(getObjectMapper());
    }

    @Override
    public Resource fromCacheData(Object cacheData) throws IOException {
        if (!isCacheEnabled() || !(cacheData instanceof String)) {
            return null;
        }

        try {
            JsonNode jsonNode = getObjectMapper().readTree((String) cacheData);
            Resource rootResource = new JacksonResource(jsonNode);
            return rootResource;
        } catch (JsonProcessingException e) {
            throw new ResourceException("JSON processing error.", e);
        } catch (IOException e) {
            throw new ResourceException("IO error.", e);
        }
    }

}
