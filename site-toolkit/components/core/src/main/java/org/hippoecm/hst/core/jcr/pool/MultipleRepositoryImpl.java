package org.hippoecm.hst.core.jcr.pool;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

public class MultipleRepositoryImpl implements MultipleRepository {

    private static ThreadLocal<Repository> tlCurrentRepository = new ThreadLocal<Repository>();
    
    protected Map<CredentialsWrapper, Repository> repositoryMap;
    protected CredentialsWrapper defaultCredentialsWrapper;
    
    public MultipleRepositoryImpl(Map<Credentials, Repository> repoMap, Credentials defaultCredentials) {
        this.repositoryMap = new HashMap<CredentialsWrapper, Repository>();
        
        for (Map.Entry<Credentials, Repository> entry : repoMap.entrySet()) {
            this.repositoryMap.put(new CredentialsWrapper(entry.getKey()), entry.getValue());
        }
        
        this.defaultCredentialsWrapper = new CredentialsWrapper(defaultCredentials);
    }
    
    public Repository getRepositoryByCredentials(Credentials credentials) {
        return this.repositoryMap.get(new CredentialsWrapper(credentials));
    }

    public String getDescriptor(String arg0) {
        String descriptor = null;
        Repository curRepository = tlCurrentRepository.get();
        
        if (curRepository != null) {
            descriptor = curRepository.getDescriptor(arg0);
        }
        
        return descriptor;
    }

    public String[] getDescriptorKeys() {
        String [] descriptorKeys = null;
        Repository curRepository = tlCurrentRepository.get();
        
        if (curRepository != null) {
            descriptorKeys = curRepository.getDescriptorKeys();
        }
        
        return descriptorKeys;
    }

    public Session login() throws LoginException, RepositoryException {
        Repository repository = this.repositoryMap.get(this.defaultCredentialsWrapper);
        
        if (repository == null) {
            throw new RepositoryException("The default repository is not available."); 
        }
        
        tlCurrentRepository.set(repository);
        
        return repository.login();
    }

    public Session login(Credentials arg0) throws LoginException, RepositoryException {
        Repository repository = this.repositoryMap.get(arg0);
        
        if (repository == null) {
            throw new RepositoryException("The repository is not available."); 
        }

        tlCurrentRepository.set(repository);
        
        return repository.login(arg0);
    }

    public Session login(String arg0) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        Repository repository = this.repositoryMap.get(this.defaultCredentialsWrapper);
        
        if (repository == null) {
            throw new RepositoryException("The default repository is not available."); 
        }

        tlCurrentRepository.set(repository);
        
        return repository.login(arg0);
    }

    public Session login(Credentials arg0, String arg1) throws LoginException, NoSuchWorkspaceException,
            RepositoryException {
        Repository repository = this.repositoryMap.get(arg0);
        
        if (repository == null) {
            throw new RepositoryException("The repository is not available."); 
        }
        
        tlCurrentRepository.set(repository);

        return repository.login(arg0, arg1);
    }

    private boolean equalsCredentials(Credentials credentials1, Credentials credentials2) {
        if (credentials1 instanceof SimpleCredentials && credentials2 instanceof SimpleCredentials) {
            return (((SimpleCredentials) credentials1).getUserID().equals(((SimpleCredentials) credentials2).getUserID()));
        } else if (credentials1 != null) {
            return credentials1.equals(credentials2);
        }
        
        return false;
    }

    private class CredentialsWrapper implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        private final Credentials credentials;

        private CredentialsWrapper(final Credentials credentials) {
            super();
            this.credentials = credentials;
        }
        
        public Credentials getCredentials() {
            return this.credentials;
        }
        
        @Override
        public boolean equals(final Object other) {
            return equalsCredentials(this.credentials, (Credentials) other);
        }
    }
}
