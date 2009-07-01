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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;

import org.apache.commons.beanutils.MethodUtils;
import org.hippoecm.hst.AbstractPersistencySpringTestCase;
import org.hippoecm.hst.content.beans.PersistableTextPage;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.persistence.ContentPersistenceBinder;
import org.hippoecm.hst.persistence.ContentPersistenceBindingException;
import org.hippoecm.hst.persistence.ContentPersistenceManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestPersistableObjectBeanManager extends AbstractPersistencySpringTestCase {
    
    private static final String HIPPOSTD_FOLDER_NODE_TYPE = "hippostd:folder";
    private static final String TEST_DOCUMENT_NODE_TYPE = "testproject:textpage";
    
    private static final String TEST_FOLDER_NODE_PATH = "/testcontent/documents/testproject/Solutions";

    private static final String TEST_EXISTING_DOCUMENT_NODE_PATH = TEST_FOLDER_NODE_PATH + "/SolutionsPage";
    
    private static final String TEST_NEW_DOCUMENT_NODE_NAME = "CollaborationPortal";
    private static final String TEST_NEW_DOCUMENT_NODE_PATH = TEST_FOLDER_NODE_PATH + "/" + TEST_NEW_DOCUMENT_NODE_NAME;
    
    private static final String TEST_NEW_FOLDER_NODE_NAME = "SubSolutions";
    private static final String TEST_NEW_FOLDER_NODE_PATH = TEST_FOLDER_NODE_PATH + "/" + TEST_NEW_FOLDER_NODE_NAME;
    
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
    public void testDocumentManipulation() throws Exception {
        Session session = null;
        
        try {
            ObjectConverter objectConverter = getObjectConverter();
            
            session = (Session) MethodUtils.invokeMethod(this.repository, "login", this.defaultCredentials);
            
            cpm = new PersistableObjectBeanManagerImpl(session, objectConverter);
            ((PersistableObjectBeanManagerImpl) cpm).setPublishAfterUpdate(true);
            
            // basic object retrieval from the content
            PersistableTextPage page = (PersistableTextPage) cpm.getObject(TEST_EXISTING_DOCUMENT_NODE_PATH);
            assertNotNull(page);
            
            try {
                // create a document with type and name 
                cpm.create(TEST_FOLDER_NODE_PATH, TEST_DOCUMENT_NODE_TYPE, TEST_NEW_DOCUMENT_NODE_NAME);
            
                // retrieves the document created just before
                PersistableTextPage newPage = (PersistableTextPage) cpm.getObject(TEST_NEW_DOCUMENT_NODE_PATH);
                assertNotNull(newPage);
                
                newPage.setTitle("Collaboration Portal Title");

                cpm.update(newPage, new ContentPersistenceBinder() {
                    public void bind(Object contentObject, Object contentNode) throws ContentPersistenceBindingException {
                        PersistableTextPage documentObject = (PersistableTextPage) contentObject;
                        Node documentNode = (Node) contentNode;
                        try {
                            documentNode.setProperty("testproject:title", documentObject.getTitle());
                        } catch (Exception e) {
                            throw new ContentPersistenceBindingException(e);
                        }
                    }
                });
                
                // retrieves the document created just before
                newPage = (PersistableTextPage) cpm.getObject(TEST_NEW_DOCUMENT_NODE_PATH);
                assertEquals("Collaboration Portal Title", newPage.getTitle());
                
            } finally {
                PersistableTextPage newPage = null;
                
                try {
                    newPage = (PersistableTextPage) cpm.getObject(TEST_NEW_DOCUMENT_NODE_PATH);
                } catch (Exception e) {
                }
                
                if (newPage != null) {
                    cpm.remove(newPage);
                }
            }
            
            cpm.save();
        } finally {
            if (session != null) session.logout();
        }
        
    }
    
    @Test
    public void testFolderCreateRemove() throws Exception {
        Session session = null;
        
        try {
            ObjectConverter objectConverter = getObjectConverter();
            
            session = (Session) MethodUtils.invokeMethod(this.repository, "login", this.defaultCredentials);
            
            cpm = new PersistableObjectBeanManagerImpl(session, objectConverter);
            
            HippoFolderBean newFolder = null;
            
            try {
                // create a document with type and name 
                cpm.create(TEST_FOLDER_NODE_PATH, HIPPOSTD_FOLDER_NODE_TYPE, TEST_NEW_FOLDER_NODE_NAME);
            
                // retrieves the document created just before
                newFolder = (HippoFolderBean) cpm.getObject(TEST_NEW_FOLDER_NODE_PATH);
                assertNotNull(newFolder);
                
            } finally {
                if (newFolder != null) {
                    cpm.remove(newFolder);
                }
            }
            
            cpm.save();
        } finally {
            if (session != null) session.logout();
        }
        
    }
    
}
