package org.hippoecm.hst.core.jcr.pool;

import javax.jcr.Repository;
import javax.jcr.Session;

import org.hippoecm.hst.core.ResourceLifecycleManagement;

public interface PoolingRepository extends Repository {

    String WHEN_EXHAUSTED_BLOCK = "block";
    String WHEN_EXHAUSTED_FAIL = "fail";
    String WHEN_EXHAUSTED_GROW = "grow";

    public int getNumActive();

    public int getNumIdle();

    public void returnSession(Session session);
    
    public ResourceLifecycleManagement getResourceLifecycleManagement(); 

}
