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
package org.onehippo.cms.services.validation.validator;

import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.onehippo.cms.services.validation.api.ValidationContext;
import org.onehippo.cms.services.validation.api.Validator;
import org.onehippo.cms.services.validation.api.Violation;
import org.onehippo.repository.util.JcrConstants;

/**
 * Validator that validates if the value is null, empty or points to the default empty_node, currently the JCR root
 * node.
 */
public class NodeReferenceValidator implements Validator<String> {

    @Override
    public Optional<Violation> validate(final ValidationContext context, final String value) {
        if (StringUtils.isBlank(value) || value.equals(JcrConstants.ROOT_NODE_ID)) {
            return Optional.of(context.createViolation());
        }

        return Optional.empty();
    }
}
