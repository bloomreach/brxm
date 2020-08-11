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
package org.onehippo.cms.services.validation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.onehippo.cms.services.validation.api.Validator;
import org.onehippo.cms.services.validation.api.internal.ValidatorInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ValidationServiceConfig {

    private static final Logger log = LoggerFactory.getLogger(ValidationServiceConfig.class);

    private final Map<String, ValidatorInstance> validatorInstances = new ConcurrentHashMap<>();

    ValidationServiceConfig(final Node configNode) {
        reconfigure(configNode);
    }

    void reconfigure(final Node config) {
        try {
            createValidators(config);
        } catch (final RepositoryException e) {
            log.error("Failed to reconfigure validator service", e);
        }
    }

    private void createValidators(final Node config) throws RepositoryException {
        validatorInstances.clear();

        final NodeIterator iterator = config.getNodes();
        while (iterator.hasNext()) {
            final Node validatorConfigNode = iterator.nextNode();
            final JcrValidatorConfig validatorConfig = new JcrValidatorConfig(validatorConfigNode);
            final String validatorName = validatorConfig.getName();
            validatorInstances.computeIfAbsent(validatorName,
                    name -> ValidatorInstanceFactory.createValidatorInstance(validatorConfig));
        }
    }

    /**
     * Returns an instance of a {@link Validator}, or null if the configuration cannot be found
     * @param name The validator name
     * @return Instance of a {@link Validator}
     */
    ValidatorInstance getValidatorInstance(final String name) {
        return validatorInstances.get(name);
    }

}
