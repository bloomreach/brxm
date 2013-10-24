/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.mock;

import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NodeType;

/**
 * Mock implementation of {@link ItemDefinition} that only implements {@link #getName}.
 */
public class MockItemDefinition implements ItemDefinition {

    private final String name;

    public MockItemDefinition(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    // REMAINING METHODS ARE NOT IMPLEMENTED

    @Override
    public NodeType getDeclaringNodeType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAutoCreated() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isMandatory() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getOnParentVersion() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isProtected() {
        throw new UnsupportedOperationException();
    }

}
