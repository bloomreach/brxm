/*
 * Copyright 2007 Hippo.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.pojo;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;
import javax.jdo.spi.PersistenceCapable;

import org.jpox.StateManager;
import org.jpox.exceptions.JPOXDataStoreException;
import org.jpox.state.StateManagerFactory;
import org.jpox.store.fieldmanager.AbstractFieldManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hippoecm.repository.api.HippoNodeType;

class FieldManagerImpl extends AbstractFieldManager {
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

    public void storeBooleanField(int fieldNumber, boolean value) {
        String field = sm.getClassMetaData().getField(fieldNumber).getColumn();
        if(log.isDebugEnabled())
            log.debug("store \""+field+"\" = \""+value+"\"");
        if (field != null) {
            try {
                node.setProperty(field, value);
            } catch (ValueFormatException ex) {
                throw new JPOXDataStoreException("ValueFormatException", ex, value);
            } catch (VersionException ex) {
                throw new JPOXDataStoreException("VersionException", ex, value);
            } catch (ConstraintViolationException ex) {
                throw new JPOXDataStoreException("ConstraintViolationException", ex, value);
            } catch (LockException ex) {
                throw new JPOXDataStoreException("LockException", ex, value);
            } catch (RepositoryException ex) {
                throw new JPOXDataStoreException("RepositoryException", ex, value);
            }
        }
    }

    public boolean fetchBooleanField(int fieldNumber) {
        String field = sm.getClassMetaData().getField(fieldNumber).getColumn();
        boolean value = false;
        if (field != null) {
            JCROID oid = (JCROID) sm.getExternalObjectId(null);
            try {
                Node node = oid.getNode(session);
                value = node.getProperty(field).getBoolean();
            } catch (ValueFormatException ex) {
                throw new JPOXDataStoreException("ValueFormatException", ex);
            } catch (VersionException ex) {
                throw new JPOXDataStoreException("VersionException", ex);
            } catch (ConstraintViolationException ex) {
                throw new JPOXDataStoreException("ConstraintViolationException", ex);
            } catch (LockException ex) {
                throw new JPOXDataStoreException("LockException", ex);
            } catch (RepositoryException ex) {
                throw new JPOXDataStoreException("RepositoryException", ex);
            }
        }
        if(log.isDebugEnabled())
            log.debug("fetch \""+field+"\" = \""+value+"\"");
        return value;
    }

    public void storeCharField(int fieldNumber, char value) {
        String field = sm.getClassMetaData().getField(fieldNumber).getColumn();
        if(log.isDebugEnabled())
            log.debug("store \""+field+"\" = \""+value+"\"");
        if (field != null) {
            try {
                node.setProperty(field, value);
            } catch (ValueFormatException ex) {
                throw new JPOXDataStoreException("ValueFormatException", ex, value);
            } catch (VersionException ex) {
                throw new JPOXDataStoreException("VersionException", ex, value);
            } catch (ConstraintViolationException ex) {
                throw new JPOXDataStoreException("ConstraintViolationException", ex, value);
            } catch (LockException ex) {
                throw new JPOXDataStoreException("LockException", ex, value);
            } catch (RepositoryException ex) {
                throw new JPOXDataStoreException("RepositoryException", ex, value);
            }
        }
    }

    public char fetchCharField(int fieldNumber) {
        String field = sm.getClassMetaData().getField(fieldNumber).getColumn();
        char value = ' ';
        if (field != null) {
            JCROID oid = (JCROID) sm.getExternalObjectId(null);
            try {
                Node node = oid.getNode(session);
                value = node.getProperty(field).getString().charAt(0);
            } catch (ValueFormatException ex) {
                throw new JPOXDataStoreException("ValueFormatException", ex);
            } catch (VersionException ex) {
                throw new JPOXDataStoreException("VersionException", ex);
            } catch (ConstraintViolationException ex) {
                throw new JPOXDataStoreException("ConstraintViolationException", ex);
            } catch (LockException ex) {
                throw new JPOXDataStoreException("LockException", ex);
            } catch (RepositoryException ex) {
                throw new JPOXDataStoreException("RepositoryException", ex);
            }
        }
        if(log.isDebugEnabled())
            log.debug("fetch \""+field+"\" = \""+value+"\"");
        return value;
    }

    public void storeByteField(int fieldNumber, byte value) {
        String field = sm.getClassMetaData().getField(fieldNumber).getColumn();
        if(log.isDebugEnabled())
            log.debug("store \""+field+"\" = \""+value+"\"");
        if (field != null) {
            try {
                node.setProperty(field, value);
            } catch (ValueFormatException ex) {
                throw new JPOXDataStoreException("ValueFormatException", ex, value);
            } catch (VersionException ex) {
                throw new JPOXDataStoreException("VersionException", ex, value);
            } catch (ConstraintViolationException ex) {
                throw new JPOXDataStoreException("ConstraintViolationException", ex, value);
            } catch (LockException ex) {
                throw new JPOXDataStoreException("LockException", ex, value);
            } catch (RepositoryException ex) {
                throw new JPOXDataStoreException("RepositoryException", ex, value);
            }
        }
    }

    public short fetchShortField(int fieldNumber) {
        String field = sm.getClassMetaData().getField(fieldNumber).getColumn();
        short value = 0;
        if (field != null) {
            JCROID oid = (JCROID) sm.getExternalObjectId(null);
            try {
                Node node = oid.getNode(session);
                value = (short) node.getProperty(field).getLong();
            } catch (ValueFormatException ex) {
                throw new JPOXDataStoreException("ValueFormatException", ex);
            } catch (VersionException ex) {
                throw new JPOXDataStoreException("VersionException", ex);
            } catch (ConstraintViolationException ex) {
                throw new JPOXDataStoreException("ConstraintViolationException", ex);
            } catch (LockException ex) {
                throw new JPOXDataStoreException("LockException", ex);
            } catch (RepositoryException ex) {
                throw new JPOXDataStoreException("RepositoryException", ex);
            }
        }
        if(log.isDebugEnabled())
            log.debug("fetch \""+field+"\" = \""+value+"\"");
        return value;
    }

    public void storeIntField(int fieldNumber, int value) {
        String field = sm.getClassMetaData().getField(fieldNumber).getColumn();
        if(log.isDebugEnabled())
            log.debug("store \""+field+"\" = \""+value+"\"");
        if (field != null) {
            try {
                node.setProperty(field, value);
            } catch (ValueFormatException ex) {
                throw new JPOXDataStoreException("ValueFormatException", ex, value);
            } catch (VersionException ex) {
                throw new JPOXDataStoreException("VersionException", ex, value);
            } catch (ConstraintViolationException ex) {
                throw new JPOXDataStoreException("ConstraintViolationException", ex, value);
            } catch (LockException ex) {
                throw new JPOXDataStoreException("LockException", ex, value);
            } catch (RepositoryException ex) {
                throw new JPOXDataStoreException("RepositoryException", ex, value);
            }
        }
    }

    public int fetchIntField(int fieldNumber) {
        String field = sm.getClassMetaData().getField(fieldNumber).getColumn();
        int value = 0;
        if (field != null) {
            JCROID oid = (JCROID) sm.getExternalObjectId(null);
            try {
                Node node = oid.getNode(session);
                value = (int) node.getProperty(field).getLong();
            } catch (ValueFormatException ex) {
                throw new JPOXDataStoreException("ValueFormatException", ex);
            } catch (VersionException ex) {
                throw new JPOXDataStoreException("VersionException", ex);
            } catch (ConstraintViolationException ex) {
                throw new JPOXDataStoreException("ConstraintViolationException", ex);
            } catch (LockException ex) {
                throw new JPOXDataStoreException("LockException", ex);
            } catch (RepositoryException ex) {
                throw new JPOXDataStoreException("RepositoryException", ex);
            }
        }
        if(log.isDebugEnabled())
            log.debug("fetch \""+field+"\" = \""+value+"\"");
        return value;
    }

    public void storeLongField(int fieldNumber, long value) {
        String field = sm.getClassMetaData().getField(fieldNumber).getColumn();
        if(log.isDebugEnabled())
            log.debug("store \""+field+"\" = \""+value+"\"");
        if (field != null) {
            try {
                node.setProperty(field, value);
            } catch (ValueFormatException ex) {
                throw new JPOXDataStoreException("ValueFormatException", ex, value);
            } catch (VersionException ex) {
                throw new JPOXDataStoreException("VersionException", ex, value);
            } catch (ConstraintViolationException ex) {
                throw new JPOXDataStoreException("ConstraintViolationException", ex, value);
            } catch (LockException ex) {
                throw new JPOXDataStoreException("LockException", ex, value);
            } catch (RepositoryException ex) {
                throw new JPOXDataStoreException("RepositoryException", ex, value);
            }
        }
    }

    public long fetchLongField(int fieldNumber) {
        String field = sm.getClassMetaData().getField(fieldNumber).getColumn();
        long value = 0L;
        if (field != null) {
            JCROID oid = (JCROID) sm.getExternalObjectId(null);
            try {
                Node node = oid.getNode(session);
                value = node.getProperty(field).getLong();
            } catch (ValueFormatException ex) {
                throw new JPOXDataStoreException("ValueFormatException", ex);
            } catch (VersionException ex) {
                throw new JPOXDataStoreException("VersionException", ex);
            } catch (ConstraintViolationException ex) {
                throw new JPOXDataStoreException("ConstraintViolationException", ex);
            } catch (LockException ex) {
                throw new JPOXDataStoreException("LockException", ex);
            } catch (RepositoryException ex) {
                throw new JPOXDataStoreException("RepositoryException", ex);
            }
        }
        if(log.isDebugEnabled())
            log.debug("fetch \""+field+"\" = \""+value+"\"");
        return value;
    }

    public void storeFloatField(int fieldNumber, float value) {
        String field = sm.getClassMetaData().getField(fieldNumber).getColumn();
        if(log.isDebugEnabled())
            log.debug("store \""+field+"\" = \""+value+"\"");
        if (field != null) {
            try {
                node.setProperty(field, value);
            } catch (ValueFormatException ex) {
                throw new JPOXDataStoreException("ValueFormatException", ex, value);
            } catch (VersionException ex) {
                throw new JPOXDataStoreException("VersionException", ex, value);
            } catch (ConstraintViolationException ex) {
                throw new JPOXDataStoreException("ConstraintViolationException", ex, value);
            } catch (LockException ex) {
                throw new JPOXDataStoreException("LockException", ex, value);
            } catch (RepositoryException ex) {
                throw new JPOXDataStoreException("RepositoryException", ex, value);
            }
        }
    }

    public float fetchFloatField(int fieldNumber) {
        String field = sm.getClassMetaData().getField(fieldNumber).getColumn();
        float value = 0.0F;
        if (field != null) {
            JCROID oid = (JCROID) sm.getExternalObjectId(null);
            try {
                Node node = oid.getNode(session);
                value = (float) node.getProperty(field).getDouble();
            } catch (ValueFormatException ex) {
                throw new JPOXDataStoreException("ValueFormatException", ex);
            } catch (VersionException ex) {
                throw new JPOXDataStoreException("VersionException", ex);
            } catch (ConstraintViolationException ex) {
                throw new JPOXDataStoreException("ConstraintViolationException", ex);
            } catch (LockException ex) {
                throw new JPOXDataStoreException("LockException", ex);
            } catch (RepositoryException ex) {
                throw new JPOXDataStoreException("RepositoryException", ex);
            }
        }
        if(log.isDebugEnabled())
            log.debug("fetch \""+field+"\" = \""+value+"\"");
        return value;
    }

    public void storeDoubleField(int fieldNumber, double value) {
        String field = sm.getClassMetaData().getField(fieldNumber).getColumn();
        if(log.isDebugEnabled())
            log.debug("store \""+field+"\" = \""+value+"\"");
        if (field != null) {
            try {
                node.setProperty(field, value);
            } catch (ValueFormatException ex) {
                throw new JPOXDataStoreException("ValueFormatException", ex, value);
            } catch (VersionException ex) {
                throw new JPOXDataStoreException("VersionException", ex, value);
            } catch (ConstraintViolationException ex) {
                throw new JPOXDataStoreException("ConstraintViolationException", ex, value);
            } catch (LockException ex) {
                throw new JPOXDataStoreException("LockException", ex, value);
            } catch (RepositoryException ex) {
                throw new JPOXDataStoreException("RepositoryException", ex, value);
            }
        }
    }

    public double fetchDoubleField(int fieldNumber) {
        String field = sm.getClassMetaData().getField(fieldNumber).getColumn();
        double value = 0.0;
        if (field != null) {
            JCROID oid = (JCROID) sm.getExternalObjectId(null);
            try {
                Node node = oid.getNode(session);
                value = node.getProperty(field).getDouble();
            } catch (ValueFormatException ex) {
                throw new JPOXDataStoreException("ValueFormatException", ex);
            } catch (VersionException ex) {
                throw new JPOXDataStoreException("VersionException", ex);
            } catch (ConstraintViolationException ex) {
                throw new JPOXDataStoreException("ConstraintViolationException", ex);
            } catch (LockException ex) {
                throw new JPOXDataStoreException("LockException", ex);
            } catch (RepositoryException ex) {
                throw new JPOXDataStoreException("RepositoryException", ex);
            }
        }
        if(log.isDebugEnabled())
            log.debug("fetch \""+field+"\" = \""+value+"\"");
        return value;
    }

    public void storeStringField(int fieldNumber, String value) {
        String field = sm.getClassMetaData().getField(fieldNumber).getColumn();
        if(log.isDebugEnabled())
            log.debug("store \""+field+"\" = \""+value+"\"");
        if (field != null) {
            try {
                node.setProperty(field, value);
            } catch (ValueFormatException ex) {
                throw new JPOXDataStoreException("ValueFormatException", ex, value);
            } catch (VersionException ex) {
                throw new JPOXDataStoreException("VersionException", ex, value);
            } catch (ConstraintViolationException ex) {
                throw new JPOXDataStoreException("ConstraintViolationException", ex, value);
            } catch (LockException ex) {
                throw new JPOXDataStoreException("LockException", ex, value);
            } catch (RepositoryException ex) {
                throw new JPOXDataStoreException("RepositoryException", ex, value);
            }
        }
    }

    public String fetchStringField(int fieldNumber) {
        String field = sm.getClassMetaData().getField(fieldNumber).getColumn();
        String value = "";
        if (field != null) {
            JCROID oid = (JCROID) sm.getExternalObjectId(null);
            try {
                Node node = oid.getNode(session);
                value = node.getProperty(field).getString();
            } catch (ValueFormatException ex) {
                throw new JPOXDataStoreException("ValueFormatException", ex);
            } catch (VersionException ex) {
                throw new JPOXDataStoreException("VersionException", ex);
            } catch (ConstraintViolationException ex) {
                throw new JPOXDataStoreException("ConstraintViolationException", ex);
            } catch (LockException ex) {
                throw new JPOXDataStoreException("LockException", ex);
            } catch (RepositoryException ex) {
                throw new JPOXDataStoreException("RepositoryException", ex);
            }
        }
        if(log.isDebugEnabled())
            log.debug("fetch \""+field+"\" = \""+value+"\"");
        return value;
    }

    public void storeObjectField(int fieldNumber, Object value) {
        String field = sm.getClassMetaData().getField(fieldNumber).getColumn();
        if(log.isDebugEnabled())
            log.debug("store \""+field+"\" = \""+value+"\"");
        if (field == null)
            return;
        if (value == null)
            throw new NullPointerException();
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
                    String nodetype = nodetypeNode.getProperty(HippoNodeType.HIPPO_NODETYPE).getString();
                    child = node.addNode(field, nodetype);
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
                throw new JPOXDataStoreException("UnsupportedRepositoryOperationException", ex);
            } catch (PathNotFoundException ex) {
                throw new JPOXDataStoreException("PathNotFoundException", ex);
            } catch (VersionException ex) {
                throw new JPOXDataStoreException("VersionException", ex, value);
            } catch (ConstraintViolationException ex) {
                throw new JPOXDataStoreException("ConstraintViolationException", ex, value);
            } catch (LockException ex) {
                throw new JPOXDataStoreException("LockException", ex, value);
            } catch (RepositoryException ex) {
                throw new JPOXDataStoreException("RepositoryException", ex, value);
            }
        }
    }

    public Object fetchObjectField(int fieldNumber) {
        String field = sm.getClassMetaData().getField(fieldNumber).getColumn();
        Object value = null;
        if (field != null) {
            JCROID oid = (JCROID) sm.getExternalObjectId(null);
            try {
                Node node = oid.getNode(session);
                Node child = node.getNode(field);
                Class clazz = sm.getClassMetaData().getField(fieldNumber).getType();
                Object id = new JCROID(child.getUUID(), clazz.getName());
                StateManager pcSM = StateManagerFactory.newStateManagerForHollow(sm.getObjectManager(), clazz, id);
                //pcSM.replaceFields(pcSM.getClassMetaData().getAllFieldNumbers(), new FieldManagerImpl(sm, session, child));
                value = pcSM.getObject();
            } catch (ValueFormatException ex) {
                throw new JPOXDataStoreException("ValueFormatException", ex);
            } catch (VersionException ex) {
                throw new JPOXDataStoreException("VersionException", ex);
            } catch (ConstraintViolationException ex) {
                throw new JPOXDataStoreException("ConstraintViolationException", ex);
            } catch (LockException ex) {
                throw new JPOXDataStoreException("LockException", ex);
            } catch (RepositoryException ex) {
                throw new JPOXDataStoreException("RepositoryException", ex);
            }
        }
        if(log.isDebugEnabled())
            log.debug("fetch \""+field+"\" = \""+value+"\"");
        return value;
    }
}
