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

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.jpox.exceptions.JPOXDataStoreException;
import org.jpox.identity.OID;

public class JCROID implements OID {
    String key;
    Node node;
    String classname;

    private JCROID() {
    }

    public JCROID(Node node, String classname) {
        try {
            this.key = node.getUUID();
            this.node = node;
            this.classname = classname;
        } catch (UnsupportedRepositoryOperationException ex) {
            throw new JPOXDataStoreException("node has no uuid");
        } catch (RepositoryException ex) {
            throw new JPOXDataStoreException(ex.getMessage(), ex);
        }
    }

    public JCROID(String key, String classname) {
        if (key == null)
            throw new NullPointerException();
        this.key = key;
        this.node = null;
        this.classname = classname;
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
        return classname;
    }

    public boolean equals(Object obj) {
        if (obj instanceof JCROID)
            return ((JCROID) obj).key.equals(key) && ((JCROID) obj).classname.equals(classname);
        else
            return false;
    }

    public int hashCode() {
        if (key == null)
            throw new NullPointerException();
        return key.hashCode() + classname.hashCode(); // FIXME
    }

    public String toString() {
        if (key == null)
            throw new NullPointerException();
        return key;
    }
}
