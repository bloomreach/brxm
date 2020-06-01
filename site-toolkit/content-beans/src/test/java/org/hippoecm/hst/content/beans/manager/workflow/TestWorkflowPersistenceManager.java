/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.manager.workflow;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;

import org.hippoecm.hst.AbstractBeanTestCase;
import org.hippoecm.hst.content.beans.ContentNodeBinder;
import org.hippoecm.hst.content.beans.ContentNodeBindingException;
import org.hippoecm.hst.content.beans.PersistableTextPage;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

        ObjectConverter objectConverter = createObjectConverter();

        wpm = new WorkflowPersistenceManagerImpl(session, objectConverter, persistBinders);

        HippoFolderBean newFolder = null;

        try {
            // create a document with type and name
            String absoluteCreatedDocumentPath = wpm.createAndReturn(TEST_FOLDER_NODE_PATH, TEST_DOCUMENT_NODE_TYPE, TEST_NEW_DOCUMENT_NODE_NAME, false);
            // retrieves the draft document created just before
            PersistableTextPage draftNewPage = (PersistableTextPage) wpm.getObject(absoluteCreatedDocumentPath);
            assertNotNull(draftNewPage);

            String[] draftAvailability =  draftNewPage.getValueProvider().getStrings("hippo:availability");
            // the wpm createAndReturn must have created a document that has 'hippo:availability = preview'
            assertEquals( "String[] availability should contain 1 string", 0, draftAvailability.length);

            // retrieves the preview document created just before
            // since admin user can read all variants, the preview is expected to be second
            PersistableTextPage previewNewPage = (PersistableTextPage) wpm.getObject(absoluteCreatedDocumentPath + "[2]");
            assertNotNull(previewNewPage);

            String[] prevAvailability =  previewNewPage.getValueProvider().getStrings("hippo:availability");
            // the wpm createAndReturn must have created a document that has 'hippo:availability = preview'
            assertEquals( "String[] availability should contain 1 string", 1, prevAvailability.length);
            assertEquals( "availability[0] should equal 'preview'", "preview", prevAvailability[0]);
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
    public void testDocumentManipulationWithDeprecatedWorkflowCallbackHandler() throws Exception {
        ObjectConverter objectConverter = createObjectConverter();

        wpm = new WorkflowPersistenceManagerImpl(session, objectConverter, persistBinders);
        wpm.setWorkflowCallbackHandler(new BaseWorkflowCallbackHandler<DocumentWorkflow>() {
            public void processWorkflow(DocumentWorkflow wf) throws Exception {
                wf.requestPublication();
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
    public void testDocumentManipulationWithDeprecatedReviewedActionsWorkflow() throws Exception {
        ObjectConverter objectConverter = createObjectConverter();

        wpm = new WorkflowPersistenceManagerImpl(session, objectConverter, persistBinders);
        wpm.setWorkflowCallbackHandler(new BaseWorkflowCallbackHandler<DocumentWorkflow>() {
            public void processWorkflow(DocumentWorkflow wf) throws Exception {
                wf.requestPublication();
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
    public void testDocumentManipulationWithDocumentWorkflow() throws Exception {
        ObjectConverter objectConverter = createObjectConverter();

        wpm = new WorkflowPersistenceManagerImpl(session, objectConverter, persistBinders);
        wpm.setWorkflowCallbackHandler(new BaseWorkflowCallbackHandler<DocumentWorkflow>() {
            public void processWorkflow(DocumentWorkflow wf) throws Exception {
                wf.requestPublication();
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
       ObjectConverter objectConverter = createObjectConverter();
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

        ObjectConverter objectConverter = createObjectConverter();

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
        ObjectConverter objectConverter = createObjectConverter();

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
       ObjectConverter objectConverter = createObjectConverter();
       wpm = new WorkflowPersistenceManagerImpl(session, objectConverter, persistBinders);
        HippoFolderBean newFolder = null;
        try {
            String folderName = "Test Folder1";
            String expectedNodeName = "test-folder1";

            // create a document with type and name
            String absoluteFolderPath = wpm.createAndReturn(TEST_AUTO_NEW_FOLDER_NODE_PATH, HIPPOSTD_FOLDER_NODE_TYPE, folderName, true);

            System.out.println(absoluteFolderPath);
            // retrieves the document created just before
            newFolder = (HippoFolderBean) wpm.getObject(absoluteFolderPath);

            System.out.println(newFolder.getDisplayName());
            // test localized name
            assertFalse(newFolder.getDisplayName().equals(newFolder.getName()));
            assertEquals(folderName, newFolder.getDisplayName());
            assertEquals(expectedNodeName, newFolder.getName());

            // test jcr low level
            Node handle = session.getNode(absoluteFolderPath);
            assertEquals("test-folder1", handle.getName());
            assertEquals("Test Folder1", handle.getProperty(HIPPO_NAME).getString());
        } finally {
            if (newFolder != null) {
                wpm.remove(newFolder);
            }
        }
        wpm.save();

    }

    @Test
    public void testCreateNoLocalizedName() throws Exception {

        ObjectConverter objectConverter = createObjectConverter();
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
            Node folderNode = session.getNode(TEST_AUTO_NEW_FOLDER_NODE_PATH + "/" + folderName);
            assertTrue(folderName.equals(folderNode.getName()));
            assertFalse(folderNode.hasProperty(HIPPO_NAME));
        } finally {
            if (newFolder != null) {
                wpm.remove(newFolder);
            }
        }
        wpm.save();

    }

    @Test
    public void testLocalizedFolderCreation() throws Exception {
        ObjectConverter objectConverter = createObjectConverter();

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
            assertEquals("new-folder4", newTmpFolder.getName());
            assertEquals("new folder4", newTmpFolder.getDisplayName());
            newTmpFolder = (HippoFolderBean) newTmpFolder.getParentBean();
            assertEquals("new-folder3", newTmpFolder.getName());
            assertEquals("new FOLDER3", newTmpFolder.getDisplayName());
            newTmpFolder = (HippoFolderBean) newTmpFolder.getParentBean();
            assertEquals("new-folder2", newTmpFolder.getName());
            assertEquals("NEW Folder2", newTmpFolder.getDisplayName());
            newTmpFolder = (HippoFolderBean) newTmpFolder.getParentBean();
            assertEquals("new-folder1", newTmpFolder.getName());
            assertEquals("new Folder1", newTmpFolder.getDisplayName());
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
