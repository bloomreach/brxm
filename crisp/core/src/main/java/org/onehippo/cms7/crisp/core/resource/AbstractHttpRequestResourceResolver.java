package org.onehippo.cms7.crisp.core.resource;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.lang.StringUtils;
import org.onehippo.cms7.crisp.api.resource.AbstractResourceResolver;

public abstract class AbstractHttpRequestResourceResolver extends AbstractResourceResolver {

    private String baseUri;
    private String searchBaseUri;
    private String searchQueryParameterName = "q";

    public AbstractHttpRequestResourceResolver() {
        super();
    }

    public String getBaseUri() {
        return baseUri;
    }

    public void setBaseUri(String baseUri) {
        this.baseUri = baseUri;
    }

    public String getSearchBaseUri() {
        return searchBaseUri;
    }

    public void setSearchBaseUri(String searchBaseUri) {
        this.searchBaseUri = searchBaseUri;
    }

    public String getSearchQueryParameterName() {
        return searchQueryParameterName;
    }

    public void setSearchQueryParameterName(String searchQueryParameterName) {
        this.searchQueryParameterName = searchQueryParameterName;
    }

    protected String buildResourceURI(final String absPath) {
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

    protected String buildSearchURI(final String baseAbsPath, final String query) {
        StringBuilder sb = new StringBuilder(80);

        if (StringUtils.isNotEmpty(searchBaseUri)) {
            sb.append(searchBaseUri);
        }

        if (StringUtils.isNotEmpty(baseAbsPath)) {
            if (StringUtils.endsWith(searchBaseUri, "/") && StringUtils.startsWith(baseAbsPath, "/")) {
                sb.append(baseAbsPath.substring(1));
            } else {
                sb.append(baseAbsPath);
            }
        }

        if (StringUtils.isNotEmpty(query)) {
            try {
                final String encodedQuery = URLEncoder.encode(query, "UTF-8");
                if (StringUtils.endsWith(searchBaseUri, "=")) {
                    sb.append(encodedQuery);
                } else {
                    sb.append(StringUtils.contains(searchBaseUri, '?') ? '&' : '?');
                    sb.append(getSearchQueryParameterName()).append('=').append(encodedQuery);
                }
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("UTF-8 should be supported.", e);
            }
        }

        return sb.toString();
    }
}
