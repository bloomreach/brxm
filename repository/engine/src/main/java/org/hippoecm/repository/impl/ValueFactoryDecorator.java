/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.impl;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;
import javax.jcr.Binary;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;

public class ValueFactoryDecorator extends SessionBoundDecorator implements ValueFactory {

    protected final ValueFactory valueFactory;

    protected ValueFactoryDecorator(SessionDecorator session, ValueFactory valueFactory) {
        super(session);
        this.valueFactory = valueFactory;
    }

    public Value createValue(final String value) {
        return valueFactory.createValue(value);
    }

    public Value createValue(final String value, final int type) throws ValueFormatException {
        return valueFactory.createValue(value, type);
    }

    public Value createValue(final long value) {
        return valueFactory.createValue(value);
    }

    public Value createValue(final double value) {
        return valueFactory.createValue(value);
    }

    public Value createValue(final boolean value) {
        return valueFactory.createValue(value);
    }

    public Value createValue(final Calendar value) {
        return valueFactory.createValue(value);
    }

    public Value createValue(final InputStream value) {
        return valueFactory.createValue(value);
    }

    public Value createValue(final Node value) throws RepositoryException {
        return valueFactory.createValue(NodeDecorator.unwrap(value));
    }

    public Value createValue(final BigDecimal value) {
        return valueFactory.createValue(value);
    }

    public Value createValue(final Binary value) {
        return valueFactory.createValue(value);
    }

    public Value createValue(final Node value, final boolean weak) throws RepositoryException {
        return valueFactory.createValue(NodeDecorator.unwrap(value), weak);
    }

    public Binary createBinary(final InputStream stream) throws RepositoryException {
        return valueFactory.createBinary(stream);
    }
}
