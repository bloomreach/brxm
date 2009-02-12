package org.hippoecm.hst.core.jcr.pool;

import java.util.List;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Repository;

import org.hippoecm.hst.core.ResourceLifecycleManagement;

public interface MultipleRepository extends Repository {
    
    Map<Credentials, Repository> getRepositoryMap();
    
    Repository getRepositoryByCredentials(Credentials credentials);
    
    List<ResourceLifecycleManagement> getResourceLifecycleManagementList();

}
