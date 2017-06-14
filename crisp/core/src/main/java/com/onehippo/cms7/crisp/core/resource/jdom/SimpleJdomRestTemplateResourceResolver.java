/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package com.onehippo.cms7.crisp.core.resource.jdom;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.onehippo.cms7.crisp.api.resource.Resource;
import com.onehippo.cms7.crisp.api.resource.ResourceException;

public class SimpleJdomRestTemplateResourceResolver extends AbstractJdomRestTemplateResourceResolver {

    private static ThreadLocal<Map<Resource, Object>> tlResourceResultCache = new ThreadLocal<Map<Resource, Object>>() {
        @Override
        protected Map<Resource, Object> initialValue() {
            return new HashMap<>();
        }
    };

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
                final byte [] bytes = body.getByteArray();
                final Element rootElem = bytesToElement(bytes);
                final Resource resource = new JdomResource(rootElem);

                if (isCacheEnabled()) {
                    tlResourceResultCache.get().put(resource, bytes);
                }

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
                final byte [] bytes = body.getByteArray();
                final Element rootElem = bytesToElement(bytes);
                final Resource rootResource = new JdomResource(rootElem);

                if (isCacheEnabled()) {
                    tlResourceResultCache.get().put(rootResource, bytes);
                }

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

        Object body = tlResourceResultCache.get().remove(resource);

        if (body != null) {
            return body;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
        ((JdomResource) resource).write(new XMLOutputter(), baos);
        return baos.toByteArray();
    }

    @Override
    public Resource fromCacheData(Object cacheData) throws IOException {
        if (!isCacheEnabled() || !(cacheData instanceof byte[])) {
            return null;
        }

        try {
            Element elem = bytesToElement((byte []) cacheData);
            Resource rootResource = new JdomResource(elem);
            return rootResource;
        } catch (JDOMException e) {
            throw new ResourceException("JDOM parse error.", e);
        } catch (IOException e) {
            throw new ResourceException("IO error.", e);
        }
    }

    private Element bytesToElement(byte [] bytes) throws JDOMException, IOException {
        InputStream input = null;

        try {
            SAXBuilder jdomBuilder = new SAXBuilder();
            input = new ByteArrayInputStream(bytes);
            final Document document = jdomBuilder.build(input);
            final Element elem = document.getRootElement();
            return elem;
        } finally {
            IOUtils.closeQuietly(input);
        }
    }
}
