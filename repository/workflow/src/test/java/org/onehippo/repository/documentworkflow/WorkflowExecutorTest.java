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
                editing_master(), //0
                editing_branch(), //1
                viewing_branch_unpublished_frozen_node(), //2
                viewing_branch_published_frozen_node(), //3
                viewing_branch_published_and_unpublished_frozen_node(), //4
                viewing_branch_published_and_unpublished_modified_frozen_node(), //5
                viewing_master_published_frozen_node(),//6
                viewing_master_unpublished_frozen_node() //7
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

    private static Object[] viewing_branch_unpublished_frozen_node(){
        try{
            final String branchId = "foo";

            final Map<String, DocumentVariant> variantsMap = new HashMap<>();
            final DocumentHandle data = EasyMock.createNiceMock(DocumentHandle.class);
            expect(data.getDocuments()).andStubReturn(variantsMap);
            expect(data.getHandle()).andStubReturn(HANDLE);

            expect(data.getBranches()).andStubReturn(Collections.singleton(branchId));
            expect(data.getBranchId()).andStubReturn(branchId);
            expect(data.isOnlyMaster()).andStubReturn(false);
            expect(data.isLiveAvailable(branchId)).andStubReturn(false);
            expect(data.isAnyBranchLiveAvailable()).andStubReturn(false);
            expect(data.isPreviewAvailable(branchId)).andStubReturn(true);
            expect(data.isModified(branchId)).andStubReturn(false);
            replay(data);

            final MockNode unpublishedVariant = HANDLE.addNode("document-1", "hippo:variant");
            final DocumentVariant unpublished = new DocumentVariant(unpublishedVariant);
            unpublishedVariant.addMixin(HIPPO_MIXIN_BRANCH_INFO);
            unpublishedVariant.setProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_ID, "master");

            unpublished.setState(HippoStdNodeType.UNPUBLISHED);
            SESSION.setPermissions(unpublishedVariant.getPath(), "hippo:author", true);
            variantsMap.put(HippoStdNodeType.UNPUBLISHED, unpublished);


            final Map<String, ?> expectedActions = HintsBuilder.build()
                    .obtainEditableInstance(true)
                    .commitEditableInstance(false)
                    .disposeEditableInstance(false)
                    .depublishBranch(false)
                    .publishBranch(false)
                    .reintegrateBranch(true)
                    .checkModified(false)
                    .branch(false)
                    .requestDelete(true)
                    .requestDepublication(false)
                    .listVersions()
                    .retrieveVersion()
                    .requestPublication(false)
                    .listBranches()
                    .checkoutBranch(true)
                    .removeBranch(true)
                    .getBranch(true)
                    .actions();
            return new Object[]{data, expectedActions};

        }
        catch (RepositoryException | WorkflowException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object[] viewing_master_unpublished_frozen_node(){
        try{
            final String branchId = "master";

            final Map<String, DocumentVariant> variantsMap = new HashMap<>();
            final DocumentHandle data = EasyMock.createNiceMock(DocumentHandle.class);
            expect(data.getDocuments()).andStubReturn(variantsMap);
            expect(data.getHandle()).andStubReturn(HANDLE);

            expect(data.getBranches()).andStubReturn(Collections.singleton(branchId));
            expect(data.getBranchId()).andStubReturn(branchId);
            expect(data.isOnlyMaster()).andStubReturn(false);
            expect(data.isLiveAvailable(branchId)).andStubReturn(false);
            expect(data.isAnyBranchLiveAvailable()).andStubReturn(false);
            expect(data.isPreviewAvailable(branchId)).andStubReturn(true);
            expect(data.isModified(branchId)).andStubReturn(false);
            replay(data);

            final MockNode unpublishedVariant = HANDLE.addNode("document-1", "hippo:variant");
            final DocumentVariant unpublished = new DocumentVariant(unpublishedVariant);
            unpublishedVariant.addMixin(HIPPO_MIXIN_BRANCH_INFO);
            unpublishedVariant.setProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_ID, "foo");

            unpublished.setState(HippoStdNodeType.UNPUBLISHED);
            SESSION.setPermissions(unpublishedVariant.getPath(), "hippo:author", true);
            variantsMap.put(HippoStdNodeType.UNPUBLISHED, unpublished);


            final Map<String, ?> expectedActions = HintsBuilder.build()
                    .obtainEditableInstance(true)
                    .commitEditableInstance(false)
                    .disposeEditableInstance(false)
                    .depublishBranch(false)
                    .publishBranch(false)
                    .reintegrateBranch(false) //TODO(mrop) should be true, adjust scxml
                    .checkModified(false)
                    .branch(true)
                    .requestDelete(true)
                    .requestDepublication(false)
                    .listVersions()
                    .retrieveVersion()
                    .requestPublication(false)
                    .listBranches()
                    .checkoutBranch(true)
                    .removeBranch(false) //TODO(mrop) should be true,  adjust scxml
                    .getBranch(true)
                    .actions();
            return new Object[]{data, expectedActions};

        }
        catch (RepositoryException | WorkflowException e) {
            throw new RuntimeException(e);
        }
    }


    private static Object[] viewing_branch_published_frozen_node(){
        try{
            final String branchId = "foo";

            final Map<String, DocumentVariant> variantsMap = new HashMap<>();
            final DocumentHandle data = EasyMock.createNiceMock(DocumentHandle.class);
            expect(data.getDocuments()).andStubReturn(variantsMap);
            expect(data.getHandle()).andStubReturn(HANDLE);

            expect(data.getBranches()).andStubReturn(Collections.singleton(branchId));
            expect(data.getBranchId()).andStubReturn(branchId);
            expect(data.isOnlyMaster()).andStubReturn(false);
            expect(data.isLiveAvailable(branchId)).andStubReturn(true);
            expect(data.isAnyBranchLiveAvailable()).andStubReturn(true);
            expect(data.isPreviewAvailable(branchId)).andStubReturn(false);
            expect(data.isModified(branchId)).andStubReturn(false);
            replay(data);

            final MockNode publishedVariant = HANDLE.addNode("document-1", "hippo:variant");
            final DocumentVariant published = new DocumentVariant(publishedVariant);
            publishedVariant.addMixin(HIPPO_MIXIN_BRANCH_INFO);
            publishedVariant.setProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_ID, "master");

            published.setState(HippoStdNodeType.PUBLISHED);
            SESSION.setPermissions(publishedVariant.getPath(), "hippo:author", true);
            variantsMap.put(HippoStdNodeType.PUBLISHED, published);


            final Map<String, ?> expectedActions = HintsBuilder.build()
                    .obtainEditableInstance(true)
                    .commitEditableInstance(false)
                    .disposeEditableInstance(false)
                    .depublishBranch(true)
                    .publishBranch(false)
                    .reintegrateBranch(false)
                    .checkModified(false)
                    .branch(false)
                    .requestDelete(false)
                    .requestDepublication(false)
                    .listVersions()
                    // .retrieveVersion() TODO (mrop) adjust scxml to include retrieveVersion
                    .requestPublication(false)
                    .listBranches()
                    .checkoutBranch(false)
                    .removeBranch(false)
                    .getBranch(true)
                    .actions();
            return new Object[]{data, expectedActions};

        }
        catch (RepositoryException | WorkflowException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object[] viewing_branch_published_and_unpublished_frozen_node(){
        try{
            final String branchId = "foo";

            final Map<String, DocumentVariant> variantsMap = new HashMap<>();
            final DocumentHandle data = EasyMock.createNiceMock(DocumentHandle.class);
            expect(data.getDocuments()).andStubReturn(variantsMap);
            expect(data.getHandle()).andStubReturn(HANDLE);

            expect(data.getBranches()).andStubReturn(Collections.singleton(branchId));
            expect(data.getBranchId()).andStubReturn(branchId);
            expect(data.isOnlyMaster()).andStubReturn(false);
            expect(data.isLiveAvailable(branchId)).andStubReturn(true);
            expect(data.isAnyBranchLiveAvailable()).andStubReturn(true);
            expect(data.isPreviewAvailable(branchId)).andStubReturn(true);
            expect(data.isModified(branchId)).andStubReturn(false);
            replay(data);

            final MockNode publishedVariant = HANDLE.addNode("document-1", "hippo:variant");
            final DocumentVariant published = new DocumentVariant(publishedVariant);
            publishedVariant.addMixin(HIPPO_MIXIN_BRANCH_INFO);
            publishedVariant.setProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_ID, "master");

            published.setState(HippoStdNodeType.PUBLISHED);
            SESSION.setPermissions(publishedVariant.getPath(), "hippo:author", true);
            variantsMap.put(HippoStdNodeType.PUBLISHED, published);


            final Map<String, ?> expectedActions = HintsBuilder.build()
                    .obtainEditableInstance(true)
                    .commitEditableInstance(false)
                    .disposeEditableInstance(false)
                    .depublishBranch(true)
                    .publishBranch(false)
                    .reintegrateBranch(false)
                    .checkModified(false)
                    .branch(false)
                    .requestDelete(false)
                    .requestDepublication(false)
                    //.retrieveVersion() //TODO(mrop) adjust scxml
                    .listVersions()
                    .requestPublication(false)
                    .listBranches()
                    .checkoutBranch(false)
                    .removeBranch(false)
                    .getBranch(true)
                    .actions();
            return new Object[]{data, expectedActions};

        }
        catch (RepositoryException | WorkflowException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object[] viewing_master_published_frozen_node(){
        try{
            final String branchId = "master";

            final Map<String, DocumentVariant> variantsMap = new HashMap<>();
            final DocumentHandle data = EasyMock.createNiceMock(DocumentHandle.class);
            expect(data.getDocuments()).andStubReturn(variantsMap);
            expect(data.getHandle()).andStubReturn(HANDLE);

            expect(data.getBranches()).andStubReturn(Collections.singleton(branchId));
            expect(data.getBranchId()).andStubReturn(branchId);
            expect(data.isOnlyMaster()).andStubReturn(false);
            expect(data.isLiveAvailable(branchId)).andStubReturn(true);
            expect(data.isAnyBranchLiveAvailable()).andStubReturn(true);
            expect(data.isPreviewAvailable(branchId)).andStubReturn(false);
            expect(data.isModified(branchId)).andStubReturn(false);
            replay(data);

            final MockNode publishedVariant = HANDLE.addNode("document-1", "hippo:variant");
            final DocumentVariant published = new DocumentVariant(publishedVariant);
            publishedVariant.addMixin(HIPPO_MIXIN_BRANCH_INFO);
            publishedVariant.setProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_ID, "foo");

            published.setState(HippoStdNodeType.PUBLISHED);
            SESSION.setPermissions(publishedVariant.getPath(), "hippo:author", true);
            variantsMap.put(HippoStdNodeType.PUBLISHED, published);


            final Map<String, ?> expectedActions = HintsBuilder.build()
                    .obtainEditableInstance(true)
                    .commitEditableInstance(false)
                    .disposeEditableInstance(false)
                    .depublishBranch(true)
                    .publishBranch(false)
                    //.publish(true) //TODO (mrop) add ("publish",true) , adjust scxml
                    .reintegrateBranch(false)
                    .checkModified(false)
                    .branch(true)
                    .requestDelete(false)
                    .requestDepublication(true)
                    .listVersions()
                    // .retrieveVersion() TODO (mrop) aa ("retrieveVersion", adjust scxml
                    .requestPublication(false)
                    .listBranches()
                    .checkoutBranch(false)
                    .removeBranch(false)
                    .getBranch(true)
                    .actions();
            return new Object[]{data, expectedActions};

        }
        catch (RepositoryException | WorkflowException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object[] viewing_branch_published_and_unpublished_modified_frozen_node(){
        try{
            final String branchId = "foo";

            final Map<String, DocumentVariant> variantsMap = new HashMap<>();
            final DocumentHandle data = EasyMock.createNiceMock(DocumentHandle.class);
            expect(data.getDocuments()).andStubReturn(variantsMap);
            expect(data.getHandle()).andStubReturn(HANDLE);

            expect(data.getBranches()).andStubReturn(Collections.singleton(branchId));
            expect(data.getBranchId()).andStubReturn(branchId);
            expect(data.isOnlyMaster()).andStubReturn(false);
            expect(data.isLiveAvailable(branchId)).andStubReturn(true);
            expect(data.isAnyBranchLiveAvailable()).andStubReturn(true);
            expect(data.isPreviewAvailable(branchId)).andStubReturn(true);
            expect(data.isModified(branchId)).andStubReturn(true);
            replay(data);

            final MockNode publishedVariant = HANDLE.addNode("document-1", "hippo:variant");
            final DocumentVariant published = new DocumentVariant(publishedVariant);
            publishedVariant.addMixin(HIPPO_MIXIN_BRANCH_INFO);
            publishedVariant.setProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_ID, "master");

            published.setState(HippoStdNodeType.PUBLISHED);
            SESSION.setPermissions(publishedVariant.getPath(), "hippo:author", true);
            variantsMap.put(HippoStdNodeType.PUBLISHED, published);


            final Map<String, ?> expectedActions = HintsBuilder.build()
                    .obtainEditableInstance(true)
                    .commitEditableInstance(false)
                    .disposeEditableInstance(false)
                    .depublishBranch(true)
                    .publishBranch(false) //TODO(mrop) should be true, adjust scxml
                    .reintegrateBranch(false)
                    .checkModified(false)
                    .branch(false)
                    .requestDelete(false)
                    .requestDepublication(false)
                    .listVersions()
                    //.retrieveVersion() //TODO(mrop) add retrieveVersion, adjust scxml
                    .requestPublication(false)
                    .listBranches()
                    .checkoutBranch(false)
                    .removeBranch(false)
                    .getBranch(true)
                    .actions();
            return new Object[]{data, expectedActions};

        }
        catch (RepositoryException | WorkflowException e) {
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
