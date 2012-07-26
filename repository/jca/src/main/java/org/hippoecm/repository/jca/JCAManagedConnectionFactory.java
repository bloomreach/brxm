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
package org.hippoecm.repository.jca;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.Subject;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Set;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.api.HippoSession;

/**
 * Implements the JCA ManagedConnectionFactory contract.
 */
public class JCAManagedConnectionFactory implements ManagedConnectionFactory {

    /**
     * Home directory.
     */
    private String location;

    /**
     * Flag indicating whether the session should be bound to the
     * transaction lyfecyle.
     * In other words, if this flag is true the handle
     * will be closed when the transaction ends.
     */
    private Boolean bindSessionToTransaction = Boolean.TRUE;

    /**
     * Repository.
     */
    private transient HippoRepository repository;

    /**
     * Log writer.
     */
    private transient PrintWriter logWriter;

    /**
     * Return the repository home directory.
     */
    public String getLocation() {
        return location;
    }

    /**
     * Set the repository home directory.
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Get the log writer.
     */
    public PrintWriter getLogWriter() {
        return logWriter;
    }

    /**
     * Set the log writer.
     */
    public void setLogWriter(PrintWriter logWriter)
            throws ResourceException {
        this.logWriter = logWriter;
    }

    /**
     * Creates a Connection Factory instance.
     */
    public Object createConnectionFactory()
            throws ResourceException {
        return createConnectionFactory(new JCAConnectionManager());
    }

    /**
     * Creates a Connection Factory instance.
     */
    public Object createConnectionFactory(ConnectionManager cm)
            throws ResourceException {
        createRepository();
        JCARepositoryHandle handle = new JCARepositoryHandle(this, cm);
        log("Created repository handle (" + handle + ")");
        return handle;
    }

    /**
     * Create a new session.
     */
    private HippoSession openSession(JCAConnectionRequestInfo cri)
            throws ResourceException {
        createRepository();
        Credentials creds = cri.getCredentials();
        String workspace = cri.getWorkspace();

        try {
            HippoSession session = (HippoSession) getRepository().login(creds,workspace);
            log("Created session (" + session + ")");
            return session;
        } catch (RepositoryException e) {
            log("Failed to create session", e);
            ResourceException exception = new ResourceException("Failed to create session: " + e.getMessage());
            exception.setLinkedException(e);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Creates a new physical connection to the underlying EIS resource manager.
     * <p/>
     * WebSphere 5.1.1 will try to recover an XA resource on startup, regardless
     * whether it was committed or rolled back. On this occasion, <code>cri</code>
     * will be <code>null</code>. In order to be interoperable, we return an
     * anonymous connection, whose XA resource is recoverable-only.
     */
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cri)
            throws ResourceException {

        if (cri == null) {
            return new AnonymousConnection();
        }
        return createManagedConnection((JCAConnectionRequestInfo) cri);
    }

    /**
     * Creates a new physical connection to the underlying EIS resource manager.
     */
    private ManagedConnection createManagedConnection(JCAConnectionRequestInfo cri)
            throws ResourceException {
        return new JCAManagedConnection(this, cri, openSession(cri));
    }

    /**
     * Returns a matched connection from the candidate set of connections.
     */
    public ManagedConnection matchManagedConnections(Set set, Subject subject, ConnectionRequestInfo cri)
            throws ResourceException {
        for (Iterator i = set.iterator(); i.hasNext();) {
            Object next = i.next();

            if (next instanceof JCAManagedConnection) {
                JCAManagedConnection mc = (JCAManagedConnection) next;
                if (equals(mc.getManagedConnectionFactory())) {
                    JCAConnectionRequestInfo otherCri = mc.getConnectionRequestInfo();
                    if(cri == null && otherCri == null)
                        return mc;
                    else if(cri.equals(otherCri))
                        return mc;
                }
            }
        }

        return null;
    }

    /**
     * Return the repository.
     */
    public Repository getRepository() {
        return repository.getRepository();
    }

    /**
     * Log a message.
     */
    public void log(String message) {
        log(message, null);
    }

    /**
     * Log a message.
     */
    public void log(String message, Throwable exception) {
        if (logWriter != null) {
            logWriter.println(message);

            if (exception != null) {
                exception.printStackTrace(logWriter);
            }
        }
    }

    /**
     * Return the hash code.
     */
    public int hashCode() {
        return location != null ? location.hashCode() : 0;
    }

    /**
     * Return true if equals.
     */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof JCAManagedConnectionFactory) {
            if(location == null) {
                return ((JCAManagedConnectionFactory)o).location == null;
            } else {
                return location.equals(((JCAManagedConnectionFactory)o).location);
            }
        } else {
            return false;
        }
    }

    /**
     * Create repository.
     */
    private void createRepository()
            throws ResourceException {
        if (repository == null) {
            try {
                JCARepositoryManager mgr = JCARepositoryManager.getInstance();
                repository = mgr.createRepository(location);
                log("Created repository (" + repository + ")");
            } catch (RepositoryException e) {
                log("Failed to create repository", e);
                ResourceException exception = new ResourceException(
                        "Failed to create session: " + e.getMessage());
                exception.setLinkedException(e);
                throw exception;
            }
        }
    }

    /**
     * Shutdown the repository.
     */
    protected void finalize() {
        JCARepositoryManager mgr = JCARepositoryManager.getInstance();
        mgr.autoShutdownRepository(location);
    }

    public Boolean getBindSessionToTransaction() {
        return bindSessionToTransaction;
    }

    public void setBindSessionToTransaction(Boolean bindSessionToTransaction) {
        this.bindSessionToTransaction = bindSessionToTransaction;
    }

}
