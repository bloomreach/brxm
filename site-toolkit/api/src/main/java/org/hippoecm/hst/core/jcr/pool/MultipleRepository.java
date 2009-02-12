package org.hippoecm.hst.core.jcr.pool;

import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Repository;

public interface MultipleRepository extends Repository {
    
    Map<Credentials, Repository> getRepositoryMap();
    
    Repository getRepositoryByCredentials(Credentials credentials);

}
