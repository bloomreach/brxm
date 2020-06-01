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
import javax.jcr.ItemNotFoundException;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;

public class PropertyDecorator extends ItemDecorator implements Property {

    protected final Property property;

    public static Property unwrap(final Property property) {
        if (property instanceof PropertyDecorator) {
            return ((PropertyDecorator)property).property;
        }
        return property;
    }

    PropertyDecorator(final SessionDecorator session, Property property) {
        super(session, property);
        this.property = unwrap(property);
    }

    public void setValue(final Value value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        property.setValue(value);
    }

    public void setValue(final Value[] values) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        property.setValue(values);
    }

    public void setValue(final String s) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        property.setValue(s);
    }

    public void setValue(final String[] strings) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        property.setValue(strings);
    }

    public void setValue(final InputStream inputStream) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        property.setValue(inputStream);
    }

    public void setValue(final long l) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        property.setValue(l);
    }

    public void setValue(final double v) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        property.setValue(v);
    }

    public void setValue(final Calendar calendar) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        property.setValue(calendar);
    }

    public void setValue(final boolean b) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        property.setValue(b);
    }

    public void setValue(final Node node) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        property.setValue(NodeDecorator.unwrap(node));
    }

    public Value getValue() throws ValueFormatException, RepositoryException {
        return property.getValue();
    }

    public Value[] getValues() throws ValueFormatException, RepositoryException {
        return property.getValues();
    }

    public String getString() throws ValueFormatException, RepositoryException {
        return property.getString();
    }

    public InputStream getStream() throws ValueFormatException, RepositoryException {
        return property.getStream();
    }

    public long getLong() throws ValueFormatException, RepositoryException {
        return property.getLong();
    }

    public double getDouble() throws ValueFormatException, RepositoryException {
        return property.getDouble();
    }

    public Calendar getDate() throws ValueFormatException, RepositoryException {
        return property.getDate();
    }

    public boolean getBoolean() throws ValueFormatException, RepositoryException {
        return property.getBoolean();
    }

    public NodeDecorator getNode() throws ValueFormatException, RepositoryException {
        return NodeDecorator.newNodeDecorator(session, property.getNode());
    }

    public long getLength() throws ValueFormatException, RepositoryException {
        return property.getLength();
    }

    public long[] getLengths() throws ValueFormatException, RepositoryException {
        return property.getLengths();
    }

    public PropertyDefinition getDefinition() throws RepositoryException {
        return property.getDefinition();
    }

    public int getType() throws RepositoryException {
        return property.getType();
    }

    public void setValue(final Binary value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        property.setValue(value);
    }

    public void setValue(final BigDecimal value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        property.setValue(value);
    }

    public Binary getBinary() throws ValueFormatException, RepositoryException {
        return property.getBinary();
    }

    public BigDecimal getDecimal() throws ValueFormatException, RepositoryException {
        return property.getDecimal();
    }

    public PropertyDecorator getProperty() throws ItemNotFoundException, ValueFormatException, RepositoryException {
        return new PropertyDecorator(session, property.getProperty());
    }

    public boolean isMultiple() throws RepositoryException {
        return property.isMultiple();
    }
}
