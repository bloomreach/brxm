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
package org.onehippo.cms.services.validation.legacy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * This utility is created to support the pre-13.3.0 configuration of document field validators.
 *
 * @deprecated This class has only a function for 13.3.0 versions and higher and will be removed
 * in a next major version.
 */
@Deprecated
public class LegacyValidatorMapper {

    private static final Set<String> combination = new HashSet<>(Arrays.asList("non-empty", "required"));

    /**
     * Map a legacy validator to one that works correctly with the current implementation if necessary.
     */
    public static Set<String> legacyMapper(final Set<String> legacyValidators, final String fieldType) {
        if (legacyValidators == null) {
            return null;
        }
        return new LinkedHashSet<>(legacyMapper(new ArrayList<>(legacyValidators), fieldType));
    }

    public static List<String> legacyMapper(final List<String> legacyValidators, final String fieldType) {
        if (legacyValidators == null) {
            return null;
        }

        if (legacyValidators.isEmpty()) {
            return Collections.emptyList();
        }

        final ArrayList<String> validators = new ArrayList<>(legacyValidators);

        if (validators.containsAll(combination)) {
            validators.remove("non-empty");
        }

        replace(validators, "resource-required", "required");
        replace(validators, "html", "non-empty-html");

        if ("html".equalsIgnoreCase(fieldType)) {
            replace(validators, "non-empty", "non-empty-html");
        }

        return validators;
    }

    private static void replace(final List<String> list, final String oldValue, final String newValue) {
        for (int i = 0; i < list.size(); i++) {
            final String value = list.get(i);
            if (value.equals(oldValue)) {
                list.set(i, newValue);
            }
        }
    }

    private LegacyValidatorMapper() {
    }
}
