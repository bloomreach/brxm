/*
 *  Copyright 2017-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.core.resource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.onehippo.cms7.crisp.api.exchange.ExchangeHint;
import org.onehippo.cms7.crisp.api.resource.Binary;
import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceException;
import org.onehippo.cms7.crisp.api.resource.ResourceResolver;
import org.onehippo.cms7.crisp.core.resource.util.CrispUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

/**
 * Abstract {@link ResourceResolver} base class using Spring Framework's {@link RestTemplate} in communication
 * with REST API backends.
 */
public abstract class AbstractRestTemplateResourceResolver extends AbstractHttpRequestResourceResolver {

    private static Logger log = LoggerFactory.getLogger(AbstractRestTemplateResourceResolver.class);

    /**
     * Client HTTP Request interceptors that are set to the default {@link RestTemplate} creation if {@link #restTemplate}
     * property is not set.
     */
    private List<ClientHttpRequestInterceptor> clientHttpRequestInterceptor;

    /**
     * {@link RestTemplate} instance to be used in communication with REST API backends.
     */
    private RestTemplate restTemplate;

    /**
     * Max byte size of {@link Binary} to keep in memory.
     */
    private long maxInMemoryBinarySize = 4096L;

    /**
     * Default constructor.
     */
    public AbstractRestTemplateResourceResolver() {
        super();
    }

    /**
     * Returns Client HTTP Request interceptors that are set to the default {@link RestTemplate} creation if {@link #restTemplate}
     * property is not set.
     * @return Client HTTP Request interceptors that are set to the default {@link RestTemplate} creation if {@link #restTemplate}
     *         property is not set
     */
    public List<ClientHttpRequestInterceptor> getClientHttpRequestInterceptor() {
        return clientHttpRequestInterceptor;
    }

    /**
     * Sets Client HTTP Request interceptors that are set to the default {@link RestTemplate} creation if {@link #restTemplate}
     * property is not set.
     * @param clientHttpRequestInterceptor Client HTTP Request interceptors that are set to the default {@link RestTemplate}
     *        creation if {@link #restTemplate} property is not set
     */
    public void setClientHttpRequestInterceptor(List<ClientHttpRequestInterceptor> clientHttpRequestInterceptor) {
        this.clientHttpRequestInterceptor = clientHttpRequestInterceptor;
    }

    /**
     * Returns the {@link RestTemplate} to be used in communication with backend REST APIs by this resource resolver.
     * @return the {@link RestTemplate} to be used in communication with backend REST APIs by this resource resolver
     */
    public RestTemplate getRestTemplate() {
        if (restTemplate == null) {
            restTemplate = createRestTemplate();
        }

        return restTemplate;
    }

    /**
     * Sets the default {@link RestTemplate} to be used in communication with backend REST APIs by this resource resolver.
     * @param defaultRestTemplate the default {@link RestTemplate} to be used in communication with backend REST
     *        APIs by this resource resolver
     */
    public void setRestTemplate(RestTemplate defaultRestTemplate) {
        this.restTemplate = defaultRestTemplate;
    }

    /**
     * Return max byte size of {@link Binary} to keep in memory.
     * @return max byte size of {@link Binary} to keep in memory
     */
    public long getMaxInMemoryBinarySize() {
        return maxInMemoryBinarySize;
    }

    /**
     * Set max byte size of {@link Binary} to keep in memory.
     * @param maxInMemoryBinarySize max byte size of {@link Binary} to keep in memory
     */
    public void setMaxInMemoryBinarySize(long maxInMemoryBinarySize) {
        this.maxInMemoryBinarySize = maxInMemoryBinarySize;
    }

    /**
     * In most REST API based backend integration, it is very likely to retrieve a search result containing multiple
     * resource items inside a root or container resource as well, such as root JSON node or XML node, it is okay
     * to invoke {@link #resolve(String, Map, ExchangeHint)} by default for this operation.
     * <P>
     * Otherwise - for example, different communications are needed for this operation by the backend -- this method
     * must be overridden properly.
     */
    @Override
    public Resource findResources(String baseAbsPath, Map<String, Object> pathVariables, ExchangeHint exchangeHint)
            throws ResourceException {
        // Suppose we retrieve a search result which is wrapped by a resource representation such as a JSON node
        // or XML node here...
        final Resource rootResource = resolve(baseAbsPath, pathVariables, exchangeHint);
        return rootResource;
    }

    @Override
    public Binary resolveBinary(String absPath, Map<String, Object> pathVariables, ExchangeHint exchangeHint) throws ResourceException {
        Binary binary = null;

        try {
            HttpMethod httpMethod = (exchangeHint != null) ? HttpMethod.resolve(exchangeHint.getMethodName()) : null;

            if (httpMethod == null) {
                httpMethod = HttpMethod.GET;
            }

            RestTemplate restTemplate = getRestTemplate();
            RequestCallback requestCallback = null;

            final Object requestObject = getRequestEntityObject(exchangeHint);
            if (requestObject != null) {
                requestCallback = new SimpleSpringHttpEntityRequestCallback(requestObject, restTemplate.getMessageConverters());
            }

            binary =
                    restTemplate.execute(
                            getBaseResourceURI(absPath),
                            httpMethod,
                            requestCallback,
                            new ResponseExtractor<SpringResourceBinary>() {
                                @Override
                                public SpringResourceBinary extractData(ClientHttpResponse response) throws IOException {
                                    extractResponseDataToExchangeHint(response, exchangeHint);

                                    SpringResourceBinary resourceBinary = null;

                                    if (response.getStatusCode().is2xxSuccessful()) {
                                        final long contentLength = response.getHeaders().getContentLength();
                                        InputStream input = null;
                                        OutputStream output = null;

                                        try {
                                            input = response.getBody();

                                            if (contentLength >=0 && contentLength <= getMaxInMemoryBinarySize()) {
                                                output = new ByteArrayOutputStream((int) contentLength);
                                                IOUtils.copy(input, output);
                                                resourceBinary = new SpringResourceBinary(new ByteArrayResource(
                                                        ((ByteArrayOutputStream) output).toByteArray()), true);
                                                output.close();
                                                output = null;
                                            } else {
                                                File file = File.createTempFile("crispbin-", ".tmp");
                                                output = new FileOutputStream(file);
                                                IOUtils.copy(input, output);
                                                output.close();
                                                output = null;
                                                resourceBinary = new SpringResourceBinary(new FileSystemResource(file), true);
                                            }
                                        } finally {
                                            CrispUtils.closeQuietly(output);
                                            CrispUtils.closeQuietly(input);
                                        }
                                    } else {
                                        throw new ResourceException("Unexpected response status: " + response.getStatusCode());
                                    }

                                    return resourceBinary;
                                }
                            },
                            pathVariables);
        } catch (RestClientResponseException e) {
            extractResponseDataToExchangeHint(e, exchangeHint);
            throw new ResourceException("REST client response error.", e);
        } catch (RestClientException e) {
            throw new ResourceException("REST client invocation error.", e);
        } catch (Exception e) {
            throw new ResourceException("Unknown error.", e);
        }

        return binary;
    }

    @Override
    public URI resolveFullURI(String absPath, Map<String, Object> pathVariables) throws ResourceException {
        final String uriString = getBaseResourceURI(absPath);

        if (uriString == null || uriString.isEmpty()) {
            return null;
        }

        return getRestTemplate().getUriTemplateHandler().expand(uriString, pathVariables);
    }

    /**
     * Create a new default {@link RestTemplate} if {@link #restTemplate} was not set.
     * @return a new default {@link RestTemplate} if {@link #restTemplate} was not set
     */
    protected RestTemplate createRestTemplate() {
        RestTemplate restTemplate = null;

        if (getClientHttpRequestFactory() != null) {
            restTemplate = new RestTemplate(getClientHttpRequestFactory());
        } else {
            restTemplate = new RestTemplate();
        }

        if (clientHttpRequestInterceptor != null) {
            restTemplate.setInterceptors(clientHttpRequestInterceptor);
        }

        return restTemplate;
    }

    /**
     * Returns true if the response entity represents a successful result.
     * @param responseEntity response entity
     * @return true if the response entity represents a successful result
     */
    protected boolean isSuccessfulResponse(final ResponseEntity responseEntity) {
        return responseEntity.getStatusCode().is2xxSuccessful();
    }

    /**
     * Return a request entity object, which needs to be understood by Spring RestTemplate, from the {@code exchangeHint}.
     * @param exchangeHint exchange hint
     * @return a request entity object, which needs to be understood by Spring RestTemplate, from the {@code exchangeHint}
     */
    protected Object getRequestEntityObject(ExchangeHint exchangeHint) {
        if (exchangeHint == null) {
            return null;
        }

        final Map<String, List<String>> requestHeaders = exchangeHint.getRequestHeaders();
        final Object requestBody = exchangeHint.getRequestBody();
        final Object requestEntity = exchangeHint.getRequest();

        final int headerKeyCount = (requestHeaders != null) ? requestHeaders.size() : 0;

        if (headerKeyCount > 0 || requestBody != null) {
            HttpHeaders headers = null;

            if (headerKeyCount > 0) {
                headers = new HttpHeaders();
                headers.putAll(requestHeaders);
            }

            return new HttpEntity<>(requestBody, headers);
        }

        return requestEntity;
    }

    /**
     * Extract response data from the given {@code response} to set status code and headers in {@code exchangeHint}.
     * @param response response
     * @param exchangeHint exchange hint
     */
    protected void extractResponseDataToExchangeHint(final ClientHttpResponse response,
            final ExchangeHint exchangeHint) {
        if (exchangeHint == null) {
            return;
        }

        try {
            exchangeHint.setResponseStatusCode(response.getRawStatusCode());

            final HttpHeaders responseHeaders = response.getHeaders();
            exchangeHint.setResponseHeaders(responseHeaders);
        } catch (Exception e) {
            log.warn("Failed to extract response data from clientHttpResponse.", e);
        }
    }

    /**
     * Extract response data from the given {@code responseEntity} to set status code and headers in {@code exchangeHint}.
     * <p>
     * <em>Note:</em> This method is supposed to be invoked only when no {@link RestClientResponseException} occurs while
     * invoking on {@link RestTemplate}.
     * @param responseEntity response entity
     * @param exchangeHint exchange hint
     */
    protected void extractResponseDataToExchangeHint(final ResponseEntity responseEntity,
            final ExchangeHint exchangeHint) {
        if (exchangeHint == null) {
            return;
        }

        try {
            exchangeHint.setResponseStatusCode(responseEntity.getStatusCodeValue());

            final HttpHeaders responseHeaders = responseEntity.getHeaders();
            exchangeHint.setResponseHeaders(responseHeaders);
        } catch (Exception e) {
            log.warn("Failed to extract response data from responseEntity.", e);
        }
    }

    /**
     * Extract response data from the given {@code responseException} to set status code and headers in {@code exchangeHint}.
     * <p>
     * <em>Note:</em> This method is supposed to be invoked only when {@link RestClientResponseException} occurs while
     * invoking on {@link RestTemplate}.
     * @param responseEntity response entity
     * @param exchangeHint exchange hint
     */
    abstract protected void extractResponseDataToExchangeHint(final RestClientResponseException responseException,
            final ExchangeHint exchangeHint);

}
