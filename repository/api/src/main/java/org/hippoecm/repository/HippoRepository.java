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
package org.hippoecm.repository;

import java.util.EnumSet;

import javax.transaction.NotSupportedException;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.repository.api.InitializationProcessor;
import org.hippoecm.repository.api.ReferenceWorkspace;
import org.hippoecm.repository.api.RepositoryMap;
import org.hippoecm.repository.api.ValueMap;

/**
 * Instances of this class represent a connection to the Repository. This interface is mainly provided to add some
 * convenience methods, additional functionality and provide a basis for starting transactional access to the
 * repository. The primary usage is to use create an authenticated session which can be used to retrieve
 * and store data. For this usage, some convenience login() methods are accessible though this interface, but these
 * are essentially the same as implemented in the JSR-170 based JCR repository, as would be returned by the method
 * getRepository().
 * @see javax.jcr.Repository
 */
public interface HippoRepository {

    /**
     * Creates a new Session for the current user, which might involve opening an anonymous session.
     * If a user identity and credentials are available through the application container,
     * then use JAAS to obtain the credentials and use login(SimpleCredentials).
     * @see Repository.login()
     */
    public Session login() throws LoginException, RepositoryException;

    /**
     * Creates a new Session for the user identifier with the indicated username and password.
     * @param username the username to use as part of the credentials
     * @param password the password to use as part of the credentials
     * @see Repository.login(Credentials)
     */
    public Session login(String username, char[] password) throws LoginException, RepositoryException;

    /**
     * Creates a new Session for the user identifier with the indicated credentials.
     * @see Repository.login(Credentials)
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
     * @return the URL of direct file path which is used to store information by the repository.
     */
    public String getLocation();

    /**
     * Returns the JCR-170 compliant repository object in use.
     * @return the JCR repository
     */
    public Repository getRepository();

    /**
     * <b>This call is not (yet) part of the API, but under evaluation.</b><p/>
     * Returns a java.util.Map representation of the subtree starting with the indicated node in the repository.
     * @param node the starting node of the subtree to map as a java.util.Map
     * @return a map representation of the content in the repository
     * @throws RepositoryException in case of a generic error occurs communicating with the repository
     */
    public RepositoryMap getRepositoryMap(Node node) throws RepositoryException;

    /**
     * <b>This call is not (yet) part of the API, but under evaluation.</b><p/>
     * Returns a ValueMap (which is an enhanced java.util.Map interface) representation of the subtree starting
     * with the indicated node in the repository.
     * @param node the starting node of the subtree to map as a java.util.Map
     * @return a map representation of the content in the repository
     * @throws RepositoryException in case of a generic error occurs communicating with the repository
     */
    public ValueMap getValueMap(Node node) throws RepositoryException;

    /**
     * <b>This call is not (yet) part of the API, but under evaluation.</b><p/>
     * returns true whenever the session has grown to such an extend that it is advisable to persist or flush changes.
     * @param session the session for which to compute the resource consumption
     * @param interests the states for which the threshold should be inspected, or null for any state
     * @return true when it is advisable to flush the state of the session, either by calling
     * session.save() or session.refresh(false)
     * @deprecated since 2.23.05 : This method has no replacement as it has never been possible to properly use it. It will be
     * removed from 2.25.xx
     */
    @Deprecated
    public boolean stateThresholdExceeded(Session session, EnumSet<SessionStateThresholdEnum> interests);

    /**
     * @return An {@link InitializationProcessor} for this repository for doing initialization tasks
     */
    public InitializationProcessor getInitializationProcessor();


    /**
     * Get or create a new reference workspace for comparing what changed since system bootstrap.
     * This is a lightweight operation only creating the workspace itself but not loading the configuration.
     *
     * @return a {@link ReferenceWorkspace}
     * @throws RepositoryException
     */
    public ReferenceWorkspace getOrCreateReferenceWorkspace() throws RepositoryException;
}
