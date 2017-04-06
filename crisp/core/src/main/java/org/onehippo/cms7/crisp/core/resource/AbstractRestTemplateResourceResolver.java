package org.onehippo.cms7.crisp.core.resource;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

public abstract class AbstractRestTemplateResourceResolver extends AbstractHttpRequestResourceResolver {

    private List<ClientHttpRequestInterceptor> clientHttpRequestInterceptor;

    public AbstractRestTemplateResourceResolver() {
        super();
    }

    public List<ClientHttpRequestInterceptor> getClientHttpRequestInterceptor() {
        return clientHttpRequestInterceptor;
    }

    public void setClientHttpRequestInterceptor(List<ClientHttpRequestInterceptor> clientHttpRequestInterceptor) {
        this.clientHttpRequestInterceptor = clientHttpRequestInterceptor;
    }

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

    protected boolean isSuccessfulResponse(final ResponseEntity responseEntity) {
        return responseEntity.getStatusCode().is2xxSuccessful();
    }
}
