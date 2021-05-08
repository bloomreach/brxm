/*
 *  Copyright 2017-2021 Hippo B.V. (http://www.onehippo.com)
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.onehippo.cms7.crisp.api.exchange.ExchangeHint;
import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceBeanMapper;
import org.onehippo.cms7.crisp.api.resource.ResourceException;
import org.onehippo.cms7.crisp.core.resource.AbstractRestTemplateResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClientResponseException;

import javax.xml.XMLConstants;

public abstract class AbstractJdomRestTemplateResourceResolver extends AbstractRestTemplateResourceResolver {

    private static Logger log = LoggerFactory.getLogger(AbstractJdomRestTemplateResourceResolver.class);

    private static final MediaType APPLICATION_ALL_XML = MediaType.parseMediaType("application/*+xml");

    private Unmarshaller unmarshaller;
    private String jaxbContextPath;
    private Class<?>[] jaxbClassesToBeBound;
    private String[] jaxbPackagesToScan;
    private ResourceBeanMapper resourceBeanMapper;

    public String getJaxbContextPath() {
        return jaxbContextPath;
    }

    public void setJaxbContextPath(String jaxbContextPath) {
        this.jaxbContextPath = jaxbContextPath;
    }

    public Class<?>[] getJaxbClassesToBeBound() {
        return jaxbClassesToBeBound;
    }

    public void setJaxbClassesToBeBound(Class<?>[] jaxbClassesToBeBound) {
        this.jaxbClassesToBeBound = jaxbClassesToBeBound;
    }

    public String[] getJaxbPackagesToScan() {
        return jaxbPackagesToScan;
    }

    public void setJaxbPackagesToScan(String[] jaxbPackagesToScan) {
        this.jaxbPackagesToScan = jaxbPackagesToScan;
    }

    public Unmarshaller getUnmarshaller() {
        if (unmarshaller == null) {
            unmarshaller = new Jaxb2Marshaller();

            if (StringUtils.hasLength(jaxbContextPath)) {
                ((Jaxb2Marshaller) unmarshaller).setContextPath(jaxbContextPath);
            }

            if (!ObjectUtils.isEmpty(jaxbClassesToBeBound)) {
                ((Jaxb2Marshaller) unmarshaller).setClassesToBeBound(jaxbClassesToBeBound);
            }

            if (!ObjectUtils.isEmpty(jaxbPackagesToScan)) {
                ((Jaxb2Marshaller) unmarshaller).setPackagesToScan(jaxbPackagesToScan);
            }
        }

        return unmarshaller;
    }

    public void setUnmarshaller(Unmarshaller unmarshaller) {
        this.unmarshaller = unmarshaller;
    }

    @Override
    public ResourceBeanMapper getResourceBeanMapper() throws ResourceException {
        if (resourceBeanMapper == null) {
            resourceBeanMapper = new JdomResourceBeanMapper(getUnmarshaller());
        }

        return resourceBeanMapper;
    }

    /**
     * Extract response data from the given {@code responseException} and set those to {@code exchangeHint}.
     * @param responseException a {@link RestClientResponseException} thrown by a {@link ResponseErrorHandler}.
     * @param exchangeHint exchange hint to hold any available response data on error
     */
    @Override
    protected void extractResponseDataToExchangeHint(final RestClientResponseException responseException,
            final ExchangeHint exchangeHint) {
        if (exchangeHint == null) {
            return;
        }

        try {
            exchangeHint.setResponseStatusCode(responseException.getRawStatusCode());

            final HttpHeaders responseHeaders = responseException.getResponseHeaders();
            exchangeHint.setResponseHeaders(responseHeaders);

            final byte[] responseBody = responseException.getResponseBodyAsByteArray();

            if (responseBody == null) {
                return;
            }

            exchangeHint.setResponseBody(responseBody);

            final MediaType contentType = responseHeaders.getContentType();

            if (contentType == null) {
                return;
            }

            if (MediaType.TEXT_XML.includes(contentType) || MediaType.APPLICATION_XML.includes(contentType)
                    || APPLICATION_ALL_XML.includes(contentType)) {
                final Element rootElem = byteArrayToElement(responseBody);
                final Resource errorInfoResource = new JdomResource(rootElem);
                exchangeHint.setResponseBody(errorInfoResource);
            }
        } catch (Exception e) {
            log.warn("Failed to extract response data from response exception.", e);
        }
    }

    protected Element byteArrayToElement(final byte[] body) throws JDOMException, IOException {
        try (InputStream input = new ByteArrayInputStream(body)) {
            return inputStreamToElement(input);
        }
    }

    protected Element byteArrayResourceToElement(final ByteArrayResource body) throws JDOMException, IOException {
        try (InputStream input = body.getInputStream()) {
            return inputStreamToElement(input);
        }
    }

    protected Element inputStreamToElement(final InputStream input) throws JDOMException, IOException {
        SAXBuilder jdomBuilder = new SAXBuilder();
        jdomBuilder.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        jdomBuilder.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        final Document document = jdomBuilder.build(input);
        final Element elem = document.getRootElement();
        return elem;
    }
}
