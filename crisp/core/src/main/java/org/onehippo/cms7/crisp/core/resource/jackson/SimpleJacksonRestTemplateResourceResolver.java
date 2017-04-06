package org.onehippo.cms7.crisp.core.resource.jackson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceContainable;
import org.onehippo.cms7.crisp.api.resource.ResourceException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
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
    public Resource resolve(String absPath, Map<String, Object> pathVariables) throws ResourceException {
        try {
            RestTemplate restTemplate = createRestTemplate();
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
    public ResourceContainable findResources(String baseAbsPath, Map<String, Object> pathVariables)
            throws ResourceException {
        try {
            RestTemplate restTemplate = createRestTemplate();
            ResponseEntity<String> result = restTemplate.getForEntity(getBaseResourceURI(baseAbsPath), String.class,
                    pathVariables);

            if (isSuccessfulResponse(result)) {
                final String bodyText = result.getBody();
                JsonNode jsonNode = getObjectMapper().readTree(bodyText);
                ResourceContainable rootResource = new JacksonResource(jsonNode);

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
