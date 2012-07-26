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
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Binary;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.OnParentVersionAction;
import javax.jcr.version.VersionException;

final public class UpdaterProperty extends UpdaterItem implements Property {

    boolean isWeakReference;
    Value value;
    Value[] values;
    ValueFactory valueFactory;

    UpdaterProperty(UpdaterSession session, UpdaterNode target) {
        super(session, target);
        this.valueFactory = session.valueFactory;
        this.isWeakReference = false;
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

        isWeakReference = "hippo:docbase".equals(origin.getName());

        register();
    }
    
    void register() throws RepositoryException {
        // if this is a facetselect/mirror/facetsearch docbase, then retarget
        // (first check is necessary for hippo namespace upgrade)
        if (isWeakReference || isStrongReference()) {
            if(values != null) {
                for(int i=0; i<values.length; i++) {
                    session.addReference(this, values[i].getString());
                }
            } else {
                session.addReference(this, value.getString());
            }
        }
    }

    void unregister() throws RepositoryException {
        if (isWeakReference || isStrongReference()) {
            if(values != null) {
                for(int i=0; i<values.length; i++) {
                    session.removeReference(this, values[i].getString());
                }
            } else {
                session.removeReference(this, value.getString());
            }
        }
    }
    
    boolean isStrongReference() throws RepositoryException {
        if (values != null) {
            if (values.length > 0) {
                return values[0].getType() == PropertyType.REFERENCE;
            }
        } else if (value != null) {
            return value.getType() == PropertyType.REFERENCE;
        }
        return false;
    }
    
    public boolean isMultiple() {
        if (values != null)
            return true;
        else
            return false;
    }

    void commit() throws RepositoryException {
        // new property has same parent as old one, e.g. nt:unstructured
        if (origin != null && origin.getParent().isSame(parent.origin)) {
            origin.remove();
        }

        String name = getName();

        boolean matchSingle = false;
        boolean matchMultiple = false;
        boolean exactMatch = false;
        Set<NodeType> nodeTypes = new HashSet<NodeType>();
        nodeTypes.add(((Node)parent.origin).getPrimaryNodeType());
        nodeTypes.addAll(Arrays.asList(((Node)parent.origin).getMixinNodeTypes()));
        for (NodeType nodeType : nodeTypes) {
            for (PropertyDefinition propDef : nodeType.getPropertyDefinitions()) {
                if (propDef.getName().equals(name)) {
                    if (propDef.isMultiple()) {
                        matchMultiple = true;
                        matchSingle = false;
                        if (values != null) {
                            exactMatch = true;
                            break;
                        }
                    } else {
                        matchSingle = true;
                        matchMultiple = false;
                        if (value != null) {
                            exactMatch = true;
                            break;
                        }
                    }
                } else if (propDef.getName().equals("*")) {
                    if (propDef.isMultiple()) {
                        if (values != null) {
                            matchMultiple = true;
                            matchSingle = false;
                        }
                    } else {
                        if (value != null) {
                            matchSingle = true;
                            matchMultiple = false;
                        }
                    }
                }
            }
            if (exactMatch) {
                break;
            }
        }
        if (matchSingle) {
            if (isMultiple()) {
                if (UpdaterEngine.log.isDebugEnabled()) {
                    UpdaterEngine.log.warn("commit set singlevalue from multivalue property " + name + " on " + getPath());
                }
                origin = ((Node)parent.origin).setProperty(name, values[0]);
            } else {
                if (UpdaterEngine.log.isDebugEnabled()) {
                    UpdaterEngine.log.debug("commit set singlevalue property " + name + " on " + getPath());
                }
                origin = ((Node)parent.origin).setProperty(name, value);
            }
        } else if (matchMultiple) {
            if (isMultiple()) {
                if (UpdaterEngine.log.isDebugEnabled()) {
                    UpdaterEngine.log.debug("commit set multivalue property " + name + " on " + getPath());
                }
                origin = ((Node)parent.origin).setProperty(name, values);
            } else {
                if (UpdaterEngine.log.isDebugEnabled()) {
                    UpdaterEngine.log.warn("commit set multivalue from singlevalue property " + name + " on " + getPath());
                }
                origin = ((Node)parent.origin).setProperty(name, new Value[] { value} );
            }
        } else {
            if (isMultiple()) {
                if (UpdaterEngine.log.isDebugEnabled()) {
                    UpdaterEngine.log.debug("commit set multivalue property " + name + " on " + getPath());
                }
                origin = ((Node)parent.origin).setProperty(name, values);
            } else {
                if (UpdaterEngine.log.isDebugEnabled()) {
                    UpdaterEngine.log.debug("commit set singlevalue property " + name + " on " + getPath());
                }
                origin = ((Node)parent.origin).setProperty(name, value);
            }
        }
    }

    @Override
    public void setName(String name) throws RepositoryException {
        super.setName(":" + name);
    }

    @Override
    public void remove() throws VersionException, LockException, ConstraintViolationException, RepositoryException {
        unregister();
        super.remove();
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
        unregister();
        if (value == null) {
            remove();
            return;
        }
        this.value = value;
        this.values = null;
        register();
    }

    public void setValue(Value[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        unregister();
        if (values == null) {
            remove();
            return;
        }
        this.value = null;
        this.values = values;
        register();
    }

    public void setValue(String value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        unregister();
        if (value == null) {
            remove();
            return;
        }
        this.value = valueFactory.createValue(value);
        this.values = null;
        register();
    }

    public void setValue(String[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        unregister();
        if (values == null) {
            remove();
            return;
        }
        this.values = new Value[values.length];
        for (int i = 0; i < values.length; i++)
            this.values[i] = valueFactory.createValue(values[i]);
        register();
    }

    public void setValue(InputStream value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        setValue(valueFactory.createValue(value));
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
        Value[] result = new Value[values.length];
        System.arraycopy(this.values, 0, result, 0, this.values.length);
        return result;
    }

    public String getString() throws ValueFormatException, RepositoryException {
        return getValue().getString();
    }

    @Deprecated
    public InputStream getStream() throws ValueFormatException, RepositoryException {
        return getValue().getStream();
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
        return new PropertyDefinition() {
            public int getRequiredType() {
                return PropertyType.UNDEFINED;
            }
            public String[] getValueConstraints() {
                return null;
            }
            public Value[] getDefaultValues() {
                return null;
            }
            public boolean isMultiple() {
                return UpdaterProperty.this.isMultiple();
            }
            public NodeType getDeclaringNodeType() {
                return null;
            }
            public String getName() {
                return "*";
            }
            public boolean isAutoCreated() {
                return false;
            }
            public boolean isMandatory() {
                return false;
            }
            public int getOnParentVersion() {
                return OnParentVersionAction.COMPUTE;
            }
            public boolean isProtected() {
                return false;
            }

            public String[] getAvailableQueryOperators() {
                throw new UpdaterException("illegal method");
            }

            public boolean isFullTextSearchable() {
                throw new UpdaterException("illegal method");
            }

            public boolean isQueryOrderable() {
                throw new UpdaterException("illegal method");
            }
        };
    }

    @Deprecated
    public int getType() throws RepositoryException {
        return PropertyType.UNDEFINED;
    }

    public void setValue(Binary value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public void setValue(BigDecimal value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public Binary getBinary() throws ValueFormatException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public BigDecimal getDecimal() throws ValueFormatException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public Property getProperty() throws ItemNotFoundException, ValueFormatException, RepositoryException {
        throw new UpdaterException("illegal method");
    }
}
