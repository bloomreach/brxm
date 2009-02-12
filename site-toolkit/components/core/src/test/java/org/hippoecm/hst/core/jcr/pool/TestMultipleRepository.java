package org.hippoecm.hst.core.jcr.pool;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.junit.Before;
import org.junit.Test;

public class TestMultipleRepository extends AbstractSpringTestCase {

    protected MultipleRepository multipleRepository;
    protected Repository repository;
    protected Credentials readOnlyCredentials;
    protected Credentials writableCredentials;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        
        this.multipleRepository = (MultipleRepository) getComponent(Repository.class.getName());
        this.repository = this.multipleRepository;
        this.readOnlyCredentials = (Credentials) getComponent(Credentials.class.getName() + ".readOnly");
        this.writableCredentials = (Credentials) getComponent(Credentials.class.getName() + ".writable");
    }

    @Test
    public void testMultipleRepository() throws LoginException, RepositoryException {
        Repository readOnlyRepository = this.multipleRepository.getRepositoryByCredentials(this.readOnlyCredentials);
        Repository writableRepository = this.multipleRepository.getRepositoryByCredentials(this.writableCredentials);
        
        assertFalse("The readOnly repository must be different one from the writable repository.", readOnlyRepository == writableRepository);
        
        Map<Credentials, Repository> repoMap = this.multipleRepository.getRepositoryMap();
        
        assertTrue("The repository retrieved by credentials is different from the entry of the map.", 
                readOnlyRepository == repoMap.get(this.readOnlyCredentials));
        assertTrue("The repository retrieved by credentials is different from the entry of the map.", 
                writableRepository == repoMap.get(this.writableCredentials));
        
        Session sessionFromReadOnlyRepository = this.repository.login(this.readOnlyCredentials);
        assertTrue("Current session's repository is not the expected repository", 
                readOnlyRepository == ((MultipleRepositoryImpl) this.multipleRepository).getCurrentThreadRepository());
        sessionFromReadOnlyRepository.logout();
        
        Session sessionFromWritableRepository = this.repository.login(this.writableCredentials);
        assertTrue("Current session's repository is not the expected repository", 
                writableRepository == ((MultipleRepositoryImpl) this.multipleRepository).getCurrentThreadRepository());
        sessionFromWritableRepository.logout();
    }

}
