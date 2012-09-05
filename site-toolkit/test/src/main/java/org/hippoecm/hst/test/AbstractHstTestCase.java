/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.test;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.core.container.ComponentManager;
import org.junit.After;
import org.junit.Before;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Abstract base class for writing tests against repository.
 *
 * Your unit test should follow the following pattern:
 *
        <code>
        public class SampleTest extends org.hippoecm.hst.test.AbstractHstTestCase {
            public void setUp() throws Exception {
                super.setUp();
                // your code here
            }
            public void tearDown() throws Exception {
                // your code here
                super.tearDown();
            }
        }
        </code>
 */
public abstract class AbstractHstTestCase extends RepositoryTestCase {

    protected final static Logger log = LoggerFactory.getLogger(AbstractHstTestCase.class);
    protected ComponentManager abstractTestComponentManager;
    
    protected Node testNode;

    private Repository repository;
    private Session session;
    
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }


    public Repository getRepository(){
        if(repository != null) {
            return repository;
        }
        repository =  new JcrHippoRepository(null);
        return repository;
    }
    
    public Session getSession(){
        try {
            if(this.session != null && this.session.isLive()) {
                return this.session;
            } else {
                this.session = getRepository().login(new SimpleCredentials("admin", "admin".toCharArray()));
            }
            return this.session;
        } catch (LoginException e) {
            e.printStackTrace();
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        return null;
    }


   
}
