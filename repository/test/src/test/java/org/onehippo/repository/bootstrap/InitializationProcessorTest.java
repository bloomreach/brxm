/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.bootstrap;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.commons.io.FileUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.hippoecm.repository.api.HippoNodeType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.webfiles.WebFileException;
import org.onehippo.cms7.services.webfiles.WebFilesService;
import org.onehippo.repository.bootstrap.util.BootstrapConstants;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.onehippo.repository.testutils.ZipTestUtil;
import org.onehippo.repository.testutils.slf4j.LoggerRecordingWrapper;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

import static org.easymock.EasyMock.and;
import static org.easymock.EasyMock.anyBoolean;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENT;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTDELETE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTPROPADD;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTPROPSET;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTRESOURCE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTROOT;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTEXTPATHS;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_EXTENSIONSOURCE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_RELOADONSTARTUP;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_SEQUENCE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_STATUS;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_UPSTREAMITEMS;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_VERSION;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_WEB_FILE_BUNDLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class InitializationProcessorTest extends RepositoryTestCase {

    private Node test;
    private Node item;
    private WebFilesService webFilesService;
    private InitializationProcessor processor;
    private LoggerRecordingWrapper loggingRecorder;
    private Logger bootstrapLogger = BootstrapConstants.log;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        test = session.getRootNode().addNode("test");
        item = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testitem", "hipposys:initializeitem");
        item.setProperty(HIPPO_STATUS, "pending");
        session.getRootNode().addNode("webfiles");
        session.save();

        webFilesService = EasyMock.createMock(WebFilesService.class);
        HippoServiceRegistry.registerService(webFilesService, WebFilesService.class);
        if (bootstrapLogger.isInfoEnabled()) {
            loggingRecorder = new LoggerRecordingWrapper(bootstrapLogger);
        } else {
            loggingRecorder = new LoggerRecordingWrapper(NOPLogger.NOP_LOGGER);
        }
        BootstrapConstants.log = loggingRecorder;
        processor = new InitializationProcessorImpl();
    }

    @After
    public void tearDown() throws Exception {
        removeNode("/hippo:configuration/hippo:initialize/testitem");
        removeNode("/hippo:configuration/hippo:initialize/upstream");
        removeNode("/webfiles");

        HippoServiceRegistry.unregisterService(webFilesService, WebFilesService.class);
        BootstrapConstants.log = bootstrapLogger;
        super.tearDown();
    }

    private List<PostStartupTask> process() {
        return processor.processInitializeItems(session);
    }

    private void checkPropNode(String expected) throws RepositoryException {
        Node node = session.getNode("/test/propnode");
        assertFalse(node.hasProperty("hippo:multi"));
        assertTrue(node.hasProperty("hippo:single"));
        assertEquals(expected, node.getProperty("hippo:single").getString());
    }

    private void checkPropNode(String[] expected) throws RepositoryException {
        Node node = session.getNode("/test/propnode");
        assertTrue(node.hasProperty("hippo:multi"));
        assertFalse(node.hasProperty("hippo:single"));
        Value[] values = node.getProperty("hippo:multi").getValues();
        assertEquals(expected.length, values.length);
        int count = 0;
        for (Value value : values) {
            assertEquals(expected[count++], value.getString());
        }
    }

    @Test
    public void testAddContentFromNode() throws Exception {
        item.setProperty(HIPPO_CONTENTROOT, "/test");
        item.setProperty(HIPPO_CONTENT, "<sv:node xmlns:sv=\"http://www.jcp.org/jcr/sv/1.0\" xmlns:nt=\"http://www.jcp.org/jcr/nt/1.0\" xmlns:jcr=\"http://www.jcp.org/jcr/1.0\" sv:name=\"testnode\"><sv:property sv:name=\"jcr:primaryType\" sv:type=\"Name\"><sv:value>nt:unstructured</sv:value></sv:property></sv:node>");
        session.save();
        final List<PostStartupTask> tasks = process();
        assertEquals("There should be no post-startup tasks", 0, tasks.size());
        assertTrue(session.nodeExists("/test/testnode"));
        assertEquals("done", item.getProperty(HIPPO_STATUS).getString());
    }

    @Test
    public void testContentDelete() throws Exception {
        final Node delete = test.addNode("delete");
        item.setProperty(HIPPO_CONTENTDELETE, delete.getPath());
        session.save();
        final List<PostStartupTask> tasks = process();
        assertEquals("There should be no post-startup tasks", 0, tasks.size());
        assertFalse(session.nodeExists("/test/delete"));
    }

    @Test
    public void testContentDeleteFailsOnSNS() throws Exception {
        final Node sns = test.addNode("sns");
        test.addNode("sns");
        item.setProperty(HIPPO_CONTENTDELETE, sns.getPath());
        session.save();
        final List<PostStartupTask> tasks = process();
        assertEquals("There should be no post-startup tasks", 0, tasks.size());
        assertEquals(2, test.getNodes("sns").getSize());
    }

    @Test
    public void testPropertyDelete() throws Exception {
        final Property delete = test.setProperty("delete", "");
        item.setProperty(HippoNodeType.HIPPO_CONTENTPROPDELETE, delete.getPath());
        session.save();
        final List<PostStartupTask> tasks = process();
        assertEquals("There should be no post-startup tasks", 0, tasks.size());
        assertFalse(session.propertyExists("/test/delete"));
    }

    @Test
    public void testPropSetNoParent() throws Exception {
        item.setProperty(HIPPO_CONTENTROOT, "/test/propnode/hippo:single");
        item.setProperty(HIPPO_CONTENTPROPSET, new String[]{"a"});
        session.save();
        final List<PostStartupTask> tasks = process();
        assertEquals("There should be no post-startup tasks", 0, tasks.size());
        assertFalse(session.propertyExists("/test/propnode/hippo:single"));
    }

    @Test
    public void testPropSetSingle() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit");
        item.setProperty(HIPPO_CONTENTROOT, "/test/propnode/hippo:single");
        item.setProperty(HIPPO_CONTENTPROPSET, new String[]{"b"});
        session.save();
        final List<PostStartupTask> tasks = process();
        assertEquals("There should be no post-startup tasks", 0, tasks.size());
        checkPropNode("b");
    }

    @Test
    public void testPropSetMulti() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit");
        item.setProperty(HIPPO_CONTENTROOT, "/test/propnode/hippo:multi");
        item.setProperty(HIPPO_CONTENTPROPSET, new String[]{"m", "n"});
        session.save();
        final List<PostStartupTask> tasks = process();
        assertEquals("There should be no post-startup tasks", 0, tasks.size());
        checkPropNode(new String[]{"m", "n"});
    }

    @Test
    public void testPropAdd() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit").setProperty("hippo:multi", new String[] { "m", "n" });
        item.setProperty(HIPPO_CONTENTROOT, "/test/propnode/hippo:multi");
        item.setProperty(HIPPO_CONTENTPROPADD, new String[] { "o", "p" });
        session.save();
        final List<PostStartupTask> tasks = process();
        assertEquals("There should be no post-startup tasks", 0, tasks.size());
        checkPropNode(new String[]{"m", "n", "o", "p"});
    }

    @Test
    public void testPlainContentResourceInitialization() throws Exception {
        item.setProperty(HIPPO_CONTENTRESOURCE, getClass().getResource("/bootstrap/foo.xml").toString());
        item.setProperty(HIPPO_CONTENTROOT, "/test");
        session.save();
        List<PostStartupTask> tasks = process();
        assertEquals("There should be no post-startup tasks", 0, tasks.size());

        assertTrue(test.hasNode("foo"));
        Node foo = test.getNode("foo");
        assertTrue(foo.hasProperty("jcr:created"));
        final Calendar created = foo.getProperty("jcr:created").getDate();

        // test reload
        item.setProperty(HIPPO_RELOADONSTARTUP, true);
        item.setProperty(HIPPO_CONTEXTPATHS, new String[] { "/test/foo" });
        item.setProperty(HIPPO_STATUS, "pending");
        session.save();
        tasks = process();
        assertEquals("There should be no post-startup tasks", 0, tasks.size());

        assertTrue(test.hasNode("foo"));
        foo = test.getNode("foo");
        assertTrue(foo.hasProperty("jcr:created"));
        assertFalse(created.equals(foo.getProperty("jcr:created").getDate()));
    }

    @Test
    public void testContentReloadWithDeltaCombines() throws Exception {
        Node fooItemNode = session.getRootNode().addNode("hippo:configuration/hippo:initialize/foo", "hipposys:initializeitem");
        fooItemNode.setProperty(HIPPO_CONTENTRESOURCE, getClass().getResource("/bootstrap/foo.xml").toString());
        fooItemNode.setProperty(HIPPO_CONTENTROOT, "/test");
        fooItemNode.setProperty(HIPPO_STATUS, "pending");
        fooItemNode.setProperty(HIPPO_SEQUENCE, 1l);
        Node barItemNode = session.getRootNode().addNode("hippo:configuration/hippo:initialize/bar", "hipposys:initializeitem");
        barItemNode.setProperty(HIPPO_CONTENTRESOURCE, getClass().getResource("/bootstrap/bar.xml").toString());
        barItemNode.setProperty(HIPPO_CONTENTROOT, "/test/foo");
        barItemNode.setProperty(HIPPO_CONTEXTPATHS, new String[]{"/test/foo/bar"});
        barItemNode.setProperty(HIPPO_STATUS, "pending");
        barItemNode.setProperty(HIPPO_SEQUENCE, 2l);
        Node deltaItemNode = session.getRootNode().addNode("hippo:configuration/hippo:initialize/delta", "hipposys:initializeitem");
        deltaItemNode.setProperty(HIPPO_CONTENTRESOURCE, getClass().getResource("/bootstrap/delta.xml").toString());
        deltaItemNode.setProperty(HIPPO_CONTENTROOT, "/test");
        deltaItemNode.setProperty(HIPPO_CONTEXTPATHS, new String[]{"/test/foo", "/test/foo/bar"});
        deltaItemNode.setProperty(HIPPO_STATUS, "pending");
        deltaItemNode.setProperty(HIPPO_SEQUENCE, 3l);
        session.save();
        process();
        barItemNode.setProperty(HIPPO_RELOADONSTARTUP, true);
        barItemNode.setProperty(HIPPO_VERSION, "1");
        session.save();
        List<InitializeItem> initializeItems = Arrays.asList(new InitializeItem(fooItemNode), new InitializeItem(barItemNode), new InitializeItem(deltaItemNode));
        InitializationProcessorImpl processor = new InitializationProcessorImpl();
        List<InitializeItem> reloadItems = Arrays.asList(new InitializeItem(barItemNode));
        processor.markReloadDownstreamItems(session, initializeItems, reloadItems);
        assertEquals("pending", deltaItemNode.getProperty(HIPPO_STATUS).getString());
        assertNotNull(deltaItemNode.getProperty(HIPPO_UPSTREAMITEMS));
        assertEquals(1, deltaItemNode.getProperty(HIPPO_UPSTREAMITEMS).getValues().length);
        assertEquals(barItemNode.getIdentifier(), deltaItemNode.getProperty(HIPPO_UPSTREAMITEMS).getValues()[0].getString());
        process();
    }

    @Test
    public void testCombineContentResourceInitialization() throws Exception {
        test.addNode("foo").addNode("bar");
        item.setProperty(HIPPO_CONTENTRESOURCE, getClass().getResource("/bootstrap/delta.xml").toString());
        item.setProperty(HIPPO_CONTENTROOT, "/test");
        session.save();
        List<PostStartupTask> tasks = process();
        assertEquals("There should be no post-startup tasks", 0, tasks.size());

        assertTrue(session.propertyExists("/test/foo/bar/baz"));
    }

    @Test
    public void testWebFileBundleInitializationFromJar() throws Exception {
        item.setProperty(HIPPO_WEB_FILE_BUNDLE, "resources");
        item.setProperty(HIPPO_EXTENSIONSOURCE, getClass().getResource("/hippo.jar").toString() + "!/hippoecm-extension.xml");
        session.save();

        final List<PostStartupTask> tasks = process();
        assertEquals("There should be one post-startup task", 1, tasks.size());

        // test the post-startup task
        final PostStartupTask importWebFiles = tasks.get(0);

        Capture<ZipFile> capturedZip = new Capture();
        webFilesService.importJcrWebFileBundle(anyObject(Session.class), and(capture(capturedZip), isA(ZipFile.class)), anyBoolean());
        expectLastCall();

        replay(webFilesService);
        importWebFiles.execute();
        verify(webFilesService);

        ZipFile zip = capturedZip.getValue();
        assertEquals(5, zip.size());
        ZipTestUtil.assertEntries(zip,
                "resources/",
                "resources/images/",
                "resources/images/hippo.png",
                "resources/javascript/",
                "resources/javascript/hippo.js"
        );

        // test reload
        item.setProperty(HIPPO_RELOADONSTARTUP, true);
        item.setProperty(HIPPO_STATUS, "pending");
        session.save();

        final List<PostStartupTask> reloadTasks = process();
        assertEquals("There should be one post-startup task after reloading a web file bundle", 1, reloadTasks.size());
        final PostStartupTask reimportWebFiles = reloadTasks.get(0);

        EasyMock.reset(webFilesService);
        webFilesService.importJcrWebFileBundle(anyObject(Session.class), anyObject(ZipFile.class), anyBoolean());
        expectLastCall();

        replay(webFilesService);
        reimportWebFiles.execute();
        verify(webFilesService);
    }

    @Test
    public void testWebFileBundleInitializationFromDirectory() throws Exception {
        final URL testBundleUrl = getClass().getResource("/hippoecm-extension.xml");

        item.setProperty(HIPPO_WEB_FILE_BUNDLE, "webfilebundle");
        item.setProperty(HIPPO_EXTENSIONSOURCE, testBundleUrl.toString());
        session.save();

        final List<PostStartupTask> tasks = process();
        assertEquals("There should be one post-startup task", 1, tasks.size());

        // test the post-startup task
        final PostStartupTask importWebFiles = tasks.get(0);

        final File testBundleDir = new File(FileUtils.toFile(testBundleUrl).getParent(), "webfilebundle");
        webFilesService.importJcrWebFileBundle(anyObject(Session.class), eq(testBundleDir), anyBoolean());
        expectLastCall();

        replay(webFilesService);
        importWebFiles.execute();
        verify(webFilesService);

        // test reload
        item.setProperty(HIPPO_RELOADONSTARTUP, true);
        item.setProperty(HIPPO_STATUS, "pending");
        session.save();

        final List<PostStartupTask> reloadTasks = process();
        assertEquals("There should be one post-startup task after reloading a web file bundle", 1, reloadTasks.size());
        final PostStartupTask reimportWebFiles = reloadTasks.get(0);

        EasyMock.reset(webFilesService);
        webFilesService.importJcrWebFileBundle(anyObject(Session.class), eq(testBundleDir), anyBoolean());
        expectLastCall();

        replay(webFilesService);
        reimportWebFiles.execute();
        verify(webFilesService);
    }

    @Test
    public void testMissingWebFileBundleInitializationLogsError() throws Exception {
        final URL testBundleUrl = getClass().getResource("/hippoecm-extension.xml");

        item.setProperty(HIPPO_WEB_FILE_BUNDLE, "noSuchDirectory");
        item.setProperty(HIPPO_EXTENSIONSOURCE, testBundleUrl.toString());
        session.save();

        final List<PostStartupTask> tasks = process();
        assertEquals("There should be one post-startup task", 1, tasks.size());

        // test the post-startup task
        final PostStartupTask importWebFiles = tasks.get(0);

        final File testBundleDir = new File(FileUtils.toFile(testBundleUrl).getParent(), "noSuchDirectory");
        webFilesService.importJcrWebFileBundle(anyObject(Session.class), eq(testBundleDir), anyBoolean());
        expectLastCall().andThrow(new WebFileException("simulate a web file exception during import"));

        replay(webFilesService);
        importWebFiles.execute();
        verify(webFilesService);

        assertEquals("expected an error message to be logged", 1, loggingRecorder.getErrorMessages().size());
    }

    @Test
    public void testResolveDownstreamContentResourceItems() throws Exception {
        item.setProperty(HIPPO_CONTENTROOT, "/");
        item.setProperty(HIPPO_CONTENTRESOURCE, "fake");
        item.setProperty(HIPPO_CONTEXTPATHS, new String[] { "/foo", "/foo/bar" } );
        Node upstreamItemNode = session.getRootNode().addNode("hippo:configuration/hippo:initialize/upstream", "hipposys:initializeitem");
        upstreamItemNode.setProperty(HIPPO_CONTEXTPATHS, new String[]{"/foo"});
        session.save();

        InitializeItem upstreamItem = new InitializeItem(upstreamItemNode);
        InitializeItem downstreamItem = new InitializeItem(item);
        final List<InitializeItem> initializeItems = Arrays.asList(downstreamItem, upstreamItem);

        InitializationProcessorImpl processor = new InitializationProcessorImpl();
        Iterator<InitializeItem> downstreamItems = processor.resolveDownstreamItems(upstreamItem, initializeItems).iterator();
        assertTrue(downstreamItems.hasNext());
        assertFalse(upstreamItemNode.isSame(downstreamItems.next().getItemNode()));
        assertFalse(downstreamItems.hasNext());

        upstreamItemNode.setProperty(HIPPO_CONTEXTPATHS, new String[]{"/foo/bar"});
        session.save();
        downstreamItems = processor.resolveDownstreamItems(upstreamItem, initializeItems).iterator();
        assertTrue(downstreamItems.hasNext());
        assertFalse(upstreamItemNode.isSame(downstreamItems.next().getItemNode()));
        assertFalse(downstreamItems.hasNext());

        upstreamItemNode.setProperty(HIPPO_CONTEXTPATHS, new String[]{"/foo"});
        item.setProperty(HIPPO_CONTEXTPATHS, new String[] { "/foobar" } );
        session.save();

        downstreamItems = processor.resolveDownstreamItems(upstreamItem, initializeItems).iterator();
        assertFalse(downstreamItems.hasNext());
    }

    @Test
    public void testResolveContentPropSetAndAddDownstreamItems() throws Exception {
        item.setProperty(HIPPO_CONTENTROOT, "/foo/bar");
        item.setProperty(HIPPO_CONTENTPROPSET, new String[] { "<dummy>" });
        Node upstreamItemNode = session.getRootNode().addNode("hippo:configuration/hippo:initialize/upstream", "hipposys:initializeitem");
        upstreamItemNode.setProperty(HIPPO_CONTEXTPATHS, new String[]{"/foo"});
        session.save();

        InitializeItem upstreamItem = new InitializeItem(upstreamItemNode);
        InitializeItem downstreamItem = new InitializeItem(item);
        List<InitializeItem> initializeItems = Arrays.asList(downstreamItem, upstreamItem);

        InitializationProcessorImpl processor = new InitializationProcessorImpl();
        Iterator<InitializeItem> downstreamItems = processor.resolveDownstreamItems(upstreamItem, initializeItems).iterator();
        assertTrue(downstreamItems.hasNext());
        assertFalse(upstreamItemNode.isSame(downstreamItems.next().getItemNode()));
        assertFalse(downstreamItems.hasNext());

        upstreamItemNode.setProperty(HIPPO_CONTEXTPATHS, new String[]{"/foo"});
        session.save();
        item.setProperty(HIPPO_CONTENTROOT, "/foobar");
        session.save();
        downstreamItems = processor.resolveDownstreamItems(upstreamItem, initializeItems).iterator();
        assertFalse(downstreamItems.hasNext());
    }

    @Test
    public void testResolveContentDeleteAndContentPropDeleteDownstreamItems() throws Exception {
        item.setProperty(HIPPO_CONTENTDELETE, "/foo/bar");
        Node upstreamItemNode = session.getRootNode().addNode("hippo:configuration/hippo:initialize/upstream", "hipposys:initializeitem");
        upstreamItemNode.setProperty(HIPPO_CONTEXTPATHS, new String[]{"/foo"});
        session.save();

        InitializeItem upstreamItem = new InitializeItem(upstreamItemNode);
        InitializeItem downstreamItem = new InitializeItem(item);
        List<InitializeItem> initializeItems = Arrays.asList(downstreamItem, upstreamItem);

        InitializationProcessorImpl processor = new InitializationProcessorImpl();
        Iterator<InitializeItem> downstreamItems = processor.resolveDownstreamItems(upstreamItem, initializeItems).iterator();
        assertTrue(downstreamItems.hasNext());
        assertFalse(upstreamItemNode.isSame(downstreamItems.next().getItemNode()));
        assertFalse(downstreamItems.hasNext());

        upstreamItemNode.setProperty(HIPPO_CONTEXTPATHS, new String[]{"/foo/bar"});
        session.save();
        downstreamItems = processor.resolveDownstreamItems(upstreamItem, initializeItems).iterator();
        assertTrue(downstreamItems.hasNext());
        assertFalse(upstreamItemNode.isSame(downstreamItems.next().getItemNode()));
        assertFalse(downstreamItems.hasNext());

        item.setProperty(HIPPO_CONTENTDELETE, "/foobar");
        upstreamItemNode.setProperty(HIPPO_CONTEXTPATHS, new String[]{"/foo"});
        session.save();
        downstreamItems = processor.resolveDownstreamItems(upstreamItem, initializeItems).iterator();
        assertFalse(downstreamItems.hasNext());
    }

    @Test
    public void testDetectDuplicateItems() throws Exception {
        final Extension extension = new Extension(session, getClass().getResource("/bootstrap/hippoecm-extension.xml"));
        final Map<String, String> itemNames = new HashMap<String, String>() {{ put("duplicate", "<extension>"); }};
        final List<InitializeItem> load = extension.load(itemNames);
        assertEquals(0, load.size());
        assertEquals(1, loggingRecorder.getErrorMessages().size());
    }

}
