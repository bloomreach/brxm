package org.onehippo.cms7.crisp.core.resource.jackson;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

public class SimpleJacksonRestTemplateResourceResolver extends AbstractJacksonRestTemplateResourceResolver {

    public SimpleJacksonRestTemplateResourceResolver() {
        super();
    }

    @Override
    public Resource resolve(String absPath, Map<String, Object> variables) throws ResourceException {
        RestTemplate restTemplate = createRestTemplate();
        ResponseEntity<String> result = restTemplate.getForEntity(buildResourceURI(absPath), String.class, variables);

        if (isSuccessful(result)) {
            try {
                JsonNode jsonNode = getObjectMapper().readTree(result.getBody());
                return new JacksonResource(jsonNode);
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
    public Iterator<Resource> findResources(String baseAbsPath, Map<String, Object> variables, String query,
            String language) throws ResourceException {
        RestTemplate restTemplate = createRestTemplate();
        ResponseEntity<String> result = restTemplate.getForEntity(buildSearchURI(baseAbsPath, query), String.class, variables);

        if (isSuccessful(result)) {
            try {
                JsonNode jsonNode = getObjectMapper().readTree(result.getBody());
                Resource rootResource = new JacksonResource(jsonNode);
                return rootResource.listChildren();
            } catch (JsonProcessingException e) {
                throw new ResourceException("JSON processing error.", e);
            } catch (IOException e) {
                throw new ResourceException("IO error.", e);
            }
        } else {
            throw new ResourceException("Unexpected response status: " + result.getStatusCode());
        }
    }

}
