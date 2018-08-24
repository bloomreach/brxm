/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
 *
 */

package org.onehippo.repository.documentworkflow;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.easymock.EasyMock;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.DocumentVariant;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.scxml.MockAccessManagedSession;
import org.onehippo.repository.scxml.MockWorkflowContext;
import org.onehippo.repository.scxml.SCXMLWorkflowContext;
import org.onehippo.repository.scxml.SCXMLWorkflowData;
import org.onehippo.repository.scxml.SCXMLWorkflowExecutor;

import static java.util.Collections.emptySet;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MIXIN_BRANCH_INFO;
import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;

@RunWith(Parameterized.class)
public class WorkflowExecutorTest extends BaseDocumentWorkflowTest {


    private static final MockNode ROOT = MockNode.root();
    private static final MockAccessManagedSession SESSION = new MockAccessManagedSession(ROOT);
    private static final MockNode HANDLE = ROOT;

    static {
        try {
            ROOT.addNode("document-1", NT_HANDLE);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        createDocumentWorkflowSCXMLRegistry();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        destroyDocumentWorkflowSCXMLRegistry();
    }


    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(
                editing_master(),
                editing_branch()
        );
    }

    private static Object[] editing_master() {

        try {

            final Map<String, DocumentVariant> variantsMap = new HashMap<>();
            final DocumentHandle data = EasyMock.createNiceMock(DocumentHandle.class);
            expect(data.getDocuments()).andStubReturn(variantsMap);
            expect(data.getHandle()).andStubReturn(HANDLE);

            expect(data.getBranches()).andStubReturn(emptySet());
            expect(data.getBranchId()).andStubReturn("master");
            replay(data);

            final MockNode draftVariant = HANDLE.addNode("document-1", "hippo:variant");
            final DocumentVariant draft = new DocumentVariant(draftVariant);
            draft.setState(HippoStdNodeType.DRAFT);
            draft.setHolder("testuser");
            SESSION.setPermissions(draftVariant.getPath(), "hippo:author", true);
            variantsMap.put(HippoStdNodeType.DRAFT, draft);


            final Map<String, ?> expectedActions = HintsBuilder.build()
                    .obtainEditableInstance(false)
                    .commitEditableInstance(true)
                    .disposeEditableInstance(true)
                    .actions();
            return new Object[]{data, expectedActions};

        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object[] editing_branch() {

        try {
            final String branchId = "foo";

            final Map<String, DocumentVariant> variantsMap = new HashMap<>();
            final DocumentHandle data = EasyMock.createNiceMock(DocumentHandle.class);
            expect(data.getDocuments()).andStubReturn(variantsMap);
            expect(data.getHandle()).andStubReturn(HANDLE);

            expect(data.getBranches()).andStubReturn(emptySet());
            expect(data.getBranchId()).andStubReturn(branchId);
            replay(data);

            final MockNode draftVariant = HANDLE.addNode("document-1", "hippo:variant");
            final DocumentVariant draft = new DocumentVariant(draftVariant);
            draftVariant.addMixin(HIPPO_MIXIN_BRANCH_INFO);
            draftVariant.setProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_ID, branchId);

            draft.setState(HippoStdNodeType.DRAFT);
            draft.setHolder("testuser");
            SESSION.setPermissions(draftVariant.getPath(), "hippo:author", true);
            variantsMap.put(HippoStdNodeType.DRAFT, draft);


            final Map<String, ?> expectedActions = HintsBuilder.build()
                    .obtainEditableInstance(false)
                    .commitEditableInstance(true)
                    .disposeEditableInstance(true)
                    .actions();
            return new Object[]{data, expectedActions};
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    private final DocumentHandle workflowData;
    private final Map<String, ?> expectedActions;
    private SCXMLWorkflowExecutor<SCXMLWorkflowContext, SCXMLWorkflowData> scxmlWorkflowExecutor;

    public WorkflowExecutorTest(final DocumentHandle workflowData, final Map<String, ?> expectedActions) {
        this.workflowData = workflowData;
        this.expectedActions = expectedActions;
    }

    @Before
    public void setUp() throws RepositoryException, WorkflowException {
        final MockWorkflowContext testuserContext = new MockWorkflowContext("testuser", SESSION);
        final SCXMLWorkflowContext context = new SCXMLWorkflowContext("documentworkflow", testuserContext);
        scxmlWorkflowExecutor = new SCXMLWorkflowExecutor<SCXMLWorkflowContext, SCXMLWorkflowData>(context, workflowData);
    }

    @Test
    public void testExpectedActions() throws WorkflowException {
        scxmlWorkflowExecutor.start(workflowData.getBranchId());
        assertMatchingKeyValues(scxmlWorkflowExecutor.getContext().getActions(), expectedActions);
    }

}
