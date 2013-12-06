/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.dashboard;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EventLabelTest extends PluginTest {

    static class TestLabel extends Label {
        private static final long serialVersionUID = 1L;

        public TestLabel(IModel model) {
            super("label", model);
        }

        public String getModelObject() {
            return getDefaultModelObjectAsString();
        }
        
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        root.addNode("test", "nt:unstructured");
    }

    private Node createEventNode(Long timestamp, String method, String user) throws RepositoryException {
        return createEventNode(timestamp, method, user, null);
    }

    private Node createEventNode(Long timestamp, String method, String user, String[] arguments) throws RepositoryException {
        Node node = root.getNode("test").addNode(timestamp.toString(), "hippolog:item");
        node.setProperty("hippolog:timestamp", timestamp);
        node.setProperty("hippolog:className", EventLabelTest.class.getName());
        node.setProperty("hippolog:methodName", method);
        node.setProperty("hippolog:user", user);
        if (arguments != null) {
            node.setProperty("hippolog:arguments", arguments);
        }
        return node;
    }

    @Test
    public void testEventWithoutDocument() throws Exception {
        Long timestamp = Calendar.getInstance().getTimeInMillis();
        Node eventNode = createEventNode(timestamp, "testMethod", "testUser");

        DocumentEvent parser = new DocumentEvent(eventNode);
        assertNull(parser.getDocumentPath());

        EventModel label = new EventModel(new JcrNodeModel(eventNode));
        TestLabel testLabel = new TestLabel(label);
        assertEquals("One minute ago, testUser called test method", testLabel.getModelObject());
    }

    @Test
    public void testEventWithSource() throws Exception {
        Node docNode = root.getNode("test").addNode("testDocument");
        Long timestamp = Calendar.getInstance().getTimeInMillis();
        Node eventNode = createEventNode(timestamp, "testDocumentMethod", "testUser");
        eventNode.setProperty("hippolog:documentPath", docNode.getPath());

        DocumentEvent parser = new DocumentEvent(eventNode);
        assertEquals("/test/testDocument", parser.getDocumentPath());

        EventModel label = new EventModel(new JcrNodeModel(eventNode), parser.getName(), null);
        TestLabel testLabel = new TestLabel(label);
        assertEquals("One minute ago, testUser called test method on testDocument", testLabel.getModelObject());
    }

    @Test
    public void testEventWithTarget() throws Exception {
        Node docNode = root.getNode("test").addNode("testDocument");
        docNode.addMixin("mix:referenceable");
        session.save();

        Long timestamp = Calendar.getInstance().getTimeInMillis();
        Node eventNode = createEventNode(timestamp, "testDocumentMethod", "testUser");
        eventNode.setProperty("hippolog:returnValue", "document[uuid=" + docNode.getUUID() + ",path='"
                + docNode.getPath() + "']");

        DocumentEvent parser = new DocumentEvent(eventNode);
        assertEquals("/test/testDocument", parser.getDocumentPath());

        EventModel label = new EventModel(new JcrNodeModel(eventNode), parser.getName(), null);
        TestLabel testLabel = new TestLabel(label);
        assertEquals("One minute ago, testUser called test method on testDocument", testLabel.getModelObject());
    }

    @Test
    public void testEventWithVersion() throws Exception {
        Node docNode = root.getNode("test").addNode("testDocument");
        docNode.addMixin("mix:versionable");
        session.save();

        Version version = docNode.checkin();

        Long timestamp = Calendar.getInstance().getTimeInMillis();
        Node eventNode = createEventNode(timestamp, "testDocumentMethod", "testUser");
        eventNode.setProperty("hippolog:returnValue", "document[uuid=" + version.getUUID() + ",path='"
                + version.getPath() + "']");

        DocumentEvent parser = new DocumentEvent(eventNode);
        assertEquals("/test/testDocument", parser.getDocumentPath());

        EventModel label = new EventModel(new JcrNodeModel(eventNode), parser.getName(), null);
        TestLabel testLabel = new TestLabel(label);
        assertEquals("One minute ago, testUser called test method on testDocument", testLabel.getModelObject());
    }

    @Test
    public void testWorkflowWithRemovedTarget() throws Exception {
        Node handleNode = root.getNode("test").addNode("testDocument", HippoNodeType.NT_HANDLE);
        handleNode.addMixin("mix:versionable");
        Node docNode = handleNode.addNode("testDocument", HippoNodeType.NT_DOCUMENT);
        docNode.addMixin("mix:versionable");
        session.save();

        Long timestamp = Calendar.getInstance().getTimeInMillis();
        Node eventNode = createEventNode(timestamp, "testDocumentMethod", "testUser");
        eventNode.setProperty("hippolog:returnValue", "document[uuid=" + docNode.getUUID() + ",path='"
                + docNode.getPath() + "']");
        docNode.remove();
        session.save();

        DocumentEvent parser = new DocumentEvent(eventNode);
        String path = parser.getDocumentPath();
        assertEquals("/test/testDocument", path);

        EventModel label = new EventModel(new JcrNodeModel(eventNode), parser.getName(), null);
        TestLabel testLabel = new TestLabel(label);
        assertEquals("One minute ago, testUser called test method on testDocument", testLabel.getModelObject());
    }

    @Test
    public void testDeleteDocumentEvent() throws Exception {
        Node docNode = root.getNode("test").addNode("testDocument");
        Long timestamp = Calendar.getInstance().getTimeInMillis();
        Node eventNode = createEventNode(timestamp, "delete", "testUser");
        eventNode.setProperty("hippolog:documentPath", docNode.getPath());

        DocumentEvent parser = new DocumentEvent(eventNode);
        IModel<String> nameModel = parser.getName();
        assertEquals("testDocument", nameModel.getObject());
    }

    @Test
    public void testDeleteFolderEvent() throws Exception {
        Node docNode = root.getNode("test").addNode("testDocument");
        Long timestamp = Calendar.getInstance().getTimeInMillis();
        Node eventNode = createEventNode(timestamp, "delete", "testUser", new String[] { "child" });
        eventNode.setProperty("hippolog:documentPath", docNode.getPath());

        DocumentEvent parser = new DocumentEvent(eventNode);
        IModel<String> nameModel = parser.getName();
        assertEquals("child", nameModel.getObject());
    }

}
