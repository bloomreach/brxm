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

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import javax.jdo.spi.PersistenceCapable;

import org.jpox.ObjectManager;
import org.jpox.store.OID;

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
