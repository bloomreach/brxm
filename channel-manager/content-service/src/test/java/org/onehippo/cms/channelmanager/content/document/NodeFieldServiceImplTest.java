/*
 * Copyright 2021 Bloomreach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms.channelmanager.content.document;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.ws.rs.core.Response.Status;

import org.apache.jackrabbit.JcrConstants;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.TestUserContext;
import org.onehippo.cms.channelmanager.content.UserContext;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.CompoundFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo.Reason;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.cms.channelmanager.content.error.NotFoundException;
import org.onehippo.repository.mock.MockNode;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.fail;
import static org.onehippo.cms.channelmanager.content.asserts.Errors.assertErrorStatusAndReason;
import static org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType.Type.BOOLEAN;
import static org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType.Type.COMPOUND;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({
        "com.sun.org.apache.xerces.*",
        "org.apache.logging.log4j.*",
})
@PrepareForTest({
        JcrUtils.class,
})
public class NodeFieldServiceImplTest {

    private Session session;
    private NodeFieldServiceImpl nodeFieldService;

    @Before
    public void setup() {
        final UserContext userContext = new TestUserContext();
        session = userContext.getSession();
        nodeFieldService = new NodeFieldServiceImpl(session);

        PowerMock.mockStatic(JcrUtils.class);
    }

    @Test
    public void addNodeFieldShouldThrowIfFieldTypesIsEmpty() {
        try {
            nodeFieldService.addNodeField("/document", new FieldPath("non-existing-field"), emptyList(), "ns:type");
            fail();
        } catch (final NotFoundException e) {
            assertErrorStatusAndReason(e, Status.NOT_FOUND, Reason.DOES_NOT_EXIST, "fieldType", "non-existing-field");
        }
    }

    @Test
    public void addNodeFieldShouldThrowIfFieldIsNotFound() {
        final FieldType fieldType = mockFieldType("field", "ns:type", 0, 1, true, COMPOUND);
        replayAll();

        try {
            nodeFieldService.addNodeField("/document", new FieldPath("non-existing-field"), singletonList(fieldType), "ns:type");
            fail();
        } catch (final NotFoundException e) {
            assertErrorStatusAndReason(e, Status.NOT_FOUND, Reason.DOES_NOT_EXIST, "fieldType", "non-existing-field");
        }
        verifyAll();
    }

    @Test
    public void addNodeFieldShouldThrowIfNestedFieldIsNotContainedByCompoundField() {
        final FieldType fieldType = mockFieldType("parent", "ns:type", 0, 1, true, BOOLEAN);
        replayAll();

        try {
            nodeFieldService.addNodeField("/document", new FieldPath("parent/field"), singletonList(fieldType), "ns:type");
            fail();
        } catch (final NotFoundException e) {
            assertErrorStatusAndReason(e, Status.NOT_FOUND, Reason.DOES_NOT_EXIST, "fieldType", "parent/field");
        }
        verifyAll();
    }

    @Test
    public void addNodeFieldShouldThrowIfNestedFieldIsNotFound() {
        final FieldType fieldType = mockFieldType("field-a", "ns:type", 0, 1, true, COMPOUND);
        final CompoundFieldType parentFieldType = new CompoundFieldType();
        parentFieldType.setId("parent");
        parentFieldType.getFields().add(fieldType);
        replayAll();

        try {
            nodeFieldService.addNodeField("/document", new FieldPath("parent/field-b"), singletonList(parentFieldType), "ns:type");
            fail();
        } catch (final NotFoundException e) {
            assertErrorStatusAndReason(e, Status.NOT_FOUND, Reason.DOES_NOT_EXIST, "fieldType", "parent/field-b");
        }
        verifyAll();
    }

    @Test
    public void addNodeFieldShouldThrowIfFieldIsNotMultiple() {
        final FieldType fieldType = mockFieldType("field", "ns:type", 0, 1, false, COMPOUND);
        replayAll();

        try {
            nodeFieldService.addNodeField("/document", new FieldPath("field"), singletonList(fieldType), "ns:type");
            fail();
        } catch (final InternalServerErrorException e) {
            assertErrorStatusAndReason(e, Status.INTERNAL_SERVER_ERROR, Reason.SERVER_ERROR, "fieldType", "not-multiple");
        }
        verifyAll();
    }

    @Test
    public void addNodeFieldShouldThrowIfMaxValuesIsNotRespected() throws Exception {
        mockDocument("document", "field", 1);
        final FieldType fieldType = mockFieldType("field", "ns:type", 0, 1, true, COMPOUND);
        replayAll();

        try {
            nodeFieldService.addNodeField("/document", new FieldPath("field"), singletonList(fieldType), "ns:type");
        } catch (final InternalServerErrorException e) {
            assertErrorStatusAndReason(e, Status.INTERNAL_SERVER_ERROR, Reason.SERVER_ERROR, "cardinality", "max-values");
        }
        verifyAll();
    }

    @Test
    public void addNodeFieldShouldThrowIfPrototypeNamespaceIsNotFound() throws Exception {
        mockDocument("document", "field", 0);
        final FieldType fieldType = mockFieldType("field", "ns:type", 0, 1, true, COMPOUND);
        final Workspace workspace = createMock(Workspace.class);
        expect(session.getWorkspace()).andReturn(workspace);
        final NamespaceRegistry nsRegistry = createMock(NamespaceRegistry.class);
        expect(workspace.getNamespaceRegistry()).andReturn(nsRegistry);
        expect(nsRegistry.getURI("ns")).andThrow(new NamespaceException());
        replayAll();

        try {
            nodeFieldService.addNodeField("/document", new FieldPath("field"), singletonList(fieldType), "ns:type");
            fail();
        } catch (final NotFoundException e) {
            assertErrorStatusAndReason(e, Status.NOT_FOUND, Reason.DOES_NOT_EXIST, "prototype", "ns:type");
        }
        verifyAll();
    }

    @Test
    public void addNodeFieldShouldThrowIfPrototypeIsNotFound() throws Exception {
        mockDocument("document", "field", 0);
        final FieldType fieldType = mockFieldType("field", "ns:type", 0, 1, true, COMPOUND);
        final Workspace workspace = createMock(Workspace.class);
        expect(session.getWorkspace()).andReturn(workspace);
        final NamespaceRegistry nsRegistry = createMock(NamespaceRegistry.class);
        expect(workspace.getNamespaceRegistry()).andReturn(nsRegistry);
        expect(nsRegistry.getURI("ns")).andReturn("http://www.onehippo.org/test/nt/1.0");
        expect(session.itemExists("/hippo:namespaces/ns/type/hipposysedit:prototypes")).andReturn(false);
        replayAll();

        try {
            nodeFieldService.addNodeField("/document", new FieldPath("field"), singletonList(fieldType), "ns:type");
            fail();
        } catch (final NotFoundException e) {
            assertErrorStatusAndReason(e, Status.NOT_FOUND, Reason.DOES_NOT_EXIST, "prototype", "ns:type");
        }
        verifyAll();
    }

    @Test
    public void addNodeFieldShouldCopyPrototypeToDocument() throws Exception {
        mockDocument("document", "field", 0);
        final FieldType fieldType = mockFieldType("field", "ns:type", 0, 1, true, COMPOUND);

        final Node prototypeNode = createMock(Node.class);
        expect(prototypeNode.getPath()).andReturn("/prototype");

        final Workspace workspace = createMock(Workspace.class);
        expect(session.getWorkspace()).andReturn(workspace);
        final NamespaceRegistry nsRegistry = createMock(NamespaceRegistry.class);
        expect(workspace.getNamespaceRegistry()).andReturn(nsRegistry);
        expect(nsRegistry.getURI("ns")).andReturn("http://www.onehippo.org/test/nt/1.0");
        expect(session.itemExists("/hippo:namespaces/ns/type/hipposysedit:prototypes")).andReturn(true);

        final Node prototypesNode = createMock(Node.class);
        expect(prototypesNode.isNode()).andReturn(true);
        expect(session.getItem("/hippo:namespaces/ns/type/hipposysedit:prototypes"))
                .andReturn(prototypesNode).anyTimes();

        final NodeIterator it = createMock(NodeIterator.class);
        expect(prototypesNode.getNodes("hipposysedit:prototype")).andReturn(it);
        expect(it.hasNext()).andReturn(true);
        expect(it.nextNode()).andReturn(prototypeNode);
        expect(prototypeNode.isNodeType(JcrConstants.NT_UNSTRUCTURED)).andReturn(false);

        final Node documentNode = createMock(Node.class);
        final Node fieldNode = mockField("field", documentNode);
        expect(JcrUtils.copy(session, "/prototype", "/document/field")).andReturn(fieldNode);

        documentNode.orderBefore(eq("field[1]"), eq("field"));
        expectLastCall();

        session.save();
        expectLastCall();
        replayAll();

        nodeFieldService.addNodeField("/document", new FieldPath("field"), singletonList(fieldType), "ns:type");
        verifyAll();
    }

    @Test
    public void addNodeFieldShouldOrderPrototype() throws Exception {
        final Node documentNode = mockDocument("document", "field", 0);
        final FieldType fieldType = mockFieldType("field", "ns:type", 0, 1, true, COMPOUND);

        final Node prototypeNode = createMock(Node.class);
        expect(prototypeNode.getPath()).andReturn("/prototype");

        final Workspace workspace = createMock(Workspace.class);
        expect(session.getWorkspace()).andReturn(workspace);
        final NamespaceRegistry nsRegistry = createMock(NamespaceRegistry.class);
        expect(workspace.getNamespaceRegistry()).andReturn(nsRegistry);
        expect(nsRegistry.getURI("ns")).andReturn("http://www.onehippo.org/test/nt/1.0");
        expect(session.itemExists("/hippo:namespaces/ns/type/hipposysedit:prototypes")).andReturn(true);

        final Node prototypesNode = createMock(Node.class);
        expect(prototypesNode.isNode()).andReturn(true);
        expect(session.getItem("/hippo:namespaces/ns/type/hipposysedit:prototypes"))
                .andReturn(prototypesNode).anyTimes();

        final Node fieldNode = mockField("field", documentNode);
        final NodeIterator it = createMock(NodeIterator.class);
        expect(prototypesNode.getNodes("hipposysedit:prototype")).andReturn(it);
        expect(it.hasNext()).andReturn(true);
        expect(it.nextNode()).andReturn(prototypeNode);
        expect(prototypeNode.isNodeType(JcrConstants.NT_UNSTRUCTURED)).andReturn(false);

        expect(JcrUtils.copy(session, "/prototype", "/document/field")).andReturn(fieldNode);

        documentNode.orderBefore(eq("field[1]"), eq("field[2]"));
        expectLastCall();

        session.save();
        expectLastCall();
        replayAll();

        nodeFieldService.addNodeField("/document", new FieldPath("field[2]"), singletonList(fieldType), "ns:type");
        verifyAll();
    }

    @Test
    public void reorderNodeFieldShouldThrowIfNewPositionIsOutOfBounds() throws Exception {
        final MockNode documentsNode = MockNode.root().addNode("documents", "nt:unstructured");
        final MockNode documentNode = documentsNode.addNode("document", "nt:unstructured");
        final MockNode fieldNode = documentNode.addNode("field", "nt:unstructured");
        expect(session.getNode("/documents/document/field")).andReturn(fieldNode);
        replayAll();

        try {
            nodeFieldService.reorderNodeField("/documents", new FieldPath("document/field"), 2);
            fail();
        } catch (final InternalServerErrorException e) {
            assertErrorStatusAndReason(e, Status.INTERNAL_SERVER_ERROR, Reason.SERVER_ERROR, "order", "out-of-bounds");
        }

        verifyAll();
    }

    @Test
    public void reorderNodeFieldShouldMoveUp() throws Exception {
        final Node documentNode = createMock(Node.class);
        final Node fieldNode = createMock(Node.class);

        expect(session.getNode("/documents/document/field[2]")).andReturn(fieldNode);
        expect(fieldNode.getParent()).andReturn(documentNode);
        expect(fieldNode.getIndex()).andReturn(2);

        final NodeIterator it = createMock(NodeIterator.class);
        expect(documentNode.getNodes("field")).andReturn(it);
        expect(it.getSize()).andReturn(2L);

        documentNode.orderBefore(eq("field[2]"), eq("field[1]"));
        expectLastCall();

        session.save();
        expectLastCall();
        replayAll();

        nodeFieldService.reorderNodeField("/documents", new FieldPath("document/field[2]"), 1);

        verifyAll();
    }

    @Test
    public void reorderNodeFieldShouldMoveDown() throws Exception {
        final Node documentNode = createMock(Node.class);
        final Node fieldNode = createMock(Node.class);

        expect(session.getNode("/documents/document/field[1]")).andReturn(fieldNode);
        expect(fieldNode.getParent()).andReturn(documentNode);
        expect(fieldNode.getIndex()).andReturn(1);

        final NodeIterator it = createMock(NodeIterator.class);
        expect(documentNode.getNodes("field")).andReturn(it);
        expect(it.getSize()).andReturn(3L);

        documentNode.orderBefore(eq("field[1]"), eq("field[3]"));
        expectLastCall();

        session.save();
        expectLastCall();
        replayAll();

        nodeFieldService.reorderNodeField("/documents", new FieldPath("document/field[1]"), 2);

        verifyAll();
    }

    @Test
    public void reorderNodeFieldShouldMoveToLastPosition() throws Exception {
        final Node documentNode = createMock(Node.class);
        final Node fieldNode = createMock(Node.class);

        expect(session.getNode("/documents/document/field[1]")).andReturn(fieldNode);
        expect(fieldNode.getParent()).andReturn(documentNode);

        final NodeIterator it = createMock(NodeIterator.class);
        expect(documentNode.getNodes("field")).andReturn(it);
        expect(it.getSize()).andReturn(2L);

        documentNode.orderBefore(eq("field[1]"), eq(null));
        expectLastCall();

        session.save();
        expectLastCall();
        replayAll();

        nodeFieldService.reorderNodeField("/documents", new FieldPath("document/field[1]"), 2);

        verifyAll();
    }

    @Test
    public void removeNodeFieldShouldThrowIfFieldTypeDoesNotExist() {
        replayAll();

        try {
            nodeFieldService.removeNodeField("/document", new FieldPath("non-existing-field"), emptyList());
        } catch (final NotFoundException e) {
            assertErrorStatusAndReason(e, Status.NOT_FOUND, Reason.DOES_NOT_EXIST, "fieldType", "non-existing-field");
        }
        verifyAll();
    }

    @Test
    public void removeNodeFieldShouldThrowIfFieldIsNotMultiple() {
        final FieldType fieldType = mockFieldType("field", "ns:type", 0, 1, false, COMPOUND);
        replayAll();

        try {
            nodeFieldService.removeNodeField("/document", new FieldPath("field"), singletonList(fieldType));
            fail();
        } catch (final InternalServerErrorException e) {
            assertErrorStatusAndReason(e, Status.INTERNAL_SERVER_ERROR, Reason.SERVER_ERROR, "fieldType", "not-multiple");
        }
        verifyAll();
    }

    @Test
    public void removeNodeFieldShouldThrowIfFieldNodeDoesNotExist() throws Exception {
        final FieldType fieldType = mockFieldType("field", "ns:type", 0, 1, true, COMPOUND);
        mockDocumentAndField("document", "field");

        session.removeItem("/document/field");
        expectLastCall().andThrow(new PathNotFoundException());
        replayAll();

        try {
            nodeFieldService.removeNodeField("/document", new FieldPath("field"), singletonList(fieldType));
        } catch (final NotFoundException e) {
            assertErrorStatusAndReason(e, Status.NOT_FOUND, Reason.DOES_NOT_EXIST, "field", "field");
        }
        verifyAll();
    }

    @Test
    public void removeNodeFieldShouldThrowIfMinValuesIsNotRespected() throws Exception {
        final FieldType fieldType = mockFieldType("field", "ns:type", 1, 1, true, COMPOUND);
        mockDocumentAndField("document", "field");
        replayAll();

        try {
            nodeFieldService.removeNodeField("/document", new FieldPath("field"), singletonList(fieldType));
        } catch (final InternalServerErrorException e) {
            assertErrorStatusAndReason(e, Status.INTERNAL_SERVER_ERROR, Reason.SERVER_ERROR, "cardinality", "min-values");
        }
        verifyAll();
    }

    @Test
    public void removeNodeFieldShouldDeleteFieldNode() throws Exception {
        final FieldType fieldType = mockFieldType("field", "ns:type", 0, 1, true, COMPOUND);
        mockDocumentAndField("document", "field");

        session.removeItem("/document/field");
        expectLastCall();
        session.save();
        expectLastCall();
        replayAll();

        nodeFieldService.removeNodeField("/document", new FieldPath("field"), singletonList(fieldType));

        verifyAll();
    }

    private Node mockDocument(final String documentName, final String fieldName, final long nrOfFields) throws Exception {
        final Node documentNode = createMock(Node.class);
        expect(session.getNode("/" + documentName)).andReturn(documentNode);

        final NodeIterator it = createMock(NodeIterator.class);
        expect(it.getSize()).andReturn(nrOfFields);
        expect(documentNode.getNodes(fieldName)).andReturn(it);
        return documentNode;
    }

    private void mockDocumentAndField(final String documentName, final String fieldName) throws Exception {
        final Node documentNode = createMock(Node.class);
        final NodeIterator it = createMock(NodeIterator.class);
        expect(it.getSize()).andReturn(1L);
        expect(documentNode.getNodes(fieldName)).andReturn(it);

        final Node fieldNode = createMock(Node.class);
        expect(fieldNode.getParent()).andReturn(documentNode);
        expect(session.getNode("/" + documentName + "/" + fieldName)).andReturn(fieldNode);
    }

    private static Node mockField(final String fieldName, final Node parent) throws Exception {
        final Node fieldNode = createMock(Node.class);
        expect(fieldNode.getName()).andReturn(fieldName);
        expect(fieldNode.getIndex()).andReturn(1).anyTimes();
        expect(fieldNode.getParent()).andReturn(parent);
        return fieldNode;
    }

    private static FieldType mockFieldType(final String id, final String jcrType, final int min, final int max,
                                           final boolean multiple, final FieldType.Type type) {
        final FieldType fieldType = createMock(FieldType.class);
        expect(fieldType.getId()).andReturn(id);
        expect(fieldType.getType()).andReturn(type).anyTimes();
        expect(fieldType.getJcrType()).andReturn(jcrType).anyTimes();
        expect(fieldType.getMinValues()).andReturn(min).anyTimes();
        expect(fieldType.getMaxValues()).andReturn(max).anyTimes();
        expect(fieldType.isMultiple()).andReturn(multiple).anyTimes();
        return fieldType;
    }

}
