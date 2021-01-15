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
import org.onehippo.cms.channelmanager.content.error.NotFoundException;
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
    public void expectToThrowIfFieldsIsEmpty() {
        compoundService.addCompoundField("/document", new FieldPath("field-a"), emptyList());
    }

    @Test
    public void expectToThrowIfFieldIsNotFound() {
        final FieldType fieldType = new StringFieldType();
        fieldType.setId("field-a");

        try {
            compoundService.addCompoundField("/document", new FieldPath("field-b"), singletonList(fieldType));
            fail();
        } catch (final NotFoundException e) {
            assertErrorStatusAndReason(e, Status.NOT_FOUND, Reason.DOES_NOT_EXIST, "fieldType", "field-b");
        }
    }

    @Test
    public void expectToThrowIfNestedFieldIsNotContainedByCompoundField() {
        final FieldType fieldType = new StringFieldType();
        fieldType.setId("parent");

        try {
            compoundService.addCompoundField("/document", new FieldPath("parent/child"), singletonList(fieldType));
            fail();
        } catch (final NotFoundException e) {
            assertErrorStatusAndReason(e, Status.NOT_FOUND, Reason.DOES_NOT_EXIST, "fieldType", "parent/child");
        }
    }

    @Test
    public void expectToThrowIfNestedFieldIsNotFound() {
        final StringFieldType nestedFieldType = new StringFieldType();
        nestedFieldType.setId("child-a");

        final CompoundFieldType compoundFieldType = new CompoundFieldType();
        compoundFieldType.setId("parent");
        compoundFieldType.getFields().add(nestedFieldType);

        try {
            compoundService.addCompoundField("/document", new FieldPath("parent/child-b"), singletonList(compoundFieldType));
            fail();
        } catch (final NotFoundException e) {
            assertErrorStatusAndReason(e, Status.NOT_FOUND, Reason.DOES_NOT_EXIST, "fieldType", "parent/child-b");
        }
    }

    @Test
    public void expectToThrowIfPrototypeNamespaceIsNotFound() throws Exception {
        final FieldType fieldType = createMock(FieldType.class);
        expect(fieldType.getId()).andReturn("field-a");
        expect(fieldType.getJcrType()).andReturn("test:string").anyTimes();

        final Workspace workspace = createMock(Workspace.class);
        expect(session.getWorkspace()).andReturn(workspace);
        final NamespaceRegistry nsRegistry = createMock(NamespaceRegistry.class);
        expect(workspace.getNamespaceRegistry()).andReturn(nsRegistry);
        expect(nsRegistry.getURI("test")).andThrow(new NamespaceException());

        replayAll();

        try {
            compoundService.addCompoundField("/document", new FieldPath("field-a"), singletonList(fieldType));
            fail();
        } catch (final NotFoundException e) {
            assertErrorStatusAndReason(e, Status.NOT_FOUND, Reason.DOES_NOT_EXIST, "prototype", "test:string");
        }
        verifyAll();
    }

    @Test
    public void expectToThrowIfPrototypeIsNotFound() throws Exception {
        final FieldType fieldType = createMock(FieldType.class);
        expect(fieldType.getId()).andReturn("field-a");
        expect(fieldType.getJcrType()).andReturn("test:string").anyTimes();

        final Workspace workspace = createMock(Workspace.class);
        expect(session.getWorkspace()).andReturn(workspace);
        final NamespaceRegistry nsRegistry = createMock(NamespaceRegistry.class);
        expect(workspace.getNamespaceRegistry()).andReturn(nsRegistry);
        expect(nsRegistry.getURI("test")).andReturn("http://www.onehippo.org/test/nt/1.0");
        expect(session.itemExists("/hippo:namespaces/test/string/hipposysedit:prototypes")).andReturn(false);

        replayAll();

        try {
            compoundService.addCompoundField("/document", new FieldPath("field-a"), singletonList(fieldType));
            fail();
        } catch (final NotFoundException e) {
            assertErrorStatusAndReason(e, Status.NOT_FOUND, Reason.DOES_NOT_EXIST, "prototype", "test:string");
        }
        verifyAll();
    }

    @Test
    public void expectToCopyPrototypeToDocument() throws Exception {
        final FieldType fieldType = createMock(FieldType.class);
        expect(fieldType.getId()).andReturn("field-a");
        expect(fieldType.getJcrType()).andReturn("test:string").anyTimes();

        final Node prototypeNode = createMock(Node.class);
        expect(prototypeNode.getPath()).andReturn("/prototype");

        final Workspace workspace = createMock(Workspace.class);
        expect(session.getWorkspace()).andReturn(workspace);
        final NamespaceRegistry nsRegistry = createMock(NamespaceRegistry.class);
        expect(workspace.getNamespaceRegistry()).andReturn(nsRegistry);
        expect(nsRegistry.getURI("test")).andReturn("http://www.onehippo.org/test/nt/1.0");
        expect(session.itemExists("/hippo:namespaces/test/string/hipposysedit:prototypes")).andReturn(true);

        final Node prototypesNode = createMock(Node.class);
        expect(prototypesNode.isNode()).andReturn(true);
        expect(session.getItem("/hippo:namespaces/test/string/hipposysedit:prototypes"))
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

        documentNode.orderBefore(eq("compound"), eq("field-a"));
        expectLastCall();

        session.save();
        expectLastCall();

        replayAll();

        compoundService.addCompoundField("/document", new FieldPath("field-a"), singletonList(fieldType));
        verifyAll();
    }

    @Test
    public void expectToOrderPrototype() throws Exception {
        final FieldType fieldType = createMock(FieldType.class);
        expect(fieldType.getId()).andReturn("field-a");
        expect(fieldType.getJcrType()).andReturn("test:string").anyTimes();

        final Node prototypeNode = createMock(Node.class);
        expect(prototypeNode.getPath()).andReturn("/prototype");

        final Workspace workspace = createMock(Workspace.class);
        expect(session.getWorkspace()).andReturn(workspace);
        final NamespaceRegistry nsRegistry = createMock(NamespaceRegistry.class);
        expect(workspace.getNamespaceRegistry()).andReturn(nsRegistry);
        expect(nsRegistry.getURI("test")).andReturn("http://www.onehippo.org/test/nt/1.0");
        expect(session.itemExists("/hippo:namespaces/test/string/hipposysedit:prototypes")).andReturn(true);

        final Node prototypesNode = createMock(Node.class);
        expect(prototypesNode.isNode()).andReturn(true);
        expect(session.getItem("/hippo:namespaces/test/string/hipposysedit:prototypes"))
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

        documentNode.orderBefore(eq("compound"), eq("field-a[2]"));
        expectLastCall();

        session.save();
        expectLastCall();

        replayAll();

        compoundService.addCompoundField("/document", new FieldPath("field-a[2]"), singletonList(fieldType));
        verifyAll();
    }
}
