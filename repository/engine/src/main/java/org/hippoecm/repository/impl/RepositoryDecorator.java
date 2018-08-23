/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.impl;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;

import org.hippoecm.hst.diagnosis.HDC;
import org.hippoecm.hst.diagnosis.Task;
import org.onehippo.repository.RepositoryService;
import org.onehippo.repository.security.JvmCredentials;

public class RepositoryDecorator implements RepositoryService {

    private Repository repository;

    public static RepositoryDecorator newRepositoryDecorator(final Repository repository) {
        return new RepositoryDecorator(repository);
    }

    public static Repository unwrap(final Repository repository) {
        if (repository instanceof RepositoryDecorator) {
            return ((RepositoryDecorator)repository).repository;
        }
        return repository;
    }

    RepositoryDecorator(final Repository repository) {
        this.repository = unwrap(repository);
    }

    @Override
    public String[] getDescriptorKeys() {
        return repository.getDescriptorKeys();
    }

    @Override
    public String getDescriptor(final String key) {
        if(REP_NAME_DESC.equals(key)) {
            return "Hippo Repository";
        } else if(REP_VENDOR_DESC.equals(key)) {
            return "BloomReach, Inc., BloomReach B.V.";
        } else if(REP_VENDOR_URL_DESC.equals(key)) {
            return "https://www.onehippo.org/";
        } else if(REP_VERSION_DESC.equals(key)) {
            return getClass().getPackage().getImplementationVersion();
        } else {
            return repository.getDescriptor(key);
        }
    }

    public boolean isStandardDescriptor(final String key) {
        return repository.isStandardDescriptor(key);
    }

    public boolean isSingleValueDescriptor(final String key) {
        return repository.isSingleValueDescriptor(key);
    }

    public Value getDescriptorValue(final String key) {
        return repository.getDescriptorValue(key);
    }

    public Value[] getDescriptorValues(final String key) {
        return repository.getDescriptorValues(key);
    }

    public Session login(final Credentials credentials) throws RepositoryException {
        return login(credentials, null);
    }

    public Session login(final String workspaceName) throws RepositoryException {
        return login(null, workspaceName);
    }

    public Session login() throws RepositoryException {
        return login(null, null);
    }

    public Session login(Credentials credentials, final String workspaceName) throws RepositoryException {
        Task loginTask = null;

        try {
            if (HDC.isStarted()) {
                loginTask = HDC.getCurrentTask().startSubtask("login");
            }

            if (credentials instanceof JvmCredentials) {
                JvmCredentials jvmCredentials = (JvmCredentials)credentials;
                credentials = new SimpleCredentials(jvmCredentials.getUserID(), jvmCredentials.getPassword());
            }
            Session session = repository.login(credentials, workspaceName);
            return new SessionDecorator(session);
        } finally {
            if (loginTask != null) {
                loginTask.stop();
            }
        }
    }
}
