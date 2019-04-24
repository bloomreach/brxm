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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.onehippo.cms.services.validation.api.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;

class ValidationServiceConfig {

    private static final Logger log = LoggerFactory.getLogger(ValidationServiceConfig.class);

    private final BiMap<String, Validator> validators = Maps.synchronizedBiMap(HashBiMap.create());

    ValidationServiceConfig(final Node configNode) {
        reconfigure(configNode);
    }

    void reconfigure(final Node config) {
        validators.clear();

        try {
            final NodeIterator iterator = config.getNodes();
            while (iterator.hasNext()) {
                final Node configNode = iterator.nextNode();
                final JcrValidatorConfig validatorConfig = new JcrValidatorConfig(configNode);
                final String validatorName = validatorConfig.getName();
                validators.computeIfAbsent(validatorName, name -> ValidatorFactory.createValidator(validatorConfig));
            }
        } catch (final RepositoryException e) {
            log.error("Failed to reconfigure validator service", e);
        }
    }

    /**
     * Returns an instance of a {@link Validator}, or null if the configuration cannot be found
     * @param name The validator name
     * @return Instance of a {@link Validator}
     */
    Validator getValidator(final String name) {
        return validators.get(name);
    }

    /**
     * Returns the name of a {@link Validator}, or null if the validator cannot be found
     * @param validator The validator instance
     * @return Name of the validator
     */
    String getValidatorName(final Validator validator) {
        return validators.inverse().get(validator);
    }
}
