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

package org.onehippo.cms.channelmanager.content.documenttype.field.type;

import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;

import javax.jcr.Node;

import org.onehippo.cms.services.validation.api.FieldContext;
import org.onehippo.cms.services.validation.api.ValidationContextException;
import org.onehippo.cms.services.validation.api.Validator;
import org.onehippo.cms.services.validation.api.ValidatorConfig;
import org.onehippo.cms.services.validation.api.ValidatorInstance;
import org.onehippo.cms.services.validation.api.Violation;

public class TestValidatorInstance implements ValidatorInstance {

    private Validator<Object> validator;

    public TestValidatorInstance(final Validator<Object> validator) {
        this.validator = validator;
    }

    @Override
    public ValidatorConfig getConfig() {
        return null;
    }

    @Override
    public String getJcrName() {
        return null;
    }

    @Override
    public String getJcrType() {
        return null;
    }

    @Override
    public String getType() {
        return null;
    }

    @Override
    public Locale getLocale() {
        return null;
    }

    @Override
    public TimeZone getTimeZone() {
        return null;
    }

    @Override
    public Node getParentNode() {
        return null;
    }

    @Override
    public Optional<Violation> validate(final FieldContext context, final Object value) throws ValidationContextException {
        return validator.validate(this, value);
    }

    @Override
    public Violation createViolation() {
        return null;
    }

    @Override
    public Violation createViolation(final String subKey) {
        return null;
    }
}
