/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.spi.rmi.server;

import org.apache.jackrabbit.spi.EventFilter;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.commons.EventFilterImpl;

import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.PathNotFoundException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.NamespaceException;
import javax.jcr.MergeException;
import javax.jcr.LoginException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemExistsException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.AccessDeniedException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.util.Set;

/**
 * <code>ServerObject</code> provides utility methods to server objects.
 */
class ServerObject extends UnicastRemoteObject {

    ServerObject() throws RemoteException {
        super();
    }

    /**
     * Returns a cleaned version of the given exception. In some cases
     * the underlying repository implementation may throw exceptions
     * that are either unserializable, use exception subclasses that are
     * only locally available, contain references to unserializable or
     * only locally available classes. This method returns a cleaned
     * version of such an exception. The returned exception contains only
     * the message string from the original exception, and uses the public
     * JCR exception class that most specifically matches the original
     * exception.
     *
     * @param ex the original exception
     * @return clean exception
     */
    protected RepositoryException getRepositoryException(
            RepositoryException ex) {
        if (ex instanceof AccessDeniedException) {
            return new AccessDeniedException(ex.getMessage());
        } else if (ex instanceof ConstraintViolationException) {
            return new ConstraintViolationException(ex.getMessage());
        } else if (ex instanceof InvalidItemStateException) {
            return new InvalidItemStateException(ex.getMessage());
        } else if (ex instanceof InvalidQueryException) {
            return new InvalidQueryException(ex.getMessage());
        } else if (ex instanceof InvalidSerializedDataException) {
            return new InvalidSerializedDataException(ex.getMessage());
        } else if (ex instanceof ItemExistsException) {
            return new ItemExistsException(ex.getMessage());
        } else if (ex instanceof ItemNotFoundException) {
            return new ItemNotFoundException(ex.getMessage());
        } else if (ex instanceof LockException) {
            return new LockException(ex.getMessage());
        } else if (ex instanceof LoginException) {
            return new LoginException(ex.getMessage());
        } else if (ex instanceof MergeException) {
            return new MergeException(ex.getMessage());
        } else if (ex instanceof NamespaceException) {
            return new NamespaceException(ex.getMessage());
        } else if (ex instanceof NoSuchNodeTypeException) {
            return new NoSuchNodeTypeException(ex.getMessage());
        } else if (ex instanceof NoSuchWorkspaceException) {
            return new NoSuchWorkspaceException(ex.getMessage());
        } else if (ex instanceof PathNotFoundException) {
            return new PathNotFoundException(ex.getMessage());
        } else if (ex instanceof ReferentialIntegrityException) {
            return new ReferentialIntegrityException(ex.getMessage());
        } else if (ex instanceof UnsupportedRepositoryOperationException) {
            return new UnsupportedRepositoryOperationException(ex.getMessage());
        } else if (ex instanceof ValueFormatException) {
            return new ValueFormatException(ex.getMessage());
        } else if (ex instanceof VersionException) {
            return new VersionException(ex.getMessage());
        } else {
            return new RepositoryException(ex.getMessage());
        }
    }

    /**
     * Creates filter using the passed repository service.
     *
     * @param service the repository service
     * @param sInfo   the session info.
     * @param filters the event filters created by the repository service.
     * @return array of filter instances created by the passed repository
     *         service.
     * @throws RepositoryException if an error occurs.
     */
    protected EventFilter[] createLocalEventFilters(RepositoryService service,
                                                  SessionInfo sInfo,
                                                  EventFilter[] filters)
            throws RepositoryException {
        if (filters == null) {
            return null;
        }
        for (int i = 0; i < filters.length; i++) {
            if (filters[i] instanceof EventFilterImpl) {
                EventFilterImpl e = (EventFilterImpl) filters[i];
                Set nodeTypeNames = e.getNodeTypeNames();
                Name[] ntNames = null;
                if (nodeTypeNames != null) {
                    ntNames = (Name[]) nodeTypeNames.toArray(new Name[nodeTypeNames.size()]);
                }
                filters[i] = service.createEventFilter(sInfo,
                        e.getEventTypes(), e.getAbsPath(), e.isDeep(),
                        e.getUUIDs(), ntNames, e.getNoLocal());
            } else {
                throw new RepositoryException("Unknown EventFilter implementation: " +
                        filters[i].getClass().getName());
            }
        }
        return filters;
    }
}
