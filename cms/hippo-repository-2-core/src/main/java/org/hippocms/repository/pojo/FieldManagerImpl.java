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
package org.hippocms.repository.pojo;

import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import javax.jdo.identity.SingleFieldIdentity;
import javax.jdo.spi.PersistenceCapable;

import org.jpox.ClassLoaderResolver;
import org.jpox.ConnectionFactory;
import org.jpox.ManagedConnection;
import org.jpox.ObjectManager;
import org.jpox.ObjectManagerFactoryImpl;
import org.jpox.PersistenceConfiguration;
import org.jpox.StateManager;
import org.jpox.exceptions.ClassNotResolvedException;
import org.jpox.exceptions.JPOXDataStoreException;
import org.jpox.exceptions.JPOXException;
import org.jpox.exceptions.JPOXObjectNotFoundException;
import org.jpox.exceptions.JPOXOptimisticException;
import org.jpox.exceptions.JPOXUserException;
import org.jpox.exceptions.NoPersistenceInformationException;
import org.jpox.metadata.AbstractClassMetaData;
import org.jpox.metadata.AbstractPropertyMetaData;
import org.jpox.metadata.ClassMetaData;
import org.jpox.metadata.ClassPersistenceModifier;
import org.jpox.metadata.ExtensionMetaData;
import org.jpox.metadata.IdentityStrategy;
import org.jpox.metadata.IdentityType;
import org.jpox.metadata.IdentityMetaData;
import org.jpox.metadata.SequenceMetaData;
import org.jpox.metadata.VersionStrategy;
import org.jpox.plugin.ConfigurationElement;
import org.jpox.sco.SCO;
import org.jpox.store.DatastoreClass;
import org.jpox.store.DatastoreContainerObject;
import org.jpox.store.DatastoreObject;
import org.jpox.store.Extent;
import org.jpox.store.FetchStatement;
import org.jpox.store.JPOXConnection;
import org.jpox.store.JPOXSequence;
import org.jpox.store.OID;
import org.jpox.store.OIDFactory;
import org.jpox.store.SCOID;
import org.jpox.store.StoreData;
import org.jpox.store.TableStoreData;
import org.jpox.store.StoreManager;
import org.jpox.store.StoreManagerFactory;

import org.jpox.store.FieldValues;
import org.jpox.JDOFetchPlanImpl;
import org.jpox.state.StateManagerFactory;
import org.jpox.state.LifeCycleState;

import org.jpox.store.exceptions.DatastorePermissionException;
import org.jpox.store.exceptions.NoExtentException;
import org.jpox.store.fieldmanager.AbstractFieldManager;
import org.jpox.store.fieldmanager.DeleteFieldManager;
import org.jpox.store.fieldmanager.PersistFieldManager;
import org.jpox.store.poid.PoidConnectionProvider;
import org.jpox.store.poid.PoidGenerator;
import org.jpox.store.scostore.ArrayStore;
import org.jpox.store.scostore.CollectionStore;
import org.jpox.store.scostore.MapStore;
import org.jpox.util.AIDUtils;
import org.jpox.util.ClassUtils;
import org.jpox.util.JPOXLogger;
import org.jpox.util.Localiser;
import org.jpox.util.StringUtils;
import org.jpox.util.TypeConversionHelper;
import org.jpox.util.MacroString.IdentifierMacro;

import javax.jcr.Session;
import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ValueFormatException;
import javax.jcr.version.VersionException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.PathNotFoundException;
import javax.jcr.ItemExistsException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.hippocms.repository.jr.embedded.HippoRepository;
import org.hippocms.repository.jr.embedded.HippoRepositoryFactory;

class FieldManagerImpl extends AbstractFieldManager {
    private StateManager sm;
    private Session session;
    private Node node;

    FieldManagerImpl(StateManager sm, Session session, Node node) {
        this.sm = sm;
        this.session = session;
        this.node = node;
    }

    FieldManagerImpl(StateManager sm, Session session) {
        this.sm = sm;
        this.session = session;
        this.node = null;
    }

    public void storeBooleanField(int fieldNumber, boolean value) {
        String field = sm.getClassMetaData().getField(fieldNumber).getColumn();
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
        if (field != null) {
            JCROID oid = (JCROID) sm.getExternalObjectId(null);
            try {
                Node node = oid.getNode(session);
                return node.getProperty(field).getBoolean();
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
        } else
            return false;
    }

    public void storeCharField(int fieldNumber, char value) {
        String field = sm.getClassMetaData().getField(fieldNumber).getColumn();
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
        if (field != null) {
            JCROID oid = (JCROID) sm.getExternalObjectId(null);
            try {
                Node node = oid.getNode(session);
                return node.getProperty(field).getString().charAt(0);
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
        } else
            return ' ';
    }

    public void storeByteField(int fieldNumber, byte value) {
        String field = sm.getClassMetaData().getField(fieldNumber).getColumn();
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
        if (field != null) {
            JCROID oid = (JCROID) sm.getExternalObjectId(null);
            try {
                Node node = oid.getNode(session);
                return (short) node.getProperty(field).getLong();
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
        } else
            return 0;
    }

    public void storeIntField(int fieldNumber, int value) {
        String field = sm.getClassMetaData().getField(fieldNumber).getColumn();
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
        if (field != null) {
            JCROID oid = (JCROID) sm.getExternalObjectId(null);
            try {
                Node node = oid.getNode(session);
                return (int) node.getProperty(field).getLong();
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
        } else
            return 0;
    }

    public void storeLongField(int fieldNumber, long value) {
        String field = sm.getClassMetaData().getField(fieldNumber).getColumn();
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
        if (field != null) {
            JCROID oid = (JCROID) sm.getExternalObjectId(null);
            try {
                Node node = oid.getNode(session);
                return node.getProperty(field).getLong();
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
        } else
            return 0L;
    }

    public void storeFloatField(int fieldNumber, float value) {
        String field = sm.getClassMetaData().getField(fieldNumber).getColumn();
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
        if (field != null) {
            JCROID oid = (JCROID) sm.getExternalObjectId(null);
            try {
                Node node = oid.getNode(session);
                return (float) node.getProperty(field).getDouble();
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
        } else
            return 0.0F;
    }

    public void storeDoubleField(int fieldNumber, double value) {
        String field = sm.getClassMetaData().getField(fieldNumber).getColumn();
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
        if (field != null) {
            JCROID oid = (JCROID) sm.getExternalObjectId(null);
            try {
                Node node = oid.getNode(session);
                return node.getProperty(field).getDouble();
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
        } else
            return 0.0;
    }

    public void storeStringField(int fieldNumber, String value) {
        String field = sm.getClassMetaData().getField(fieldNumber).getColumn();
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
        if (field != null) {
            JCROID oid = (JCROID) sm.getExternalObjectId(null);
            try {
                Node node = oid.getNode(session);
                return node.getProperty(field).getString();
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
        } else
            return "";
    }

    public void storeObjectField(int fieldNumber, Object value) {
        String field = sm.getClassMetaData().getField(fieldNumber).getColumn();
        if (value == null)
            throw new NullPointerException();
        StateManager valueSM = sm.getObjectManager().findStateManager((PersistenceCapable) value);
        if (valueSM == null) { // If not already persisted
            PersistenceCapable pc = (PersistenceCapable) value;
            if (pc == null)
                throw new NullPointerException();
            try {
                Node child = node.addNode(field);
                child.addMixin("mix:referenceable"); // FIXME: should be either per node type definition or not necessary at all
                Object id = new JCROID(child.getUUID());
                StateManager pcSM = StateManagerFactory
                        .newStateManagerForPersistentClean(sm.getObjectManager(), id, pc);
                pcSM.provideFields(pcSM.getClassMetaData().getAllFieldNumbers(), new FieldManagerImpl(pcSM, session,
                        child));
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
        if (field != null) {
            JCROID oid = (JCROID) sm.getExternalObjectId(null);
            try {
                Node node = oid.getNode(session);
                Node child = node.getNode(field);
                Object id = new JCROID(child.getUUID());
                Class clazz = sm.getClassMetaData().getField(fieldNumber).getType();
                StateManager pcSM = StateManagerFactory.newStateManagerForHollow(sm.getObjectManager(), clazz, id);
                //pcSM.replaceFields(pcSM.getClassMetaData().getAllFieldNumbers(), new FieldManagerImpl(sm, session, child));
                return pcSM.getObject();
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
        } else
            return null;
    }
}
