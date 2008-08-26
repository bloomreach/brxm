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
package org.hippoecm.repository.security.user;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.PasswordHelper;
import org.hippoecm.repository.security.ManagerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UserManager backend that stores the users inside the JCR repository
 */
public class RepositoryUserManager extends AbstractUserManager {

    /** SVN id placeholder */
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    /**
     * Logger
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    //------------------------< Interface Impl >--------------------------//

    /**
     * {@inheritDoc}
     */
    public void initManager(ManagerContext context) throws RepositoryException {
        initialized = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean authenticate(SimpleCredentials creds) throws RepositoryException {
        if (!isInitialized()) {
            throw new IllegalStateException("Not initialized.");
        }
        try {
            return PasswordHelper.checkHash(creds.getPassword(), getPasswordHash(getUserNode(creds.getUserID())));
        } catch (NoSuchAlgorithmException e) {
            throw new RepositoryException("Unknown algorithm found when authenticating user: " + creds.getUserID(), e);
        } catch (UnsupportedEncodingException e) {
            throw new RepositoryException("Unsupported encoding found when authenticating user: " + creds.getUserID(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> listUsers() throws RepositoryException {
        if (!isInitialized()) {
            throw new IllegalStateException("Not initialized.");
        }

        // find users managed by this provider as sub nodes off the users path
        StringBuffer statement = new StringBuffer();
        statement.append("SELECT * FROM ").append(HippoNodeType.NT_USER);
        statement.append(" WHERE ");
        statement.append("  jcr:path LIKE '").append(usersPath).append("/%").append("'");
        statement.append(" AND jcr:PrimaryType = '").append(HippoNodeType.NT_USER).append("')");

        //log.debug("Searching for users: {}", statement);

        Set<String> userIds = new HashSet<String>();

        // find users managed by this provider
        QueryManager qm;
        try {
            qm = session.getWorkspace().getQueryManager();
            Query q = qm.createQuery(statement.toString(), Query.SQL);
            QueryResult result = q.execute();
            NodeIterator iter = result.getNodes();
            while (iter.hasNext()) {
                userIds.add(iter.nextNode().getName());
            }
        } catch (RepositoryException e) {
            log.warn("Exception while parsing users from path: {}", usersPath);
        }
        return Collections.unmodifiableSet(userIds);
    }

    //------------------------< Private Helper methods >--------------------------//

    /**
     * Get the (optionally) hashed password of the user
     * @return the password hash
     * @throws RepositoryException
     */
    private String getPasswordHash(Node user) throws RepositoryException {
        return user.getProperty(HippoNodeType.HIPPO_PASSWORD).getString();
    }

}
