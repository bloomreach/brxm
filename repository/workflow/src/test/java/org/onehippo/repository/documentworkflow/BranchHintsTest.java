/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.onehippo.repository.documentworkflow;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.stream.Collectors;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.WorkflowException;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.branch.BranchHandle;
import org.onehippo.repository.documentworkflow.integration.AbstractDocumentWorkflowIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

public class BranchHintsTest extends AbstractDocumentWorkflowIntegrationTest {


    private DocumentHandle documentHandle;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        documentHandle = new DocumentHandle(handle);
        documentHandle.initialize();
    }

    @Test
    public void when_thereIsNoFrozenNode_then_workflowHints() throws RepositoryException, WorkflowException {
        BranchHandle branchHandle = new BranchHandleImpl("master", handle);
        final Map<String, Serializable> hints = new HintsBuilder().build().depublish(true).publish(true).hints();
        final Map<String, Serializable> actual = new BranchHints.Builder().branchHandle(branchHandle).documentHandle(documentHandle).hints(hints)
                .build().getHints();
        assertThat(actual).isEqualToComparingFieldByFieldRecursively(hints);
    }

    @Test
    public void when_unpublishedIsFrozenNodeNotMaster_then_PublishIsFalse() throws RepositoryException, WorkflowException, RemoteException {
        final DocumentWorkflow documentWorkflow = getDocumentWorkflow(handle);
        documentWorkflow.branch("branch1", "branch1");
        documentWorkflow.checkoutBranch("master");
        BranchHandle branchHandle = new BranchHandleImpl("branch1", handle);
        final Map<String, Serializable> actual = new BranchHints.Builder().branchHandle(branchHandle).documentHandle(documentHandle).hints(documentWorkflow.hints()).build().getHints();
        final Map<String, Serializable> expected = HintsBuilder.build().publish(false).hints();
        assertHint(actual, expected, "publish");
    }

    @Test
    public void when_unpublishedIsFrozenNodeNotMaster_then_depublishIsFalse() throws RepositoryException, WorkflowException, RemoteException {
        final DocumentWorkflow documentWorkflow = getDocumentWorkflow(handle);
        documentWorkflow.branch("branch1", "branch1");
        documentWorkflow.checkoutBranch("master");
        BranchHandle branchHandle = new BranchHandleImpl("branch1", handle);
        final Map<String, Serializable> actual = new BranchHints.Builder().branchHandle(branchHandle).documentHandle(documentHandle).hints(documentWorkflow.hints()).build().getHints();
        final Map<String, Serializable> expected = HintsBuilder.build().depublish(false).hints();
        assertHint(actual, expected, "depublish");
    }

    @Test
    public void when_unpublishedIsFrozenNodeMaster_then_depublishIsTrue() throws RepositoryException, WorkflowException, RemoteException {
        final DocumentWorkflow documentWorkflow = getDocumentWorkflow(handle);
        documentWorkflow.branch("branch1", "branch1");
        BranchHandle branchHandle = new BranchHandleImpl("master", handle);
        final Map<String, Serializable> actual = new BranchHints.Builder().branchHandle(branchHandle).documentHandle(documentHandle).hints(documentWorkflow.hints()).build().getHints();
        final Map<String, Serializable> expected = HintsBuilder.build().depublish(true).hints();
        assertHint(actual, expected, "depublish");
    }



    @Test
    public void when_NotEditingCurrentBranchAndUnpublishedExists_then_removeBranchIsTrue() throws WorkflowException, RepositoryException, RemoteException {
        final DocumentWorkflow documentWorkflow = getDocumentWorkflow(handle);
        documentWorkflow.branch("branch1", "branch1");
        documentWorkflow.checkoutBranch("master");
        documentWorkflow.branch("branch2", "branch2");
        documentWorkflow.checkoutBranch("branch2");
        BranchHandle branchHandle = new BranchHandleImpl("branch1", handle);
        documentWorkflow.obtainEditableInstance();
        final Map<String, Serializable> actual = new BranchHints.Builder().branchHandle(branchHandle).documentHandle(documentHandle).hints(documentWorkflow.hints()).build().getHints();
        final Map<String, Serializable> expected = HintsBuilder.build().removeBranch(true).hints();
        assertHint(actual, expected, "removeBranch");
    }

    @Test
    public void when_CurrentBranchIsMasterUnpublishedVariantNotMaster_then_CheckoutBranchTrue() throws WorkflowException, RepositoryException, RemoteException {
        final DocumentWorkflow documentWorkflow = getDocumentWorkflow(handle);
        documentWorkflow.branch("branch1", "branch1");
        BranchHandle branchHandle = new BranchHandleImpl("master", handle);
        documentWorkflow.obtainEditableInstance();
        final Map<String, Serializable> actual = new BranchHints.Builder().branchHandle(branchHandle).documentHandle(documentHandle).hints(documentWorkflow.hints()).build().getHints();
        final Map<String, Serializable> expected = HintsBuilder.build().checkoutBranch(true).hints();
        assertHint(actual, expected, "checkoutBranch");
    }

    private void assertHint(final Map<String, Serializable> actual, final Map<String, Serializable> expected, final String depublish) {
        final Map.Entry<String, Serializable> actualHint = actual.entrySet().stream().filter(entry -> entry.getKey().equals(depublish)).collect(Collectors.toList()).get(0);
        final Map.Entry<String, Serializable> expectedHint = expected.entrySet().stream().filter(entry -> entry.getKey().equals(depublish)).collect(Collectors.toList()).get(0);
        assertThat(actualHint).isEqualTo(expectedHint);
    }


}