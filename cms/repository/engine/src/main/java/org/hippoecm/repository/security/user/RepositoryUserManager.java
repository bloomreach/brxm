/*
 * Copyright 2008 Hippo
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
package org.hippoecm.repository.security.user;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.transaction.NotSupportedException;

import org.hippoecm.repository.security.AAContext;
import org.hippoecm.repository.security.RepositoryAAContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UserManager backend that stores the users inside the JCR repository
 */
public class RepositoryUserManager implements UserManager {

    /** SVN id placeholder */
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    
    /**
     * The current context
     */
    private RepositoryAAContext context;

    /**
     * The system/root session
     */
    private Session session;

    /**
     * The path from the root containing the groups
     */
    private String usersPath;

    /**
     * Is the class initialized
     */
    private boolean initialized = false;


    /**
     * The current groups
     */
    private Set<User> users = new HashSet<User>();
    
    /**
     * Logger
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());


    //------------------------< Interface Impl >--------------------------//
    /**
     * {@inheritDoc}
     */
    public void init(AAContext aacontext) throws UserException {
        context = (RepositoryAAContext) aacontext;
        session = context.getRootSession();
        usersPath = context.getPath();
        loadUsers();
        initialized = true;
    }

    /**
     * {@inheritDoc}
     */
    public Set<User> listUsers() throws UserException {
        if (!initialized) {
            throw new IllegalStateException("Not initialized.");
        }
        return Collections.unmodifiableSet(users);
    }

    /**
     * {@inheritDoc}
     */
    public boolean addUser(User user) throws NotSupportedException, UserException {
        throw new NotSupportedException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    public boolean deleteUser(User user) throws NotSupportedException, UserException {
        throw new NotSupportedException("Not implemented");
    }

    
    //------------------------< Private Helper methods >--------------------------//
    /**
     * Load all the users from the repository. All users are added to the Set of users
     * @see User
     */
    private void loadUsers() {
        log.debug("Searching for users path: {}", usersPath);
        try {
            Node usersPathNode = session.getRootNode().getNode(usersPath);
            log.debug("Found users node: {}", usersPath);
            NodeIterator groupIter = usersPathNode.getNodes();
            while (groupIter.hasNext()) {
                User user = new RepositoryUser();
                user.init(context, groupIter.nextNode().getName());
                users.add(user);
            }
        } catch (RepositoryException e) {
            log.warn("Exception while parsing users from path: {}", usersPath);
        }
    }
}
