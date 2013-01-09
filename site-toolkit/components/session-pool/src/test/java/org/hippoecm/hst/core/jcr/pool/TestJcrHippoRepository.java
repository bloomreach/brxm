/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.core.jcr.pool;

import static org.junit.Assert.assertNotNull;

import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.repository.VMHippoRepository;
import org.junit.Test;

public class TestJcrHippoRepository {
    
    private SimpleCredentials defaultCreds = new SimpleCredentials("admin", "admin".toCharArray());
    
    @Test
    public void testLocalRepositories() throws Exception {
        JcrHippoRepository orgRepository1 = new JcrHippoRepository();
        Session session1 = orgRepository1.login(defaultCreds);
        assertNotNull(session1);
        session1.logout();
        
        JcrHippoRepository orgRepository2 = new JcrHippoRepository();
        Session session2 = orgRepository2.login(defaultCreds);
        assertNotNull(session2);
        session2.logout();
        
        // even though a JcrHippoRepository is closed, 
        // the internal hippo repository must not be closed
        // because other JcrHippoRepository instance could have been still used (though session-pool).
        // Local repository is supposed to be closed by itself (e.g. its own servlet)
        orgRepository1.closeHippoRepository();
        
        // because a JcrHippoRepository should not close the internal hippo repository,
        // the other JcrHippoRepository instance must be able to use the intenal hippo repository to allow log in.
        session2 = orgRepository2.login(defaultCreds);
        assertNotNull(session2);
        session2.logout();
    }
    
    @Test
    public void testVMRepositories() throws Exception {
        JcrHippoRepository orgRepository = new JcrHippoRepository();
        Session session = orgRepository.login(defaultCreds);
        assertNotNull(session);
        session.logout();
        
        VMHippoRepository.register("", orgRepository.hippoRepository);
        
        JcrHippoRepository vmRepository1 = new JcrHippoRepository("vm://");
        Session session1 = vmRepository1.login(defaultCreds);
        assertNotNull(session1);
        session1.logout();
        
        JcrHippoRepository vmRepository2 = new JcrHippoRepository("vm://");
        Session session2 = vmRepository2.login(defaultCreds);
        assertNotNull(session2);
        session2.logout();
        
        // even though a JcrHippoRepository is closed, 
        // the internal hippo repository must not be closed
        // because other JcrHippoRepository instance could have been still used (though session-pool).
        // Local repository is supposed to be closed by itself (e.g. its own servlet)
        vmRepository1.closeHippoRepository();
        
        // because a JcrHippoRepository should not close the internal hippo repository,
        // the other JcrHippoRepository instance must be able to use the intenal hippo repository to allow log in.
        session2 = vmRepository2.login(defaultCreds);
        assertNotNull(session2);
        session2.logout();
        
    }
}
