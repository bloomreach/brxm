/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.repository.updater;

import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;

final public class UpdaterProperty extends UpdaterItem implements Property {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    boolean isMultiple;
    Value value;
    Value[] values;
    ValueFactory valueFactory;

    UpdaterProperty(UpdaterSession session, UpdaterNode target) {
        super(session, target);
        this.valueFactory = session.valueFactory;
    }

    UpdaterProperty(UpdaterSession session, Property origin, UpdaterNode target) throws RepositoryException {
        super(session, origin, target);
        this.valueFactory = session.valueFactory;
        if (origin.getDefinition().isMultiple()) {
            value = null;
            values = origin.getValues();
        } else {
            value = origin.getValue();
            values = null;
        }
    }

    public boolean isMultiple() {
        if (values != null)
            return true;
        else
            return false;
    }

    void commit() throws RepositoryException {
        if(origin != null) {
            if(values != null) {
                for(int i=0; i<values.length; i++) {
                    values[i] = session.retarget(values[i]);
                }
            } else {
                value = session.retarget(value);
            }
        }
    }

    @Override
    public void setName(String name) throws RepositoryException {
        super.setName(":" + name);
    }

    // javax.jcr.Item interface

    public boolean isNode() {
        return false;
    }

    @Deprecated
    public void accept(ItemVisitor visitor) throws RepositoryException {
        visitor.visit(this);
    }

    // javax.jcr.Property interface

    public void setValue(Value value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        if (value == null) {
            remove();
            return;
        }
        isMultiple = false;
        this.value = value;
        this.values = null;
    }

    public void setValue(Value[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        if (values == null) {
            remove();
            return;
        }
        isMultiple = true;
        this.value = null;
        this.values = values;
    }

    public void setValue(String value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        if (value == null) {
            remove();
            return;
        }
        isMultiple = false;
        this.value = valueFactory.createValue(value);
        this.values = null;
    }

    public void setValue(String[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        if (value == null) {
            remove();
            return;
        }
        isMultiple = true;
        this.values = new Value[values.length];
        for (int i = 0; i < values.length; i++)
            this.values[i] = valueFactory.createValue(values[i]);
    }

    @Deprecated
    public void setValue(InputStream value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public void setValue(long value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        setValue(valueFactory.createValue(value));
    }

    public void setValue(double value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        setValue(valueFactory.createValue(value));
    }

    public void setValue(Calendar value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        setValue(valueFactory.createValue(value));
    }

    public void setValue(boolean value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        setValue(valueFactory.createValue(value));
    }

    public void setValue(Node value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        setValue(valueFactory.createValue(value));
    }

    public Value getValue() throws ValueFormatException, RepositoryException {
        if (value == null)
            throw new ValueFormatException();
        return value;
    }

    public Value[] getValues() throws ValueFormatException, RepositoryException {
        if (values == null)
            throw new ValueFormatException();
        return values;
    }

    public String getString() throws ValueFormatException, RepositoryException {
        return getValue().getString();
    }

    @Deprecated
    public InputStream getStream() throws ValueFormatException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public long getLong() throws ValueFormatException, RepositoryException {
        return getValue().getLong();
    }

    public double getDouble() throws ValueFormatException, RepositoryException {
        return getValue().getDouble();
    }

    public Calendar getDate() throws ValueFormatException, RepositoryException {
        return getValue().getDate();
    }

    public boolean getBoolean() throws ValueFormatException, RepositoryException {
        return getValue().getBoolean();
    }

    public Node getNode() throws ValueFormatException, RepositoryException {
        return origin.getSession().getNodeByUUID(getValue().getString());
    }

    @Deprecated
    public long getLength() throws ValueFormatException, RepositoryException {
        return -1;
    }

    @Deprecated
    public long[] getLengths() throws ValueFormatException, RepositoryException {
        long[] lengths = new long[getValues().length];
        for (int i = 0; i < lengths.length; i++)
            lengths[i] = -1;
        return lengths;
    }

    @Deprecated
    public PropertyDefinition getDefinition() throws RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public int getType() throws RepositoryException {
        throw new UpdaterException("illegal method");
    }
}
