/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.validator;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ValidatorUtils {

    public static final String REQUIRED_VALIDATOR = "required";
    public static final String RESOURCE_REQUIRED_VALIDATOR = "resource-required";
    public static final String NON_EMPTY_VALIDATOR = "non-empty";
    
    public static final Set<String> REQUIRED_VALIDATORS = 
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(REQUIRED_VALIDATOR, RESOURCE_REQUIRED_VALIDATOR)));

    public static boolean hasRequiredValidator(final Set<String> validators) {
        return validators.stream().anyMatch(REQUIRED_VALIDATORS::contains);
    }
    
    private ValidatorUtils() {};
}
