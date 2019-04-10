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
package org.onehippo.cms7.crisp.core.resource.jdom;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.jdom2.Element;
import org.onehippo.cms7.crisp.api.exchange.ExchangeHint;
import org.onehippo.cms7.crisp.api.resource.Binary;
import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

public class SimpleJdomRestTemplateResourceResolver extends AbstractJdomRestTemplateResourceResolver {

    public SimpleJdomRestTemplateResourceResolver() {
        super();
    }

    @Override
    public Resource resolve(String absPath, Map<String, Object> pathVariables, ExchangeHint exchangeHint) throws ResourceException {
        try {
            final HttpMethod httpMethod = (exchangeHint != null) ? HttpMethod.resolve(exchangeHint.getMethodName()) : HttpMethod.GET;
            final Object requestObject = getRequestEntityObject(exchangeHint);

            RestTemplate restTemplate = getRestTemplate();
            ResponseEntity<ByteArrayResource> result;

            if (HttpMethod.POST.equals(httpMethod)) {
                result = restTemplate.postForEntity(getBaseResourceURI(absPath), requestObject,
                        ByteArrayResource.class, pathVariables);
            } else {
                result = restTemplate.getForEntity(getBaseResourceURI(absPath),
                        ByteArrayResource.class, pathVariables);
            }

            extractResponseDataToExchangeHint(result, exchangeHint);

            if (isSuccessfulResponse(result)) {
                final ByteArrayResource body = result.getBody();
                final Element rootElem = byteArrayResourceToElement(body);
                final Resource resource = new JdomResource(rootElem);
                return resource;
            } else {
                throw new ResourceException("Unexpected response status: " + result.getStatusCode());
            }
        } catch (RestClientResponseException e) {
            extractResponseDataToExchangeHint(e, exchangeHint);
            throw new ResourceException("REST client response error.", e);
        } catch (RestClientException e) {
            throw new ResourceException("REST client invocation error.", e);
        } catch (IOException e) {
            throw new ResourceException("IO error.", e);
        } catch (Exception e) {
            throw new ResourceException("Unknown error.", e);
        }
    }

    @Override
    public Resource resolveBinaryAsResource(String absPath, Map<String, Object> pathVariables,
            ExchangeHint exchangeHint) throws ResourceException {
        final Binary binary = resolveBinary(absPath, pathVariables, exchangeHint);

        if (binary != null) {
            try (InputStream input = new BufferedInputStream(binary.getInputStream())) {
                final Element rootElem = inputStreamToElement(input);
                return new JdomResource(rootElem);
            } catch (Exception e) {
                throw new ResourceException("Error in JDOM conversion to resource.", e);
            }
        }

        return null;
    }

    @Override
    public Resource findResources(String baseAbsPath, Map<String, Object> pathVariables, ExchangeHint exchangeHint)
            throws ResourceException {
        try {
            final HttpMethod httpMethod = (exchangeHint != null) ? HttpMethod.resolve(exchangeHint.getMethodName()) : HttpMethod.GET;
            final Object requestObject = getRequestEntityObject(exchangeHint);

            RestTemplate restTemplate = getRestTemplate();
            ResponseEntity<ByteArrayResource> result;

            if (HttpMethod.POST.equals(httpMethod)) {
                result = restTemplate.postForEntity(getBaseResourceURI(baseAbsPath), requestObject,
                        ByteArrayResource.class, pathVariables);
            } else {
                result = restTemplate.getForEntity(getBaseResourceURI(baseAbsPath),
                        ByteArrayResource.class, pathVariables);
            }

            extractResponseDataToExchangeHint(result, exchangeHint);

            if (isSuccessfulResponse(result)) {
                final ByteArrayResource body = result.getBody();
                final Element rootElem = byteArrayResourceToElement(body);
                final Resource rootResource = new JdomResource(rootElem);
                return rootResource;
            } else {
                throw new ResourceException("Unexpected response status: " + result.getStatusCode());
            }
        } catch (RestClientResponseException e) {
            extractResponseDataToExchangeHint(e, exchangeHint);
            throw new ResourceException("REST client response error.", e);
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
}
