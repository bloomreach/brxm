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

import javax.jcr.Node;

import org.onehippo.cms.services.validation.api.internal.ValidatorConfig;

public class TestValidatorConfig implements ValidatorConfig {

    private final String name;
    private final String className;
    private final Node node;

    TestValidatorConfig(final String name, final String className) {
        this(name, className, null);
    }

    TestValidatorConfig(final String name, final String className, final Node node) {
        this.name = name;
        this.className = className;
        this.node = node;
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
    public Node getNode() {
        return node;
    }
}
