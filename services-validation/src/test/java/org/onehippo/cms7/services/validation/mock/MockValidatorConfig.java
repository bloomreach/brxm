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
package org.onehippo.cms7.services.validation.mock;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.onehippo.cms7.services.validation.ValidatorConfig;

public class MockValidatorConfig implements ValidatorConfig {

    private final String name;

    public MockValidatorConfig(final String name) {
        this.name = name;
    }

    @Override
    public void reconfigure(final Node node) throws RepositoryException {
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getClassName() {
        return null;
    }

    @Override
    public boolean hasProperty(final String name) {
        return false;
    }

    @Override
    public String getProperty(final String name) {
        return null;
    }
}
