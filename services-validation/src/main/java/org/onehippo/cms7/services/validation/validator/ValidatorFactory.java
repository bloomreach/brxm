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
package org.onehippo.cms7.services.validation.validator;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.onehippo.cms7.services.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface ValidatorFactory extends Serializable {

    Logger log = LoggerFactory.getLogger(ValidatorFactory.class);

    static Validator create(final AbstractValidatorConfig config) {
        final String className = config.getClassName();
        try {
            final Constructor<?> constructor = Class.forName(className).getConstructor(AbstractValidatorConfig.class);
            return (Validator) constructor.newInstance(config);
        } catch (ClassNotFoundException e) {
            log.error("Failed to locate class '{}' on classpath", className);
        } catch (NoSuchMethodException e) {
            log.error("Class '{}' is missing a constructor with single argument ValidatorConfig)", className);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            log.error("Error instantiating class '{}'", className, e);
        }
        return null;
    }
}
