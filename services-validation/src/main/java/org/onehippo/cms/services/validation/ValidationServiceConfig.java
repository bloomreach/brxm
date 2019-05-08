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
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.contenttype.ContentTypeService;
import org.onehippo.cms7.services.contenttype.EffectiveNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ValidationServiceConfig {

    private static final Logger log = LoggerFactory.getLogger(ValidationServiceConfig.class);

    private final Map<String, ValidatorInstance> validatorInstances = new ConcurrentHashMap<>();
    private final Map<String, ValidatorInstance> requiredValidatorInstances = new ConcurrentHashMap<>();
    private final ContentTypeService contentTypeService;

    ValidationServiceConfig(final Node configNode) {
        reconfigure(configNode);

        contentTypeService = HippoServiceRegistry.getService(ContentTypeService.class);
    }

    void reconfigure(final Node config) {
        try {
            createValidators(config, "validators", validatorInstances);
            createValidators(config, "requiredValidators", requiredValidatorInstances);
        } catch (final RepositoryException e) {
            log.error("Failed to reconfigure validator service", e);
        }
    }

    private static void createValidators(final Node config,
                                         final String validatorsName,
                                         final Map<String, ValidatorInstance> instances) throws RepositoryException {
        instances.clear();

        final Node validators = config.getNode(validatorsName);
        final NodeIterator iterator = validators.getNodes();
        while (iterator.hasNext()) {
            final Node validatorConfigNode = iterator.nextNode();
            final JcrValidatorConfig validatorConfig = new JcrValidatorConfig(validatorConfigNode);
            final String validatorName = validatorConfig.getName();
            instances.computeIfAbsent(validatorName,
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

    /**
     * Returns an instance of a required {@link Validator}, or null if the configuration cannot be found.
     * @param type The type of the field
     * @return Instance of a {@link Validator}
     */
    ValidatorInstance getRequiredValidatorInstance(final String type) {
        final ValidatorInstance requiredValidator = requiredValidatorInstances.get(type);

        if (requiredValidator != null) {
            return requiredValidator;
        }

        try {
            return getRequiredValidatorForSuperType(type);
        } catch (RepositoryException e) {
            log.warn("Could not find required validator for type '{}'", type, e);
            return null;
        }
    }

    private ValidatorInstance getRequiredValidatorForSuperType(final String type) throws RepositoryException {
        final EffectiveNodeType effectiveNodeType = contentTypeService.getEffectiveNodeTypes().getType(type);

        for (String superType : effectiveNodeType.getSuperTypes()) {
            final ValidatorInstance requiredValidator = getValidatorInstance(superType);
            if (requiredValidator != null) {
                return requiredValidator;
            }
        }

        return null;
    }
}
