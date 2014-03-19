/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

public class FolderWorkflowTest extends RepositoryTestCase {

    private static final String CONTENT_ON_COPY = "new-content";


    Node root, node;
    WorkflowManager manager;
    String[] content = {
        "/test/f", "hippostd:folder",
        "/test/attic", "hippostd:folder",
        "/test/aap", "hippostd:folder",
            "/test/aap/noot", "nt:unstructured",
                "/test/aap/noot/mies", "hippostd:folder",
                    "/test/aap/noot/mies/vuur", "nt:unstructured",
                        "/test/aap/noot/mies/vuur/jot", "nt:unstructured",
                            "/test/aap/noot/mies/vuur/jot/gijs", "hippo:coredocument",
                                "/test/aap/noot/mies/vuur/jot/gijs/duif", "hippo:document",
                                    "jcr:mixinTypes", "mix:versionable"
    };

    Value[] embeddedModifyOnCopy;
    Value[] internalModifyOnCopy;
    Value embeddedAttic;
    Value internalAttic;

    @Before
    public void setUp() throws Exception {

        super.setUp();

        root = session.getRootNode().addNode("test");
        session.save();

        build(content, session);
        session.save();
        node = root.getNode("f");
        manager = ((HippoWorkspace)session.getWorkspace()).getWorkflowManager();

        Node folderWorkflowConfig = session.getNode("/hippo:configuration/hippo:workflows/embedded/folder-extended/hipposys:config");
        if (folderWorkflowConfig.hasProperty("modify-on-copy")) {
            embeddedModifyOnCopy = folderWorkflowConfig.getProperty("modify-on-copy").getValues();
        }
        folderWorkflowConfig.setProperty("modify-on-copy", new String[] { "./hippostd:content", CONTENT_ON_COPY });
        if (folderWorkflowConfig.hasProperty("attic")) {
            embeddedAttic = folderWorkflowConfig.getProperty("attic").getValue();
        }
        folderWorkflowConfig.setProperty("attic", "/test/attic");

        folderWorkflowConfig = session.getNode("/hippo:configuration/hippo:workflows/internal/folder/hipposys:config");
        if (folderWorkflowConfig.hasProperty("modify-on-copy")) {
            internalModifyOnCopy = folderWorkflowConfig.getProperty("modify-on-copy").getValues();
        }
        folderWorkflowConfig.setProperty("modify-on-copy", new String[] { "./hippostd:content", CONTENT_ON_COPY });
        if (folderWorkflowConfig.hasProperty("attic")) {
            internalAttic = folderWorkflowConfig.getProperty("attic").getValue();
        }
        folderWorkflowConfig.setProperty("attic", "/test/attic");

        session.save();
    }

    @After
    public void tearDown() throws Exception {
        Node folderWorkflowConfig = session.getNode(
                "/hippo:configuration/hippo:workflows/embedded/folder-extended/hipposys:config");
        folderWorkflowConfig.setProperty("modify-on-copy", embeddedModifyOnCopy);
        folderWorkflowConfig.setProperty("attic", embeddedAttic);
        folderWorkflowConfig = session.getNode("/hippo:configuration/hippo:workflows/internal/folder/hipposys:config");
        folderWorkflowConfig.setProperty("modify-on-copy", internalModifyOnCopy);
        folderWorkflowConfig.setProperty("attic", internalAttic);
        session.save();

        super.tearDown();
    }

    @Test
    public void testDeleteFolderWithHandlesFails() throws Exception {
        final Node g = node.addNode("g", "hippostd:folder");
        g.addNode("h", "hippo:handle");
        session.save();

        Workflow workflow = manager.getWorkflow("internal", node);
        assertNotNull(workflow);
        assertTrue(workflow instanceof FolderWorkflow);
        try {
            ((FolderWorkflow) workflow).delete("g");
            fail("Succeeded in deleting non-empty folder");
        } catch (WorkflowException we) {
            // expected
        }
        assertTrue(node.hasNode("g"));
    }

    @Test
    public void testDeleteFolderWithTranslationsButNoHandlesPass() throws Exception {
        final Node g = node.addNode("g", "hippostd:folder");
        g.addMixin("hippo:translated");
        Node translation = g.addNode("hippo:translation", "hippo:translation");
        translation.setProperty("hippo:message", "test");
        translation.setProperty("hippo:language", "en");
        session.save();

        Workflow workflow = manager.getWorkflow("internal", node);
        assertNotNull(workflow);
        assertTrue(workflow instanceof FolderWorkflow);
        ((FolderWorkflow) workflow).delete("g");
        assertFalse(node.hasNode("g"));
    }

    @Test
    public void testFolder() throws RepositoryException, WorkflowException, RemoteException {
        FolderWorkflow workflow = (FolderWorkflow) manager.getWorkflow("internal", node);
        assertNotNull(workflow);
        Map<String,Set<String>> types = workflow.list();
        assertNotNull(types);
        assertTrue(types.containsKey("new-folder"));
        assertTrue(types.get("new-folder").contains("hippostd:folder"));
        String path = workflow.add("new-folder", "hippostd:folder", "d");
        assertNotNull(path);
        node = session.getRootNode().getNode(path.substring(1));
        assertEquals("/test/f/d",node.getPath());
        assertTrue(node.isNodeType("hippostd:folder"));
        assertFalse(node.hasProperty(HippoNodeType.HIPPO_AVAILABILITY));
    }

    @Test
    public void testDirectory() throws RepositoryException, WorkflowException, RemoteException {
        FolderWorkflow workflow = (FolderWorkflow) manager.getWorkflow("internal", node);
        assertNotNull(workflow);
        Map<String,Set<String>> types = workflow.list();
        assertNotNull(types);
        assertTrue(types.containsKey("new-collection"));
        assertTrue(types.get("new-collection").contains("hippostd:directory"));
        String path = workflow.add("new-collection", "hippostd:directory", "d");
        assertNotNull(path);
        node = session.getRootNode().getNode(path.substring(1));
        assertEquals("/test/f/d",node.getPath());
        assertTrue(node.isNodeType("hippostd:directory"));
        assertFalse(node.hasProperty(HippoNodeType.HIPPO_AVAILABILITY));
    }

    @Test
    public void testNonExistent() throws RepositoryException, WorkflowException, RemoteException {
        FolderWorkflow workflow = (FolderWorkflow) manager.getWorkflow("internal", node);
        assertNotNull(workflow);
        Map<String,Set<String>> types = workflow.list();
        assertNotNull(types);

        assertFalse(types.containsKey("new-does-not-exist"));
        try {
            workflow.add("new-does-not-exists", "does-not-exist", "d");
            fail("exception expected when using undefined category");
        } catch(WorkflowException ex) {
            // expected
        }

        assertTrue(types.containsKey("new-folder"));
        assertFalse(types.get("new-folder").contains("does-not-exist"));
        try {
            String path = workflow.add("new-folder", "does-not-exist", "d");
            fail("exception expected when usng undefined prototype");
        } catch(WorkflowException ex) {
            // expected
        }
    }

    @Test
    public void testTemplateDocument() throws RepositoryException, WorkflowException, RemoteException {
        Node source = session.getNode("/hippo:configuration/hippo:queries/hippo:templates/simple/hippostd:templates/new-document");
        assertFalse(source.hasProperty(HippoNodeType.HIPPO_AVAILABILITY));

        FolderWorkflow workflow = (FolderWorkflow) manager.getWorkflow("internal", node);
        assertNotNull(workflow);
        Map<String,Set<String>> types = workflow.list();
        assertNotNull(types);
        assertTrue(types.containsKey("simple"));
        assertTrue(types.get("simple").contains("new-document"));
        String path = workflow.add("simple", "new-document", "d");
        assertNotNull(path);

        Node docNode = session.getNode(path);
        assertEquals("/test/f/d/d",docNode.getPath());
        assertTrue(docNode.isNodeType(HippoNodeType.NT_DOCUMENT));

        Node parent = docNode.getParent();
        assertEquals(parent.getName(), docNode.getName());
        assertEquals(HippoNodeType.NT_HANDLE, parent.getPrimaryNodeType().getName());

        assertTrue(docNode.isNodeType("hippostd:document"));
        assertTrue(docNode.hasProperty(HippoNodeType.HIPPO_AVAILABILITY));
        assertEquals(0, docNode.getProperty(HippoNodeType.HIPPO_AVAILABILITY).getValues().length);
    }

    @Test
    public void testArchiveDocument() throws Exception {
        FolderWorkflow workflow = (FolderWorkflow) manager.getWorkflow("internal", node);
        assumeNotNull(workflow);
        final String docPath = workflow.add("simple", "new-document", "d");
        assumeTrue(session.nodeExists(docPath));
        workflow.archive("d");
        assertFalse(session.nodeExists(docPath));
    }

    @Test
    public void testReorderFolder() throws RepositoryException, WorkflowException, RemoteException {
        Map<String, String> nodes = new HashMap<String, String>();

        Node node = root.addNode("f","hippostd:folder");
        node.addMixin("mix:versionable");
        nodes.put("aap", node.addNode("aap").getIdentifier());
        nodes.put("noot", node.addNode("noot").getIdentifier());
        nodes.put("mies", node.addNode("mies").getIdentifier());
        nodes.put("zorro", node.addNode("zorro").getIdentifier());
        nodes.put("foo", node.addNode("foo").getIdentifier());
        nodes.put("bar", node.addNode("bar").getIdentifier());
        nodes.put("bar[2]", node.addNode("bar").getIdentifier());

        session.save();

        NodeIterator it = node.getNodes();
        assertEquals("aap", it.nextNode().getName());
        assertEquals("noot", it.nextNode().getName());
        assertEquals("mies", it.nextNode().getName());
        assertEquals("zorro", it.nextNode().getName());
        assertEquals("foo", it.nextNode().getName());
        assertEquals("bar", it.nextNode().getName());
        assertEquals("bar", it.nextNode().getName());

        WorkflowManager manager = ((HippoWorkspace)session.getWorkspace()).getWorkflowManager();
        FolderWorkflow workflow = (FolderWorkflow) manager.getWorkflow("internal", node);

        /*
         * aap      aap
         * noot     bar
         * mies     foo
         * zorro => mies
         * foo      noot
         * bar      zorro
         */
        List<String> newOrder = new LinkedList<String>();
        newOrder.add("aap");
        newOrder.add("bar[2]");
        newOrder.add("foo");
        newOrder.add("mies");
        newOrder.add("noot");
        newOrder.add("zorro");
        newOrder.add("bar");

        workflow.reorder(newOrder);
        node.getSession().refresh(false);

        it = node.getNodes();
        assertEquals(nodes.get("aap"), it.nextNode().getIdentifier());
        assertEquals(nodes.get("bar[2]"), it.nextNode().getIdentifier());
        assertEquals(nodes.get("foo"), it.nextNode().getIdentifier());
        assertEquals(nodes.get("mies"), it.nextNode().getIdentifier());
        assertEquals(nodes.get("noot"), it.nextNode().getIdentifier());
        assertEquals(nodes.get("zorro"), it.nextNode().getIdentifier());
        assertEquals(nodes.get("bar"), it.nextNode().getIdentifier());

    }

    /* The following two tests can only be executed if repository is run
     * locally, and the method copy in FolderWorkflowImpl is made public,
     * which is shouldn't be.  They where used for development purposes,
     * mainly.

    @Test
    public void testCopyFolder() throws RepositoryException, RemoteException {
        FolderWorkflowImpl workflow;
        workflow = new FolderWorkflowImpl(session, session, session.getRootNode().getNode(
                                "hippo:configuration/hippo:queries/hippo:templates/folder/hippostd:templates/document folder"));
        assertFalse(session.getRootNode().getNode("test").hasNode("folder"));
        TreeMap<String,String[]> renames = new TreeMap<String,String[]>();
        renames.put("./_name", new String[] { "f" });
        workflow.copy(session.getRootNode().getNode(
                                "hippo:configuration/hippo:queries/hippo:templates/folder/hippostd:templates/document folder"),
                                session.getRootNode().getNode("test"), renames, ".");
        assertTrue(session.getRootNode().getNode("test").hasNode("f"));
    }

    @Test
    public void testCopyDocument() throws RepositoryException, RemoteException {
        FolderWorkflowImpl workflow;
        workflow = new FolderWorkflowImpl(session, session, session.getRootNode().getNode(
                                                                 "hippo:configuration/hippo:queries/hippo:templates/document"));
        assertFalse(session.getRootNode().getNode("test").hasNode("document"));
        assertFalse(session.getRootNode().getNode("test").hasNode("d"));
        TreeMap<String,String[]> renames = new TreeMap<String,String[]>();
        renames.put("./_name", new String[] { "d" });
        renames.put("./_node/_name", new String[] { "d" });
        workflow.copy(session.getRootNode().getNode(
                                       "hippo:configuration/hippo:queries/hippo:templates/simple/hippostd:templates/document"),
                                       session.getRootNode().getNode("test"), renames, ".");
        assertTrue(session.getRootNode().getNode("test").hasNode("d"));
        assertTrue(session.getRootNode().getNode("test").getNode("d").hasNode("d"));
    }

    */

    @Test
    public void testCopyDocument() throws RepositoryException, RemoteException, WorkflowException {
        Node originalDocument = createDocument();

        FolderWorkflow workflow = (FolderWorkflow) manager.getWorkflow("internal", node);
        Document copy = workflow.copy(new Document(originalDocument), new Document(node), "dc");
        Node copyNode = copy.getNode(session);
        assertEquals("/test/f/dc/dc", copyNode.getPath());
        assertTrue(copyNode.isNodeType("hippostd:document"));
        assertEquals(CONTENT_ON_COPY, copyNode.getProperty("hippostd:content").getString());
    }

    @Test
    public void testCopyDocumentToDifferentFolder() throws RepositoryException, RemoteException, WorkflowException {
        Node originalDocument = createDocument();

        Node target = session.getNode("/test/aap");
        FolderWorkflow workflow = (FolderWorkflow) manager.getWorkflow("internal", node);
        Document copy = workflow.copy(new Document(originalDocument), new Document(target), "dc");
        Node copyNode = copy.getNode(session);
        assertEquals("/test/aap/dc/dc", copyNode.getPath());
        assertTrue(copyNode.isNodeType("hippostd:document"));
        assertEquals(CONTENT_ON_COPY, copyNode.getProperty("hippostd:content").getString());
    }

    private Node createDocument() throws PathNotFoundException, RepositoryException, ItemExistsException,
            VersionException, ConstraintViolationException, LockException, AccessDeniedException,
            ReferentialIntegrityException, InvalidItemStateException, NoSuchNodeTypeException {
        Node source = session.getNode("/hippo:configuration/hippo:queries/hippo:templates/simple/hippostd:templates/new-document");
        Node originalHandle = JcrUtils.copy(source, "d", node);
        Node originalDocument = originalHandle.getNode("new-document");
        session.move(originalDocument.getPath(), originalHandle.getPath() + "/d");
        session.save();
        return originalDocument;
    }

    private static void createDirectories(Session session, WorkflowManager manager, Node node, Random random, int numiters)
        throws RepositoryException, WorkflowException, RemoteException {
        Vector<String> paths = new Vector<String>();
        Vector<String> worklog = new Vector<String>();
        for(int itercount=0; itercount<numiters; ++itercount) {
            int parentIndex = (paths.size() > 0 ? random.nextInt(paths.size()) : -1);
            String parentPath;
            Node parent;
            if(parentIndex >= 0) {
                parentPath = paths.get(parentIndex);
                parent = node.getNode(parentPath.substring(2));
            } else {
                parentPath = ".";
                parent = node;
            }
            session.refresh(false);
            FolderWorkflow workflow = (FolderWorkflow) manager.getWorkflow("internal", parent);
            FolderWorkflow folderWorkflow = workflow;
            assertNotNull(workflow);
            Map<String,Set<String>> types = workflow.list();
            assertNotNull(types);
            assertTrue(types.containsKey("new-folder"));
            assertTrue(types.get("new-folder").contains("hippostd:folder"));
            String childPath = workflow.add("new-folder", "hippostd:folder", "f");
            assertNotNull(childPath);
            Node child = session.getRootNode().getNode(childPath.substring(1));
            assertTrue(child.isNodeType("hippostd:folder"));
            assertTrue(child.isNodeType("hippo:document"));
            assertTrue(child.isNodeType("mix:versionable"));
            childPath = parentPath + "/f";
            //assertEquals("/test/f"+childPath.substring(1), child.getPath());
            if(!paths.contains(childPath)) {
                paths.add(childPath);
            }
            worklog.add(childPath);
        }
    }

    @Test
    public void testExtensive() throws RepositoryException, WorkflowException, RemoteException {
        createDirectories(session, manager, node, new Random(72099L), 100);
    }

    private Exception concurrentError = null;

    private class ConcurrentRunner extends Thread {
        long seed;
        int niters;
        ConcurrentRunner(long seed, int niters) {
            this.seed = seed;
            this.niters = niters;
        }
        public void run() {
            try {
                Session session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
                WorkflowManager manager = ((HippoWorkspace)session.getWorkspace()).getWorkflowManager();
                Node test = session.getRootNode().getNode(node.getPath().substring(1));
                createDirectories(session, manager, node, new Random(seed), niters);
            } catch(RepositoryException ex) {
                concurrentError = ex;
            } catch(WorkflowException ex) {
                concurrentError = ex;
            } catch(RemoteException ex) {
                concurrentError = ex;
            }
        }
    }

    @Ignore
    public void testConcurrent() throws Exception {
        final int niters = 50;
        Thread thread1 = new ConcurrentRunner(2095487L, niters);
        Thread thread2 = new ConcurrentRunner(70178491L, niters);
        thread1.start();
        thread2.start();
        thread2.join();
        thread1.join();
        if(concurrentError != null) {
            throw concurrentError;
        }
    }

    @Ignore
    public void testMoreConcurrent() throws Exception {
        final int nthreads = 20;
        final int niters = 30;
        long seed = 1209235890128L;
        Thread[] threads = new Thread[nthreads];
        for(int i=0; i<nthreads; i++) {
            threads[i] = new ConcurrentRunner(seed++, niters);
        }
        for(int i=0; i<nthreads; i++) {
            threads[i].start();
        }
        for(int i=0; i<nthreads; i++) {
            threads[i].join();
        }
        if(concurrentError != null) {
            throw concurrentError;
        }
    }
}
