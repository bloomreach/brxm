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

package org.onehippo.cms.services.validation.api;

import java.util.Map;

public interface ViolationFactory {

    /**
     * Creates a translated violation, using the name of the current validator as the translation key.
     *
     * @return the violation
     */
    Violation createViolation();

    /**
     * Creates a translated violation, using the name of the current validator as the translation key. Variables in the
     * message can be replaced by parameters. For example in a message {@code "This message has a ${variable}"} the part
     * from "${" until "}" can be replaced by a parameter. The parameter map must contain the variable name as key and
     * the value as value.
     *
     * @param parameters a map of parameter names and values for variable substitution.
     * @return the violation
     */
    Violation createViolation(Map<String, String> parameters);

    /**
     * Creates a translated violation, using the name of the current validator plus the provided sub-key as the
     * translation key. The validator name and subkey must be concatenated using a {@code #} sign.
     *
     * @param subKey the additional key for the violation.
     * @return the violation
     */
    Violation createViolation(String subKey);

    /**
     * Creates a translated violation, using the name of the current validator plus the provided sub-key as the
     * translation key. The validator name and subkey must be concatenated using a {@code #} sign. Variables in the
     * message can be replaced by parameters. For example in a message {@code "This message has a ${variable}"} the part
     * from "${" until "}" can be replaced by a parameter. The parameter map must contain the variable name as key and
     * he value as value.
     *
     * @param subKey     the additional key for the violation.
     * @param parameters a map of parameter names and values for variable substitution.
     * @return the violation
     */
    Violation createViolation(String subKey, Map<String, String> parameters);

}
