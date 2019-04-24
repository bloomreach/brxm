/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms.services.validation;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.onehippo.cms.services.validation.api.Validator;
import org.onehippo.cms.services.validation.api.ValidatorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ValidatorFactory {

    private static final Logger log = LoggerFactory.getLogger(ValidatorFactory.class);

    private ValidatorFactory() {
    }

    static Validator createValidator(final ValidatorConfig config) {
        Validator validator = null;

        final String className = config.getClassName();

        try {
            validator = createValidatorWithProperties(className, config.getProperties());

            if (validator == null) {
                validator = createValidatorWithoutProperties(className);
            }

            if (validator == null) {
                log.error("Cannot create validator '{}': class '{}' does not have a public constructor"
                        + " with a single argument Map<String, String>, nor a public no-arguments constructor.",
                        config.getName(), className);
            }
        } catch (ClassNotFoundException e) {
            log.error("Cannot create validator '{}': failed to locate class '{}' on classpath.", config.getName(), className);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            log.error("Cannot create validator '{}': failed to instantiate class '{}'.", config.getName(), className, e);
        }

        return validator;
    }

    private static Validator createValidatorWithProperties(final String className, final Map<String, String> properties)
            throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, InstantiationException {
        try {
            final Constructor<?> constructor = Class.forName(className).getConstructor(Map.class);
            return (Validator) constructor.newInstance(properties);
        } catch (NoSuchMethodException e) {
            log.info("Class '{}' does not have a public constructor with single argument Map<String, String>", className, e);
        }

        return null;
    }

    private static Validator createValidatorWithoutProperties(final String className)
            throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, InstantiationException {
        try {
            final Constructor<?> constructor = Class.forName(className).getConstructor();
            return (Validator) constructor.newInstance();
        } catch (NoSuchMethodException e) {
            log.info("Class '{}' does not have a no-arguments constructor", className, e);
        }

        return null;
    }
}
