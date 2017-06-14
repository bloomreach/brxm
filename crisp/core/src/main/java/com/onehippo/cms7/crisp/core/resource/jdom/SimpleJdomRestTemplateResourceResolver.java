/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package com.onehippo.cms7.crisp.core.resource.jdom;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.onehippo.cms7.crisp.api.resource.Resource;
import com.onehippo.cms7.crisp.api.resource.ResourceException;

public class SimpleJdomRestTemplateResourceResolver extends AbstractJdomRestTemplateResourceResolver {

    private boolean cacheEnabled;

    public SimpleJdomRestTemplateResourceResolver() {
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
            ResponseEntity<ByteArrayResource> result = restTemplate.getForEntity(getBaseResourceURI(absPath),
                    ByteArrayResource.class, pathVariables);

            if (isSuccessfulResponse(result)) {
                final ByteArrayResource body = result.getBody();
                final Element rootElem = byteArrayResourceToElement(body);
                final Resource resource = new JdomResource(rootElem);
                return resource;
            } else {
                throw new ResourceException("Unexpected response status: " + result.getStatusCode());
            }
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
            ResponseEntity<ByteArrayResource> result = restTemplate.getForEntity(getBaseResourceURI(baseAbsPath),
                    ByteArrayResource.class, pathVariables);

            if (isSuccessfulResponse(result)) {
                final ByteArrayResource body = result.getBody();
                final Element rootElem = byteArrayResourceToElement(body);
                final Resource rootResource = new JdomResource(rootElem);
                return rootResource;
            } else {
                throw new ResourceException("Unexpected response status: " + result.getStatusCode());
            }
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
        return (isCacheEnabled() && resource instanceof JdomResource);
    }

    @Override
    public Object toCacheData(Resource resource) throws IOException {
        if (!isCacheEnabled() || !(resource instanceof JdomResource)) {
            return null;
        }

        return resource;
    }

    @Override
    public Resource fromCacheData(Object cacheData) throws IOException {
        return (JdomResource) cacheData;
    }

    private Element byteArrayResourceToElement(final ByteArrayResource body) throws JDOMException, IOException {
        InputStream input = null;

        try {
            SAXBuilder jdomBuilder = new SAXBuilder();
            input = body.getInputStream();
            final Document document = jdomBuilder.build(input);
            final Element elem = document.getRootElement();
            return elem;
        } finally {
            IOUtils.closeQuietly(input);
        }
    }
}
