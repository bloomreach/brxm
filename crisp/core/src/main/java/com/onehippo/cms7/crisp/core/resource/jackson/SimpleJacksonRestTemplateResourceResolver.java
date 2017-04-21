/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package com.onehippo.cms7.crisp.core.resource.jackson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.onehippo.cms7.crisp.api.resource.Resource;
import com.onehippo.cms7.crisp.api.resource.ResourceException;

public class SimpleJacksonRestTemplateResourceResolver extends AbstractJacksonRestTemplateResourceResolver {

    private static ThreadLocal<Map<Resource, Object>> tlResourceResultCache = new ThreadLocal<Map<Resource, Object>>() {
        @Override
        protected Map<Resource, Object> initialValue() {
            return new HashMap<>();
        }
    };

    private boolean cacheEnabled;

    public SimpleJacksonRestTemplateResourceResolver() {
        super();
    }

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    @Override
    public Resource resolve(String absPath, Map<String, Object> pathVariables) throws ResourceException {
        try {
            RestTemplate restTemplate = getRestTemplate();
            ResponseEntity<String> result = restTemplate.getForEntity(getBaseResourceURI(absPath), String.class,
                    pathVariables);

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
    public Resource findResources(String baseAbsPath, Map<String, Object> pathVariables)
            throws ResourceException {
        try {
            RestTemplate restTemplate = getRestTemplate();
            ResponseEntity<String> result = restTemplate.getForEntity(getBaseResourceURI(baseAbsPath), String.class,
                    pathVariables);

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
    public Object toCacheData(Resource resource) {
        if (!isCacheEnabled() || !(resource instanceof JacksonResource)) {
            return null;
        }

        Object body = tlResourceResultCache.get().get(resource);

        if (body != null) {
            tlResourceResultCache.get().remove(resource);
            return body;
        } else {
            return ((JacksonResource) resource).toJsonString(getObjectMapper());
        }
    }

    @Override
    public Resource fromCacheData(Object cacheData) {
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
