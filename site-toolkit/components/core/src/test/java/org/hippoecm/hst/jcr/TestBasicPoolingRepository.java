package org.hippoecm.hst.jcr;

import javax.jcr.Repository;
import javax.jcr.Session;

import org.hippoecm.hst.test.AbstractSpringTestCase;

public class TestBasicPoolingRepository extends AbstractSpringTestCase
{

    public void testBasicPoolingRepository() throws Exception
    {
        BasicPoolingRepository poolingRepository = (BasicPoolingRepository) getApplicationContext().getBean("repository");
        Repository repository = poolingRepository;
        
        Session [] sessions = new Session[poolingRepository.getMaxActive()];
        
        for (int i = 0; i < poolingRepository.getMaxActive(); i++)
        {
            sessions[i] = repository.login();
        }
        
        try
        {
            Session oneMore = repository.login();
            fail("The pool should limit the maximum active session object.");
        }
        catch (Exception e)
        {
        }

        for (int i = 0; i < poolingRepository.getMaxActive(); i++)
        {
            sessions[i].logout();
        }
    }
    
}
