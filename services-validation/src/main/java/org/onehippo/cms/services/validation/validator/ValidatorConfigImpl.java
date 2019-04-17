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
package org.onehippo.cms.services.validation.validator;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

import org.onehippo.cms.services.validation.api.ValidatorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidatorConfigImpl implements ValidatorConfig {

    private static final Logger log = LoggerFactory.getLogger(ValidatorConfigImpl.class);

    private String name;
    private String className;
    private final Map<String, String> properties = new HashMap<>();

    public ValidatorConfigImpl(final Node configNode) throws RepositoryException {
        reconfigure(configNode);
    }

    @Override
    public void reconfigure(final Node node) throws RepositoryException {
        if (!node.hasProperty(CLASS_NAME)) {
            throw new IllegalStateException("Required property '"+ CLASS_NAME + "' is not found.");
        }

        name = node.getName();
        className = node.getProperty(CLASS_NAME).getString();

        properties.clear();
        final PropertyIterator it = node.getProperties();
        while (it.hasNext()) {
            final Property property = it.nextProperty();
            final String propertyName = property.getName();
            if (!propertyName.contains(":")) {
                if (property.isMultiple()) {
                    log.warn("Property '"+ propertyName + " is multiple which is not supported in the validator configuration.");
                } else {
                    properties.put(propertyName, property.getString());
                }
            }
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public boolean hasProperty(final String name) {
        return properties.containsKey(name);
    }

    @Override
    public String getProperty(final String name) {
        return properties.get(name);
    }

}
