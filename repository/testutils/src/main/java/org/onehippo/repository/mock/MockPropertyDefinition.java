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

import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

/**
 * Mock implementation of {@link PropertyDefinition} that only supports {@link #getName} and {@link #isMultiple}.
 */
public class MockPropertyDefinition extends MockItemDefinition implements PropertyDefinition {

    private final boolean isMultiple;

    public MockPropertyDefinition(final String name, final boolean isMultiple) {
        super(name);
        this.isMultiple = isMultiple;
    }

    @Override
    public boolean isMultiple() {
        return isMultiple;
    }

    // REMAINING METHODS ARE NOT IMPLEMENTED

    @Override
    public int getRequiredType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getValueConstraints() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value[] getDefaultValues() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getAvailableQueryOperators() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isFullTextSearchable() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isQueryOrderable() {
        throw new UnsupportedOperationException();
    }

}
