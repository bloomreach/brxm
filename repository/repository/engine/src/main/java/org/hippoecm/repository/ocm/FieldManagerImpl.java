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
package org.hippoecm.repository.ocm;

import java.util.Date;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;
import javax.jdo.spi.PersistenceCapable;

import org.apache.jackrabbit.JcrConstants;
import org.hippoecm.repository.DerivedDataEngine;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.jpox.StateManager;
import org.jpox.exceptions.JPOXDataStoreException;
import org.jpox.metadata.AbstractClassMetaData;
import org.jpox.state.StateManagerFactory;
import org.jpox.store.fieldmanager.AbstractFieldManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FieldManagerImpl extends AbstractFieldManager {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    protected final Logger log = LoggerFactory.getLogger(FieldManagerImpl.class);

    private StateManager sm;
    private Session session;
    private Node node;
    private Node types;

    FieldManagerImpl(StateManager sm, Session session, Node types, Node node) {
        this.sm = sm;
        this.session = session;
        this.node = node;
        this.types = types;
    }

    FieldManagerImpl(StateManager sm, Session session, Node types) {
        this.sm = sm;
        this.session = session;
        this.node = null;
        this.types = types;
    }

    static class Entry {
        Node node;
        String relPath;
    }

    private Node getNode(Node node, String field, String nodetype) throws RepositoryException {
        HierarchyResolver.Entry last = new HierarchyResolver.Entry();
        node = (Node)((HippoWorkspace)session.getWorkspace()).getHierarchyResolver().getItem(node, field, false, last);
        if (node == null && last.node != null) {
            if(!last.node.isCheckedOut()) {
                last.node.checkout();
            }
            if (nodetype != null) {
                node = last.node.addNode(last.relPath, nodetype);
            } else {
                node = last.node.addNode(last.relPath);
            }
            if(node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                node.addMixin(HippoNodeType.NT_HARDDOCUMENT);
            } else if(node.isNodeType(HippoNodeType.NT_REQUEST)) {
                node.addMixin("mix:referenceable");
            }
        }
        return node;
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
    
    @Override
    public void storeBooleanField(int fieldNumber, boolean value) {
        AbstractClassMetaData cmd = sm.getClassMetaData();
        while (fieldNumber < cmd.getNoOfInheritedManagedFields()) {
            cmd = cmd.getSuperAbstractClassMetaData();
        }
        fieldNumber -= cmd.getNoOfInheritedManagedFields();
        String field = cmd.getField(fieldNumber).getColumn();
        if (log.isDebugEnabled())
            log.debug("store \"" + field + "\" = \"" + value + "\"");
        if (field != null) {
            try {
                HierarchyResolver.Entry last = new HierarchyResolver.Entry();
                Property property =((HippoWorkspace)session.getWorkspace()).getHierarchyResolver().getProperty(node, field, last);
                if (property == null) {
                    if(!last.node.isCheckedOut()) {
                        checkoutNode(last.node);
                    }
                    property = last.node.setProperty(last.relPath, value);
                } else {
                    if(!property.getParent().isCheckedOut()) {
                        checkoutNode(property.getParent());
                    }
                    property.setValue(value);
                }
            } catch (ValueFormatException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("ValueFormatException", ex, value);
            } catch (VersionException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("VersionException", ex, value);
            } catch (ConstraintViolationException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("ConstraintViolationException", ex, value);
            } catch (LockException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("LockException", ex, value);
            } catch (RepositoryException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("RepositoryException", ex, value);
            }
        }
    }

    @Override
    public boolean fetchBooleanField(int fieldNumber) {
        AbstractClassMetaData cmd = sm.getClassMetaData();
        while (fieldNumber < cmd.getNoOfInheritedManagedFields()) {
            cmd = cmd.getSuperAbstractClassMetaData();
        }
        fieldNumber -= cmd.getNoOfInheritedManagedFields();
        String field = cmd.getField(fieldNumber).getColumn();
        boolean value = false;
        if (field != null) {
            JCROID oid = (JCROID) sm.getExternalObjectId(null);
            try {
                Node node = oid.getNode(session);
                value =((HippoWorkspace)session.getWorkspace()).getHierarchyResolver().getProperty(node, field).getBoolean();
            } catch (ValueFormatException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("ValueFormatException", ex);
            } catch (VersionException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("VersionException", ex);
            } catch (ConstraintViolationException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("ConstraintViolationException", ex);
            } catch (LockException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("LockException", ex);
            } catch (RepositoryException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("RepositoryException", ex);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("fetch \"" + (sm.getClassMetaData().getField(fieldNumber) != null ?
                                    sm.getClassMetaData().getField(fieldNumber).getFullFieldName() : "unknown")
                                 + "\" = \"" + field + "\" = \"" + value + "\"");
        }
        return value;
    }

    public void storeCharField(int fieldNumber, char value) {
        AbstractClassMetaData cmd = sm.getClassMetaData();
        while (fieldNumber < cmd.getNoOfInheritedManagedFields()) {
            cmd = cmd.getSuperAbstractClassMetaData();
        }
        fieldNumber -= cmd.getNoOfInheritedManagedFields();
        String field = cmd.getField(fieldNumber).getColumn();
        if (log.isDebugEnabled())
            log.debug("store \"" + field + "\" = \"" + value + "\"");
        if (field != null) {
            try {
                HierarchyResolver.Entry last = new HierarchyResolver.Entry();
                Property property =((HippoWorkspace)session.getWorkspace()).getHierarchyResolver().getProperty(node, field, last);
                if (property == null) {
                    if(!last.node.isCheckedOut()) {
                        checkoutNode(last.node);
                    }
                    property = last.node.setProperty(last.relPath, value);
                } else {
                    if(!property.getParent().isCheckedOut()) {
                        checkoutNode(property.getParent());
                    }
                    property.setValue(value);
                }
            } catch (ValueFormatException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("ValueFormatException", ex, value);
            } catch (VersionException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("VersionException", ex, value);
            } catch (ConstraintViolationException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("ConstraintViolationException", ex, value);
            } catch (LockException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("LockException", ex, value);
            } catch (RepositoryException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("RepositoryException", ex, value);
            }
        }
    }

    public char fetchCharField(int fieldNumber) {
        AbstractClassMetaData cmd = sm.getClassMetaData();
        while (fieldNumber < cmd.getNoOfInheritedManagedFields()) {
            cmd = cmd.getSuperAbstractClassMetaData();
        }
        fieldNumber -= cmd.getNoOfInheritedManagedFields();
        String field = cmd.getField(fieldNumber).getColumn();
        char value = ' ';
        if (field != null) {
            JCROID oid = (JCROID) sm.getExternalObjectId(null);
            try {
                Node node = oid.getNode(session);
                value =((HippoWorkspace)session.getWorkspace()).getHierarchyResolver().getProperty(node, field).getString().charAt(0);
            } catch (ValueFormatException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("ValueFormatException", ex);
            } catch (VersionException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("VersionException", ex);
            } catch (ConstraintViolationException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("ConstraintViolationException", ex);
            } catch (LockException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("LockException", ex);
            } catch (RepositoryException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("RepositoryException", ex);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("fetch \"" + (sm.getClassMetaData().getField(fieldNumber) != null ?
                                    sm.getClassMetaData().getField(fieldNumber).getFullFieldName() : "unknown")
                                 + "\" = \"" + field + "\" = \"" + value + "\"");
        }
        return value;
    }

    public void storeByteField(int fieldNumber, byte value) {
        AbstractClassMetaData cmd = sm.getClassMetaData();
        while (fieldNumber < cmd.getNoOfInheritedManagedFields()) {
            cmd = cmd.getSuperAbstractClassMetaData();
        }
        fieldNumber -= cmd.getNoOfInheritedManagedFields();
        String field = cmd.getField(fieldNumber).getColumn();
        if (log.isDebugEnabled())
            log.debug("store \"" + field + "\" = \"" + value + "\"");
        if (field != null) {
            try {
                HierarchyResolver.Entry last = new HierarchyResolver.Entry();
                Property property =((HippoWorkspace)session.getWorkspace()).getHierarchyResolver().getProperty(node, field, last);
                if (property == null) {
                    if(!last.node.isCheckedOut()) {
                        checkoutNode(last.node);
                    }
                    property = last.node.setProperty(last.relPath, value);
                } else {
                    if(!property.getParent().isCheckedOut()) {
                        checkoutNode(property.getParent());
                    }
                    property.setValue(value);
                }
            } catch (ValueFormatException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("ValueFormatException", ex, value);
            } catch (VersionException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("VersionException", ex, value);
            } catch (ConstraintViolationException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("ConstraintViolationException", ex, value);
            } catch (LockException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("LockException", ex, value);
            } catch (RepositoryException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("RepositoryException", ex, value);
            }
        }
    }

    public short fetchShortField(int fieldNumber) {
        AbstractClassMetaData cmd = sm.getClassMetaData();
        while (fieldNumber < cmd.getNoOfInheritedManagedFields()) {
            cmd = cmd.getSuperAbstractClassMetaData();
        }
        fieldNumber -= cmd.getNoOfInheritedManagedFields();
        String field = cmd.getField(fieldNumber).getColumn();
        short value = 0;
        if (field != null) {
            JCROID oid = (JCROID) sm.getExternalObjectId(null);
            try {
                Node node = oid.getNode(session);
                value = (short)((HippoWorkspace)session.getWorkspace()).getHierarchyResolver().getProperty(node, field).getLong();
            } catch (ValueFormatException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("ValueFormatException", ex);
            } catch (VersionException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("VersionException", ex);
            } catch (ConstraintViolationException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("ConstraintViolationException", ex);
            } catch (LockException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("LockException", ex);
            } catch (RepositoryException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("RepositoryException", ex);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("fetch \"" + (sm.getClassMetaData().getField(fieldNumber) != null ?
                                    sm.getClassMetaData().getField(fieldNumber).getFullFieldName() : "unknown")
                                 + "\" = \"" + field + "\" = \"" + value + "\"");
        }
        return value;
    }

    public void storeIntField(int fieldNumber, int value) {
        AbstractClassMetaData cmd = sm.getClassMetaData();
        while (fieldNumber < cmd.getNoOfInheritedManagedFields()) {
            cmd = cmd.getSuperAbstractClassMetaData();
        }
        fieldNumber -= cmd.getNoOfInheritedManagedFields();
        String field = cmd.getField(fieldNumber).getColumn();
        if (log.isDebugEnabled())
            log.debug("store \"" + field + "\" = \"" + value + "\"");
        if (field != null) {
            try {
                HierarchyResolver.Entry last = new HierarchyResolver.Entry();
                Property property =((HippoWorkspace)session.getWorkspace()).getHierarchyResolver().getProperty(node, field, last);
                if (property == null) {
                    if(!last.node.isCheckedOut()) {
                        checkoutNode(last.node);
                    }
                    property = last.node.setProperty(last.relPath, value);
                } else {
                    if(!property.getParent().isCheckedOut()) {
                        checkoutNode(property.getParent());
                    }
                    property.setValue(value);
                }
            } catch (ValueFormatException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("ValueFormatException", ex, value);
            } catch (VersionException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("VersionException", ex, value);
            } catch (ConstraintViolationException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("ConstraintViolationException", ex, value);
            } catch (LockException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("LockException", ex, value);
            } catch (RepositoryException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("RepositoryException", ex, value);
            }
        }
    }

    public int fetchIntField(int fieldNumber) {
        AbstractClassMetaData cmd = sm.getClassMetaData();
        while (fieldNumber < cmd.getNoOfInheritedManagedFields()) {
            cmd = cmd.getSuperAbstractClassMetaData();
        }
        fieldNumber -= cmd.getNoOfInheritedManagedFields();
        String field = cmd.getField(fieldNumber).getColumn();
        int value = 0;
        if (field != null) {
            JCROID oid = (JCROID) sm.getExternalObjectId(null);
            try {
                Node node = oid.getNode(session);
                value = (int)((HippoWorkspace)session.getWorkspace()).getHierarchyResolver().getProperty(node, field).getLong();
            } catch (ValueFormatException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("ValueFormatException", ex);
            } catch (VersionException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("VersionException", ex);
            } catch (ConstraintViolationException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("ConstraintViolationException", ex);
            } catch (LockException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("LockException", ex);
            } catch (RepositoryException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("RepositoryException", ex);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("fetch \"" + (sm.getClassMetaData().getField(fieldNumber) != null ?
                                    sm.getClassMetaData().getField(fieldNumber).getFullFieldName() : "unknown")
                                 + "\" = \"" + field + "\" = \"" + value + "\"");
        }
        return value;
    }

    public void storeLongField(int fieldNumber, long value) {
        AbstractClassMetaData cmd = sm.getClassMetaData();
        while (fieldNumber < cmd.getNoOfInheritedManagedFields()) {
            cmd = cmd.getSuperAbstractClassMetaData();
        }
        fieldNumber -= cmd.getNoOfInheritedManagedFields();
        String field = cmd.getField(fieldNumber).getColumn();
        if (log.isDebugEnabled())
            log.debug("store \"" + field + "\" = \"" + value + "\"");
        if (field != null) {
            try {
                HierarchyResolver.Entry last = new HierarchyResolver.Entry();
                Property property =((HippoWorkspace)session.getWorkspace()).getHierarchyResolver().getProperty(node, field, last);
                if (property == null) {
                    if(!last.node.isCheckedOut()) {
                        checkoutNode(last.node);
                    }
                    property = last.node.setProperty(last.relPath, value);
                } else {
                    if(!property.getParent().isCheckedOut()) {
                        checkoutNode(property.getParent());
                    }
                    property.setValue(value);
                }
            } catch (ValueFormatException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("ValueFormatException", ex, value);
            } catch (VersionException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("VersionException", ex, value);
            } catch (ConstraintViolationException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("ConstraintViolationException", ex, value);
            } catch (LockException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("LockException", ex, value);
            } catch (RepositoryException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("RepositoryException", ex, value);
            }
        }
    }

    public long fetchLongField(int fieldNumber) {
        AbstractClassMetaData cmd = sm.getClassMetaData();
        while (fieldNumber < cmd.getNoOfInheritedManagedFields()) {
            cmd = cmd.getSuperAbstractClassMetaData();
        }
        fieldNumber -= cmd.getNoOfInheritedManagedFields();
        String field = cmd.getField(fieldNumber).getColumn();
        long value = 0L;
        if (field != null) {
            JCROID oid = (JCROID) sm.getExternalObjectId(null);
            try {
                Node node = oid.getNode(session);
                value =((HippoWorkspace)session.getWorkspace()).getHierarchyResolver().getProperty(node, field).getLong();
            } catch (ValueFormatException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("ValueFormatException", ex);
            } catch (VersionException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("VersionException", ex);
            } catch (ConstraintViolationException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("ConstraintViolationException", ex);
            } catch (LockException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("LockException", ex);
            } catch (RepositoryException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("RepositoryException", ex);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("fetch \"" + (sm.getClassMetaData().getField(fieldNumber) != null ?
                                    sm.getClassMetaData().getField(fieldNumber).getFullFieldName() : "unknown")
                                 + "\" = \"" + field + "\" = \"" + value + "\"");
        }
        return value;
    }

    public void storeFloatField(int fieldNumber, float value) {
        AbstractClassMetaData cmd = sm.getClassMetaData();
        while (fieldNumber < cmd.getNoOfInheritedManagedFields()) {
            cmd = cmd.getSuperAbstractClassMetaData();
        }
        fieldNumber -= cmd.getNoOfInheritedManagedFields();
        String field = cmd.getField(fieldNumber).getColumn();
        if (log.isDebugEnabled())
            log.debug("store \"" + field + "\" = \"" + value + "\"");
        if (field != null) {
            try {
                HierarchyResolver.Entry last = new HierarchyResolver.Entry();
                Property property = ((HippoWorkspace)node.getSession().getWorkspace()).getHierarchyResolver().getProperty(node, field, last);
                if (property == null) {
                    if(!last.node.isCheckedOut()) {
                        checkoutNode(last.node);
                    }
                    property = last.node.setProperty(last.relPath, value);
                } else {
                    if(!property.getParent().isCheckedOut()) {
                        checkoutNode(property.getParent());
                    }
                    property.setValue(value);
                }
            } catch (ValueFormatException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("ValueFormatException", ex, value);
            } catch (VersionException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("VersionException", ex, value);
            } catch (ConstraintViolationException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("ConstraintViolationException", ex, value);
            } catch (LockException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("LockException", ex, value);
            } catch (RepositoryException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("RepositoryException", ex, value);
            }
        }
    }

    public float fetchFloatField(int fieldNumber) {
        AbstractClassMetaData cmd = sm.getClassMetaData();
        while (fieldNumber < cmd.getNoOfInheritedManagedFields()) {
            cmd = cmd.getSuperAbstractClassMetaData();
        }
        fieldNumber -= cmd.getNoOfInheritedManagedFields();
        String field = cmd.getField(fieldNumber).getColumn();
        float value = 0.0F;
        if (field != null) {
            JCROID oid = (JCROID) sm.getExternalObjectId(null);
            try {
                Node node = oid.getNode(session);
                value = (float) ((HippoWorkspace)node.getSession().getWorkspace()).getHierarchyResolver().getProperty(node, field).getDouble();
            } catch (ValueFormatException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("ValueFormatException", ex);
            } catch (VersionException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("VersionException", ex);
            } catch (ConstraintViolationException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("ConstraintViolationException", ex);
            } catch (LockException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("LockException", ex);
            } catch (RepositoryException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("RepositoryException", ex);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("fetch \"" + (sm.getClassMetaData().getField(fieldNumber) != null ?
                                    sm.getClassMetaData().getField(fieldNumber).getFullFieldName() : "unknown")
                                 + "\" = \"" + field + "\" = \"" + value + "\"");
        }
        return value;
    }

    public void storeDoubleField(int fieldNumber, double value) {
        AbstractClassMetaData cmd = sm.getClassMetaData();
        while (fieldNumber < cmd.getNoOfInheritedManagedFields()) {
            cmd = cmd.getSuperAbstractClassMetaData();
        }
        fieldNumber -= cmd.getNoOfInheritedManagedFields();
        String field = cmd.getField(fieldNumber).getColumn();
        if (log.isDebugEnabled())
            log.debug("store \"" + field + "\" = \"" + value + "\"");
        if (field != null) {
            try {
                HierarchyResolver.Entry last = new HierarchyResolver.Entry();
                Property property = ((HippoWorkspace)node.getSession().getWorkspace()).getHierarchyResolver().getProperty(node, field, last);
                if (property == null) {
                    if(!last.node.isCheckedOut()) {
                        checkoutNode(last.node);
                    }
                    property = last.node.setProperty(last.relPath, value);
                } else {
                    if(!property.getParent().isCheckedOut()) {
                        checkoutNode(property.getParent());
                    }
                    property.setValue(value);
                }
            } catch (ValueFormatException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("ValueFormatException", ex, value);
            } catch (VersionException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("VersionException", ex, value);
            } catch (ConstraintViolationException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("ConstraintViolationException", ex, value);
            } catch (LockException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("LockException", ex, value);
            } catch (RepositoryException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("RepositoryException", ex, value);
            }
        }
    }

    public double fetchDoubleField(int fieldNumber) {
        AbstractClassMetaData cmd = sm.getClassMetaData();
        while (fieldNumber < cmd.getNoOfInheritedManagedFields()) {
            cmd = cmd.getSuperAbstractClassMetaData();
        }
        fieldNumber -= cmd.getNoOfInheritedManagedFields();
        String field = cmd.getField(fieldNumber).getColumn();
        double value = 0.0;
        if (field != null) {
            JCROID oid = (JCROID) sm.getExternalObjectId(null);
            try {
                Node node = oid.getNode(session);
                value = ((HippoWorkspace)node.getSession().getWorkspace()).getHierarchyResolver().getProperty(node, field).getDouble();
            } catch (ValueFormatException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("ValueFormatException", ex);
            } catch (VersionException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("VersionException", ex);
            } catch (ConstraintViolationException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("ConstraintViolationException", ex);
            } catch (LockException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("LockException", ex);
            } catch (RepositoryException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("RepositoryException", ex);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("fetch \"" + (sm.getClassMetaData().getField(fieldNumber) != null ?
                                    sm.getClassMetaData().getField(fieldNumber).getFullFieldName() : "unknown")
                                 + "\" = \"" + field + "\" = \"" + value + "\"");
        }
        return value;
    }

    public void storeStringField(int fieldNumber, String value) {
        AbstractClassMetaData cmd = sm.getClassMetaData();
        while (fieldNumber < cmd.getNoOfInheritedManagedFields()) {
            cmd = cmd.getSuperAbstractClassMetaData();
        }
        fieldNumber -= cmd.getNoOfInheritedManagedFields();
        String field = cmd.getField(fieldNumber).getColumn();
        if (log.isDebugEnabled())
            log.debug("store \"" + field + "\" = \"" + value + "\"");
        if (field != null && !field.equals("jcr:uuid")) {
            try {
                HierarchyResolver.Entry last = new HierarchyResolver.Entry();
                Property property = ((HippoWorkspace)node.getSession().getWorkspace()).getHierarchyResolver().getProperty(node, field, last);
                if (property == null) {
                    if ("{.}".equals(last.relPath) || "{_name}".equals(last.relPath)) {
                        // if(!last.node.getParent().isCheckedOut()) {
                        //     checkoutNode(last.node.getParent());
                        // }
                        // last.node.getSession().move(last.node.getPath(), last.node.getParent().getPath() + "/" + value);
                        throw new JPOXDataStoreException("Node renaming is not supported");
                    } else {
                        if(!last.node.isCheckedOut()) {
                            checkoutNode(last.node);
                        }
                        try {
                            property = last.node.setProperty(last.relPath, value);
                        } catch(ConstraintViolationException ex) {
                            property = last.node.setProperty(last.relPath, value.split(","));
                        }
                    }
                } else {
                    if(!property.getParent().isCheckedOut()) {
                        checkoutNode(property.getParent());
                    }
                    if(property.getDefinition().isMultiple()) {
                        if(value == null)
                            property.remove();
                        else
                            property.setValue(value.split(","));
                    } else
                        property.setValue(value);
                }
            } catch (ValueFormatException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("ValueFormatException", ex, value);
            } catch (VersionException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("VersionException", ex, value);
            } catch (ConstraintViolationException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("ConstraintViolationException", ex, value);
            } catch (LockException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("LockException", ex, value);
            } catch (RepositoryException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("RepositoryException", ex, value);
            }
        }
    }

    public String fetchStringField(int fieldNumber) {
        AbstractClassMetaData cmd = sm.getClassMetaData();
        while (fieldNumber < cmd.getNoOfInheritedManagedFields()) {
            cmd = cmd.getSuperAbstractClassMetaData();
        }
        fieldNumber -= cmd.getNoOfInheritedManagedFields();
        String field = cmd.getField(fieldNumber).getColumn();
        String value = "";
        if (log.isDebugEnabled()) {
            log.debug("fetching " + fieldNumber + "\"" + (cmd.getField(fieldNumber) != null ?
                                    cmd.getField(fieldNumber).getFullFieldName() : "unknown")
                                 + "\" = \"" + field + "\" = \"" + value + "\"");
        }
        if (field != null) {
            JCROID oid = (JCROID) sm.getExternalObjectId(null);
            try {
                Node node = oid.getNode(session);
                Property property = ((HippoWorkspace)node.getSession().getWorkspace()).getHierarchyResolver().getProperty(node, field);
                if (property != null) {
                    if(property.getDefinition().isMultiple()) {
                        Value[] values = property.getValues();
                        if(values != null) {
                            StringBuffer sb = new StringBuffer();
                            for(int i=0; i<values.length; i++) {
                                if(i>0)
                                    sb.append(",");
                                sb.append(values[i].getString());
                            }
                        } else
                            value = null;
                    } else {
                        value = property.getString();
                    }
                } else {
                    Node ref = node;
                    String prop = field;
                    if (field.lastIndexOf('/') > -1) {
                        ref = ((HippoWorkspace) node.getSession().getWorkspace()).getHierarchyResolver().getNode(node,
                                field.substring(0, field.lastIndexOf('/')));
                        prop = field.substring(field.lastIndexOf('/') + 1);
                    }
                    if ("{.}".equals(prop) || "{_name}".equals(prop)) {
                        value = ref.getName();
                    } else {
                        value = null;
                    }
                }
            } catch (ValueFormatException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("ValueFormatException", ex);
            } catch (VersionException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("VersionException", ex);
            } catch (ConstraintViolationException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("ConstraintViolationException", ex);
            } catch (LockException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("LockException", ex);
            } catch (RepositoryException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("RepositoryException", ex);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("fetch \"" + (cmd.getField(fieldNumber) != null ?
                                    cmd.getField(fieldNumber).getFullFieldName() : "unknown")
                                 + "\" = \"" + field + "\" = \"" + value + "\"");
        }
        return value;
    }

    public void storeObjectField(int fieldNumber, Object value) {
        if(value instanceof Date) {
            storeLongField(fieldNumber, ((Date)value).getTime());
            return;
        }
        AbstractClassMetaData cmd = sm.getClassMetaData();
        while (fieldNumber < cmd.getNoOfInheritedManagedFields()) {
            cmd = cmd.getSuperAbstractClassMetaData();
        }
        fieldNumber -= cmd.getNoOfInheritedManagedFields();
        String field = cmd.getField(fieldNumber).getColumn();
        if (log.isDebugEnabled())
            log.debug("store \"" + field + "\" = \"" + value + "\"");
        if (field == null)
            return;
        if (value == null) {
            try {
                Item removal = ((HippoWorkspace)node.getSession().getWorkspace()).getHierarchyResolver().getItem(node, field);
                if(removal != null) {
                    if(!removal.getParent().isCheckedOut()) {
                        checkoutNode(removal.getParent());
                    }
                    if(removal instanceof Node) {
                        DerivedDataEngine.removal((Node)removal);
                    }
                    removal.remove();
                }
            } catch (InvalidItemStateException ex) {
                if (log.isDebugEnabled()) {
                    log.debug("node already deleted: " + ex.getMessage());
                }
            } catch (VersionException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("VersionException", ex);
            } catch (RepositoryException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("RepositoryException", ex);
            }
            return;
            // throw new NullPointerException();
        }
        if(value instanceof String[]) {
            try {
                HierarchyResolver.Entry last = new HierarchyResolver.Entry();
                Property property = ((HippoWorkspace)node.getSession().getWorkspace()).getHierarchyResolver().getProperty(node, field, last);
                if (property == null) {
                    if ("{.}".equals(last.relPath) || "{_name}".equals(last.relPath)) {
                        throw new JPOXDataStoreException("Node renaming is not supported");
                    } else {
                        if(!last.node.isCheckedOut()) {
                            checkoutNode(last.node);
                        }
                        property = last.node.setProperty(last.relPath, (String[]) value);
                    }
                } else {
                    if (!property.getParent().isCheckedOut()) {
                        checkoutNode(property.getParent());
                    }
                    property.setValue((String[])value);
                }
            } catch (ValueFormatException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("ValueFormatException", ex, value);
            } catch (VersionException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("VersionException", ex, value);
            } catch (ConstraintViolationException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("ConstraintViolationException", ex, value);
            } catch (LockException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("LockException", ex, value);
            } catch (RepositoryException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("RepositoryException", ex, value);
            }
            return;
        }
        StateManager valueSM = sm.getObjectManager().findStateManager((PersistenceCapable) value);
        if (valueSM == null) { // If not already persisted
            PersistenceCapable pc = (PersistenceCapable) value;
            if (pc == null)
                throw new NullPointerException();
            try {
                Node child;
                Object id;
                String classname = value.getClass().getName();
                Node nodetypeNode = types.getNode(classname);
                String nodetype = nodetypeNode.getProperty(HippoNodeType.HIPPOSYS_NODETYPE).getString();
                if (value instanceof Document && ((Document) value).isCloned() != null) {
                    HierarchyResolver.Entry last = new HierarchyResolver.Entry();
                    child = (Node) ((HippoWorkspace)node.getSession().getWorkspace()).getHierarchyResolver().getItem(node, field, false, last);
                    if (child != null) {
                        if(!child.getParent().isCheckedOut()) {
                            child.getParent().checkout();
                        }
                        DerivedDataEngine.removal(child);
                        child.remove();
                    }
                    Document document = (Document) value;
                    child = node.getSession().getNodeByUUID(document.isCloned().getIdentity());
                    if(!last.node.isCheckedOut()) {
                        last.node.checkout();
                    }
                    child = ((HippoSession)node.getSession()).copy(child, last.node.getPath() + "/" + last.relPath);
                    if(log.isDebugEnabled()) {
                        log.debug("copying \"" + field + "\" from cloned");
                    }
                    document.setIdentity(child.getUUID());
                } else
                    child = getNode(node, field, nodetype);

                id = new JCROID(child.getUUID(), classname);
                StateManager pcSM = StateManagerFactory
                        .newStateManagerForPersistentClean(sm.getObjectManager(), id, pc);
                pcSM.provideFields(pcSM.getClassMetaData().getAllFieldNumbers(), new FieldManagerImpl(pcSM, session,
                        types, child));
            } catch (ItemExistsException ex) {
                try {
                    throw new JPOXDataStoreException("ItemExistsException", ex, node.getPath() + "/" + field);
                } catch (RepositoryException ex2) {
                    throw new JPOXDataStoreException("ItemExistsException", ex);
                }
            } catch (NoSuchNodeTypeException ex) {
                try {
                    throw new JPOXDataStoreException("NoSuchNodeTypeException", ex, node.getPath() + "/" + field);
                } catch (RepositoryException ex2) {
                    throw new JPOXDataStoreException("ItemExistsException", ex);
                }
            } catch (UnsupportedRepositoryOperationException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("UnsupportedRepositoryOperationException", ex);
            } catch (PathNotFoundException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("PathNotFoundException", ex);
            } catch (VersionException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("VersionException", ex, value);
            } catch (ConstraintViolationException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("ConstraintViolationException", ex, value);
            } catch (LockException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("LockException", ex, value);
            } catch (RepositoryException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("RepositoryException", ex, value);
            }
        }
    }

    public Object fetchObjectField(int fieldNumber) {
        AbstractClassMetaData cmd = sm.getClassMetaData();
        while (fieldNumber < cmd.getNoOfInheritedManagedFields()) {
            cmd = cmd.getSuperAbstractClassMetaData();
        }
        fieldNumber -= cmd.getNoOfInheritedManagedFields();
        String field = cmd.getField(fieldNumber).getColumn();
        Object value = null;
        if (log.isDebugEnabled()) {
            log.debug("fetching \"" + (cmd.getField(fieldNumber) != null ?
                                    cmd.getField(fieldNumber).getFullFieldName() : "unknown")
                                 + "\" = \"" + field + "\" = \"" + value + "\"");
        }
        if (field != null) {
            JCROID oid = (JCROID) sm.getExternalObjectId(null);
            try {
                Node node = oid.getNode(session);
                Class clazz = cmd.getField(fieldNumber).getType();
                if (Date.class.isAssignableFrom(clazz)) {
                    Property property = ((HippoWorkspace)node.getSession().getWorkspace()).getHierarchyResolver().getProperty(node, field);
                    if (property != null) {
                        value = new Date(property.getLong());
                    }
                } else if (clazz.isArray() && String.class.isAssignableFrom(clazz.getComponentType())) {
                    Property property = ((HippoWorkspace)node.getSession().getWorkspace()).getHierarchyResolver().getProperty(node, field);
                    if (property != null) {
                        Value[] propertyValues = property.getValues();
                        String[] strings = new String[propertyValues.length];
                        for (int i=0; i<propertyValues.length; i++) {
                            strings[i] = propertyValues[i].getString();
                        }
                        value = strings;
                    } else {
                        value = null;
                    }
                } else {
                    Item child = ((HippoWorkspace)node.getSession().getWorkspace()).getHierarchyResolver().getItem(node, field);
                    if (child != null) {
                        Object id = new JCROID(((Node)child).getUUID(), clazz.getName());
                        StateManager pcSM = StateManagerFactory.newStateManagerForHollow(sm.getObjectManager(), clazz, id);
                        //pcSM.replaceFields(pcSM.getClassMetaData().getAllFieldNumbers(), new FieldManagerImpl(sm, session, child));
                        value = pcSM.getObject();
                    }
                }
            } catch (ValueFormatException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("ValueFormatException", ex);
            } catch (VersionException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("VersionException", ex);
            } catch (ConstraintViolationException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("ConstraintViolationException", ex);
            } catch (LockException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("LockException", ex);
            } catch (RepositoryException ex) {
                if(log.isDebugEnabled()) {
                    log.debug("failed", ex);
                }
                throw new JPOXDataStoreException("RepositoryException", ex);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("fetch \"" + (sm.getClassMetaData().getField(fieldNumber) != null ?
                                    sm.getClassMetaData().getField(fieldNumber).getFullFieldName() : "unknown")
                                 + "\" = \"" + field + "\" = \"" + value + "\"");
        }
        return value;
    }
}
