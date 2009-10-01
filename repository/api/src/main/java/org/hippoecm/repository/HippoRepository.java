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
package org.hippoecm.repository;

import javax.transaction.NotSupportedException;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.repository.api.RepositoryMap;
import org.hippoecm.repository.api.ValueMap;

/**
 * Instances of this class represent a connection to the Repository. This interface is mainly provided to add some
 * convienience methods, additional functionality and provide a basis for starting transactional access to the
 * repository. The primary usage is to use create an authenticated session which can be used to retrieve
 * and store data. For this usage, some convenience login() methods are accessible though this interface, but these
 * are essentially the same as implemented in the JSR-170 based JCR repository, as would be returned by the method
 * getRepository(). 
 */
public interface HippoRepository {
    final static String SVN_ID = "$Id$";

    /**
     * Creates a new Session for the current user, which might involve opening an anonymous session.
     * If a user identity and credentials are available through the application container,
     * then use JAAS to obtain the credentials and use login(SimpleCredentials).
     * @return 
     * @throws LoginException  indicates an authenticaion failure
     * @throws RepositoryException indicates some other failure while authenticating (such as connection error)
     */
    public Session login() throws LoginException, RepositoryException;

    /**
     * Creates a new Session for the user identifier with the indicated username and password.
     * @param username the username to use as part of the credentials
     * @param password the password to use as part of the credentials
     * @return a authenticated session based on the given credentials
     * @throws LoginException indicates an authenticaion failure
     * @throws RepositoryException indicates some other failure while authenticating (such as connection error)
     */
    public Session login(String username, char[] password) throws LoginException, RepositoryException;

    /**
     * Creates a new Session for the user identifier with the indicated credentials.
     * @param credentials
     * @return a authenticated session based on the given credentials
     * @throws LoginException indicates an authenticaion failure
     * @throws RepositoryException indicates some other failure while authenticating (such as connection error)
     */
    public Session login(SimpleCredentials credentials) throws LoginException, RepositoryException;

    /**
     * Closes the repository connection. When the repository is running locally,
     * this also involves shutting down the repository.
     */
    public void close();

    /**
     * Get a UserTransaction from the JTA transaction manager through JNDI.
     * @param session the session for which the transaction support is requested
     * @return a new JTA UserTransaction object
     * @throws RepositoryException generic error such as a connection error
     * @throws NotSupportedException indicates transactions are not supported in the set up
     */
    public UserTransaction getUserTransaction(Session session) throws RepositoryException, NotSupportedException;

    /**
     * Get a UserTransaction from the JTA transaction manager through JNDI.
     * @param tm the JTA transaction manager to use
     * @param session the JCR session for which to start the transaction
     * @return a new UserTransaction object
     * @throws NotSupportedException indicates transactions are not supported in the set up
     */
    public UserTransaction getUserTransaction(TransactionManager tm, Session session) throws NotSupportedException;

    /**
     * Get the location where the repository stores information.
     * @return the url of direct file path which is used to store information by the repository.
     */
    public String getLocation();

    /**
     * Returns the JCR-170 compliant repository object in use.
     * @return the JCR repository
     */
    public Repository getRepository();

    /**
     * This call is not (yet) part of the API, but under evaluation.
     */
    public RepositoryMap getRepositoryMap(Node node) throws RepositoryException;

    /**
     * This call is not (yet) part of the API, but under evaluation.
     */
    public ValueMap getValueMap(Node node) throws RepositoryException;
}
