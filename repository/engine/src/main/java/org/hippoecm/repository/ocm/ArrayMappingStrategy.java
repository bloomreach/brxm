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

import java.lang.reflect.Array;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.JcrConstants;
import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.identity.OIDImpl;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.store.ObjectProvider;
import org.datanucleus.store.types.ObjectStringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mapping strategy for simple types like primitives, Strings, wrappers of primitives, and objects with an
 * ObjectStringConverter.
 */
public class ArrayMappingStrategy extends ValueMappingStrategy {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    protected final Logger log = LoggerFactory.getLogger(ArrayMappingStrategy.class);

    protected Class componentType;

    public ArrayMappingStrategy(ObjectProvider op, AbstractMemberMetaData mmd, Session session, ColumnResolver columnResolver, TypeResolver typeResolver, Node node) {
        super(op, mmd, session, columnResolver, typeResolver, node);
        componentType = type.getComponentType();
    }

    @Override
    public Object fetch() {
        if (mmd.getColumn() == null) { // unmapped field in object
            return null;
        }
        try {
            Value[] values = fetchValuesField();
            if(values == null) {
                return null;
            }
            if (Boolean.TYPE.isAssignableFrom(componentType)) {
                boolean result[] = new boolean[values.length];
                for (int i = 0; i < result.length; i++) {
                    result[i] = values[i].getBoolean();
                }
                return result;
            } else if (Byte.TYPE.isAssignableFrom(componentType)) {
                byte result[] = new byte[values.length];
                for (int i = 0; i < result.length; i++) {
                    result[i] = (byte) (values[i].getLong() & 0xff);
                }
                return result;
            } else if (Character.TYPE.isAssignableFrom(componentType)) {
                char result[] = new char[values.length];
                for (int i = 0; i < result.length; i++) {
                    result[i] = (char) values[i].getLong();
                }
                return result;
            } else if (Double.TYPE.isAssignableFrom(componentType)) {
                double result[] = new double[values.length];
                for (int i = 0; i < result.length; i++) {
                    result[i] = values[i].getDouble();
                }
                return result;
            } else if (Float.TYPE.isAssignableFrom(componentType)) {
                float result[] = new float[values.length];
                for (int i = 0; i < result.length; i++) {
                    result[i] = (float) values[i].getDouble();
                }
                return result;
            } else if (Integer.TYPE.isAssignableFrom(componentType)) {
                int result[] = new int[values.length];
                for (int i = 0; i < result.length; i++) {
                    result[i] = (int) values[i].getLong();
                }
                return result;
            } else if (Long.TYPE.isAssignableFrom(componentType)) {
                long result[] = new long[values.length];
                for (int i = 0; i < result.length; i++) {
                    result[i] = values[i].getLong();
                }
                return result;
            } else if (Short.TYPE.isAssignableFrom(componentType)) {
                short result[] = new short[values.length];
                for (int i = 0; i < result.length; i++) {
                    result[i] = (short) values[i].getLong();
                }
                return result;
            } else if (Boolean.class.isAssignableFrom(componentType)) {
                Boolean result[] = new Boolean[values.length];
                for (int i = 0; i < result.length; i++) {
                    result[i] = values[i].getBoolean();
                }
                return result;
            } else if (String.class.isAssignableFrom(componentType)) {
                String result[] = new String[values.length];
                for (int i = 0; i < result.length; i++) {
                    result[i] = values[i].getString();
                }
                return result;
            } else if (Byte.class.isAssignableFrom(componentType)) {
                Byte result[] = new Byte[values.length];
                for (int i = 0; i < result.length; i++) {
                    result[i] = (byte) (values[i].getLong() & 0xff);
                }
                return result;
            } else if (Character.class.isAssignableFrom(componentType)) {
                Character result[] = new Character[values.length];
                for (int i = 0; i < result.length; i++) {
                    result[i] = (char) values[i].getLong();
                }
                return result;
            } else if (Double.class.isAssignableFrom(componentType)) {
                Double result[] = new Double[values.length];
                for (int i = 0; i < result.length; i++) {
                    result[i] = values[i].getDouble();
                }
                return result;
            } else if (Float.class.isAssignableFrom(componentType)) {
                Float result[] = new Float[values.length];
                for (int i = 0; i < result.length; i++) {
                    result[i] = (float) values[i].getDouble();
                }
                return result;
            } else if (Integer.class.isAssignableFrom(componentType)) {
                Integer result[] = new Integer[values.length];
                for (int i = 0; i < result.length; i++) {
                    result[i] = (int) values[i].getLong();
                }
                return result;
            } else if (Long.class.isAssignableFrom(componentType)) {
                Long result[] = new Long[values.length];
                for (int i = 0; i < result.length; i++) {
                    result[i] = values[i].getLong();
                }
                return result;
            } else if (Short.class.isAssignableFrom(componentType)) {
                Short result[] = new Short[values.length];
                for (int i = 0; i < result.length; i++) {
                    result[i] = (short) values[i].getLong();
                }
                return result;
            } else {
                // check String converter
                ObjectStringConverter converter = null;
//        if (Date.class.isAssignableFrom(type)) {
//            converter = new DateToGeneralizedTimeStringConverter();
//        } else if (Calendar.class.isAssignableFrom(type)) {
//            converter = new CalendarToGeneralizedTimeStringConverter();
//        } else {
                converter = op.getExecutionContext().getOMFContext().getTypeManager().getStringConverter(componentType);
//        }
                if (converter != null) {
                    Object[] result = (Object[]) Array.newInstance(componentType, values.length);
                    for (int i = 0; i < result.length; i++) {
                        result[i] = converter.toObject(values[i].getString());
                    }
                    return result;
                } else {
                    throw new NucleusException("Cant obtain value for field " + mmd.getFullFieldName() + " since type="
                            + mmd.getTypeName() + " is not supported for this datastore");
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

    private Value[] fetchValuesField() {
        String fieldName = mmd.getName();

        if (log.isDebugEnabled()) {
            log.debug("Fetching field=" + fieldName + " fullfield=" + mmd.getFullFieldName() + " type="
                    + mmd.getTypeName() + " class=" + mmd.getClassName());
        }

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
            Property prop = columnResolver.resolveProperty(node, mmd.getColumn());
            if (prop != null) {
                return prop.getValues();
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

    @Override
    public void insert(Object value) {
        store(value);
    }

    @Override
    public void update(Object value) {
        store(value);
    }

    private void store(Object object) {
        if (log.isDebugEnabled()) {
            log.debug("Storing field=" + mmd.getName() + " culumn=" + mmd.getColumn() + " fullfield="
                    + mmd.getFullFieldName() + " type=" + mmd.getTypeName() + " class=" + mmd.getClassName() + " value="
                    + object);
        }

        if (mmd.getColumn() == null) {
            return;
        }
        try {
            ValueFactory vf = session.getValueFactory();
            if (object != null) {
                int propertyType;
            Value[] values = new Value[Array.getLength(object)];
            if (Boolean.TYPE.isAssignableFrom(componentType)) {
                propertyType = PropertyType.BOOLEAN;
                for(int i=0; i<values.length; i++) {
                    values[i] = vf.createValue(Array.getBoolean(object, i));
                }
            } else if (Byte.TYPE.isAssignableFrom(componentType)) {
                propertyType = PropertyType.LONG;
                for(int i=0; i<values.length; i++) {
                    values[i] = vf.createValue(Array.getByte(object, i));
                }
            } else if (Character.TYPE.isAssignableFrom(componentType)) {
                propertyType = PropertyType.LONG;
                for(int i=0; i<values.length; i++) {
                    values[i] = vf.createValue(Array.getChar(object, i));
                }
            } else if (Double.TYPE.isAssignableFrom(componentType)) {
                propertyType = PropertyType.DOUBLE;
                for(int i=0; i<values.length; i++) {
                    values[i] = vf.createValue(Array.getDouble(object, i));
                }
            } else if (Float.TYPE.isAssignableFrom(componentType)) {
                propertyType = PropertyType.DOUBLE;
                for(int i=0; i<values.length; i++) {
                    values[i] = vf.createValue(Array.getFloat(object, i));
                }
            } else if (Integer.TYPE.isAssignableFrom(componentType)) {
                propertyType = PropertyType.LONG;
                for(int i=0; i<values.length; i++) {
                    values[i] = vf.createValue(Array.getInt(object, i));
                }
            } else if (Long.TYPE.isAssignableFrom(componentType)) {
                propertyType = PropertyType.LONG;
                for(int i=0; i<values.length; i++) {
                    values[i] = vf.createValue(Array.getLong(object, i));
                }
            } else if (Short.TYPE.isAssignableFrom(componentType)) {
                propertyType = PropertyType.LONG;
                for(int i=0; i<values.length; i++) {
                    values[i] = vf.createValue(Array.getShort(object, i));
                }
            } else if (Boolean.class.isAssignableFrom(componentType)) {
                propertyType = PropertyType.BOOLEAN;
                for(int i=0; i<values.length; i++) {
                    values[i] = vf.createValue(Array.getBoolean(object, i));
                }
            } else if (String.class.isAssignableFrom(componentType)) {
                propertyType = PropertyType.STRING;
                for(int i=0; i<values.length; i++) {
                    values[i] = vf.createValue((String)Array.get(object, i));
                }
            } else if (Byte.class.isAssignableFrom(componentType)) {
                propertyType = PropertyType.LONG;
                for(int i=0; i<values.length; i++) {
                    values[i] = vf.createValue((Byte)Array.get(object, i));
                }
            } else if (Character.class.isAssignableFrom(componentType)) {
                propertyType = PropertyType.LONG;
                for(int i=0; i<values.length; i++) {
                    values[i] = vf.createValue((Character)Array.get(object, i));
                }
            } else if (Double.class.isAssignableFrom(componentType)) {
                propertyType = PropertyType.DOUBLE;
                for(int i=0; i<values.length; i++) {
                    values[i] = vf.createValue((Double)Array.get(object, i));
                }
            } else if (Float.class.isAssignableFrom(componentType)) {
                propertyType = PropertyType.DOUBLE;
                for(int i=0; i<values.length; i++) {
                    values[i] = vf.createValue((Float)Array.get(object, i));
                }
            } else if (Integer.class.isAssignableFrom(componentType)) {
                propertyType = PropertyType.LONG;
                for(int i=0; i<values.length; i++) {
                    values[i] = vf.createValue((Integer)Array.get(object, i));
                }
            } else if (Long.class.isAssignableFrom(componentType)) {
                propertyType = PropertyType.LONG;
                for(int i=0; i<values.length; i++) {
                    values[i] = vf.createValue((Long)Array.get(object, i));
                }
            } else if (Short.class.isAssignableFrom(componentType)) {
                propertyType = PropertyType.LONG;
                for(int i=0; i<values.length; i++) {
                    values[i] = vf.createValue((Short)Array.get(object, i));
                }
            } else {
                propertyType = PropertyType.STRING;
            // check String converter
            ObjectStringConverter converter = null;
//            if (Date.class.isAssignableFrom(type)) {
//                converter = new DateToGeneralizedTimeStringConverter();
//            } else if (Calendar.class.isAssignableFrom(type)) {
//                converter = new CalendarToGeneralizedTimeStringConverter();
//            } else {
            converter = op.getExecutionContext().getOMFContext().getTypeManager().getStringConverter(type);
//            }
            if (converter != null) {
                for(int i=0; i<values.length; i++) {
                    values[i] = vf.createValue(converter.toString(Array.get(object, i)));
                }
            } else {
                throw new NucleusException("Field " + mmd.getFullFieldName() + " cannot be persisted because type="
                        + mmd.getTypeName() + " is not supported for this datastore");
            }
        }
        storeValues(values, propertyType);
    } else {
        // Retaining backwards compatible
        storeValues(null, PropertyType.UNDEFINED);
    }
        } catch(RepositoryException ex) {
            throw new NucleusException(ex.getMessage(), ex);
        }
    }

    private void storeValues(Value[] values, int type) {
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
                property = node.setProperty(mmd.getColumn(), values, type);
            } else {
                if (!property.getParent().isCheckedOut()) {
                    checkoutNode(property.getParent());
                }
                property.setValue(values);
            }
        } catch (ValueFormatException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("ValueFormatException", ex, values);
        } catch (RepositoryException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("RepositoryException", ex, values);
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
