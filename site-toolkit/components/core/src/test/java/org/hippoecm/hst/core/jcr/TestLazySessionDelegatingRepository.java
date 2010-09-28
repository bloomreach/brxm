package org.hippoecm.hst.core.jcr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.http.HttpSessionBindingListener;

import org.hippoecm.hst.test.AbstractHstTestCase;
import org.junit.Before;
import org.junit.Test;

public class TestLazySessionDelegatingRepository  extends AbstractHstTestCase {
    
    private LazySessionDelegatingRepository repository;
    
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        Repository delegatee = getRepository();
        repository = new LazySessionDelegatingRepository(delegatee);
        repository.setLogoutOnSessionUnbound(true);
    }
    
    @Test
    public void testLazySession() throws Exception {
        Credentials creds = new SimpleCredentials("admin", "admin".toCharArray());
        Session session = repository.login(creds);
        
        assertFalse(((LazySession) session).lastLoggedIn() > 0);
        
        assertTrue(session instanceof Session);
        assertTrue(session instanceof LazySession);
        assertTrue(session instanceof Serializable);
        assertTrue(session instanceof HttpSessionBindingListener);
        
        assertTrue(session.isLive());
        assertFalse(((LazySession) session).lastLoggedIn() > 0);
        
        String userID = session.getUserID();
        assertTrue(((LazySession) session).lastLoggedIn() > 0);
        assertEquals(0, ((LazySession) session).lastRefreshed());
        
        long time = System.currentTimeMillis();
        session.refresh(false);
        assertTrue("session is not refreshed: " + ((LazySession) session).lastRefreshed(), time <= ((LazySession) session).lastRefreshed());
        
        session.logout();
    }
    
    @Test
    public void testLazySessionLogoutSession() throws Exception {
        Credentials creds = new SimpleCredentials("admin", "admin".toCharArray());
        Session session = repository.login(creds);
        String userID = session.getUserID();
        assertTrue(((LazySession) session).lastLoggedIn() > 0);
        
        ((LazySession) session).logoutSession();
        
        assertFalse(((LazySession) session).lastLoggedIn() > 0);
        
        session.logout();
    }
    
    @Test
    public void testLazySessionHttpSessionBinding() throws Exception {
        Credentials creds = new SimpleCredentials("admin", "admin".toCharArray());
        Session session = repository.login(creds);
        String userID = session.getUserID();
        assertTrue(((LazySession) session).lastLoggedIn() > 0);
        
        ((HttpSessionBindingListener) session).valueUnbound(null);
        
        assertFalse(((LazySession) session).lastLoggedIn() > 0);
        
        session.logout();
    }
    
    @Test
    public void testLazySessionObject() throws Exception {
        Credentials creds = new SimpleCredentials("admin", "admin".toCharArray());
        Session session1 = repository.login(creds);
        Session session2 = repository.login(creds);
        
        assertFalse(session1.equals(session2));
        assertFalse(session1.hashCode() == session2.hashCode());
        assertFalse(session1.toString().equals(session2.toString()));
        
        String userID1 = session1.getUserID();
        String userID2 = session2.getUserID();
        
        assertTrue(((LazySession) session1).lastLoggedIn() > 0);
        assertTrue(((LazySession) session2).lastLoggedIn() > 0);
        assertTrue(userID1.equals(userID2));
        assertFalse(session1.equals(session2));
        assertFalse(session1.hashCode() == session2.hashCode());
        assertFalse(session1.toString().equals(session2.toString()));
        
        session1.logout();
        session2.logout();
        
        // for debugging purpose, you can confirm the logging of "LazySession object is being finalized."
        System.gc();
        Thread.sleep(1000);
    }
    
}
