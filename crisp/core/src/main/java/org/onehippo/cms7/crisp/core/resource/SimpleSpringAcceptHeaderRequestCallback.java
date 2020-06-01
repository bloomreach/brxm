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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RequestCallback;

/**
 * Simple AcceptHeaderRequestCallback implementation, mostly forked from org.springframework.web.client.RestTemplate.AcceptHeaderRequestCallback,
 * only changing the following:
 * <ul>
 * <li>Adding logger.
 * <li>Adding messageConverters
 * </ul>
 */
class SimpleSpringAcceptHeaderRequestCallback implements RequestCallback {

    private static Logger logger = LoggerFactory.getLogger(SimpleSpringAcceptHeaderRequestCallback.class);

    private final Type responseType;
    private final List<HttpMessageConverter<?>> messageConverters;

    SimpleSpringAcceptHeaderRequestCallback(Type responseType, List<HttpMessageConverter<?>> messageConverters) {
        this.responseType = responseType;
        this.messageConverters = messageConverters;
    }

    @Override
    public void doWithRequest(ClientHttpRequest request) throws IOException {
        if (this.responseType != null) {
            Class<?> responseClass = null;
            if (this.responseType instanceof Class) {
                responseClass = (Class<?>) this.responseType;
            }
            List<MediaType> allSupportedMediaTypes = new ArrayList<MediaType>();
            for (HttpMessageConverter<?> converter : getMessageConverters()) {
                if (responseClass != null) {
                    if (converter.canRead(responseClass, null)) {
                        allSupportedMediaTypes.addAll(getSupportedMediaTypes(converter));
                    }
                }
                else if (converter instanceof GenericHttpMessageConverter) {
                    GenericHttpMessageConverter<?> genericConverter = (GenericHttpMessageConverter<?>) converter;
                    if (genericConverter.canRead(this.responseType, null, null)) {
                        allSupportedMediaTypes.addAll(getSupportedMediaTypes(converter));
                    }
                }
            }
            if (!allSupportedMediaTypes.isEmpty()) {
                MediaType.sortBySpecificity(allSupportedMediaTypes);
                if (logger.isDebugEnabled()) {
                    logger.debug("Setting request Accept header to " + allSupportedMediaTypes);
                }
                request.getHeaders().setAccept(allSupportedMediaTypes);
            }
        }
    }

    protected List<HttpMessageConverter<?>> getMessageConverters() {
        if (messageConverters == null) {
            return Collections.emptyList();
        }

        return messageConverters;
    }

    private List<MediaType> getSupportedMediaTypes(HttpMessageConverter<?> messageConverter) {
        List<MediaType> supportedMediaTypes = messageConverter.getSupportedMediaTypes();
        List<MediaType> result = new ArrayList<MediaType>(supportedMediaTypes.size());
        for (MediaType supportedMediaType : supportedMediaTypes) {
            if (supportedMediaType.getCharset() != null) {
                supportedMediaType =
                        new MediaType(supportedMediaType.getType(), supportedMediaType.getSubtype());
            }
            result.add(supportedMediaType);
        }
        return result;
    }

}
