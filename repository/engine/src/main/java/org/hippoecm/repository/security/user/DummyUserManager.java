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
package org.hippoecm.repository.security.user;

import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.security.ManagerContext;

/**
 * Dummy user implementation that is used when the provider doesn't support
 * it's own user manager implementation.
 */
public class DummyUserManager extends AbstractUserManager {


    public static final String PROVIDER_ID = "<dummyProvider>";

    public DummyUserManager() {
        providerId = PROVIDER_ID;
    }

    @Override
    public boolean isPasswordExpired(String rawUserId) throws RepositoryException {
        return false;
    }

    public void initManager(ManagerContext context) throws RepositoryException {
        initialized = true;
    }

    public boolean authenticate(SimpleCredentials creds) throws RepositoryException {
        return false;
    }

    public void syncUserInfo(String userId) {
        return;
    }

    public String getNodeType() {
        return HippoNodeType.NT_USER;
    }

    public boolean isCaseSensitive() {
        return true;
    }

    @Override
    public boolean isAutoSave() {
        return true;
    }
}
