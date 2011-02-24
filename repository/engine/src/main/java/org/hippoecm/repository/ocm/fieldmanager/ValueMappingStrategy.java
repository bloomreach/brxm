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
package org.hippoecm.repository.ocm.fieldmanager;

import java.util.Calendar;
import java.util.Date;
import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
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
import org.hippoecm.repository.DerivedDataEngine;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.hippoecm.repository.ocm.JcrOID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Mapping strategy for simple types like primitives, Strings, wrappers of primitives, and objects with an
 * ObjectStringConverter.
 */
public class ValueMappingStrategy extends AbstractMappingStrategy {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    protected final Logger log = LoggerFactory.getLogger(ValueMappingStrategy.class);

    public ValueMappingStrategy(ObjectProvider op, AbstractMemberMetaData mmd, Session session) {
        super(op, mmd, session, null, null);
    }

    public ValueMappingStrategy(ObjectProvider op, AbstractMemberMetaData mmd, Session session, Node types, Node node) {
        super(op, mmd, session, types, node);
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
        
        if (Document.class.isAssignableFrom(type) || WorkflowImpl.class.isAssignableFrom(type)) {
            return fetchObjectField();
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

        // TODO Localize this
        throw new NucleusException("Cant obtain value for field " + mmd.getFullFieldName() + " since type="
                + mmd.getTypeName() + " is not supported for this datastore");
    }

    private Value fetchValueField() {
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
            Property prop = ((HippoWorkspace)node.getSession().getWorkspace()).getHierarchyResolver().getProperty(node, mmd.getColumn());
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

    private Object fetchObjectField() {
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

            Node child = ((HippoWorkspace)node.getSession().getWorkspace()).getHierarchyResolver().getNode(node, mmd.getColumn());
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
                    node = ((HippoWorkspace) node.getSession().getWorkspace()).getHierarchyResolver().getNode(node,
                            name.substring(0, name.lastIndexOf('/')));
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
                storeObjectField(value);
                //throw new NucleusException("Field " + mmd.getFullFieldName() + " cannot be persisted because type="
                //        + mmd.getTypeName() + " is not supported for this datastore");
            }
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
            Node node = oid.getNode(session);

            if(value instanceof Document && ((Document)value).isCloned()!= null) {
                    HierarchyResolver.Entry last = new HierarchyResolver.Entry();
                    Node child = (Node) ((HippoWorkspace)node.getSession().getWorkspace()).getHierarchyResolver().getItem(node, mmd.getColumn(), false, last);
                    if (child != null) {
                        if(!child.getParent().isCheckedOut()) {
                            child.getParent().checkout();
                        }
                        DerivedDataEngine.removal(child);
                        child.remove();
                        op.getExecutionContext().deleteObject(value); //op.getExecutionContext().removeObjectFromCache(value, op.getExecutionContext().getApiAdapter()); //
                    }
                    Document document = (Document) value;
                    child = node.getSession().getNodeByUUID(document.isCloned().getIdentity());
                    if(!last.node.isCheckedOut()) {
                        last.node.checkout();
                    }
                    child = ((HippoSession)node.getSession()).copy(child, last.node.getPath() + "/" + last.relPath);
                    if(log.isDebugEnabled()) {
                        log.debug("copying \"" + mmd.getColumn() + "\" from cloned");
                    }
                    document.setIdentity(child.getIdentifier());

                Class clazz = mmd.getType();
                Object id = new JcrOID(child.getIdentifier(), clazz.getName());
                //StateManager pcSM = StateManagerFactory.newStateManagerForEmbedded(op.getExecutionContext(), value, false);
                StateManager pcSM = StateManagerFactory.newStateManagerForHollowPreConstructed(op.getExecutionContext(), id, value);
//StateManager pcSM = op.getExecutionContext().
pcSM.makePersistent();
//pcSM.flush();
                //pcSM.saveFields();
            } else {     
            HierarchyResolver.Entry last = new HierarchyResolver.Entry();
            Node child = (Node) ((HippoWorkspace)node.getSession().getWorkspace()).getHierarchyResolver().getItem(node, mmd.getColumn(), false, last);
            if (child != null) {
                Class clazz = mmd.getType();
                Object id = new JcrOID(child.getIdentifier(), clazz.getName());
                if(value == null) {
			Node n = child.getParent();
			String m = child.getName();
                child.remove();
                        op.getExecutionContext().deleteObject(value); // op.getExecutionContext().removeObjectFromCache(value, op.getExecutionContext().getApiAdapter()); //op.getExecutionContext().deleteObject(value);
                } else {
                StateManager pcSM = StateManagerFactory.newStateManagerForPersistentClean(op.getExecutionContext(), id, value);
                pcSM.flush();
                }
            } else if(value != null) {
                Class clazz = mmd.getType();
                child = last.node.addNode(last.relPath, types.getNode(clazz.getName()).getProperty("hipposys:nodetype").getString());
                if(child.isNodeType("hippo:document")) {
                    child.addMixin("hippo:harddocument");
                } else if(child.isNodeType("hippo:request")) {
                    child.addMixin("mix:referenceable");
                }
                Object id = new JcrOID(child.getIdentifier(), clazz.getName());
                if(value == null) {
                child.remove();
                        op.getExecutionContext().deleteObject(value); // op.getExecutionContext().removeObjectFromCache(value, op.getExecutionContext().getApiAdapter()); //op.getExecutionContext().deleteObject(value);
                } else {
                StateManager pcSM = StateManagerFactory.newStateManagerForPersistentClean(op.getExecutionContext(), id, value);
                pcSM.flush();
pcSM.makePersistent();
                }
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
            Property property = ((HippoWorkspace) session.getWorkspace()).getHierarchyResolver().getProperty(node,
                    mmd.getColumn());
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
            Property property = ((HippoWorkspace) session.getWorkspace()).getHierarchyResolver().getProperty(node,
                    mmd.getColumn());
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
            Property property = ((HippoWorkspace) session.getWorkspace()).getHierarchyResolver().getProperty(node,
                    mmd.getColumn());
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
            Property property = ((HippoWorkspace) session.getWorkspace()).getHierarchyResolver().getProperty(node,
                    mmd.getColumn());
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
