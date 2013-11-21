/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.decorating;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.onehippo.repository.RepositoryService;

/**
 * Simple {@link Repository Repository} decorator.
 */
public class RepositoryDecorator implements RepositoryService {

    private DecoratorFactory factory;

    private Repository repository;

    public RepositoryDecorator(DecoratorFactory factory, Repository repository) {
        this.factory = factory;
        this.repository = repository;
    }

    public static Repository unwrap(Repository repository) {
        if (repository == null) {
            return null;
        }
        if (repository instanceof RepositoryDecorator) {
            repository = ((RepositoryDecorator)repository).repository;
        }
        return repository;
    }

    /**
     * Forwards the method call to the underlying repository.
     */
    public String[] getDescriptorKeys() {
        return repository.getDescriptorKeys();
    }

    /**
     * Forwards the method call to the underlying repository.
     */
    public String getDescriptor(String key) {
        return repository.getDescriptor(key);
    }

    /**
     * Forwards the method call to the underlying repository. The returned
     * session is wrapped into a session decorator using the decorator factory.
     *
     * @return decorated session
     */
    public Session login(Credentials credentials, String workspaceName) throws LoginException,
            NoSuchWorkspaceException, RepositoryException {
        Session session = repository.login(credentials, workspaceName);
        return factory.getSessionDecorator(this, session);
    }

    /**
     * Calls <code>login(credentials, null)</code>.
     *
     * @return decorated session
     * @see #login(Credentials, String)
     */
    public Session login(Credentials credentials) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return login(credentials, null);
    }

    /**
     * Calls <code>login(null, workspaceName)</code>.
     *
     * @return decorated session
     * @see #login(Credentials, String)
     */
    public Session login(String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return login(null, workspaceName);
    }

    /**
     * Calls <code>login(null, null)</code>.
     *
     * @return decorated session
     * @see #login(Credentials, String)
     */
    public Session login() throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return login(null, null);
    }

    public boolean isStandardDescriptor(String key) {
        return repository.isStandardDescriptor(key);
    }

    public boolean isSingleValueDescriptor(String key) {
        return repository.isSingleValueDescriptor(key);
    }

    public Value getDescriptorValue(String key) {
        return repository.getDescriptorValue(key);
    }

    public Value[] getDescriptorValues(String key) {
        return repository.getDescriptorValues(key);
    }

}
