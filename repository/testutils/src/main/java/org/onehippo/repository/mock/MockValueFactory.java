/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;

/**
 * Mocked value factory. It only supports {@link #createBinary(java.io.InputStream)}.
 */
public class MockValueFactory implements ValueFactory {

    @Override
    public Binary createBinary(final InputStream stream) throws RepositoryException {
        try {
            return new MockBinary(stream);
        } catch (IOException e) {
            throw new RepositoryException("Cannot create mock binary", e);
        }
    }

    // REMAINING METHODS ARE NOT IMPLEMENTED

    @Override
    public Value createValue(final String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value createValue(final String value, final int type) throws ValueFormatException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value createValue(final long value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value createValue(final double value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value createValue(final BigDecimal value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value createValue(final boolean value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value createValue(final Calendar value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value createValue(final InputStream value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value createValue(final Binary value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value createValue(final Node value) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value createValue(final Node value, final boolean weak) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

}

