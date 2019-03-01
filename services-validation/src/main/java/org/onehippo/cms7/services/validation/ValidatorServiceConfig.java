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
package org.onehippo.cms7.services.validation;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.onehippo.cms7.services.validation.validator.ValidatorConfigImpl;
import org.onehippo.cms7.services.validation.validator.ValidatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidatorServiceConfig implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(ValidatorServiceConfig.class);

    private final Map<String, Validator> validators = new HashMap<>();

    ValidatorServiceConfig(final Node configNode) {
        reconfigure(configNode);
    }

    void reconfigure(final Node config) {
        validators.clear();

        try {
            final NodeIterator iterator = config.getNodes();
            while (iterator.hasNext()) {
                final Node configNode = iterator.nextNode();
                final ValidatorConfig validatorConfig = new ValidatorConfigImpl(configNode);

                if (!validators.containsKey(validatorConfig.getName())) {
                    final Validator validator = ValidatorFactory.create(validatorConfig);
                    if (validator == null) {
                        log.error("Failed to create validator '" + validatorConfig.getName() + "'.");
                    } else {
                        validators.put(configNode.getName(), validator);
                    }
                }

            }
        } catch (final RepositoryException e) {
            log.error("Failed to create validator config.");
        }
    }

    /**
     * Returns instance of {@link Validator} or null if the configuration cannot be found
     * @param name The validator name
     * @return Instance of a {@link Validator}
     */
    Validator getValidator(final String name) {
        return validators.getOrDefault(name, null);
    }
}
