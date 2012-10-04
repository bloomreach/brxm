/*
 *  Copyright 2012 Hippo.
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
package org.onehippo.repository.mock;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;

import javax.jcr.Binary;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

/**
 * Mock version of a {@link Value}. It only supports type 'string'.
 */
public class MockValue implements Value {


    private String value;

    @SuppressWarnings("unused")
    public MockValue() {
        // used by JAXB
    }

    public MockValue(String value) {
        this.value = value;
    }

    public MockValue(Value value) throws RepositoryException {
        this.value = value.getString();
    }

    @Override
    public String getString() {
        return value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof MockValue) {
            MockValue other = (MockValue)o;
            return value.equals(other.value);
        }
        return false;
    }

    @Override
    public InputStream getStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Binary getBinary() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLong() {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getDouble() {
        throw new UnsupportedOperationException();
    }

    @Override
    public BigDecimal getDecimal() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Calendar getDate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean getBoolean() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getType() {
        return PropertyType.STRING;
    }

}
