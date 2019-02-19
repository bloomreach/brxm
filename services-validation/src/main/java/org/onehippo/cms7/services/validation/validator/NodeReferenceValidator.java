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

import org.apache.commons.lang.StringUtils;
import org.onehippo.cms7.services.validation.exception.InvalidValidatorException;
import org.onehippo.cms7.services.validation.exception.ValidatorException;
import org.onehippo.cms7.services.validation.field.FieldContext;

/**
 * Validator that validates if the value is null, empty or points to the default empty_node, currently the JCR root
 * node.
 */
public class NodeReferenceValidator extends AbstractFieldValidator<String> {

    private static final String ROOT_NODE_UUID = "cafebabe-cafe-babe-cafe-babecafebabe";

    public NodeReferenceValidator(final AbstractValidatorConfig config) {
        super(config);
    }

    @Override
    public void init(final FieldContext context) throws InvalidValidatorException {
        if (!"String".equals(context.getType())) {
            throw new InvalidValidatorException("Invalid validation exception; cannot validate non-string field for " +
                    "emptiness");
        }
    }

    @Override
    public boolean isValid(final FieldContext context, final String value) throws ValidatorException {
        return StringUtils.isNotBlank(value) && !value.equals(ROOT_NODE_UUID);
    }

}
