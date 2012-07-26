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

import java.io.Serializable;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.identity.OID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrOID implements OID, Serializable {

    private static final long serialVersionUID = 1L;


    static final Logger log = LoggerFactory.getLogger(JcrOID.class);

    public String key;
    public String classname;
    Node node;

    public JcrOID(Node node, String classname) {
        try {
            this.key = node.getIdentifier();
            this.node = node;
            this.classname = classname;
        } catch (UnsupportedRepositoryOperationException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException("node has no uuid");
        } catch (RepositoryException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            throw new NucleusDataStoreException(ex.getMessage(), ex);
        }
    }

    public JcrOID(String key, String classname) {
        if (key == null)
            throw new NullPointerException();
        this.key = key;
        this.node = null;
        this.classname = classname;
    }

    public static Node getNode(Session session, String key) {
        try {
            return session.getNodeByIdentifier(key);
        } catch (ItemNotFoundException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
        } catch (ValueFormatException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
        } catch (VersionException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
        } catch (ConstraintViolationException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
        } catch (LockException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
        } catch (RepositoryException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
        }
        return null;
    }

    public Node getNode(Session session) {
        if (node != null)
            return node;
        try {
            node = session.getNodeByIdentifier(key);
        } catch (ItemNotFoundException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
        } catch (ValueFormatException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
        } catch (VersionException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
        } catch (ConstraintViolationException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
        } catch (LockException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
        } catch (RepositoryException ex) {
            if (log.isDebugEnabled()) {
                log.debug("failed", ex);
            }
            log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
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

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof JcrOID)
            return ((JcrOID) obj).key.equals(key) && ((JcrOID) obj).classname.equals(classname);
        else
            return false;
    }

    @Override
    public int hashCode() {
        if (key == null)
            throw new NullPointerException();
        return key.hashCode() ^ classname.hashCode();
    }

    @Override
    public String toString() {
        if (key == null)
            throw new NullPointerException();
        return key;
    }
}
