package org.hippoecm.hst.core.jcr.pool;

import javax.jcr.Credentials;
import javax.jcr.Repository;

public interface MultipleRepository extends Repository {
    
    Repository getRepositoryByCredentials(Credentials credentials);
    
}
