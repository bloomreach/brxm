/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hippoecm.repository.util.JcrCompactNodeTypeDefWriter;
import org.onehippo.cm.ResourceInputProvider;
import org.onehippo.cm.model.Source;

public class JcrResourceInputProvider implements ResourceInputProvider {

    private static final String CND_RESOURCE_PATH_PREFIX = "cnd:";

    private final Session session;

    public JcrResourceInputProvider(final Session session) {
        this.session = session;
    }

    @Override
    public boolean hasResource(final Source source, final String resourcePath) {
        throw new UnsupportedOperationException();
    }

    public static String createResourcePath(final Property property, final int valueIndex) throws RepositoryException {
        return property.getPath()+"["+valueIndex+"]";
    }

    public String createCndResourcePath(final String prefix) throws RepositoryException {
        return CND_RESOURCE_PATH_PREFIX + prefix;
    }

    @Override
    public InputStream getResourceInputStream(final Source source, final String resourcePath) throws IOException {
        if (resourcePath.startsWith(CND_RESOURCE_PATH_PREFIX)) {
            final String nsPrefix = resourcePath.substring(CND_RESOURCE_PATH_PREFIX.length());
            try {
                final String resource = JcrCompactNodeTypeDefWriter.compactNodeTypeDef(session.getWorkspace(), nsPrefix);
                return IOUtils.toInputStream(resource, StandardCharsets.UTF_8);
            } catch (RepositoryException e) {
                throw new IOException("Failed to export cnd for namespace prefix: "+nsPrefix, e);
            }
        }
        final String propertyPath = StringUtils.substringBeforeLast(resourcePath, "[");
        final int valueIndex = Integer.parseInt(resourcePath.substring(propertyPath.length()+1, resourcePath.length()-1));
        try {
            Property property = session.getProperty(propertyPath);
            if (!property.isMultiple()) {
                if (valueIndex == 0) {
                    return property.getValue().getBinary().getStream();
                } else {
                    throw new IOException(String.format("Invalid JCR property %s value index %s: property is single value",
                            propertyPath, valueIndex));
                }
            } else {
                Value[] values = property.getValues();
                if (valueIndex >= 0 && valueIndex < values.length) {
                    return values[valueIndex].getBinary().getStream();
                } else {
                    throw new IOException(String.format("Invalid JCR property %s value index %s.",
                            propertyPath, valueIndex));
                }
            }
        } catch (RepositoryException e) {
            throw new IOException("Failed to retrieve property value at: "+propertyPath, e);
        }
    }

    @Override
    public Path getResourcePath(final Source source, final String resourcePath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getResourceModulePath(final Source source, final String resourcePath) {
        throw new UnsupportedOperationException();
    }
}
