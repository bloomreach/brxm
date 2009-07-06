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

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;

import org.apache.commons.beanutils.MethodUtils;
import org.hippoecm.hst.AbstractPersistencySpringTestCase;
import org.hippoecm.hst.content.beans.PersistableTextPage;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.persistence.ContentNodeBinder;
import org.hippoecm.hst.persistence.ContentPersistenceBindingException;
import org.hippoecm.hst.persistence.ContentPersistenceManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestPersistableObjectBeanManager extends AbstractPersistencySpringTestCase {
    
    private static final String HIPPOSTD_FOLDER_NODE_TYPE = "hippostd:folder";
    private static final String TEST_DOCUMENT_NODE_TYPE = "testproject:textpage";
    
    private static final String TEST_CONTENTS_PATH = "/testpreview/testproject/hst:content";
    private static final String TEST_FOLDER_NODE_PATH = TEST_CONTENTS_PATH + "/Solutions";

    private static final String TEST_EXISTING_DOCUMENT_NODE_PATH = TEST_FOLDER_NODE_PATH + "/SolutionsPage";
    
    private static final String TEST_NEW_DOCUMENT_NODE_NAME = "CollaborationPortal";
    private static final String TEST_NEW_DOCUMENT_NODE_PATH = TEST_FOLDER_NODE_PATH + "/" + TEST_NEW_DOCUMENT_NODE_NAME;
    
    private static final String TEST_NEW_FOLDER_NODE_NAME = "SubSolutions";
    private static final String TEST_NEW_FOLDER_NODE_PATH = TEST_FOLDER_NODE_PATH + "/" + TEST_NEW_FOLDER_NODE_NAME;
    
    private static final String TEST_AUTO_NEW_FOLDER_NODE_NAME = "comments/tests";
    private static final String TEST_AUTO_NEW_FOLDER_NODE_PATH = TEST_CONTENTS_PATH + "/" + TEST_AUTO_NEW_FOLDER_NODE_NAME;
    
    protected Object repository;
    protected Credentials defaultCredentials;
    private ContentPersistenceManager cpm;
    private Map<String, ContentNodeBinder> persistBinders;
    
    @Before
    public void setUp() throws Exception {
        super.setUp();
        
        this.repository = getComponent(Repository.class.getName());
        this.defaultCredentials = getComponent(Credentials.class.getName());
        this.persistBinders = new HashMap<String, ContentNodeBinder>();
        this.persistBinders.put("testproject:textpage", new PersistableTextPageBinder());
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
            
            cpm = new PersistableObjectBeanManagerImpl(session, objectConverter, persistBinders);
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
                newPage.setBodyContent("<h1>Welcome to the Collaboration Portal!</h1>");

                // custom mapping binder is already provided during CPM instantiation.
                // but you can also provide your custom binder as the second parameter.
                // if any binder is not found and the first parameter (newPage) is instanceof ContentPersistenceBinder,
                // then the POJO object in the first parameter will be used as a binder. 
                cpm.update(newPage);
                
                cpm.save();
                
                // retrieves the document created just before
                newPage = (PersistableTextPage) cpm.getObject(TEST_NEW_DOCUMENT_NODE_PATH);
                assertEquals("Collaboration Portal Title", newPage.getTitle());
                assertEquals("<h1>Welcome to the Collaboration Portal!</h1>", newPage.getBodyContent());
                
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
            
            cpm = new PersistableObjectBeanManagerImpl(session, objectConverter, persistBinders);
            
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
    
    @Test
    public void testFolderAutoCreateRemove() throws Exception {
        Session session = null;
        
        try {
            ObjectConverter objectConverter = getObjectConverter();
            
            session = (Session) MethodUtils.invokeMethod(this.repository, "login", this.defaultCredentials);
            
            cpm = new PersistableObjectBeanManagerImpl(session, objectConverter, persistBinders);
            
            HippoFolderBean newFolder = null;
            
            try {
                // create a document with type and name 
                cpm.create(TEST_AUTO_NEW_FOLDER_NODE_PATH, HIPPOSTD_FOLDER_NODE_TYPE, "testfolder", true);
            
                // retrieves the document created just before
                newFolder = (HippoFolderBean) cpm.getObject(TEST_AUTO_NEW_FOLDER_NODE_PATH + "/testfolder");
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
    
    private class PersistableTextPageBinder implements ContentNodeBinder {
        
        public void bind(Object content, Node node) throws ContentPersistenceBindingException {
            PersistableTextPage page = (PersistableTextPage) content;
            
            try {
                node.setProperty("testproject:title", page.getTitle());
                Node htmlNode = node.getNode("testproject:body");
                htmlNode.setProperty("hippostd:content", page.getBodyContent());
            } catch (Exception e) {
                throw new ContentPersistenceBindingException(e);
            }
        }
        
    }
    
}
