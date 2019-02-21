/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.mock.broker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.onehippo.cms7.crisp.api.broker.AbstractResourceServiceBroker;
import org.onehippo.cms7.crisp.api.broker.ResourceServiceBroker;
import org.onehippo.cms7.crisp.api.exchange.ExchangeHint;
import org.onehippo.cms7.crisp.api.resource.Binary;
import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceBeanMapper;
import org.onehippo.cms7.crisp.api.resource.ResourceException;
import org.onehippo.cms7.crisp.core.resource.jackson.JacksonResource;
import org.onehippo.cms7.crisp.core.resource.jackson.JacksonResourceBeanMapper;
import org.onehippo.cms7.crisp.mock.module.MockCrispHstServices;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Simple mocking adapter class to provide a {@link ResourceServiceBroker} in unit tests to support mocking
 * JSON (Jackson) based backends.
 * <p>
 * For example, in a unit test, you can configure the default {@link ResourceServiceBroker} like the following:
 * <pre>
 * final URL exampleJsonFileUrl = getClass().getResource("example-json-output.json");
 *
 * MockCrispHstServices.setDefaultResourceServiceBroker(new MockJacksonResourceServiceBrokerAdapter() {
 *      &#64;Override
 *      public Resource findResources(String resourceSpace, String baseAbsPath, Map<String, Object> pathVariables,
 *              ExchangeHint exchangeHint) throws ResourceException {
 *         // return json output from local file to mock crisp broker...
 *         return urlToJacksonResource(exampleJsonFileUrl);
 *     }
 * });
 * </pre>
 * @see MockCrispHstServices
 */
public class MockJacksonResourceServiceBrokerAdapter extends AbstractResourceServiceBroker {

    private static ObjectMapper defaultObjectMapper = new ObjectMapper();
    private static ResourceBeanMapper defaultResourceBeanMapper = new JacksonResourceBeanMapper(defaultObjectMapper);

    private ObjectMapper objectMapper;
    private ResourceBeanMapper resourceBeanMapper;

    @Override
    public Resource resolve(String resourceSpace, String absPath, Map<String, Object> pathVariables,
            ExchangeHint exchangeHint) throws ResourceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Binary resolveBinary(String resourceSpace, String absPath, Map<String, Object> pathVariables,
            ExchangeHint exchangeHint) throws ResourceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Resource findResources(String resourceSpace, String baseAbsPath, Map<String, Object> pathVariables,
            ExchangeHint exchangeHint) throws ResourceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResourceBeanMapper getResourceBeanMapper(String resourceSpace) throws ResourceException {
        return getResourceBeanMapper();
    }

    public ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            return defaultObjectMapper;
        }

        return objectMapper;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ResourceBeanMapper getResourceBeanMapper() {
        if (resourceBeanMapper == null) {
            return defaultResourceBeanMapper;
        }

        return resourceBeanMapper;
    }

    public void setResourceBeanMapper(ResourceBeanMapper resourceBeanMapper) {
        this.resourceBeanMapper = resourceBeanMapper;
    }

    protected Resource urlToJacksonResource(final URL url) throws ResourceException {
        try (InputStream input = url.openStream()) {
            return inputToJacksonResource(input);
        } catch (IOException e) {
            throw new ResourceException("Failed to read resource.", e);
        }
    }

    protected Resource fileToJacksonResource(final File file) throws ResourceException {
        try (InputStream input = new FileInputStream(file)) {
            return inputToJacksonResource(input);
        } catch (IOException e) {
            throw new ResourceException("Failed to read resource.", e);
        }
    }

    private Resource inputToJacksonResource(final InputStream input) throws IOException {
        final JsonNode jsonNode = getObjectMapper().readTree(IOUtils.toString(input, "UTF-8"));
        return new JacksonResource(jsonNode);
    }
}
