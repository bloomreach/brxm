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
package org.hippoecm.hst.content.beans.manager;

import static org.junit.Assert.assertNotNull;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.Session;

import org.apache.commons.beanutils.MethodUtils;
import org.hippoecm.hst.AbstractPersistencySpringTestCase;
import org.hippoecm.hst.content.beans.PersistableTextPage;
import org.hippoecm.hst.persistence.ContentPersistenceManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestPersistableObjectBeanManager extends AbstractPersistencySpringTestCase {
    
    protected Object repository;
    protected Credentials defaultCredentials;
    private ContentPersistenceManager cpm;
    
    @Before
    public void setUp() throws Exception {
        super.setUp();
        
        this.repository = getComponent(Repository.class.getName());
        this.defaultCredentials = getComponent(Credentials.class.getName());
    }
    
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        
        if (this.repository != null) {
            MethodUtils.invokeMethod(this.repository, "close", null);
        }
    }
    
    @Test
    public void testBasicUsage() throws Exception {
        Session session = null;
        
        try {
            ObjectConverter objectConverter = getObjectConverter();
            
            session = (Session) MethodUtils.invokeMethod(this.repository, "login", this.defaultCredentials);
            
            cpm = new PersistableObjectBeanManagerImpl(session, objectConverter);
            
            PersistableTextPage page1 = (PersistableTextPage) cpm.getObject("/testcontent/documents/testproject/Solutions/SolutionsPage");
            assertNotNull(page1);
            
//            cpm.create("/testcontent/documents/testproject/Solutions", "testproject:textpage", "CollaborationPortal");
//            PersistableTextPage page2 = (PersistableTextPage) cpm.getObject("/testcontent/documents/testproject/Solutions/CollaborationPortal");
//            assertNotNull(page2);
            
//          assertEquals(comment1, testComment1);
//          Comment testComment2 = (Comment) cpm.getObject("/content/blog/comments/comment2");
//          assertEquals(comment2, testComment2);
//          
//          testComment1.setTitle("testcomment1 - title");
//          testComment1.setContent("testcomment1 - content");
//          
//          cpm.update(testComment1);
//          
//          Comment testComment12 = (Comment) cpm.getObject("/content/blog/comments/comment1");
//          assertFalse(comment1.equals(testComment12));
//          assertEquals(testComment1, testComment12);
//          
//          cpm.remove(testComment1);
//          Comment testComment13 = (Comment) cpm.getObject("/content/blog/comments/comment1");
//          assertTrue(testComment13 == null);
//          
//          cpm.save();
        } finally {
            if (session != null) session.logout();
        }
        
    }
    
}
