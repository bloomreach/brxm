/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.pagecomposer.jaxrs.services.validators;

import java.util.ArrayList;
import java.util.List;

import org.hippoecm.hst.core.request.HstRequestContext;

public class ValidatorBuilder {

    private final List<Validator> validators = new ArrayList<>();

    public static ValidatorBuilder builder() {
        return new ValidatorBuilder();
    }

    public ValidatorBuilder add(Validator validator) {
        validators.add(validator);
        return this;
    }

    public Validator build() {
        return new Validator() {
            @Override
            public void validate(HstRequestContext requestContext) throws RuntimeException {
                for (Validator each : validators) {
                    each.validate(requestContext);
                }
            }
        };
    }
}
