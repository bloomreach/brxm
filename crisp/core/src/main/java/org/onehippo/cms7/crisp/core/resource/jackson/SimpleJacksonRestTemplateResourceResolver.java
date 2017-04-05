package org.onehippo.cms7.crisp.core.resource.jackson;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceContainable;
import org.onehippo.cms7.crisp.api.resource.ResourceException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

public class SimpleJacksonRestTemplateResourceResolver extends AbstractJacksonRestTemplateResourceResolver {

    private static ThreadLocal<Map<ResourceContainable, Object>> tlResourceResultCache = new ThreadLocal<Map<ResourceContainable, Object>>() {
        @Override
        protected Map<ResourceContainable, Object> initialValue() {
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
    public Resource resolve(String absPath, Map<String, Object> variables) throws ResourceException {
        RestTemplate restTemplate = createRestTemplate();
        ResponseEntity<String> result = restTemplate.getForEntity(buildResourceURI(absPath), String.class,
                (variables != null) ? variables : Collections.emptyMap());

        if (isSuccessful(result)) {
            try {
                final String bodyText = result.getBody();
                JsonNode jsonNode = getObjectMapper().readTree(bodyText);
                Resource resource = new JacksonResource(jsonNode);

                if (isCacheEnabled()) {
                    tlResourceResultCache.get().put(resource, bodyText);
                }

                return resource;
            } catch (JsonProcessingException e) {
                throw new ResourceException("JSON processing error.", e);
            } catch (IOException e) {
                throw new ResourceException("IO error.", e);
            }
        } else {
            throw new ResourceException("Unexpected response status: " + result.getStatusCode());
        }
    }

    @Override
    public ResourceContainable findResources(String baseAbsPath, Map<String, Object> variables, String query,
            String language) throws ResourceException {
        RestTemplate restTemplate = createRestTemplate();
        ResponseEntity<String> result = restTemplate.getForEntity(buildSearchURI(baseAbsPath, query), String.class,
                (variables != null) ? variables : Collections.emptyMap());

        if (isSuccessful(result)) {
            try {
                final String bodyText = result.getBody();
                JsonNode jsonNode = getObjectMapper().readTree(bodyText);
                ResourceContainable rootResource = new JacksonResource(jsonNode);

                if (isCacheEnabled()) {
                    tlResourceResultCache.get().put(rootResource, bodyText);
                }

                return rootResource;
            } catch (JsonProcessingException e) {
                throw new ResourceException("JSON processing error.", e);
            } catch (IOException e) {
                throw new ResourceException("IO error.", e);
            }
        } else {
            throw new ResourceException("Unexpected response status: " + result.getStatusCode());
        }
    }

    @Override
    public boolean isCacheable(ResourceContainable resourceContainer) {
        return (isCacheEnabled() && resourceContainer instanceof JacksonResource);
    }

    @Override
    public Object toCacheData(ResourceContainable resourceContainer) {
        if (!isCacheEnabled() || !(resourceContainer instanceof JacksonResource)) {
            return null;
        }

        Object body = tlResourceResultCache.get().get(resourceContainer);

        if (body != null) {
            tlResourceResultCache.get().remove(resourceContainer);
            return body;
        } else {
            return ((JacksonResource) resourceContainer).toJsonString(getObjectMapper());
        }
    }

    @Override
    public ResourceContainable fromCacheData(Object cacheData) {
        if (!isCacheEnabled() || !(cacheData instanceof String)) {
            return null;
        }

        try {
            JsonNode jsonNode = getObjectMapper().readTree((String) cacheData);
            ResourceContainable rootResource = new JacksonResource(jsonNode);
            return rootResource;
        } catch (JsonProcessingException e) {
            throw new ResourceException("JSON processing error.", e);
        } catch (IOException e) {
            throw new ResourceException("IO error.", e);
        }
    }

}
