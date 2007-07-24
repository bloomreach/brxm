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
import org.jpox.store.exceptions.DatastorePermissionException;
import org.jpox.store.exceptions.NoExtentException;
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

import org.hippocms.repository.jr.embedded.HippoRepository;
import org.hippocms.repository.jr.embedded.HippoRepositoryFactory;

public class JCROID implements OID {
    static Integer keySequenceSeed;
    static {
        keySequenceSeed = new Integer(0);
    }
    String key;
    boolean isTemporary;
    Node node;

    public JCROID() {
        synchronized (keySequenceSeed) {
            this.key = Integer.toString(++keySequenceSeed);
        }
        this.node = null;
    }

    public JCROID(Node node) {
        synchronized (keySequenceSeed) {
            this.key = Integer.toString(++keySequenceSeed);
        }
        this.node = node;
    }

    public JCROID(String key) {
        if (key == null)
            throw new NullPointerException();
        this.key = key;
        this.node = null;
    }

    void validateKeyValue(ObjectManager om, PersistenceCapable pc, String newKey) {
        if (isTemporary) {
            om.replaceObjectId(pc, key, newKey);
            key = newKey;
            isTemporary = false;
        }
    }

    Node getNode(Session session) {
        if (node != null)
            return node;
        try {
            node = session.getNodeByUUID(key);
        } catch (ItemNotFoundException ex) {
            System.err.println(ex.getMessage());
        } catch (ValueFormatException ex) {
            System.err.println(ex.getMessage());
        } catch (VersionException ex) {
            System.err.println(ex.getMessage());
        } catch (ConstraintViolationException ex) {
            System.err.println(ex.getMessage());
        } catch (LockException ex) {
            System.err.println(ex.getMessage());
        } catch (RepositoryException ex) {
            System.err.println(ex.getMessage());
        }
        return node;
    }

    public Object getKeyValue() {
        if (key == null)
            throw new NullPointerException();
        return key;
    }

    public String getPcClass() {
        return "org.hippocms.repository.workflow.TestServiceImpl";
    }

    public boolean equals(Object obj) {
        if (obj instanceof JCROID)
            return ((JCROID) obj).key.equals(key);
        else
            return false;
    }

    public int hashCode() {
        if (key == null)
            throw new NullPointerException();
        return key.hashCode();
    }

    public String toString() {
        if (key == null)
            throw new NullPointerException();
        return key;
    }
}
