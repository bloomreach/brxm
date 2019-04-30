/*
 *  Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.services.validation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

import org.onehippo.cms.services.validation.api.internal.ValidatorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JcrValidatorConfig implements ValidatorConfig {

    private static final Logger log = LoggerFactory.getLogger(JcrValidatorConfig.class);
    static final String CLASS_NAME = "hipposys:className";

    private String name;
    private String className;
    private Map<String, String> properties;

    JcrValidatorConfig(final Node configNode) throws RepositoryException {
        reconfigure(configNode);
    }

    void reconfigure(final Node node) throws RepositoryException {
        if (!node.hasProperty(CLASS_NAME)) {
            throw new IllegalStateException("Node " + node.getPath() + " does not have required property '"+ CLASS_NAME + "'");
        }

        name = node.getName();
        className = node.getProperty(CLASS_NAME).getString();
        properties = Collections.unmodifiableMap(readProperties(node));
    }

    private static Map<String, String> readProperties(final Node node) throws RepositoryException {
        final Map<String, String> properties = new HashMap<>();

        final PropertyIterator it = node.getProperties();
        while (it.hasNext()) {
            final Property property = it.nextProperty();
            final String propertyName = property.getName();
            if (!propertyName.contains(":")) {
                if (property.isMultiple()) {
                    log.warn("Ignoring property '{}' of node '{}' because it is multiple", propertyName, node.getPath());
                } else {
                    properties.put(propertyName, property.getString());
                }
            }
        }

        return properties;
    }

    public String getName() {
        return name;
    }

    public String getClassName() {
        return className;
    }

    public Map<String, String> getProperties() {
        return properties;
    }
}
