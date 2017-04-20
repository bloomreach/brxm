package com.onehippo.cms7.crisp.core.resource;

import org.apache.commons.lang.StringUtils;
import org.springframework.http.client.ClientHttpRequestFactory;

import com.onehippo.cms7.crisp.api.resource.AbstractResourceResolver;

public abstract class AbstractHttpRequestResourceResolver extends AbstractResourceResolver {

    private ClientHttpRequestFactory clientHttpRequestFactory;
    private String baseUri;

    public AbstractHttpRequestResourceResolver() {
        super();
    }

    public ClientHttpRequestFactory getClientHttpRequestFactory() {
        return clientHttpRequestFactory;
    }

    public void setClientHttpRequestFactory(ClientHttpRequestFactory clientHttpRequestFactory) {
        this.clientHttpRequestFactory = clientHttpRequestFactory;
    }

    public String getBaseUri() {
        return baseUri;
    }

    public void setBaseUri(String baseUri) {
        this.baseUri = baseUri;
    }

    protected String getBaseResourceURI(final String absPath) {
        StringBuilder sb = new StringBuilder(80);

        if (StringUtils.isNotEmpty(baseUri)) {
            sb.append(baseUri);
        }

        if (StringUtils.isNotEmpty(absPath)) {
            if (StringUtils.endsWith(baseUri, "/") && StringUtils.startsWith(absPath, "/")) {
                sb.append(absPath.substring(1));
            } else {
                sb.append(absPath);
            }
        }

        return sb.toString();
    }

}
