package org.onehippo.cms7.crisp.core.resource;

import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

public abstract class AbstractRestTemplateResourceResolver extends AbstractHttpRequestResourceResolver {

    private ClientHttpRequestFactory clientHttpRequestFactory;

    public AbstractRestTemplateResourceResolver() {
        super();
    }

    public ClientHttpRequestFactory getClientHttpRequestFactory() {
        return clientHttpRequestFactory;
    }

    public void setClientHttpRequestFactory(ClientHttpRequestFactory clientHttpRequestFactory) {
        this.clientHttpRequestFactory = clientHttpRequestFactory;
    }

    protected RestTemplate createRestTemplate() {
        if (getClientHttpRequestFactory() != null) {
            return new RestTemplate(getClientHttpRequestFactory());
        } else {
            return new RestTemplate();
        }
    }

    protected boolean isSuccessful(final ResponseEntity responseEntity) {
        return responseEntity.getStatusCode().is2xxSuccessful();
    }
}
