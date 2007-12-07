/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository;

import javax.transaction.NotSupportedException;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

public interface HippoRepository {

    /**
     * Creates a new Session for the current user, which might involves opening an anonymous session,
     * however if a user identity and credentials are available through the application container,
     * then use JAAS to obtain the credentials.
     */
    public Session login() throws LoginException, RepositoryException;

    /**
     * Creates a new Session for the user identifier with the indicated username and password.
     */
    public Session login(String username, char[] password) throws LoginException, RepositoryException;

    /**
     * Creates a new Session for the user identifier with the indicated credentials.
     */
    public Session login(SimpleCredentials credentials) throws LoginException, RepositoryException;

    /**
     * Prepares the repository for application shutdown.  When the repository is running locally,
     * this also involves shutting down the repository.
     */
    public void close();
    
    /**
     * Get a UserTransaction from the JTA transaction manager through JNDI
     * @param session
     * @return a new UserTransaction object
     * @throws RepositoryException
     * @throws NotSupportedException
     */
    public UserTransaction getUserTransaction(Session session) throws RepositoryException, NotSupportedException;

    /**
     * Get a UserTransaction from the JTA transaction manager through JNDI
     * @param tm
     * @param session
     * @return a new UserTransaction object
     * @throws RepositoryException
     * @throws NotSupportedException
     */
    public UserTransaction getUserTransaction(TransactionManager tm, Session session) throws NotSupportedException;

    /**
     * Get the location, where the repository stores information.
     * @return the url of direct file path which is used to store information by the repository
     */
    public String getLocation();

    /**
     * Returns the JCR-170 compliant repository object in use.
     * @return the JCR repository
     */
    public Repository getRepository();
}
