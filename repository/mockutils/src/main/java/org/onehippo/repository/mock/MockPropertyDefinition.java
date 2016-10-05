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

import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

/**
 * Mock implementation of {@link PropertyDefinition} that only supports {@link #getName} and {@link #isMultiple}.
 */
public class MockPropertyDefinition extends MockItemDefinition implements PropertyDefinition {

    private final int requiredType;
    private final boolean multiple;

    private boolean fullTextSearchable = true;
    private boolean queryOrderable = true;

    /**
     * Constructor.
     * @deprecated Use {@link #MockPropertyDefinition(String, int, boolean)} instead.
     * @param name property name
     * @param isMultiple whether or not it is multiple
     */
    @Deprecated
    public MockPropertyDefinition(final String name, final boolean isMultiple) {
        this(name, PropertyType.STRING, isMultiple);
    }

    /**
     * Constructor.
     * @param name property name
     * @param requiredType required type
     * @param multiple whether or not it is multiple
     */
    public MockPropertyDefinition(final String name, final int requiredType, final boolean multiple) {
        super(name);
        this.requiredType = requiredType;
        this.multiple = multiple;
    }

    @Override
    public int getRequiredType() {
        return requiredType;
    }

    @Override
    public boolean isMultiple() {
        return multiple;
    }

    @Override
    public boolean isFullTextSearchable() {
        return fullTextSearchable;
    }

    public void setFullTextSearchable(boolean fullTextSearchable) {
        this.fullTextSearchable = fullTextSearchable;
    }

    @Override
    public boolean isQueryOrderable() {
        return queryOrderable;
    }

    public void setQueryOrderable(boolean queryOrderable) {
        this.queryOrderable = queryOrderable;
    }

    // REMAINING METHODS ARE NOT IMPLEMENTED

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

}
