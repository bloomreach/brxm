/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestClientException;

/**
 * Simple HttpEntityRequestCallback implementation, mostly forked from org.springframework.web.client.RestTemplate.HttpEntityRequestCallback,
 * only changing the following:
 * <ul>
 * <li>Adding logger.
 * <li>Adding messageConverters
 * </ul>
 */
class SimpleSpringHttpEntityRequestCallback extends SimpleSpringAcceptHeaderRequestCallback {

    private static Logger logger = LoggerFactory.getLogger(SimpleSpringHttpEntityRequestCallback.class);

    private final HttpEntity<?> requestEntity;

    SimpleSpringHttpEntityRequestCallback(Object requestBody, List<HttpMessageConverter<?>> messageConverters) {
        this(requestBody, messageConverters, null);
    }

    SimpleSpringHttpEntityRequestCallback(Object requestBody, List<HttpMessageConverter<?>> messageConverters, Type responseType) {
        super(responseType, messageConverters);
        if (requestBody instanceof HttpEntity) {
            this.requestEntity = (HttpEntity<?>) requestBody;
        }
        else if (requestBody != null) {
            this.requestEntity = new HttpEntity<Object>(requestBody);
        }
        else {
            this.requestEntity = HttpEntity.EMPTY;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void doWithRequest(ClientHttpRequest httpRequest) throws IOException {
        super.doWithRequest(httpRequest);
        if (!this.requestEntity.hasBody()) {
            HttpHeaders httpHeaders = httpRequest.getHeaders();
            HttpHeaders requestHeaders = this.requestEntity.getHeaders();
            if (!requestHeaders.isEmpty()) {
                httpHeaders.putAll(requestHeaders);
            }
            if (httpHeaders.getContentLength() < 0) {
                httpHeaders.setContentLength(0L);
            }
        }
        else {
            Object requestBody = this.requestEntity.getBody();
            Class<?> requestType = requestBody.getClass();
            HttpHeaders requestHeaders = this.requestEntity.getHeaders();
            MediaType requestContentType = requestHeaders.getContentType();
            for (HttpMessageConverter<?> messageConverter : getMessageConverters()) {
                if (messageConverter.canWrite(requestType, requestContentType)) {
                    if (!requestHeaders.isEmpty()) {
                        httpRequest.getHeaders().putAll(requestHeaders);
                    }
                    if (logger.isDebugEnabled()) {
                        if (requestContentType != null) {
                            logger.debug("Writing [" + requestBody + "] as \"" + requestContentType +
                                    "\" using [" + messageConverter + "]");
                        }
                        else {
                            logger.debug("Writing [" + requestBody + "] using [" + messageConverter + "]");
                        }

                    }
                    ((HttpMessageConverter<Object>) messageConverter).write(
                            requestBody, requestContentType, httpRequest);
                    return;
                }
            }
            String message = "Could not write request: no suitable HttpMessageConverter found for request type [" +
                    requestType.getName() + "]";
            if (requestContentType != null) {
                message += " and content type [" + requestContentType + "]";
            }
            throw new RestClientException(message);
        }
    }

}
