/*
 *  Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.services.validation;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.onehippo.cms.services.validation.api.internal.ValidatorConfig;

public class JcrValidatorConfig implements ValidatorConfig {

    static final String CLASS_NAME = "hipposys:className";

    private final String name;
    private final String className;
    private final Node configNode;

    public JcrValidatorConfig(final Node node) throws RepositoryException {
        if (!node.hasProperty(CLASS_NAME)) {
            throw new IllegalStateException("Node " + node.getPath() + " does not have required property '"+ CLASS_NAME + "'");
        }

        name = node.getName();
        className = node.getProperty(CLASS_NAME).getString();
        configNode = node;
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
        return configNode;
    }
}
