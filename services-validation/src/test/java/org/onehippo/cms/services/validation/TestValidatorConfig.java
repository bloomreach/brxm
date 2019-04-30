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

package org.onehippo.cms.services.validation;

import java.util.Collections;
import java.util.Map;

import org.onehippo.cms.services.validation.api.internal.ValidatorConfig;

public class TestValidatorConfig implements ValidatorConfig {

    private String name;
    private String className;
    private Map<String, String> properties;

    TestValidatorConfig(final String name, final String className) {
        this(name, className, Collections.emptyMap());
    }

    TestValidatorConfig(final String name, final String className, final Map<String, String> properties) {
        this.name = name;
        this.className = className;
        this.properties = properties;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }

}
