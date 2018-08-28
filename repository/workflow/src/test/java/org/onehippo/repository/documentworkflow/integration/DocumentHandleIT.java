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

package org.onehippo.repository.documentworkflow.integration;

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.assertj.core.api.Assertions;
import org.hippoecm.repository.api.WorkflowException;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentHandle;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MIXIN_BRANCH_INFO;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.standardworkflow.DocumentVariant.MASTER_BRANCH_ID;

public class DocumentHandleIT extends AbstractDocumentWorkflowIntegrationTest {

    private DocumentHandle documentHandle;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        documentHandle = new DocumentHandle(handle);
    }

    @Test
    public void is_only_master_if_there_no_branches() throws WorkflowException {
        documentHandle.initialize();
        Assertions.assertThat(documentHandle.isOnlyMaster()).isTrue();
    }


    @Test
    public void is_not_only_master_if_draft_is_branch() throws WorkflowException, RepositoryException, RemoteException {

        final String branchId = "foo";
        final Node draftVariant = getDocumentWorkflow(handle).obtainEditableInstance().getNode(session);
        draftVariant.addMixin(HIPPO_MIXIN_BRANCH_INFO);
        draftVariant.setProperty(HIPPO_PROPERTY_BRANCH_ID, branchId);

        documentHandle.initialize();
        Assertions.assertThat(documentHandle.isOnlyMaster()).isFalse();
    }

    @Test
    public void is_not_only_master_if_branch_in_version_history() throws WorkflowException, RepositoryException {

        final String branchId = "foo";
        final String branchName = "Foo";
        getDocumentWorkflow(handle).branch(branchId, branchName);

        documentHandle.initialize();
        Assertions.assertThat(documentHandle.isOnlyMaster()).isFalse();
    }


    @Test
    public void is_modified_if_there_is_only_unpublished() throws WorkflowException {
        documentHandle.initialize();
        Assertions.assertThat(documentHandle.isModified()).isTrue();
    }

    @Test
    public void is_not_modified_if_unpublished_equals_published() throws WorkflowException, RepositoryException, RemoteException {

        getDocumentWorkflow(handle).publishBranch(MASTER_BRANCH_ID);

        documentHandle.initialize();
        Assertions.assertThat(documentHandle.isModified()).isFalse();
    }

    @Test
    public void is_modified_if_published_availability_not_live() throws WorkflowException, RepositoryException, RemoteException {

        final String branchId = MASTER_BRANCH_ID;
        getDocumentWorkflow(handle).publishBranch(branchId);
        getDocumentWorkflow(handle).depublishBranch(branchId);

        documentHandle.initialize();
        Assertions.assertThat(documentHandle.isModified()).isTrue();
    }


    @Test
    public void is_modified_if_unpublished_branch_is_in_version_history() throws WorkflowException, RepositoryException, RemoteException {

        final String branchId = "foo";
        final String branchName = "Foo";
        getDocumentWorkflow(handle).branch(branchId, branchName);
        getDocumentWorkflow(handle).checkoutBranch(MASTER_BRANCH_ID);

        documentHandle.initialize();
        Assertions.assertThat(documentHandle.isModified()).isTrue();

        getDocumentWorkflow(handle).publishBranch(branchId);
    }

    @Test
    public void is_not_modified_if_published_branch_is_in_version_history() throws WorkflowException, RepositoryException, RemoteException {

        final String branchId = "foo";
        final String branchName = "Foo";
        getDocumentWorkflow(handle).branch(branchId, branchName);
        getDocumentWorkflow(handle).checkoutBranch(MASTER_BRANCH_ID);
        getDocumentWorkflow(handle).publishBranch(branchId);

        documentHandle.initialize(branchId);
        Assertions.assertThat(documentHandle.isModified()).isFalse();
    }

    @Test
    public void master_not_modified_if_branch_gets_modified() throws WorkflowException, RepositoryException, RemoteException {

        documentHandle.initialize(MASTER_BRANCH_ID);
        Assertions.assertThat(documentHandle.isModified()).isTrue();
        getDocumentWorkflow(handle).publish();
        Assertions.assertThat(documentHandle.isModified()).isFalse();

        final String branchId = "foo";
        final String branchName = "Foo";
        getDocumentWorkflow(handle).branch(branchId, branchName);
        getDocumentWorkflow(handle).obtainEditableInstance(branchId);
        getDocumentWorkflow(handle).commitEditableInstance();

        documentHandle.initialize(MASTER_BRANCH_ID);
        Assertions.assertThat(documentHandle.isModified()).isFalse();

        documentHandle.initialize(branchId);
        Assertions.assertThat(documentHandle.isModified()).isTrue();
    }
}

