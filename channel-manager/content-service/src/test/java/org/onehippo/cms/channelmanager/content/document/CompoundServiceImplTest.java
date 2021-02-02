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
import org.onehippo.cms.channelmanager.content.documenttype.field.type.StringFieldType;
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
public class CompoundServiceImplTest {

    private Session session;
    private CompoundServiceImpl compoundService;

    @Before
    public void setup() {
        final UserContext userContext = new TestUserContext();
        session = userContext.getSession();
        compoundService = new CompoundServiceImpl(session);

        PowerMock.mockStatic(JcrUtils.class);
    }

    @Test(expected = NotFoundException.class)
    public void addCompoundFieldShouldThrowIfFieldTypesIsEmpty() {
        compoundService.addCompoundField("/document", new FieldPath("field"), emptyList(), "type");
    }

    @Test
    public void addCompoundFieldShouldThrowIfFieldIsNotFound() {
        final FieldType fieldType = new StringFieldType();
        fieldType.setId("field-b");

        try {
            compoundService.addCompoundField("/document", new FieldPath("field-a"), singletonList(fieldType), "type");
            fail();
        } catch (final NotFoundException e) {
            assertErrorStatusAndReason(e, Status.NOT_FOUND, Reason.DOES_NOT_EXIST, "fieldType", "field-a");
        }
    }

    @Test
    public void addCompoundFieldShouldThrowIfNestedFieldIsNotContainedByCompoundField() {
        final FieldType fieldType = new StringFieldType();
        fieldType.setId("parent");

        try {
            compoundService.addCompoundField("/document", new FieldPath("parent/child"), singletonList(fieldType), "type");
            fail();
        } catch (final NotFoundException e) {
            assertErrorStatusAndReason(e, Status.NOT_FOUND, Reason.DOES_NOT_EXIST, "fieldType", "parent/child");
        }
    }

    @Test
    public void addCompoundFieldShouldThrowIfNestedFieldIsNotFound() {
        final StringFieldType nestedFieldType = new StringFieldType();
        nestedFieldType.setId("child-a");

        final CompoundFieldType compoundFieldType = new CompoundFieldType();
        compoundFieldType.setId("parent");
        compoundFieldType.getFields().add(nestedFieldType);

        try {
            compoundService.addCompoundField("/document", new FieldPath("parent/child-b"), singletonList(compoundFieldType), "type");
            fail();
        } catch (final NotFoundException e) {
            assertErrorStatusAndReason(e, Status.NOT_FOUND, Reason.DOES_NOT_EXIST, "fieldType", "parent/child-b");
        }
    }

    @Test
    public void addCompoundFieldShouldThrowIfFieldIsNotMultiple() {
        final FieldType fieldType = createMock(FieldType.class);
        expect(fieldType.getId()).andReturn("field-a");
        expect(fieldType.isMultiple()).andReturn(false);

        replayAll();

        try {
            compoundService.addCompoundField("/document", new FieldPath("field-a"), singletonList(fieldType), "type");
            fail();
        } catch (final InternalServerErrorException e) {
            assertErrorStatusAndReason(e, Status.INTERNAL_SERVER_ERROR, Reason.SERVER_ERROR, "fieldType", "not-multiple");
        }
        verifyAll();
    }

    @Test
    public void addCompoundFieldShouldThrowIfFieldIsNotCompoundOrChoice() {
        final FieldType fieldType = createMock(FieldType.class);
        expect(fieldType.getId()).andReturn("field-a");
        expect(fieldType.getType()).andReturn(BOOLEAN).anyTimes();
        expect(fieldType.isMultiple()).andReturn(true);

        replayAll();

        try {
            compoundService.addCompoundField("/document", new FieldPath("field-a"), singletonList(fieldType), "type");
            fail();
        } catch (final InternalServerErrorException e) {
            assertErrorStatusAndReason(e, Status.INTERNAL_SERVER_ERROR, Reason.SERVER_ERROR, "fieldType", "not-compound-or-choice");
        }
        verifyAll();
    }

    @Test
    public void addCompoundFieldShouldThrowIfPrototypeNamespaceIsNotFound() throws Exception {
        final FieldType fieldType = createMock(FieldType.class);
        expect(fieldType.getId()).andReturn("field-a");
        expect(fieldType.getJcrType()).andReturn("ns:type");
        expect(fieldType.getType()).andReturn(COMPOUND).anyTimes();
        expect(fieldType.isMultiple()).andReturn(true);

        final Workspace workspace = createMock(Workspace.class);
        expect(session.getWorkspace()).andReturn(workspace);
        final NamespaceRegistry nsRegistry = createMock(NamespaceRegistry.class);
        expect(workspace.getNamespaceRegistry()).andReturn(nsRegistry);
        expect(nsRegistry.getURI("ns")).andThrow(new NamespaceException());

        replayAll();

        try {
            compoundService.addCompoundField("/document", new FieldPath("field-a"), singletonList(fieldType), "ns:type");
            fail();
        } catch (final NotFoundException e) {
            assertErrorStatusAndReason(e, Status.NOT_FOUND, Reason.DOES_NOT_EXIST, "prototype", "ns:type");
        }
        verifyAll();
    }

    @Test
    public void addCompoundFieldShouldThrowIfPrototypeIsNotFound() throws Exception {
        final FieldType fieldType = createMock(FieldType.class);
        expect(fieldType.getId()).andReturn("field-a");
        expect(fieldType.getJcrType()).andReturn("ns:type").anyTimes();
        expect(fieldType.isMultiple()).andReturn(true);
        expect(fieldType.getType()).andReturn(COMPOUND).anyTimes();

        final Workspace workspace = createMock(Workspace.class);
        expect(session.getWorkspace()).andReturn(workspace);
        final NamespaceRegistry nsRegistry = createMock(NamespaceRegistry.class);
        expect(workspace.getNamespaceRegistry()).andReturn(nsRegistry);
        expect(nsRegistry.getURI("ns")).andReturn("http://www.onehippo.org/test/nt/1.0");
        expect(session.itemExists("/hippo:namespaces/ns/type/hipposysedit:prototypes")).andReturn(false);

        replayAll();

        try {
            compoundService.addCompoundField("/document", new FieldPath("field-a"), singletonList(fieldType), "ns:type");
            fail();
        } catch (final NotFoundException e) {
            assertErrorStatusAndReason(e, Status.NOT_FOUND, Reason.DOES_NOT_EXIST, "prototype", "ns:type");
        }
        verifyAll();
    }

    @Test
    public void addCompoundFieldShouldCopyPrototypeToDocument() throws Exception {
        final FieldType fieldType = createMock(FieldType.class);
        expect(fieldType.getId()).andReturn("field-a");
        expect(fieldType.getJcrType()).andReturn("ns:type").anyTimes();
        expect(fieldType.isMultiple()).andReturn(true);
        expect(fieldType.getType()).andReturn(COMPOUND).anyTimes();

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
        final Node compoundNode = createMock(Node.class);
        expect(compoundNode.getName()).andReturn("compound");
        expect(compoundNode.getIndex()).andReturn(1).anyTimes();
        expect(compoundNode.getParent()).andReturn(documentNode);
        expect(JcrUtils.copy(session, "/prototype", "/document/field-a")).andReturn(compoundNode);

        documentNode.orderBefore(eq("compound[1]"), eq("field-a"));
        expectLastCall();

        session.save();
        expectLastCall();

        replayAll();

        compoundService.addCompoundField("/document", new FieldPath("field-a"), singletonList(fieldType), "ns:type");
        verifyAll();
    }

    @Test
    public void addCompoundFieldShouldOrderPrototype() throws Exception {
        final FieldType fieldType = createMock(FieldType.class);
        expect(fieldType.getId()).andReturn("field-a");
        expect(fieldType.getJcrType()).andReturn("ns:type").anyTimes();
        expect(fieldType.isMultiple()).andReturn(true);
        expect(fieldType.getType()).andReturn(COMPOUND).anyTimes();

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
        final Node compoundNode = createMock(Node.class);
        expect(compoundNode.getName()).andReturn("compound");
        expect(compoundNode.getIndex()).andReturn(1).anyTimes();
        expect(compoundNode.getParent()).andReturn(documentNode);
        expect(JcrUtils.copy(session, "/prototype", "/document/field-a")).andReturn(compoundNode);

        documentNode.orderBefore(eq("compound[1]"), eq("field-a[2]"));
        expectLastCall();

        session.save();
        expectLastCall();

        replayAll();

        compoundService.addCompoundField("/document", new FieldPath("field-a[2]"), singletonList(fieldType), "ns:type");
        verifyAll();
    }

    @Test
    public void reorderCompoundFieldShouldThrowIfNewPositionIsOutOfBounds() throws Exception {
        final MockNode root = MockNode.root();
        final MockNode documents = root.addNode("documents", "nt:unstructured");
        final MockNode document = documents.addNode("document", "nt:unstructured");
        final MockNode field = document.addNode("field", "nt:unstructured");
        expect(session.getNode("/documents/document/field")).andReturn(field);

        replayAll();

        try {
            compoundService.reorderCompoundField("/documents", new FieldPath("document/field"), 2);
            fail();
        } catch (final InternalServerErrorException e) {
            assertErrorStatusAndReason(e, Status.INTERNAL_SERVER_ERROR, Reason.SERVER_ERROR, "order", "out-of-bounds");
        }

        verifyAll();
    }

    @Test
    public void reorderCompoundFieldShouldMoveUp() throws Exception {
        final Node document = createMock(Node.class);
        final Node field = createMock(Node.class);

        expect(session.getNode("/documents/document/field[2]")).andReturn(field);
        expect(field.getParent()).andReturn(document);
        expect(field.getIndex()).andReturn(2);

        final NodeIterator it = createMock(NodeIterator.class);
        expect(document.getNodes("field")).andReturn(it);
        expect(it.getSize()).andReturn(2L);

        document.orderBefore(eq("field[2]"), eq("field[1]"));
        expectLastCall();

        session.save();
        expectLastCall();

        replayAll();

        compoundService.reorderCompoundField("/documents", new FieldPath("document/field[2]"), 1);

        verifyAll();
    }

    @Test
    public void reorderCompoundFieldShouldMoveDown() throws Exception {
        final Node document = createMock(Node.class);
        final Node field = createMock(Node.class);

        expect(session.getNode("/documents/document/field[1]")).andReturn(field);
        expect(field.getParent()).andReturn(document);
        expect(field.getIndex()).andReturn(1);

        final NodeIterator it = createMock(NodeIterator.class);
        expect(document.getNodes("field")).andReturn(it);
        expect(it.getSize()).andReturn(3L);

        document.orderBefore(eq("field[1]"), eq("field[3]"));
        expectLastCall();

        session.save();
        expectLastCall();

        replayAll();

        compoundService.reorderCompoundField("/documents", new FieldPath("document/field[1]"), 2);

        verifyAll();
    }

    @Test
    public void reorderCompoundFieldShouldMoveToLastPosition() throws Exception {
        final Node document = createMock(Node.class);
        final Node field = createMock(Node.class);

        expect(session.getNode("/documents/document/field[1]")).andReturn(field);
        expect(field.getParent()).andReturn(document);

        final NodeIterator it = createMock(NodeIterator.class);
        expect(document.getNodes("field")).andReturn(it);
        expect(it.getSize()).andReturn(2L);

        document.orderBefore(eq("field[1]"), eq(null));
        expectLastCall();

        session.save();
        expectLastCall();

        replayAll();

        compoundService.reorderCompoundField("/documents", new FieldPath("document/field[1]"), 2);

        verifyAll();
    }

    @Test
    public void removeCompoundFieldShouldThrowIfFieldTypeDoesNotExist() {
        replayAll();

        try {
            compoundService.removeCompoundField("/document", new FieldPath("field"), emptyList());
        } catch (final NotFoundException e) {
            assertErrorStatusAndReason(e, Status.NOT_FOUND, Reason.DOES_NOT_EXIST, "fieldType", "field");
        }
        verifyAll();
    }

    @Test
    public void removeCompoundFieldShouldThrowIfFieldIsNotMultiple() {
        final FieldType fieldType = createMock(FieldType.class);
        expect(fieldType.getId()).andReturn("field-a");
        expect(fieldType.isMultiple()).andReturn(false);

        replayAll();

        try {
            compoundService.removeCompoundField("/document", new FieldPath("field-a"), singletonList(fieldType));
            fail();
        } catch (final InternalServerErrorException e) {
            assertErrorStatusAndReason(e, Status.INTERNAL_SERVER_ERROR, Reason.SERVER_ERROR, "fieldType", "not-multiple");
        }
        verifyAll();
    }

    @Test
    public void removeCompoundFieldShouldThrowIfFieldIsNotCompoundOrChoice() {
        final FieldType fieldType = createMock(FieldType.class);
        expect(fieldType.getId()).andReturn("field-a");
        expect(fieldType.getType()).andReturn(BOOLEAN).anyTimes();
        expect(fieldType.isMultiple()).andReturn(true);

        replayAll();

        try {
            compoundService.removeCompoundField("/document", new FieldPath("field-a"), singletonList(fieldType));
            fail();
        } catch (final InternalServerErrorException e) {
            assertErrorStatusAndReason(e, Status.INTERNAL_SERVER_ERROR, Reason.SERVER_ERROR, "fieldType", "not-compound-or-choice");
        }
        verifyAll();
    }

    @Test
    public void removeCompoundFieldShouldThrowIfFieldNodeDoesNotExist() throws Exception {
        final Node compoundNode = createMock(Node.class);
        final Node parentNode = createMock(Node.class);
        expect(compoundNode.getParent()).andReturn(parentNode);
        expect(session.getNode("/document/field")).andReturn(compoundNode);
        final NodeIterator it = createMock( NodeIterator.class);
        expect(it.getSize()).andReturn(1L);
        expect(parentNode.getNodes("field")).andReturn(it);

        session.removeItem("/document/field");
        expectLastCall().andThrow(new PathNotFoundException());

        final FieldType fieldType = createMock(FieldType.class);
        expect(fieldType.getId()).andReturn("field");
        expect(fieldType.getJcrType()).andReturn("ns:type").anyTimes();
        expect(fieldType.getType()).andReturn(COMPOUND).anyTimes();
        expect(fieldType.isMultiple()).andReturn(true);
        expect(fieldType.getMinValues()).andReturn(0);

        replayAll();

        try {
            compoundService.removeCompoundField("/document", new FieldPath("field"), singletonList(fieldType));
        } catch (final NotFoundException e) {
            assertErrorStatusAndReason(e, Status.NOT_FOUND, Reason.DOES_NOT_EXIST, "compound", "field");
        }
        verifyAll();
    }

    @Test
    public void removeCompoundFieldShouldThrowIfMinValuesIsNotRespected() throws Exception {
        final Node compoundNode = createMock(Node.class);
        final Node parentNode = createMock(Node.class);
        expect(compoundNode.getParent()).andReturn(parentNode);
        expect(session.getNode("/document/field")).andReturn(compoundNode);
        final NodeIterator it = createMock( NodeIterator.class);
        expect(it.getSize()).andReturn(1L);
        expect(parentNode.getNodes("field")).andReturn(it);

        final FieldType fieldType = createMock(FieldType.class);
        expect(fieldType.getId()).andReturn("field");
        expect(fieldType.getJcrType()).andReturn("ns:type").anyTimes();
        expect(fieldType.getType()).andReturn(COMPOUND).anyTimes();
        expect(fieldType.isMultiple()).andReturn(true);
        expect(fieldType.getMinValues()).andReturn(1).anyTimes();

        replayAll();

        try {
            compoundService.removeCompoundField("/document", new FieldPath("field"), singletonList(fieldType));
        } catch (final InternalServerErrorException e) {
            assertErrorStatusAndReason(e, Status.INTERNAL_SERVER_ERROR, Reason.SERVER_ERROR, "cardinality", "min-values");
        }
        verifyAll();
    }

    @Test
    public void removeCompoundFieldShouldDeleteFieldNode() throws Exception {
        final Node compoundNode = createMock(Node.class);
        final Node parentNode = createMock(Node.class);
        expect(compoundNode.getParent()).andReturn(parentNode);
        expect(session.getNode("/document/field")).andReturn(compoundNode);
        final NodeIterator it = createMock( NodeIterator.class);
        expect(it.getSize()).andReturn(1L);
        expect(parentNode.getNodes("field")).andReturn(it);

        final FieldType fieldType = createMock(FieldType.class);
        expect(fieldType.getId()).andReturn("field");
        expect(fieldType.getJcrType()).andReturn("ns:type").anyTimes();
        expect(fieldType.getType()).andReturn(COMPOUND).anyTimes();
        expect(fieldType.isMultiple()).andReturn(true);
        expect(fieldType.getMinValues()).andReturn(0).anyTimes();

        session.removeItem("/document/field");
        expectLastCall();
        session.save();
        expectLastCall();

        replayAll();

        compoundService.removeCompoundField("/document", new FieldPath("field"), singletonList(fieldType));

        verifyAll();
    }
}
