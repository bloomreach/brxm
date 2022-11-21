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
package org.hippoecm.frontend.editor.validator.required;

import java.util.Optional;

import javax.jcr.Node;

import org.onehippo.cms.services.validation.api.ValidationContext;
import org.onehippo.cms.services.validation.api.Validator;
import org.onehippo.cms.services.validation.api.Violation;

/**
 * A required compound validates that there is at least one compound node.
 * This is useful for plugins like content-blocks, which can contain zero blocks.
 * <p/>
 * Required compound fields are not affected by this validator however, as the wicket editor ensures there is always at
 * least one compound node.
 */
public class RequiredCompoundValidator implements Validator<Node> {

    @Override
    public Optional<Violation> validate(final ValidationContext context, final Node value) {
        return value != null
                ? Optional.empty()
                : Optional.of(context.createViolation());
    }

}
