/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.repository.ocm;

import java.lang.reflect.InvocationTargetException;
import java.util.Calendar;
import java.util.Date;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.JcrConstants;
import org.datanucleus.StateManager;
import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.identity.OIDImpl;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.state.StateManagerFactory;
import org.datanucleus.store.ObjectProvider;
import org.datanucleus.store.types.ObjectStringConverter;
import org.hippoecm.repository.api.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Mapping strategy for simple types like primitives, Strings, wrappers of primitives, and objects with an
 * ObjectStringConverter.
 */
public class ValueMappingStrategy extends AbstractMappingStrategy {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    protected final Logger log = LoggerFactory.getLogger(ValueMappingStrategy.class);

    public ValueMappingStrategy(ObjectProvider op, AbstractMemberMetaData mmd, Session session, ColumnResolver columnResolver, TypeResolver typeResolver, Node node) {
        super(op, mmd, session, columnResolver, typeResolver, node);
    }

    private Node getNode() {
        Node node = null;
        Object objectId = op.getExternalObjectId();
        if (objectId instanceof JcrOID) {
            node = ((JcrOID)objectId).getNode(session);
        } else if (objectId instanceof OIDImpl) {
            Object objectKey = ((OIDImpl)objectId).getKeyValue();
            if (objectKey instanceof String) {
                node = JcrOID.getNode(session, (String)objectKey);
            } else {
                throw new NucleusDataStoreException("OID");
            }
        } else {
            throw new NucleusDataStoreException("OID");
        }
        return node;
    }

    @Override
    public Object fetch() {

        // check for null column -> non mapped field in object
        if (mmd.getColumn() == null) {
            return null;
        }
        
        // check primitives
        if (Boolean.TYPE.isAssignableFrom(type)) {
            return fetchBooleanField();
        }
        if (Byte.TYPE.isAssignableFrom(type)) {
            return fetchByteField();
        }
        if (Character.TYPE.isAssignableFrom(type)) {
            return fetchCharField();
        }
        if (Double.TYPE.isAssignableFrom(type)) {
            return fetchDoubleField();
        }
        if (Float.TYPE.isAssignableFrom(type)) {
            return fetchFloatField();
        }
        if (Integer.TYPE.isAssignableFrom(type)) {
            return fetchIntField();
        }
        if (Long.TYPE.isAssignableFrom(type)) {
            return fetchLongField();
        }
        if (Short.TYPE.isAssignableFrom(type)) {
            return fetchShortField();
        }
        // check wrappers of primitives
        if (String.class.isAssignableFrom(type)) {
            return fetchStringField();
        }
        if (Boolean.class.isAssignableFrom(type)) {
            return fetchBooleanField();
        }
        if (Byte.class.isAssignableFrom(type)) {
            return fetchByteField();
        }
        if (Character.class.isAssignableFrom(type)) {
            return fetchCharField();
        }
        if (Float.class.isAssignableFrom(type)) {
            return fetchFloatField();
        }
        if (Double.class.isAssignableFrom(type)) {
            return fetchDoubleField();
        }
        if (Short.class.isAssignableFrom(type)) {
            return fetchShortField();
        }
        if (Integer.class.isAssignableFrom(type)) {
            return fetchIntField();
        }
        if (Long.class.isAssignableFrom(type)) {
            return fetchLongField();
        }
        if (Date.class.isAssignableFrom(type)) {
            Calendar cal = fetchDateField();
            if(cal != null) {
                return cal.getTime();
            } else {
                return null;
            }
        }
        if (Calendar.class.isAssignableFrom(type)) {
            return fetchDateField();
        }

        // check String converter
        ObjectStringConverter converter = null;
        converter = op.getExecutionContext().getOMFContext().getTypeManager().getStringConverter(type);
        if (converter != null) {
            return converter.toObject(fetchStringField());
        }

        if (type.isEnum()) {
            return fetchEnumField(type);
        }

        try {
            Node node = getNode();
            PropertyDefinition def = columnResolver.resolvePropertyDefinition(node, mmd.getColumn(), PropertyType.REFERENCE);
            if (def != null && !def.getName().equals("*")) {
                return fetchReferenceField(type);
            }
        } catch (RepositoryException ex) {
            log.error("internal error resolving field", ex);
        }
        
        return fetchObjectField();
    }

    private Value fetchValueField() {
        String fieldName = mmd.getName();

        if (log.isDebugEnabled()) {
            log.debug("Fetching field=" + fieldName + " fullfield=" + mmd.getFullFieldName() + " type="
                    + mmd.getTypeName() + " class=" + mmd.getClassName());
        }

        try {
            Node node = getNode();
            Property prop = columnResolver.resolveProperty(node, mmd.getColumn());
            if (prop != null) {
                return prop.getValue();
            } else {
                return null;
            }
        } catch (ValueFormatException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("ValueFormatException", ex);
        } catch (VersionException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("VersionException", ex);
        } catch (ConstraintViolationException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("ConstraintViolationException", ex);
        } catch (LockException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("LockException", ex);
        } catch (RepositoryException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("RepositoryException", ex);
        }
    }
    
    private Object fetchReferenceField(Class type) {
        try {
            Value value = fetchValueField();
            if (value != null) {
                if(value.getType() == PropertyType.REFERENCE || value.getType() == PropertyType.WEAKREFERENCE) {
                    try {
                        Object o = type.getConstructor(new Class[] { String.class }).newInstance(new Object[] { value.getString() });
                        return o;
                    } catch(NoSuchMethodException ex) {
                        throw new NucleusDataStoreException("Error creating reference object", ex);
                    } catch(IllegalAccessException ex) {
                        throw new NucleusDataStoreException("Error creating reference object", ex);
                    } catch(InstantiationException ex) {
                        throw new NucleusDataStoreException("Error creating reference object", ex);
                    } catch(InvocationTargetException ex) {
                        throw new NucleusDataStoreException("Error creating reference object", ex);
                    }
                } else {
                    throw new NucleusDataStoreException("ValueFormatException");
                }
            } else {
                return null;
            }
        } catch (ValueFormatException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("ValueFormatException", ex);
        } catch (IllegalStateException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("IllegalStateException", ex);
        } catch (RepositoryException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("RepositoryException", ex);
        }
    }

    private Object fetchObjectField() {
        String fieldName = mmd.getName();

        if (log.isDebugEnabled()) {
            log.debug("Fetching field=" + fieldName + " fullfield=" + mmd.getFullFieldName() + " type="
                    + mmd.getTypeName() + " class=" + mmd.getClassName());
        }
        
        try {
            Node node = getNode();
            Node child = columnResolver.resolveNode(node, mmd.getColumn());
            if (child != null) {
                Class clazz = mmd.getType();
                Object id = new JcrOID(child.getIdentifier(), clazz.getName());
                StateManager pcSM = StateManagerFactory.newStateManagerForHollow(op.getExecutionContext(), clazz, id);
                //pcSM.replaceFields(pcSM.getClassMetaData().getAllFieldNumbers(), new FieldManagerImpl(sm, session, child));
                return pcSM.getObject();
            }
        } catch (ValueFormatException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("ValueFormatException", ex);
        } catch (IllegalStateException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("IllegalStateException", ex);
        } catch (RepositoryException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("RepositoryException", ex);
        }
        return null;
    }

    private String fetchStringField() {
        try {
            Value value = fetchValueField();
            if (value != null) {
                return value.getString();
            } else {
                // check if the node name itself was requested
                if (name.lastIndexOf('/') > -1) {
                    // strip slash and set the node and name
                    node = columnResolver.resolveNode(node, name.substring(0, name.lastIndexOf('/')));
                    name = name.substring(name.lastIndexOf('/') + 1);
                }
                
                if ("{.}".equals(name) || "{_name}".equals(name)) {
                    // return the node name
                    return node.getName();
                } else {
                    return null;
                }
            }
            
        } catch (ValueFormatException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("ValueFormatException", ex);
        } catch (IllegalStateException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("IllegalStateException", ex);
        } catch (RepositoryException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("RepositoryException", ex);
        }
    }

    protected Enum fetchEnumField(Class type) {
        String stringValue = fetchStringField();
        if (stringValue == null) {
            return null;
        }
        Enum value = Enum.valueOf(type, stringValue);
        return value;
    }

    private boolean fetchBooleanField() {
        Value value = fetchValueField();
        if (value == null) {
            return false;
        }
        try {
            return value.getBoolean();
        } catch (ValueFormatException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("ValueFormatException", ex);
        } catch (IllegalStateException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("IllegalStateException", ex);
        } catch (RepositoryException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("RepositoryException", ex);
        }
    }

    private Calendar fetchDateField() {
        Value value = fetchValueField();
        if (value == null) {
            return null;
        }
        try {
            return value.getDate();
        } catch (ValueFormatException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("ValueFormatException", ex);
        } catch (IllegalStateException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("IllegalStateException", ex);
        } catch (RepositoryException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("RepositoryException", ex);
        }
    }

    private byte fetchByteField() {
        Value value = fetchValueField();
        if (value == null) {
            return 0;
        }
        try {
            return (byte) value.getLong();
        } catch (ValueFormatException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("ValueFormatException", ex);
        } catch (IllegalStateException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("IllegalStateException", ex);
        } catch (RepositoryException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("RepositoryException", ex);
        }
    }

    private char fetchCharField() {
        Value value = fetchValueField();
        if (value == null) {
            return ' ';
        }
        try {
            return value.getString().charAt(0);
        } catch (ValueFormatException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("ValueFormatException", ex);
        } catch (IllegalStateException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("IllegalStateException", ex);
        } catch (RepositoryException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("RepositoryException", ex);
        }
    }

    private double fetchDoubleField() {
        Value value = fetchValueField();
        if (value == null) {
            return 0;
        }
        try {
            return value.getDouble();
        } catch (ValueFormatException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("ValueFormatException", ex);
        } catch (IllegalStateException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("IllegalStateException", ex);
        } catch (RepositoryException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("RepositoryException", ex);
        }
    }

    private float fetchFloatField() {
        Value value = fetchValueField();
        if (value == null) {
            return 0;
        }
        try {
            return (float) value.getDouble();
        } catch (ValueFormatException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("ValueFormatException", ex);
        } catch (IllegalStateException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("IllegalStateException", ex);
        } catch (RepositoryException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("RepositoryException", ex);
        }
    }

    private int fetchIntField() {
        Value value = fetchValueField();
        if (value == null) {
            return 0;
        }
        try {
            return (int) value.getLong();
        } catch (ValueFormatException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("ValueFormatException", ex);
        } catch (IllegalStateException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("IllegalStateException", ex);
        } catch (RepositoryException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("RepositoryException", ex);
        }
    }

    private long fetchLongField() {
        Value value = fetchValueField();
        if (value == null) {
            return 0;
        }
        try {
            return value.getLong();
        } catch (ValueFormatException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("ValueFormatException", ex);
        } catch (IllegalStateException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("IllegalStateException", ex);
        } catch (RepositoryException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("RepositoryException", ex);
        }
    }

    private short fetchShortField() {
        Value value = fetchValueField();
        if (value == null) {
            return 0;
        }
        try {
            return (short) value.getLong();
        } catch (ValueFormatException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("ValueFormatException", ex);
        } catch (IllegalStateException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("IllegalStateException", ex);
        } catch (RepositoryException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("RepositoryException", ex);
        }
    }

    @Override
    public void insert(Object value) {
        store(value);
    }

    @Override
    public void update(Object value) {
        store(value);
    }

    private void store(Object value) {
        if (log.isDebugEnabled()) {
            log.debug("Storing field=" + mmd.getName() + " column=" + mmd.getColumn() + " fullfield="
                    + mmd.getFullFieldName() + " type=" + mmd.getTypeName() + " class=" + mmd.getClassName() + " value="
                    + value);
        }

        if (mmd.getColumn() == null) {
            return;
        }
        if (mmd.getName().equals("jcr:uuid")) { // FIXME: on isProtected already covered
            return;
        }
        // check primitives
        if (Boolean.TYPE.isAssignableFrom(type)) {
            storeBooleanField((Boolean) value);
        } else if (Byte.TYPE.isAssignableFrom(type)) {
            storeLongField((Long) value);
        } else if (Character.TYPE.isAssignableFrom(type)) {
            storeStringField((String) value);
        } else if (Float.TYPE.isAssignableFrom(type)) {
            storeDoubleField((Double) value);
        } else if (Double.TYPE.isAssignableFrom(type)) {
            storeDoubleField((Double) value);
        } else if (Short.TYPE.isAssignableFrom(type)) {
            storeLongField((Long) value);
        } else if (Integer.TYPE.isAssignableFrom(type)) {
            storeLongField((Long) value);
        } else if (Long.TYPE.isAssignableFrom(type)) {
            storeLongField((Long) value);
        }
        
        // check String
        else if (String.class.isAssignableFrom(type)) {
            storeStringField((String) value);
        }

        // check wrappers of primitives
        else if (Boolean.class.isAssignableFrom(type)) {
            storeBooleanField((Boolean) value);
        } else if (Byte.class.isAssignableFrom(type)) {
            storeLongField((Long) value);
        } else if (Character.class.isAssignableFrom(type)) {
            storeStringField((String) value);
        } else if (Float.class.isAssignableFrom(type)) {
            storeDoubleField((Double) value);
        } else if (Double.class.isAssignableFrom(type)) {
            storeDoubleField((Double) value);
        } else if (Short.class.isAssignableFrom(type)) {
            storeLongField((Long) value);
        } else if (Integer.class.isAssignableFrom(type)) {
            storeLongField((Long) value);
        } else if (Long.class.isAssignableFrom(type)) {
            storeLongField((Long) value);
        } else if (Date.class.isAssignableFrom(type)) {
            if(value != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime((Date)value);
                storeDateField(cal);
            } else {
                storeDateField(null);
            }
        } else if (Calendar.class.isAssignableFrom(type)) {
            storeDateField((Calendar)value);
        } else {
            // check String converter
            ObjectStringConverter converter = null;
            converter = op.getExecutionContext().getOMFContext().getTypeManager().getStringConverter(type);
            if (converter != null) {
                storeStringField(converter.toString(value));
//            } else if (type.isEnum()) {
//                storeEnumField(value);
            } else {
                try {
                    Node node = getNode();
                    PropertyDefinition def = columnResolver.resolvePropertyDefinition(node, mmd.getColumn(), PropertyType.REFERENCE);
                    if (def != null && !def.getName().equals("*") && Document.class.isAssignableFrom(type)) {
                        // FIXME: using the Document class here creates an unwanted dependency on Hippo specific implementation
                        storeReferenceField((Document)value);
                        return;
                    }
                } catch (RepositoryException ex) {
                    log.error("internal error resolving field", ex);
                }

                storeObjectField(value);
                //throw new NucleusException("Field " + mmd.getFullFieldName() + " cannot be persisted because type="
                //        + mmd.getTypeName() + " is not supported for this datastore");
            }
        }
    }

    private void storeReferenceField(Document value) {
        try {
            Property property = columnResolver.resolveProperty(node, mmd.getColumn());
            if (property == null) {
                if (!node.isCheckedOut()) {
                    checkoutNode(node);
                }
                if (value != null) {
                    property = node.setProperty(mmd.getColumn(), value.getIdentity());
                }
            } else {
                if (!property.getParent().isCheckedOut()) {
                    checkoutNode(property.getParent());
                }
                if (value != null) {
                    property.setValue(value.getIdentity());
                }
            }
        } catch (ValueFormatException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("ValueFormatException", ex);
        } catch (IllegalStateException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("IllegalStateException", ex);
        } catch (RepositoryException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("RepositoryException", ex);
        }
    }

    private void storeObjectField(Object value) {
        String fieldName = mmd.getName();

        if (log.isDebugEnabled()) {
            log.debug("Fetching field=" + fieldName + " fullfield=" + mmd.getFullFieldName() + " type="
                    + mmd.getTypeName() + " class=" + mmd.getClassName());
        }

        JcrOID oid = (JcrOID) op.getExternalObjectId();

        try {
            JcrOID clonedSource;
            Node node = oid.getNode(session);
            ColumnResolver.NodeLocation location = columnResolver.resolveNodeLocation(node, mmd.getColumn());
            Node child = location.child;
            if (value instanceof Cloneable && (clonedSource = columnResolver.resolveClone((Cloneable)value)) != null) {
                Node clonedNode = clonedSource.getNode(session);
                if (child != null) {
                    if (!child.getParent().isCheckedOut()) {
                        child.getParent().checkout();
                    }
                    op.getExecutionContext().deleteObject(value); // FIXME: huh?
                }
                if (!location.parent.isCheckedOut()) {
                    location.parent.checkout();
                }
                child = columnResolver.copyClone(clonedNode, (Cloneable)value, location.parent, location.name, child);
                Class clazz = mmd.getType();
                Object id = new JcrOID(child.getIdentifier(), clazz.getName());
                StateManager pcSM = StateManagerFactory.newStateManagerForHollowPreConstructed(op.getExecutionContext(), id, value);
                pcSM.makePersistent();
            } else {
                if (child != null) {
                    Class clazz = mmd.getType();
                    Object id = new JcrOID(child.getIdentifier(), clazz.getName());
                    if (value == null) {
                        Node n = child.getParent();
                        if (!n.isCheckedOut()) {
                            checkoutNode(n);
                        }
                        child.remove();
                        op.getExecutionContext().deleteObject(value);
                    } else {
                        StateManager pcSM = StateManagerFactory.newStateManagerForPersistentClean(op.getExecutionContext(), id, value);
                        pcSM.flush();
                    }
                } else if (value != null) {
                    String[] nodeTypes = typeResolver.resolve(mmd.getType().getName());
                    if (nodeTypes.length > 0) {
                        child = location.parent.addNode(location.name, nodeTypes[0]);
                        for (int i = 1; i < nodeTypes.length; i++) {
                            child.addMixin(nodeTypes[i]);
                        }
                    } else {
                        child = location.parent.addNode(location.name);
                    }
                    Object id = new JcrOID(child.getIdentifier(), mmd.getType().getName());
                    StateManager pcSM = StateManagerFactory.newStateManagerForPersistentClean(op.getExecutionContext(), id, value);
                    pcSM.flush();
                    pcSM.makePersistent();
                }
            }
        } catch (ValueFormatException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("ValueFormatException", ex);
        } catch (IllegalStateException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("IllegalStateException", ex);
        } catch (RepositoryException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("RepositoryException", ex);
        }
    }

    private void storeEnumField(Enum value) {
        throw new NucleusException("Field " + mmd.getFullFieldName() + " cannot be persisted because type="
                + mmd.getTypeName() + " is not supported for this datastore");
    }
    
    private void storeBooleanField(Boolean value) {
        try {
            JcrOID oid = (JcrOID) op.getExternalObjectId();
            Node node = oid.getNode(session);
            Property property = columnResolver.resolveProperty(node, mmd.getColumn());
            if (property == null) {
                if (!node.isCheckedOut()) {
                    checkoutNode(node);
                }
                property = node.setProperty(mmd.getColumn(), value.booleanValue());
            } else {
                if (!property.getParent().isCheckedOut()) {
                    checkoutNode(property.getParent());
                }
                property.setValue(value);
            }
        } catch (ValueFormatException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("ValueFormatException", ex, value);
        } catch (RepositoryException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("RepositoryException", ex, value);
        }
    }

    private void storeDateField(Calendar value) {
        try {
            JcrOID oid = (JcrOID) op.getExternalObjectId();
            Node node = oid.getNode(session);
            Property property = columnResolver.resolveProperty(node, mmd.getColumn());
            if (property == null) {
                if (!node.isCheckedOut()) {
                    checkoutNode(node);
                }
                property = node.setProperty(mmd.getColumn(), value);
            } else {
                if (!property.getParent().isCheckedOut()) {
                    checkoutNode(property.getParent());
                }
                property.setValue(value);
            }
        } catch (ValueFormatException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("ValueFormatException", ex, value);
        } catch (RepositoryException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("RepositoryException", ex, value);
        }
    }

    private void storeStringField(String value) {
       try {
            Node node = null;
            Object objectId = op.getExternalObjectId();
            if(objectId instanceof JcrOID) {
                node = ((JcrOID) objectId).getNode(session);
            } else if(objectId instanceof OIDImpl) {
                Object objectKey = ((OIDImpl) objectId).getKeyValue();
                if(objectKey instanceof String) {
                    node = JcrOID.getNode(session, (String) objectKey);
                } else {
                    throw new NucleusDataStoreException("OID");
                }
            } else {
                throw new NucleusDataStoreException("OID");
            }
            Property property = columnResolver.resolveProperty(node, mmd.getColumn());
            if (property == null) {
                if (!node.isCheckedOut()) {
                    checkoutNode(node);
                }
                property = node.setProperty(mmd.getColumn(), value);
            } else {
if(property.getDefinition().isProtected()) return; // FIXME
                if (!property.getParent().isCheckedOut()) {
                    checkoutNode(property.getParent());
                }
                property.setValue(value);
            }
        } catch (ValueFormatException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("ValueFormatException", ex, value);
        } catch (RepositoryException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("RepositoryException", ex, value);
        }
    }
    
    private void storeDoubleField(Object value) {
        throw new NucleusException("Field " + mmd.getFullFieldName() + " cannot be persisted because type="
                + mmd.getTypeName() + " is not supported for this datastore");
    }

    private void storeLongField(Long value) {
        try {
            JcrOID oid = (JcrOID) op.getExternalObjectId();
            Node node = oid.getNode(session);
            Property property = columnResolver.resolveProperty(node, mmd.getColumn());
            if (property == null) {
                if (!node.isCheckedOut()) {
                    checkoutNode(node);
                }
                property = node.setProperty(mmd.getColumn(), value.longValue());
            } else {
                if (!property.getParent().isCheckedOut()) {
                    checkoutNode(property.getParent());
                }
                property.setValue(value);
            }
        } catch (ValueFormatException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("ValueFormatException", ex, value);
        } catch (RepositoryException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("RepositoryException", ex, value);
        }
    }

    private void checkoutNode(Node node) throws UnsupportedRepositoryOperationException, LockException, ItemNotFoundException, AccessDeniedException, RepositoryException {
        Node root = node.getSession().getRootNode();
        Node versionable = node;
        while (!versionable.isSame(root)) {
            if (versionable.isNodeType(JcrConstants.MIX_VERSIONABLE)) {
                versionable.checkout();
                break;
            }
            versionable = versionable.getParent();
        }
    }
}
