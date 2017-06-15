/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package org.onehippo.cms7.crisp.core.resource;

import java.util.List;

import org.onehippo.cms7.crisp.api.resource.ResourceResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
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
