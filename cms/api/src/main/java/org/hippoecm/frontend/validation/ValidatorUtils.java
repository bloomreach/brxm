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
package org.hippoecm.frontend.validation;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidatorUtils {

    private static final Logger log = LoggerFactory.getLogger(ValidatorUtils.class);

    public static final String NON_EMPTY_VALIDATOR = "non-empty";
    public static final String OPTIONAL_VALIDATOR = "optional";
    public static final String REQUIRED_VALIDATOR = "required";
    public static final String RESOURCE_REQUIRED_VALIDATOR = "resource-required";

    public static final Set<String> REQUIRED_VALIDATORS =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(REQUIRED_VALIDATOR, RESOURCE_REQUIRED_VALIDATOR)));

    public static boolean hasRequiredValidator(final Set<String> validators) {
        return validators.stream().anyMatch(REQUIRED_VALIDATORS::contains);
    }

    public static ValidationScope getValidationScope(final String scope) {
        try {
            if (StringUtils.isNotBlank(scope)) {
                return ValidationScope.valueOf(scope.toUpperCase());
            }
        } catch (IllegalArgumentException e) {
            if (log.isWarnEnabled()) {
                log.warn("Invalid scope '{}'. Must be one of {}. Using DOCUMENT scope as default.", scope, 
                        StringUtils.join(ValidationScope.values(), ", "), e);
            }
        }
        return ValidationScope.DOCUMENT;
    }

    private ValidatorUtils() {}
}
