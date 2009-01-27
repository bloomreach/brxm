package org.hippoecm.hst.jcr;

import javax.jcr.Repository;
import javax.jcr.Session;

public interface PoolingRepository extends Repository
{
    
    public int getNumActive();
    
    public int getNumIdle();
    
    public void returnSession(Session session);
    
}
