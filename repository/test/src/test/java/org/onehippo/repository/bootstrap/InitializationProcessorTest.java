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
package org.onehippo.repository.bootstrap;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipFile;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.InitializationProcessor;
import org.hippoecm.repository.api.PostStartupTask;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.webresources.WebResourceException;
import org.onehippo.cms7.services.webresources.WebResourcesService;
import org.onehippo.repository.bootstrap.InitializationProcessorImpl;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.onehippo.repository.testutils.ZipTestUtil;
import org.onehippo.repository.testutils.slf4j.LoggerRecordingWrapper;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

import static org.easymock.EasyMock.and;
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
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTEXTNODENAME;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTEXTPATHS;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_EXTENSIONSOURCE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_RELOADONSTARTUP;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_SEQUENCE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_STATUS;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_UPSTREAMITEMS;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_VERSION;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_WEBRESOURCEBUNDLE;
import static org.onehippo.repository.bootstrap.InitializationProcessorImpl.ContentFileInfo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class InitializationProcessorTest extends RepositoryTestCase {

    private Node test;
    private Node item;
    private WebResourcesService webResourcesService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        test = session.getRootNode().addNode("test");
        item = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        item.setProperty(HIPPO_STATUS, "pending");
        session.getRootNode().addNode("webresources");
        session.save();

        webResourcesService = EasyMock.createMock(WebResourcesService.class);
        HippoServiceRegistry.registerService(webResourcesService, WebResourcesService.class);
    }

    @After
    public void tearDown() throws Exception {
        removeNode("/hippo:configuration/hippo:initialize/testnode");
        removeNode("/webresources");

        HippoServiceRegistry.unregisterService(webResourcesService, WebResourcesService.class);

        super.tearDown();
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

    private List<PostStartupTask> process(Logger logger) {
        InitializationProcessor processor = new InitializationProcessorImpl(logger);
        return processor.processInitializeItems(session);
    }

    @Test
    public void testAddContentFromNode() throws Exception {
        item.setProperty(HIPPO_CONTENTROOT, "/test");
        item.setProperty(HIPPO_CONTENT, "<sv:node xmlns:sv=\"http://www.jcp.org/jcr/sv/1.0\" xmlns:nt=\"http://www.jcp.org/jcr/nt/1.0\" xmlns:jcr=\"http://www.jcp.org/jcr/1.0\" sv:name=\"testnode\"><sv:property sv:name=\"jcr:primaryType\" sv:type=\"Name\"><sv:value>nt:unstructured</sv:value></sv:property></sv:node>");
        session.save();
        final List<PostStartupTask> tasks = process(null);
        assertEquals("There should be no post-startup tasks", 0, tasks.size());
        assertTrue(session.nodeExists("/test/testnode"));
        assertEquals("done", item.getProperty(HIPPO_STATUS).getString());
    }

    @Test
    public void testContentDelete() throws Exception {
        final Node delete = test.addNode("delete");
        item.setProperty(HIPPO_CONTENTDELETE, delete.getPath());
        session.save();
        final List<PostStartupTask> tasks = process(null);
        assertEquals("There should be no post-startup tasks", 0, tasks.size());
        assertFalse(session.nodeExists("/test/delete"));
    }

    @Test
    public void testContentDeleteFailsOnSNS() throws Exception {
        final Node sns = test.addNode("sns");
        test.addNode("sns");
        item.setProperty(HIPPO_CONTENTDELETE, sns.getPath());
        session.save();
        final List<PostStartupTask> tasks = process(NOPLogger.NOP_LOGGER);
        assertEquals("There should be no post-startup tasks", 0, tasks.size());
        assertEquals(2, test.getNodes("sns").getSize());
    }

    @Test
    public void testPropertyDelete() throws Exception {
        final Property delete = test.setProperty("delete", "");
        item.setProperty(HippoNodeType.HIPPO_CONTENTPROPDELETE, delete.getPath());
        session.save();
        final List<PostStartupTask> tasks = process(null);
        assertEquals("There should be no post-startup tasks", 0, tasks.size());
        assertFalse(session.propertyExists("/test/delete"));
    }

    @Test
    public void testPropSetNoParent() throws Exception {
        item.setProperty(HIPPO_CONTENTROOT, "/test/propnode/hippo:single");
        item.setProperty(HIPPO_CONTENTPROPSET, new String[]{"a"});
        session.save();
        final List<PostStartupTask> tasks = process(NOPLogger.NOP_LOGGER);
        assertEquals("There should be no post-startup tasks", 0, tasks.size());
        assertFalse(session.propertyExists("/test/propnode/hippo:single"));
    }

    @Test
    public void testPropSetSingle() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit");
        item.setProperty(HIPPO_CONTENTROOT, "/test/propnode/hippo:single");
        item.setProperty(HIPPO_CONTENTPROPSET, new String[]{"b"});
        session.save();
        final List<PostStartupTask> tasks = process(null);
        assertEquals("There should be no post-startup tasks", 0, tasks.size());
        checkPropNode("b");
    }

    @Test
    public void testPropSetMulti() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit");
        item.setProperty(HIPPO_CONTENTROOT, "/test/propnode/hippo:multi");
        item.setProperty(HIPPO_CONTENTPROPSET, new String[]{"m", "n"});
        session.save();
        final List<PostStartupTask> tasks = process(null);
        assertEquals("There should be no post-startup tasks", 0, tasks.size());
        checkPropNode(new String[]{"m", "n"});
    }

    @Test
    public void testPropAdd() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit").setProperty("hippo:multi", new String[] { "m", "n" });
        item.setProperty(HIPPO_CONTENTROOT, "/test/propnode/hippo:multi");
        item.setProperty(HIPPO_CONTENTPROPADD, new String[] { "o", "p" });
        session.save();
        final List<PostStartupTask> tasks = process(null);
        assertEquals("There should be no post-startup tasks", 0, tasks.size());
        checkPropNode(new String[]{"m", "n", "o", "p"});
    }

    @Test
    public void testPlainContentResourceInitialization() throws Exception {
        item.setProperty(HIPPO_CONTENTRESOURCE, getClass().getResource("/bootstrap/foo.xml").toString());
        item.setProperty(HIPPO_CONTENTROOT, "/test");
        session.save();
        List<PostStartupTask> tasks = process(null);
        assertEquals("There should be no post-startup tasks", 0, tasks.size());

        assertTrue(test.hasNode("foo"));
        Node foo = test.getNode("foo");
        assertTrue(foo.hasProperty("jcr:created"));
        final Calendar created = foo.getProperty("jcr:created").getDate();

        // test reload
        item.setProperty(HIPPO_RELOADONSTARTUP, true);
        item.setProperty(HIPPO_CONTEXTNODENAME, "foo");
        item.setProperty(HIPPO_STATUS, "pending");
        session.save();
        tasks = process(null);
        assertEquals("There should be no post-startup tasks", 0, tasks.size());

        assertTrue(test.hasNode("foo"));
        foo = test.getNode("foo");
        assertTrue(foo.hasProperty("jcr:created"));
        assertFalse(created.equals(foo.getProperty("jcr:created").getDate()));
    }

    @Test
    public void testContentReloadWithDeltaCombines() throws Exception {
        Node fooItem = session.getRootNode().addNode("hippo:configuration/hippo:initialize/foo", "hipposys:initializeitem");
        fooItem.setProperty(HIPPO_CONTENTRESOURCE, getClass().getResource("/bootstrap/foo.xml").toString());
        fooItem.setProperty(HIPPO_CONTENTROOT, "/test");
        fooItem.setProperty(HIPPO_STATUS, "pending");
        fooItem.setProperty(HIPPO_SEQUENCE, 1l);
        Node barItem = session.getRootNode().addNode("hippo:configuration/hippo:initialize/bar", "hipposys:initializeitem");
        barItem.setProperty(HIPPO_CONTENTRESOURCE, getClass().getResource("/bootstrap/bar.xml").toString());
        barItem.setProperty(HIPPO_CONTENTROOT, "/test/foo");
        barItem.setProperty(HIPPO_CONTEXTNODENAME, "bar");
        barItem.setProperty(HIPPO_STATUS, "pending");
        barItem.setProperty(HIPPO_SEQUENCE, 2l);
        Node deltaItem = session.getRootNode().addNode("hippo:configuration/hippo:initialize/delta", "hipposys:initializeitem");
        deltaItem.setProperty(HIPPO_CONTENTRESOURCE, getClass().getResource("/bootstrap/delta.xml").toString());
        deltaItem.setProperty(HIPPO_CONTENTROOT, "/test");
        deltaItem.setProperty(HIPPO_STATUS, "pending");
        deltaItem.setProperty(HIPPO_SEQUENCE, 3l);
        session.save();
        process(null);
//        session.exportSystemView("/test", System.out, true, false);
        barItem.setProperty(HIPPO_RELOADONSTARTUP, true);
        barItem.setProperty(HIPPO_VERSION, "1");
        session.save();
        InitializationProcessorImpl processor = new InitializationProcessorImpl();
        Set<String> reloadItems = new HashSet<>();
        reloadItems.add(barItem.getIdentifier());
        processor.markReloadDownstreamItems(session, reloadItems);
        assertEquals("pending", deltaItem.getProperty(HIPPO_STATUS).getString());
        assertNotNull(deltaItem.getProperty(HIPPO_UPSTREAMITEMS));
        assertEquals(1, deltaItem.getProperty(HIPPO_UPSTREAMITEMS).getValues().length);
        assertEquals(barItem.getIdentifier(), deltaItem.getProperty(HIPPO_UPSTREAMITEMS).getValues()[0].getString());
        process(null);
        session.exportSystemView("/test", System.out, true, false);
//        assertTrue(session.propertyExists("/test/foo/bar/baz"));
    }

    @Test
    public void testCombineContentResourceInitialization() throws Exception {
        test.addNode("foo").addNode("bar");
        item.setProperty(HIPPO_CONTENTRESOURCE, getClass().getResource("/bootstrap/delta.xml").toString());
        item.setProperty(HIPPO_CONTENTROOT, "/test");
        session.save();
        List<PostStartupTask> tasks = process(null);
        assertEquals("There should be no post-startup tasks", 0, tasks.size());

        assertTrue(test.hasNode("foo"));
        assertTrue(item.hasProperty(HIPPO_CONTEXTPATHS));
        final Value[] values = item.getProperty(HIPPO_CONTEXTPATHS).getValues();
        assertEquals(2, values.length);
        assertEquals("/test/foo", values[0].getString());
        assertEquals("/test/foo/bar", values[1].getString());
    }

    @Test
    public void testWebResourceBundleInitializationFromJar() throws Exception {
        item.setProperty(HIPPO_WEBRESOURCEBUNDLE, "resources");
        item.setProperty(HIPPO_EXTENSIONSOURCE, getClass().getResource("/hippo.jar").toString() + "!/hippoecm-extension.xml");
        session.save();

        final List<PostStartupTask> tasks = process(null);
        assertEquals("There should be one post-startup task", 1, tasks.size());

        // test the post-startup task
        final PostStartupTask importWebResources = tasks.get(0);

        Capture<ZipFile> capturedZip = new Capture();
        webResourcesService.importJcrWebResourceBundle(anyObject(Session.class), and(capture(capturedZip), isA(ZipFile.class)));
        expectLastCall();

        replay(webResourcesService);
        importWebResources.execute();
        verify(webResourcesService);

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

        final List<PostStartupTask> reloadTasks = process(null);
        assertEquals("There should be one post-startup task after reloading a web resource bundle", 1, reloadTasks.size());
        final PostStartupTask reimportWebresources = reloadTasks.get(0);

        EasyMock.reset(webResourcesService);
        webResourcesService.importJcrWebResourceBundle(anyObject(Session.class), anyObject(ZipFile.class));
        expectLastCall();

        replay(webResourcesService);
        reimportWebresources.execute();
        verify(webResourcesService);
    }

    @Test
    public void testWebResourceBundleInitializationFromDirectory() throws Exception {
        final URL testBundleUrl = getClass().getResource("/hippoecm-extension.xml");

        item.setProperty(HIPPO_WEBRESOURCEBUNDLE, "webresourcebundle");
        item.setProperty(HIPPO_EXTENSIONSOURCE, testBundleUrl.toString());
        session.save();

        final List<PostStartupTask> tasks = process(null);
        assertEquals("There should be one post-startup task", 1, tasks.size());

        // test the post-startup task
        final PostStartupTask importWebResources = tasks.get(0);

        final File testBundleDir = new File(FileUtils.toFile(testBundleUrl).getParent(), "webresourcebundle");
        webResourcesService.importJcrWebResourceBundle(anyObject(Session.class), eq(testBundleDir));
        expectLastCall();

        replay(webResourcesService);
        importWebResources.execute();
        verify(webResourcesService);

        // test reload
        item.setProperty(HIPPO_RELOADONSTARTUP, true);
        item.setProperty(HIPPO_STATUS, "pending");
        session.save();

        final List<PostStartupTask> reloadTasks = process(null);
        assertEquals("There should be one post-startup task after reloading a web resource bundle", 1, reloadTasks.size());
        final PostStartupTask reimportWebresources = reloadTasks.get(0);

        EasyMock.reset(webResourcesService);
        webResourcesService.importJcrWebResourceBundle(anyObject(Session.class), eq(testBundleDir));
        expectLastCall();

        replay(webResourcesService);
        reimportWebresources.execute();
        verify(webResourcesService);
    }

    @Test
    public void testMissingWebResourceBundleInitializationLogsError() throws Exception {
        final URL testBundleUrl = getClass().getResource("/hippoecm-extension.xml");

        item.setProperty(HIPPO_WEBRESOURCEBUNDLE, "noSuchDirectory");
        item.setProperty(HIPPO_EXTENSIONSOURCE, testBundleUrl.toString());
        session.save();

        final LoggerRecordingWrapper recordingLogger = new LoggerRecordingWrapper(NOPLogger.NOP_LOGGER);
        final List<PostStartupTask> tasks = process(recordingLogger);
        assertEquals("There should be one post-startup task", 1, tasks.size());

        // test the post-startup task
        final PostStartupTask importWebResources = tasks.get(0);

        final File testBundleDir = new File(FileUtils.toFile(testBundleUrl).getParent(), "noSuchDirectory");
        webResourcesService.importJcrWebResourceBundle(anyObject(Session.class), eq(testBundleDir));
        expectLastCall().andThrow(new WebResourceException("simulate a web resource exception during import"));

        replay(webResourcesService);
        importWebResources.execute();
        verify(webResourcesService);

        assertEquals("expected an error message to be logged", 1, recordingLogger.getErrorMessages().size());
    }

    @Test
    public void testResolveDownstreamItemsBasedOnContentRoot() throws Exception {
        item.setProperty(HIPPO_CONTENTROOT, "/");
        item.setProperty(HIPPO_CONTEXTNODENAME, "foo");
        session.save();

        InitializationProcessorImpl processor = new InitializationProcessorImpl(null);
        Iterator<Node> downstreamItems = processor.resolveDownstreamItemsBasedOnContentRoot(session, "/", "foo").iterator();
        assertTrue(downstreamItems.hasNext());
        downstreamItems.next();
        assertFalse(downstreamItems.hasNext());

        item.setProperty(HIPPO_CONTENTROOT, "/foo");
        item.setProperty(HIPPO_CONTEXTNODENAME, "bar");
        session.save();

        downstreamItems = processor.resolveDownstreamItemsBasedOnContentRoot(session, "/", "foo").iterator();
        assertTrue(downstreamItems.hasNext());
        downstreamItems.next();
        assertFalse(downstreamItems.hasNext());

        item.setProperty(HIPPO_CONTENTROOT, "/");
        item.setProperty(HIPPO_CONTEXTNODENAME, "foobar");
        session.save();

        downstreamItems = processor.resolveDownstreamItemsBasedOnContentRoot(session, "/", "foo").iterator();
        assertFalse(downstreamItems.hasNext());
    }

    @Test
    public void testResolveDownstreamItemsBasedOnContextPaths() throws Exception {
        item.setProperty(HIPPO_CONTENTROOT, "/");
        item.setProperty(HIPPO_CONTEXTPATHS, new String[] { "/foo", "/foo/bar" } );
        session.save();

        InitializationProcessorImpl processor = new InitializationProcessorImpl(null);
        Iterator<Node> downstreamItems = processor.resolveDownstreamItemsBasedOnContextPaths(session, "/", "foo").iterator();
        assertTrue(downstreamItems.hasNext());
        downstreamItems.next();
        assertFalse(downstreamItems.hasNext());

        downstreamItems = processor.resolveDownstreamItemsBasedOnContextPaths(session, "/foo", "bar").iterator();
        assertTrue(downstreamItems.hasNext());
        downstreamItems.next();
        assertFalse(downstreamItems.hasNext());

        item.setProperty(HIPPO_CONTEXTPATHS, new String[] { "/foobar" } );
        session.save();

        downstreamItems = processor.resolveDownstreamItemsBasedOnContextPaths(session, "/", "foo").iterator();
        assertFalse(downstreamItems.hasNext());
    }

    @Test
    public void testReadContentFileInfo() throws Exception {
        item.setProperty(HIPPO_CONTENTRESOURCE, getClass().getResource("/bootstrap/foo.xml").toString());
        session.save();

        InitializationProcessorImpl processor = new InitializationProcessorImpl(null);
        ContentFileInfo contentFileInfo = processor.readContentFileInfo(item);

        assertEquals("foo", contentFileInfo.contextNodeName);
        assertNull(contentFileInfo.deltaDirective);

        item.setProperty(HIPPO_CONTENTRESOURCE, getClass().getResource("/bootstrap/delta.xml").toString());
        session.save();

        contentFileInfo = processor.readContentFileInfo(item);

        assertEquals("foo", contentFileInfo.contextNodeName);
        assertEquals("combine", contentFileInfo.deltaDirective);
    }

    /*
     * REPO-969: It works fine when the file: URL is on non-Windows system,
     *           but it throws "IllegalArgumentException: URI is not hierarchical"
     *           if the file: URL denotes a Windows URL like 'file:C:/a/b/c/...'.
     */
    @Test
    public void testGetBaseZipFileFromURL() throws Exception {
        URL url = new URL("file:/a/b/c.jar!/d/e/f.xml");
        assertEquals("/a/b/c.jar!/d/e/f.xml", url.getFile());
        File baseFile = new InitializationProcessorImpl().getBaseZipFileFromURL(url);
        assertTrue(baseFile.getPath().endsWith("c.jar"));

        url = new URL("file:C:/a/b/c.jar!/d/e/f.xml");
        assertEquals("C:/a/b/c.jar!/d/e/f.xml", url.getFile());
        baseFile = new InitializationProcessorImpl().getBaseZipFileFromURL(url);
        assertTrue(baseFile.getPath().endsWith("c.jar"));
    }

}
