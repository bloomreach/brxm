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
package org.hippoecm.frontend.plugins.richtext.validation;

import java.util.Optional;

import org.onehippo.cms.services.validation.api.ValidationContext;
import org.onehippo.cms.services.validation.api.ValidationContextException;
import org.onehippo.cms.services.validation.api.Validator;
import org.onehippo.cms.services.validation.api.Violation;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.htmlprocessor.HtmlProcessorService;

public class RequiredFormattedTextValidator implements Validator<String> {

    private final HtmlProcessorService htmlProcessorService;

    // constructor must be public because class is instantiated via reflection
    @SuppressWarnings("WeakerAccess")
    public RequiredFormattedTextValidator() {
        htmlProcessorService = HippoServiceRegistry.getService(HtmlProcessorService.class);

        if (htmlProcessorService == null) {
            throw new ValidationContextException("Failed to get HtmlProcessorService, cannot check visibility of HTML input");
        }
    }

    @Override
    public Optional<Violation> validate(final ValidationContext context, final String html) {

        if (!htmlProcessorService.isVisible(html)) {
            return Optional.of(context.createViolation());
        }

        return Optional.empty();
    }
}
