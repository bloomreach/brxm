package org.hippoecm.hst.jcr.pool;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.hippoecm.hst.core.jcr.pool.BasicPoolingRepository;
import org.hippoecm.hst.test.AbstractSpringTestCase;

public class TestBasicPoolingRepository extends AbstractSpringTestCase
{

    public void testBasicPoolingRepository() throws Exception
    {
        BasicPoolingRepository poolingRepository = (BasicPoolingRepository) getComponent("poolingRepository");
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
                fail("The session from the pool should be read-only session by default login().");
            }
            catch (UnsupportedRepositoryOperationException uroe)
            {
                // good enough.
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
            // get a writable session by using credentials.
            Credentials credentials = new SimpleCredentials("admin", "admin".toCharArray());

            int previousActiveCount = poolingRepository.getNumActive();
            
            // Check if the session pool is not affected.
            session = poolingRepository.login(credentials);
            assertEquals("Active session count changed by creating a writable session.", 
                         previousActiveCount, poolingRepository.getNumActive()); 
            
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
