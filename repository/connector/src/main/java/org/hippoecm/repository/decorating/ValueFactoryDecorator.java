/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.decorating;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;
import javax.jcr.Binary;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;

/**
 */
public class ValueFactoryDecorator extends AbstractDecorator implements ValueFactory {

    protected final ValueFactory valueFactory;

    protected ValueFactoryDecorator(DecoratorFactory factory, Session session, ValueFactory valueFactory) {
        super(factory, session);
        this.valueFactory = valueFactory;
    }

    /**
     * @inheritDoc
     */
    public Value createValue(String value) {
        return valueFactory.createValue(value);
    }

    /**
     * @inheritDoc
     */
    public Value createValue(String value, int type) throws ValueFormatException {
        return valueFactory.createValue(value, type);
    }

    /**
     * @inheritDoc
     */
    public Value createValue(long value) {
        return valueFactory.createValue(value);
    }

    /**
     * @inheritDoc
     */
    public Value createValue(double value) {
        return valueFactory.createValue(value);
    }

    /**
     * @inheritDoc
     */
    public Value createValue(boolean value) {
        return valueFactory.createValue(value);
    }

    /**
     * @inheritDoc
     */
    public Value createValue(Calendar value) {
        return valueFactory.createValue(value);
    }

    /**
     * @inheritDoc
     */
    public Value createValue(InputStream value) {
        return valueFactory.createValue(value);
    }

    /**
     * @inheritDoc
     */
    public Value createValue(Node value) throws RepositoryException {
        return valueFactory.createValue(NodeDecorator.unwrap(value));
    }

    public Value createValue(BigDecimal value) {
        return valueFactory.createValue(value);
    }

    public Value createValue(Binary value) {
        return valueFactory.createValue(value);
    }

    public Value createValue(Node value, boolean weak) throws RepositoryException {
        return valueFactory.createValue(value, weak);
    }

    public Binary createBinary(InputStream stream) throws RepositoryException {
        return valueFactory.createBinary(stream);
    }
}
