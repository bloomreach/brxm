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
package org.hippoecm.repository.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Manifest;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.decorating.DecoratorFactory;

/**
 * Simple {@link Repository Repository} decorator.
 */
public class RepositoryDecorator extends org.hippoecm.repository.decorating.RepositoryDecorator implements Repository {

    private Repository repository;
    DecoratorFactory factory;

    public RepositoryDecorator(DecoratorFactory factory, Repository repository) {
        super(factory, repository);
        this.repository = repository;
        this.factory = factory;
    }

    /**
     * Calls <code>login(credentials, null)</code>.
     *
     * @return decorated session
     * @see #login(Credentials, String)
     */
    @Override
    public Session login(Credentials credentials) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return login(credentials, null);
    }

    /**
     * Calls <code>login(null, workspaceName)</code>.
     *
     * @return decorated session
     * @see #login(Credentials, String)
     */
    @Override
    public Session login(String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return login(null, workspaceName);
    }

    /**
     * Calls <code>login(null, null)</code>.
     *
     * @return decorated session
     * @see #login(Credentials, String)
     */
    @Override
    public Session login() throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return login(null, null);
    }

    @Override
    public Session login(Credentials credentials, String workspaceName) throws LoginException,
            NoSuchWorkspaceException, RepositoryException {
        Session session = repository.login(credentials, workspaceName);
        return DecoratorFactoryImpl.getSessionDecorator(session);
    }

    @Override
    public String getDescriptor(String key) {
        if(REP_NAME_DESC.equals(key)) {
            return "Hippo Repository";
        } else if(REP_VENDOR_DESC.equals(key)) {
            return "Hippo B.V.";
        } else if(REP_VENDOR_URL_DESC.equals(key)) {
            return "http://www.onehippo.org/";
        } else if(REP_VERSION_DESC.equals(key)) {
            try {
                InputStream istream = HippoRepositoryFactory.getManifest(getClass()).openStream();
                if (istream != null) {
                    Manifest manifest = new Manifest(istream);
                    return manifest.getMainAttributes().getValue("Implementation-Version") + " build " + manifest.getMainAttributes().getValue("Implementation-Build");
                } else {
                    return null;
                }
            } catch(IOException ex) {
                return null;
            }
        } else {
            return super.getDescriptor(key);
        }
    }
}
