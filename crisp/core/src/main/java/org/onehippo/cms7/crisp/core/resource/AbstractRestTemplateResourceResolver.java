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
package org.onehippo.cms7.crisp.core.resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.onehippo.cms7.crisp.api.resource.Binary;
import org.onehippo.cms7.crisp.api.resource.ResourceException;
import org.onehippo.cms7.crisp.api.resource.ResourceResolver;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Abstract {@link ResourceResolver} base class using Spring Framework's {@link RestTemplate} in communication
 * with REST API backends.
 */
public abstract class AbstractRestTemplateResourceResolver extends AbstractHttpRequestResourceResolver {

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

    @Override
    public Binary resolveBinary(String absPath, Map<String, Object> pathVariables) throws ResourceException {
        Binary binary = null;

        try {
            RestTemplate restTemplate = getRestTemplate();
            binary = restTemplate.execute(getBaseResourceURI(absPath), HttpMethod.GET, null,
                    new ResponseExtractor<FileBinary>() {
                        @Override
                        public FileBinary extractData(ClientHttpResponse response) throws IOException {
                            FileBinary fileBinary = null;
                            InputStream input = null;

                            if (response.getStatusCode().is2xxSuccessful()) {
                                try {
                                    fileBinary = new FileBinary();
                                    File file = File.createTempFile("crispbin-", ".tmp");
                                    input = response.getBody();
                                    fileBinary.save(file, input);
                                } finally {
                                    IOUtils.closeQuietly(input);
                                }
                            } else {
                                throw new ResourceException("Unexpected response status: " + response.getStatusCode());
                            }

                            return fileBinary;
                        }
                    }, pathVariables);
        } catch (RestClientException e) {
            throw new ResourceException("REST client invocation error.", e);
        } catch (Exception e) {
            throw new ResourceException("Unknown error.", e);
        }

        return binary;
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
}
