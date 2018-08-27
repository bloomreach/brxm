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

import java.util.Collections;
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

/**
 * This class tests the actions that are set by the workflow executor
 * If differs from the DocumentWorkflowTest on the following points:
 * <ul>
 *     <li>Each test has one assertions</li>
 *     <li>The WorkflowData has been mocked</li>
 * </ul>
 *
 * This enables testing cases where frozen nodes influence the actions.
 *
 */
public class WorkflowExecutorTest extends BaseDocumentWorkflowTest {

    public static final String MASTER = "master";
    private MockNode root;
    private MockAccessManagedSession session;
    private MockNode handle;
    private Map<String, DocumentVariant> variantsMap;

    @BeforeClass
    public static void beforeClass() throws Exception {
        createDocumentWorkflowSCXMLRegistry();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        destroyDocumentWorkflowSCXMLRegistry();
    }

    @Before
    public void setUp() throws RepositoryException {
        root = MockNode.root();
        session = new MockAccessManagedSession(root);
        handle = root.addNode("document-1", NT_HANDLE);
        variantsMap = new HashMap<>();
    }

    /**
     * <ul>
     *     <li>Go to the content perspective</li>
     *     <li>Create a new document</li>
     * </ul>
     */
    @Test
    public void editing_master() throws WorkflowException, RepositoryException {
        String branchId = MASTER;
        final DocumentHandle data = EasyMock.createNiceMock(DocumentHandle.class);
        expect(data.getDocuments()).andStubReturn(variantsMap);
        expect(data.getHandle()).andStubReturn(handle);
        expect(data.getBranches()).andStubReturn(emptySet());
        expect(data.getBranchId()).andStubReturn(branchId);
        replay(data);


        new DocumentVariantBuilder(session, handle, variantsMap)
                .relPath("document-1")
                .state(HippoStdNodeType.DRAFT)
                .holder("testuser")
                .permissions("hippo:author")
                .branchId(branchId)
                .add();


        final Map<String, ?> expectedActions = HintsBuilder.build()
                .branch(false)
                .checkModified(false)
                .checkoutBranch(false)
                .commitEditableInstance(true)
                .depublishBranch(false)
                .disposeEditableInstance(true)
                .getBranch(false)
                .listBranches()
                .listVersions()
                .obtainEditableInstance(true)
                .publishBranch(false)
                .reintegrateBranch(false)
                .removeBranch(false)
                .requestDelete(false)
                .requestDepublication(false)
                .requestPublication(false)
                .actions();
        assertExpectedActions(data, expectedActions);
    }

    /**
     * <ul>
     *     <li>Go to the content perspective</li>
     *     <li>Create a new document</li>
     *     <li>Save & Close</li>
     *     <li>Add to branch "foo"</li>
     *     <li>Edit the document</li>
     * </ul>
     *
     * In version history there is now a version with label "foo-unpublished"
     *
     * Under the handle the following variants exist:
     * <ul>
     *     <li>draft ("foo")</li>
     *     <li>unpublished("master")</li>
     * </ul>
     */
    @Test
    public void editing_branch() throws RepositoryException, WorkflowException {
        final String branchId = "foo";

        final DocumentHandle data = EasyMock.createNiceMock(DocumentHandle.class);
        expect(data.getDocuments()).andStubReturn(variantsMap);
        expect(data.getHandle()).andStubReturn(handle);

        expect(data.getBranches()).andStubReturn(emptySet());
        expect(data.getBranchId()).andStubReturn(branchId);
        expect(data.isOnlyMaster()).andStubReturn(false);
        expect(data.isLiveAvailable(branchId)).andStubReturn(false);
        expect(data.isAnyBranchLiveAvailable()).andStubReturn(false);
        expect(data.isPreviewAvailable(branchId)).andStubReturn(true);
        expect(data.isModified(branchId)).andStubReturn(false);
        replay(data);


        new DocumentVariantBuilder(session, handle, variantsMap)
                .relPath("document-1")
                .state(HippoStdNodeType.DRAFT)
                .holder("testuser")
                .permissions("hippo:author")
                .branchId(branchId)
                .add();

        new DocumentVariantBuilder(session, handle, variantsMap)
                .relPath("document-1")
                .state(HippoStdNodeType.UNPUBLISHED)
                .permissions("hippo:author")
                .branchId(MASTER)
                .add();

        final Map<String, ?> expectedActions = HintsBuilder.build()
                .branch(false)
                .checkModified(true)
                .checkoutBranch(false)
                .commitEditableInstance(true)
                .depublishBranch(false)
                .disposeEditableInstance(true)
                .getBranch(false)
                .listBranches()
                .listVersions()
                .obtainEditableInstance(true)
                .publishBranch(false)
                .reintegrateBranch(false)
                .removeBranch(false)
                .requestDelete(false)
                .requestDepublication(false)
                .requestPublication(false)
                .retrieveVersion()
                .actions();
        assertExpectedActions(data, expectedActions);
    }


    /**
     * <ul>
     *     <li>Go to the content perspective</li>
     *     <li>Open a document</li>
     *     <li>Add it to branch "foo"</li>
     *     <li>Select "master"</li>
     *     <li>Edit and save</li>
     *     <li>Select branch "foo"</li>
     * </ul>
     */
    @Test
    public void viewing_branch_unpublished_frozen_node() throws WorkflowException, RepositoryException {
        final String branchId = "foo";

        final DocumentHandle data = EasyMock.createNiceMock(DocumentHandle.class);
        expect(data.getDocuments()).andStubReturn(variantsMap);
        expect(data.getHandle()).andStubReturn(handle);

        expect(data.getBranches()).andStubReturn(Collections.singleton(branchId));
        expect(data.getBranchId()).andStubReturn(branchId);
        expect(data.isOnlyMaster()).andStubReturn(false);
        expect(data.isLiveAvailable(branchId)).andStubReturn(false);
        expect(data.isAnyBranchLiveAvailable()).andStubReturn(false);
        expect(data.isPreviewAvailable(branchId)).andStubReturn(true);
        expect(data.isModified(branchId)).andStubReturn(false);
        replay(data);

        new DocumentVariantBuilder(session, handle, variantsMap)
                .relPath("document-1")
                .state(HippoStdNodeType.UNPUBLISHED)
                .permissions("hippo:author")
                .branchId(MASTER)
                .add();


        final Map<String, ?> expectedActions = HintsBuilder.build()
                .branch(false)
                .checkModified(false)
                .checkoutBranch(true)
                .commitEditableInstance(false)
                .depublishBranch(false)
                .disposeEditableInstance(false)
                .getBranch(true)
                .listBranches()
                .listVersions()
                .obtainEditableInstance(true)
                .publishBranch(false)
                .reintegrateBranch(true)
                .removeBranch(true)
                .requestDelete(true)
                .requestDepublication(false)
                .requestPublication(false)
                .retrieveVersion()
                .retrieveVersion()
                .actions();

        assertExpectedActions(data, expectedActions);
    }

    /**
     * <ul>
     *     <li>Go to the content perspective</li>
     *     <li>Open a document</li>
     *     <li>Add it to branch "foo"</li>
     *     <li>Select "master"</li>
     * </ul>
     * @throws WorkflowException
     * @throws RepositoryException
     */
    @Test
    public void viewing_master_unpublished_frozen_node() throws WorkflowException, RepositoryException {
        final String branchId = "master";

        final Map<String, DocumentVariant> variantsMap = new HashMap<>();
        final DocumentHandle data = EasyMock.createNiceMock(DocumentHandle.class);
        expect(data.getDocuments()).andStubReturn(variantsMap);
        expect(data.getHandle()).andStubReturn(handle);

        expect(data.getBranches()).andStubReturn(Collections.singleton(branchId));
        expect(data.getBranchId()).andStubReturn(branchId);
        expect(data.isOnlyMaster()).andStubReturn(false);
        expect(data.isLiveAvailable(branchId)).andStubReturn(false);
        expect(data.isAnyBranchLiveAvailable()).andStubReturn(false);
        expect(data.isPreviewAvailable(branchId)).andStubReturn(true);
        expect(data.isModified(branchId)).andStubReturn(false);
        replay(data);

        new DocumentVariantBuilder(session, handle, variantsMap)
                .relPath("document-1")
                .state(HippoStdNodeType.UNPUBLISHED)
                .permissions("hippo:author")
                .branchId("foo")
                .add();

        final Map<String, ?> expectedActions = HintsBuilder.build()
                .branch(true)
                .checkModified(false)
                .checkoutBranch(true)
                .commitEditableInstance(false)
                .depublishBranch(false)
                .disposeEditableInstance(false)
                .getBranch(true)
                .listBranches()
                .listVersions()
                .obtainEditableInstance(true)
                .publishBranch(false)
                .reintegrateBranch(false)
                .removeBranch(false)
                .requestDelete(true)
                .requestDepublication(false)
                .requestPublication(false)
                .retrieveVersion()
                .actions();
        assertExpectedActions(data, expectedActions);
    }

   

    


    /**
     *
     * <ul>
     *     <li>Open a document</li>
     *     <li>Add it to branch "foo" ( unpublished variant becomes "foo" )</li>
     *     <li>Make sure "foo" gets published</li>
     *     <li>Select branch "master"</li>
     *     <li>Edit & Save ( unpublished variant becomes "master"</li>
     *     <li>Publish ( published variant becomes "master" )</li>
     * </ul>
     * @throws WorkflowException
     * @throws RepositoryException
     */
    @Test
    public void viewing_branch_published_and_unpublished_frozen_node() throws WorkflowException, RepositoryException {
        final String branchId = "foo";

        final Map<String, DocumentVariant> variantsMap = new HashMap<>();
        final DocumentHandle data = EasyMock.createNiceMock(DocumentHandle.class);
        expect(data.getDocuments()).andStubReturn(variantsMap);
        expect(data.getHandle()).andStubReturn(handle);

        expect(data.getBranches()).andStubReturn(Collections.singleton(branchId));
        expect(data.getBranchId()).andStubReturn(branchId);
        expect(data.isOnlyMaster()).andStubReturn(false);
        expect(data.isLiveAvailable(branchId)).andStubReturn(true);
        expect(data.isAnyBranchLiveAvailable()).andStubReturn(true);
        expect(data.isPreviewAvailable(branchId)).andStubReturn(true);
        expect(data.isModified(branchId)).andStubReturn(false);
        replay(data);


        new DocumentVariantBuilder(session, handle, variantsMap)
                .relPath("document-1")
                .state(HippoStdNodeType.PUBLISHED)
                .permissions("hippo:author")
                .branchId(MASTER)
                .add();

        new DocumentVariantBuilder(session, handle, variantsMap)
                .relPath("document-1")
                .state(HippoStdNodeType.UNPUBLISHED)
                .permissions("hippo:author")
                .branchId(MASTER)
                .add();


        final Map<String, ?> expectedActions = HintsBuilder.build()
                .branch(false)
                .checkModified(false)
                .checkoutBranch(true)
                .commitEditableInstance(false)
                .depublishBranch(true)
                .disposeEditableInstance(false)
                .getBranch(true)
                .listBranches()
                .listVersions()
                .obtainEditableInstance(true)
                .publishBranch(false)
                .reintegrateBranch(true)
                .removeBranch(false)
                .requestDelete(false)
                .requestDepublication(false)
                .requestPublication(false)
                .retrieveVersion()
                .actions();
        assertExpectedActions(data, expectedActions);
    }



    /**
     *
     * <ul>
     *     <li>Open a document</li>
     *     <li>Add it to branch "foo" ( unpublished variant becomes "foo" )</li>
     *     <li>Make sure "foo" gets published</li>
     *     <li>Select branch "master"</li>
     *     <li>Edit & Save ( unpublished variant becomes "master"</li>
     *     <li>Publish ( published variant becomes "master" )</li>
     *     <li>Select branch "foo"</li>
     * </ul>
     * @throws WorkflowException
     * @throws RepositoryException
     */
    @Test
    public void viewing_branch_published_and_unpublished_modified_frozen_node() throws WorkflowException, RepositoryException {
        final String branchId = "foo";

        final Map<String, DocumentVariant> variantsMap = new HashMap<>();
        final DocumentHandle data = EasyMock.createNiceMock(DocumentHandle.class);
        expect(data.getDocuments()).andStubReturn(variantsMap);
        expect(data.getHandle()).andStubReturn(handle);

        expect(data.getBranches()).andStubReturn(Collections.singleton(branchId));
        expect(data.getBranchId()).andStubReturn(branchId);
        expect(data.isOnlyMaster()).andStubReturn(false);
        expect(data.isLiveAvailable(branchId)).andStubReturn(true);
        expect(data.isAnyBranchLiveAvailable()).andStubReturn(true);
        expect(data.isPreviewAvailable(branchId)).andStubReturn(true);
        expect(data.isModified(branchId)).andStubReturn(true);
        replay(data);

        new DocumentVariantBuilder(session, handle, variantsMap)
                .relPath("document-1")
                .state(HippoStdNodeType.PUBLISHED)
                .permissions("hippo:author")
                .branchId(MASTER)
                .add();

        new DocumentVariantBuilder(session, handle, variantsMap)
                .relPath("document-1")
                .state(HippoStdNodeType.UNPUBLISHED)
                .permissions("hippo:author")
                .branchId(MASTER)
                .add();

        final Map<String, ?> expectedActions = HintsBuilder.build()
                .branch(false)
                .checkModified(false)
                .checkoutBranch(true)
                .commitEditableInstance(false)
                .depublishBranch(true)
                .disposeEditableInstance(false)
                .getBranch(true)
                .listBranches()
                .listVersions()
                .obtainEditableInstance(true)
                .publishBranch(true)
                .reintegrateBranch(true)
                .removeBranch(false)
                .requestDelete(false)
                .requestDepublication(false)
                .requestPublication(false)
                .retrieveVersion()
                .actions();
        assertExpectedActions(data, expectedActions);
    }

    private void assertExpectedActions(final DocumentHandle data, final Map<String, ?> expectedActions) throws RepositoryException, WorkflowException {
        final MockWorkflowContext testuserContext = new MockWorkflowContext("testuser", session);
        final SCXMLWorkflowContext context = new SCXMLWorkflowContext("documentworkflow", testuserContext);
        SCXMLWorkflowExecutor scxmlWorkflowExecutor = new SCXMLWorkflowExecutor<SCXMLWorkflowContext, SCXMLWorkflowData>(context, data);
        scxmlWorkflowExecutor.start(data.getBranchId());
        assertMatchingKeyValues(scxmlWorkflowExecutor.getContext().getActions(), expectedActions);
    }

}
