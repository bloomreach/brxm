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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.PasswordHelper;
import org.hippoecm.repository.security.ManagerContext;

/**
 * UserManager backend that stores the users inside the JCR repository
 */
public class RepositoryUserManager extends AbstractUserManager {

    /** SVN id placeholder */
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public void initManager(ManagerContext context) throws RepositoryException {
        initialized = true;
    }
    
    /**
     * Authenticate the user against the hash stored in the user node
     */
    public boolean authenticate(SimpleCredentials creds) throws RepositoryException {
        if (!isInitialized()) {
            throw new IllegalStateException("Not initialized.");
        }
        // check if user exists
        if (!hasUser(creds.getUserID())) {
            return false;
        }
        try {
            return PasswordHelper.checkHash(creds.getPassword(), getPasswordHash(getUser(creds.getUserID())));
        } catch (NoSuchAlgorithmException e) {
            throw new RepositoryException("Unknown algorithm found when authenticating user: " + creds.getUserID(), e);
        } catch (UnsupportedEncodingException e) {
            throw new RepositoryException("Unsupported encoding found when authenticating user: " + creds.getUserID(), e);
        }
    }

    public String getNodeType() {
        return HippoNodeType.NT_USER;
    }
    
    /**
     * The backend is the repository, no need to sync anything.
     */
    public void syncUserInfo(String userId) {
        return;
    }
    
    /**
     * Get the (optionally) hashed password of the user
     * @return the password hash
     * @throws RepositoryException
     */
    private String getPasswordHash(Node user) throws RepositoryException {
        if (user.hasProperty(HippoNodeType.HIPPO_PASSWORD)) {
            return user.getProperty(HippoNodeType.HIPPO_PASSWORD).getString();
        } else {
            return null;
        }
    }
}
