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
package org.hippoecm.hst.content.beans.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.hst.AbstractBeanTestCase;
import org.hippoecm.hst.content.beans.ContentNodeBinder;
import org.hippoecm.hst.content.beans.ContentNodeBindingException;
import org.hippoecm.hst.content.beans.PersistableTextPage;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowCallbackHandler;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManager;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManagerImpl;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.repository.reviewedactions.FullReviewedActionsWorkflow;
import org.junit.Before;
import org.junit.Test;

public class TestWorkflowPersistenceManager extends AbstractBeanTestCase {

    private static final String HIPPOSTD_FOLDER_NODE_TYPE = "hippostd:folder";
    private static final String TEST_DOCUMENT_NODE_TYPE = "unittestproject:textpage";

    private static final String TEST_CONTENTS_PATH = "/unittestcontent/documents/unittestproject";
    private static final String TEST_FOLDER_NODE_PATH = TEST_CONTENTS_PATH + "/common";

    private static final String TEST_EXISTING_DOCUMENT_NODE_PATH = TEST_FOLDER_NODE_PATH + "/homepage";

    private static final String TEST_NEW_DOCUMENT_NODE_NAME = "about";
    private static final String TEST_NEW_DOCUMENT_NODE_PATH = TEST_FOLDER_NODE_PATH + "/" + TEST_NEW_DOCUMENT_NODE_NAME;

    private static final String TEST_NEW_FOLDER_NODE_NAME = "subcommon";
    private static final String TEST_NEW_FOLDER_NODE_PATH = TEST_FOLDER_NODE_PATH + "/" + TEST_NEW_FOLDER_NODE_NAME;

    private static final String TEST_AUTO_NEW_FOLDER_NODE_NAME = "comments/tests";
    private static final String TEST_AUTO_NEW_FOLDER_NODE_PATH = TEST_CONTENTS_PATH + "/" + TEST_AUTO_NEW_FOLDER_NODE_NAME;

    private WorkflowPersistenceManager wpm;
    private Map<String, ContentNodeBinder> persistBinders;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.persistBinders = new HashMap<String, ContentNodeBinder>();
        this.persistBinders.put("unittestproject:textpage", new PersistableTextPageBinder());
    }

    @Test
    public void testNewDocumentIsPreviewAvailable() throws Exception {

        ObjectConverter objectConverter = getObjectConverter();

        wpm = new WorkflowPersistenceManagerImpl(session, objectConverter, persistBinders);

        HippoFolderBean newFolder = null;

        try {
            // create a document with type and name
            String absoluteCreatedDocumentPath = wpm.createAndReturn(TEST_FOLDER_NODE_PATH, TEST_DOCUMENT_NODE_TYPE, TEST_NEW_DOCUMENT_NODE_NAME, false);
            // retrieves the document created just before
            PersistableTextPage newPage = (PersistableTextPage) wpm.getObject(absoluteCreatedDocumentPath);
            assertNotNull(newPage);

            String[] availability =  newPage.getValueProvider().getStrings("hippo:availability");
            // the wpm createAndReturn must have created a document that has 'hippo:availability = preview'
            assertEquals( "String[] availability should contain 1 string", 1, availability.length);
            assertEquals( "availability[0] should equal 'preview'", "preview", availability[0]);
        } finally {
            PersistableTextPage newPage = null;

            try {
                newPage = (PersistableTextPage) wpm.getObject(TEST_NEW_DOCUMENT_NODE_PATH);
            } catch (Exception e) {
            }

            if (newPage != null) {
                wpm.remove(newPage);
            }
        }
    }
    
    @Test
    public void testDocumentManipulation() throws Exception {
        ObjectConverter objectConverter = getObjectConverter();

        wpm = new WorkflowPersistenceManagerImpl(session, objectConverter, persistBinders);
        wpm.setWorkflowCallbackHandler(new WorkflowCallbackHandler<FullReviewedActionsWorkflow>() {
            public void processWorkflow(FullReviewedActionsWorkflow wf) throws Exception {
                FullReviewedActionsWorkflow fraw = (FullReviewedActionsWorkflow) wf;
                fraw.requestPublication();
            }
        });

        // basic object retrieval from the content
        PersistableTextPage page = (PersistableTextPage) wpm.getObject(TEST_EXISTING_DOCUMENT_NODE_PATH);
        assertNotNull(page);

        try {
            // create a document with type and name
            String absoluteCreatedDocumentPath = wpm.createAndReturn(TEST_FOLDER_NODE_PATH, TEST_DOCUMENT_NODE_TYPE, TEST_NEW_DOCUMENT_NODE_NAME, false);

            // retrieves the document created just before
            PersistableTextPage newPage = (PersistableTextPage) wpm.getObject(absoluteCreatedDocumentPath);
            assertNotNull(newPage);

            newPage.setTitle("Title of the about page");
            newPage.setBodyContent("<h1>Welcome to the about page!</h1>");

            // custom mapping binder is already provided during WPM instantiation.
            // but you can also provide your custom binder as the second parameter.
            // if any binder is not found and the first parameter (newPage) is instanceof ContentPersistenceBinder,
            // then the POJO object in the first parameter will be used as a binder.
            wpm.update(newPage);

            // retrieves the document created just before
            newPage = (PersistableTextPage) wpm.getObject(TEST_NEW_DOCUMENT_NODE_PATH);
            assertEquals("Title of the about page", newPage.getTitle());
            assertEquals("<h1>Welcome to the about page!</h1>", newPage.getBodyContent());

        } finally {
            PersistableTextPage newPage = null;

            try {
                newPage = (PersistableTextPage) wpm.getObject(TEST_NEW_DOCUMENT_NODE_PATH);
            } catch (Exception e) {
            }

            if (newPage != null) {
                wpm.remove(newPage);
            }
        }
    }

    @Test
    public void testFolderCreateRemove() throws Exception {
       ObjectConverter objectConverter = getObjectConverter();
       wpm = new WorkflowPersistenceManagerImpl(session, objectConverter, persistBinders);

        HippoFolderBean newFolder = null;

        try {
            // create a document with type and name
            String absoluteCreatedDocumentPath = wpm.createAndReturn(TEST_FOLDER_NODE_PATH, HIPPOSTD_FOLDER_NODE_TYPE, TEST_NEW_FOLDER_NODE_NAME, false);

            // retrieves the document created just before
            newFolder = (HippoFolderBean) wpm.getObject(absoluteCreatedDocumentPath);
            assertNotNull(newFolder);

        } finally {
            if (newFolder != null) {
                wpm.remove(newFolder);
            }
        }

        wpm.save();
    }

    @Test
    public void testFolderAutoCreateRemove() throws Exception {

        ObjectConverter objectConverter = getObjectConverter();

        wpm = new WorkflowPersistenceManagerImpl(session, objectConverter, persistBinders);

        HippoFolderBean newFolder = null;

        try {
            // create a document with type and name
            String absoluteCreatedDocumentPath = wpm.createAndReturn(TEST_AUTO_NEW_FOLDER_NODE_PATH, HIPPOSTD_FOLDER_NODE_TYPE, "testfolder", true);

            // retrieves the document created just before
            newFolder = (HippoFolderBean) wpm.getObject(absoluteCreatedDocumentPath);
            assertNotNull(newFolder);
        } finally {
            if (newFolder != null) {
                wpm.remove(newFolder);
            }
        }

        wpm.save();

    }

    @Test
    public void testCreateDocumentUnderFolderHavingSameNameDocumentSibling() throws Exception {
        ObjectConverter objectConverter = getObjectConverter();

        wpm = new WorkflowPersistenceManagerImpl(session, objectConverter, persistBinders);

        String docPath = wpm.createAndReturn(TEST_AUTO_NEW_FOLDER_NODE_PATH, TEST_DOCUMENT_NODE_TYPE, "node-a", true);
        HippoDocumentBean newDocument = (HippoDocumentBean) wpm.getObject(docPath);
        assertNotNull(newDocument);

        docPath = wpm.createAndReturn(TEST_AUTO_NEW_FOLDER_NODE_PATH + "/node-a", TEST_DOCUMENT_NODE_TYPE, "node-b", true);
        newDocument = (HippoDocumentBean) wpm.getObject(docPath);
        assertNotNull(newDocument);

        docPath = wpm.createAndReturn(TEST_AUTO_NEW_FOLDER_NODE_PATH + "/node-a", TEST_DOCUMENT_NODE_TYPE, "node-c", true);
        newDocument = (HippoDocumentBean) wpm.getObject(docPath);
        assertNotNull(newDocument);

        wpm.save();
    }

    @Test
    public void testCreateLocalizedName() throws Exception {
       ObjectConverter objectConverter = getObjectConverter();
       wpm = new WorkflowPersistenceManagerImpl(session, objectConverter, persistBinders);
        HippoFolderBean newFolder = null;
        try {
            String folderName = "Test Folder1";
            String expectedNodeName = "test-folder1";

            // create a document with type and name
            String absoluteDocumentHandlePath = wpm.createAndReturn(TEST_AUTO_NEW_FOLDER_NODE_PATH, HIPPOSTD_FOLDER_NODE_TYPE, folderName, true);

            // retrieves the document created just before
            newFolder = (HippoFolderBean) wpm.getObject(absoluteDocumentHandlePath);

            // test localized name
            assert !newFolder.getLocalizedName().equals(newFolder.getName());
            assert newFolder.getLocalizedName().equals(folderName);
            assert expectedNodeName.equals(newFolder.getName());

            // test jcr low level
            Item item = session.getItem(absoluteDocumentHandlePath);
            assert expectedNodeName.equals(item.getName());
            assert item instanceof Node;
            assert ((Node) item).hasNode("hippo:translation");
            assert ((Node) item).getNode("hippo:translation").getProperty("hippo:message").getString().equals(folderName);
        } finally {
            if (newFolder != null) {
                wpm.remove(newFolder);
            }
        }
        wpm.save();

    }

    @Test
    public void testCreateNoLocalizedName() throws Exception {

        ObjectConverter objectConverter = getObjectConverter();
        wpm = new WorkflowPersistenceManagerImpl(session, objectConverter, persistBinders);
        HippoFolderBean newFolder = null;
        try {
            String folderName = "test-folder2";

            // create a document with type and name
            String absoluteDocumentHandlePath = wpm.createAndReturn(TEST_AUTO_NEW_FOLDER_NODE_PATH, HIPPOSTD_FOLDER_NODE_TYPE, folderName, true);

            // retrieves the document created just before
            newFolder = (HippoFolderBean) wpm.getObject(absoluteDocumentHandlePath);
            assert folderName.equals(newFolder.getName());

            // the created node shouldn't have a translation child, the passed node name should be sufficient
            Item item = session.getItem(TEST_AUTO_NEW_FOLDER_NODE_PATH + "/" + folderName);
            assert folderName.equals(item.getName());
            assert item instanceof Node;
            assert !((Node) item).hasNode("hippo:translation");
        } finally {
            if (newFolder != null) {
                wpm.remove(newFolder);
            }
        }
        wpm.save();

    }

    @Test
    public void testLocalizedFolderCreation() throws Exception {
        ObjectConverter objectConverter = getObjectConverter();

        wpm = new WorkflowPersistenceManagerImpl(session, objectConverter, persistBinders);
        HippoFolderBean newFolder = null;
        try {
            String folderName = "new folder4";
            String newFolderPath = TEST_CONTENTS_PATH + "/new Folder1/NEW Folder2/new FOLDER3";

            // create a document with type and name
            String absoluteDocumentHandlePath = wpm.createAndReturn(newFolderPath, HIPPOSTD_FOLDER_NODE_TYPE, folderName, true);

            // retrieves the document created just before
            newFolder = (HippoFolderBean) wpm.getObject(absoluteDocumentHandlePath);

            HippoFolderBean newTmpFolder = newFolder;
            // check the created folder names
            assert "new-folder4".equals(newTmpFolder.getName());
            assert "new folder4".equals(newTmpFolder.getLocalizedName());
            newTmpFolder = (HippoFolderBean) newTmpFolder.getParentBean();
            assert "new-folder3".equals(newTmpFolder.getName());
            assert "new FOLDER3".equals(newTmpFolder.getLocalizedName());
            newTmpFolder = (HippoFolderBean) newTmpFolder.getParentBean();
            assert "new-folder2".equals(newTmpFolder.getName());
            assert "NEW Folder2".equals(newTmpFolder.getLocalizedName());
            newTmpFolder = (HippoFolderBean) newTmpFolder.getParentBean();
            assert "new-folder1".equals(newTmpFolder.getName());
            assert "new Folder1".equals(newTmpFolder.getLocalizedName());
        } finally {
            if (newFolder != null) {
                wpm.remove(newFolder);
            }
        }
        wpm.save();

    }

    private class PersistableTextPageBinder implements ContentNodeBinder {

        public boolean bind(Object content, Node node) throws ContentNodeBindingException {
            PersistableTextPage page = (PersistableTextPage) content;

            try {
                node.setProperty("unittestproject:title", page.getTitle());
                Node htmlNode = node.getNode("unittestproject:body");
                htmlNode.setProperty("hippostd:content", page.getBodyContent());
            } catch (Exception e) {
                throw new ContentNodeBindingException(e);
            }

            // FIXME: return true only if actual changes happen.
            return true;
        }

    }

}
