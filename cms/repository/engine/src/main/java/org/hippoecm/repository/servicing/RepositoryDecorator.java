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
package org.hippoecm.repository.servicing;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.FacetedNavigationEngine;
import org.hippoecm.repository.FacetedNavigationEngineFirstImpl;
import org.hippoecm.repository.FacetedNavigationEngineWrapperImpl;
import org.hippoecm.repository.HippoRepositoryClassLoader;
import org.hippoecm.repository.jackrabbit.RepositoryImpl;

/**
 * Simple {@link Repository Repository} decorator.
 */
public class RepositoryDecorator implements Repository {

    private DecoratorFactory factory;

    private Repository repository;

    private FacetedNavigationEngine facetedEngine = null;

    private Session clSession;
    private ClassLoader loader;

    public RepositoryDecorator(DecoratorFactory factory, Repository repository) {
        this.factory = factory;
        this.repository = repository;
    }

    FacetedNavigationEngine getFacetedNavigationEngine() {
        if(facetedEngine == null) {
            if(repository instanceof RepositoryImpl) {
                facetedEngine = ((RepositoryImpl)repository).getFacetedNavigationEngine();
            } else {
                facetedEngine = new FacetedNavigationEngineWrapperImpl(new FacetedNavigationEngineFirstImpl());
            }
        }
        return facetedEngine;
    }

    /**
     * Returns a classloader for the repository.  A separate session is instantiated
     * so that loaded classes are shared between all sessions.
     */
    public synchronized ClassLoader getClassLoader() {
        if (loader == null) {
            try {
                clSession = repository.login();
                loader = new HippoRepositoryClassLoader(clSession);
            } catch (RepositoryException e) {
                e.printStackTrace();
                clSession = null;
                loader = null;
            }
        }
        return loader;
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
	/* FIXME: should get username from credentials and have some method to map these to facets
	 * match
	 */
        ServicingSessionImpl servicingSession = (ServicingSessionImpl) factory.getSessionDecorator(this, session);
	/* FIXME: then should initialize faceted engine this way, but missing input
         * FacetedNavigationEngine.Context context = getFacetedNavigationEngine().prepare(null, null, null, servicingSession);
	 * servicingSession.setFacetedNavigationContext(context);
	 */
        return servicingSession;
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

}
