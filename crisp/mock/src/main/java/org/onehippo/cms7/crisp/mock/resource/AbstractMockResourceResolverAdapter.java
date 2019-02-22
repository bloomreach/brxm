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
package org.onehippo.cms7.crisp.mock.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import org.onehippo.cms7.crisp.api.exchange.ExchangeHint;
import org.onehippo.cms7.crisp.api.resource.AbstractResourceResolver;
import org.onehippo.cms7.crisp.api.resource.Binary;
import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceBeanMapper;
import org.onehippo.cms7.crisp.api.resource.ResourceException;
import org.onehippo.cms7.crisp.api.resource.ResourceResolver;

/**
 * Abstract base {@link ResourceResolver} adapter class.
 */
public abstract class AbstractMockResourceResolverAdapter extends AbstractResourceResolver {

    private ResourceBeanMapper resourceBeanMapper;

    @Override
    public Resource resolve(String absPath, Map<String, Object> pathVariables, ExchangeHint exchangeHint)
            throws ResourceException {
        return null;
    }

    @Override
    public Binary resolveBinary(String absPath, Map<String, Object> pathVariables, ExchangeHint exchangeHint)
            throws ResourceException {
        return null;
    }

    @Override
    public Resource findResources(String baseAbsPath, Map<String, Object> pathVariables, ExchangeHint exchangeHint)
            throws ResourceException {
        return null;
    }

    @Override
    public ResourceBeanMapper getResourceBeanMapper() throws ResourceException {
        return resourceBeanMapper;
    }

    public void setResourceBeanMapper(ResourceBeanMapper resourceBeanMapper) {
        this.resourceBeanMapper = resourceBeanMapper;
    }

    /**
     * Read the data from the {@code url} and convert the data to a {@link Resource} object.
     * @param url url containing the data
     * @return the data from the {@code url} and convert the data to a {@link Resource} object
     * @throws ResourceException if any error occurs
     */
    protected Resource urlToResource(final URL url) throws ResourceException {
        try (InputStream input = url.openStream()) {
            return inputToResource(input);
        } catch (IOException e) {
            throw new ResourceException("Failed to read resource.", e);
        }
    }

    /**
     * Read the data from the {@code file} and convert the data to a {@link Resource} object.
     * @param file file containing the data
     * @return the data from the {@code file} and convert the data to a {@link Resource} object
     * @throws ResourceException if any error occurs
     */
    protected Resource fileToResource(final File file) throws ResourceException {
        try (InputStream input = new FileInputStream(file)) {
            return inputToResource(input);
        } catch (IOException e) {
            throw new ResourceException("Failed to read resource.", e);
        }
    }

    /**
     * Read the data from the {@code inputStream} and convert the data to a {@link Resource} object.
     * @param inputStream the input stream containing the data
     * @return the data from the {@code inputStream} and convert the data to a {@link Resource} object
     * @throws ResourceException if any error occurs
     */
    protected abstract Resource inputToResource(final InputStream inputStream) throws IOException;

}
