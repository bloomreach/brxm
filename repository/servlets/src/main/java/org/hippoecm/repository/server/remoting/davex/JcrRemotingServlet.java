/**
 * Copyright 2012 Hippo.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.server.remoting.davex;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrRemotingServlet extends org.apache.jackrabbit.server.remoting.davex.JcrRemotingServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(JcrRemotingServlet.class);

    public static final String REPOSITORY_ADDRESS_PARAM = "repository-address";

    private String repositoryAddress;
    private JcrHippoRepositoryWrapper repository;

    @Override
    public void init() throws ServletException {
        super.init();
        repositoryAddress = StringUtils.defaultIfEmpty(getInitParameter(REPOSITORY_ADDRESS_PARAM), "vm://");
    }

    @Override
    public void destroy() {
        if (repository != null) {
            repository.closeHippoRepository();
        }
        super.destroy();
    }

    /**
     * Returns the repository available from the servlet context of this
     * servlet.
     */
    @Override
    protected Repository getRepository() {
        if (repository == null) {
            try {
                repository = new JcrHippoRepositoryWrapper(HippoRepositoryFactory.getHippoRepository(repositoryAddress));
            } catch (RepositoryException e) {
                log.error("Repository is not found.", e);
            }
        }
        return repository;
    }
}

/**
 * JCR Repository implementation wrapping HippoRepository.
 */
class JcrHippoRepositoryWrapper implements Repository {

    private HippoRepository hippoRepository;

    JcrHippoRepositoryWrapper(HippoRepository hippoRepository) {
        if (hippoRepository == null) {
            throw new IllegalArgumentException("HippoRepository cannot be null!");
        }

        this.hippoRepository = hippoRepository;
    }

    public String getDescriptor(String key) {
        return hippoRepository.getRepository().getDescriptor(key);
    }

    public String[] getDescriptorKeys() {
        return hippoRepository.getRepository().getDescriptorKeys();
    }

    public Session login() throws LoginException, RepositoryException {
        return hippoRepository.login();
    }

    public Session login(Credentials credentials) throws LoginException, RepositoryException {
        if (!(credentials instanceof SimpleCredentials)) {
            throw new IllegalArgumentException("Only javax.jcr.SimpleCredentials is supported.");
        }

        return hippoRepository.login((SimpleCredentials) credentials);
    }

    public Session login(String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return login();
    }

    public Session login(Credentials credentials, String workspaceName) throws LoginException,
            NoSuchWorkspaceException, RepositoryException {
        return login(credentials);
    }

    public Value getDescriptorValue(String key) {
        return hippoRepository.getRepository().getDescriptorValue(key);
    }

    public Value[] getDescriptorValues(String key) {
        return hippoRepository.getRepository().getDescriptorValues(key);
    }

    public boolean isSingleValueDescriptor(String key) {
        return hippoRepository.getRepository().isSingleValueDescriptor(key);
    }

    public boolean isStandardDescriptor(String key) {
        return hippoRepository.getRepository().isStandardDescriptor(key);
    }

    void closeHippoRepository() {
        if (hippoRepository != null) {
            final String location = hippoRepository.getLocation();
            boolean isRemoteRepository = location != null && (location.startsWith("rmi:") || location.startsWith("http:") || location.startsWith("https:"));
            if (isRemoteRepository) {
                hippoRepository.close();
            }
        }
    }
}
