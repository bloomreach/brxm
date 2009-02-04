package org.hippoecm.hst.jcr.pool;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.hippoecm.hst.core.jcr.pool.BasicPoolingRepository;
import org.hippoecm.hst.core.jcr.pool.PoolingRepository;
import org.hippoecm.hst.test.AbstractSpringTestCase;

public class TestBasicPoolingRepository extends AbstractSpringTestCase
{

    public void testBasicPoolingRepository() throws Exception
    {
        BasicPoolingRepository poolingRepository = (BasicPoolingRepository) getComponent(PoolingRepository.class.getName());
        Repository repository = poolingRepository;
        
        Session [] sessions = new Session[poolingRepository.getMaxActive()];
        
        for (int i = 0; i < poolingRepository.getMaxActive(); i++)
        {
            sessions[i] = repository.login();
        }
        
        assertEquals("Active session count is not the same as the maximum available session count.", 
                     poolingRepository.getMaxActive(), poolingRepository.getNumActive());
        
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
        
        assertEquals("Active session count is not zero.", 0, poolingRepository.getNumActive());
        
        Session session = null;
        
        try
        {
            session = poolingRepository.login();
            Node node = session.getRootNode();
            assertNotNull("The root node is null.", node);
            
            try
            {
                session.save();
            }
            catch (UnsupportedRepositoryOperationException uroe)
            {
                fail("The session from the pool is not able to save.");
            }
        }
        finally
        {
            if (session != null)
                session.logout();
        }
        
        session = null;
        
        try
        {
            SimpleCredentials defaultCredentials = poolingRepository.getDefaultCredentials();
            SimpleCredentials testCredentials = new SimpleCredentials(defaultCredentials.getUserID(), defaultCredentials.getPassword());

            // Check if the session pool returns a session by a same credentials info.
            session = poolingRepository.login(testCredentials);
            assertNotNull("session is null.", session);
            
            session.save();
        }
        catch (UnsupportedRepositoryOperationException uroe)
        {
            fail("The session is not writable one: " + session);
        }
        finally
        {
            if (session != null)
                session.logout();
        }        
    }
    
}
