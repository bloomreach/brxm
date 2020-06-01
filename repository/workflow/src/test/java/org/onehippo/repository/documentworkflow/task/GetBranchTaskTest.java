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

package org.onehippo.repository.documentworkflow.task;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.WorkflowException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests some validations that cannot be easilly tested via workflow execution.
 */
public class GetBranchTaskTest {

    @Test
    public void doExecute_branchId_and_state_null() throws RepositoryException {
        final GetBranchTask getBranchTask = new GetBranchTask();
        try {
            getBranchTask.doExecute();
        } catch (WorkflowException e) {
            assertEquals("branchId and state are both required but branchId = null and state = null", e.getMessage());
        }
    }

    @Test
    public void doExecute_branchId_null() throws RepositoryException {
        final GetBranchTask getBranchTask = new GetBranchTask();
        getBranchTask.setState("abc");
        try {
            getBranchTask.doExecute();
        } catch (WorkflowException e) {
            assertEquals("branchId and state are both required but branchId = null and state = abc", e.getMessage());
        }
    }

    @Test
    public void doExecute_state_null() throws RepositoryException {
        final GetBranchTask getBranchTask = new GetBranchTask();
        getBranchTask.setBranchId("xyz");
        try {
            getBranchTask.doExecute();
        } catch (WorkflowException e) {
            assertEquals("branchId and state are both required but branchId = xyz and state = null", e.getMessage());
        }
    }

    @Test
    public void doExecute_state_unknown() throws RepositoryException {
        final GetBranchTask getBranchTask = new GetBranchTask();
        getBranchTask.setBranchId("xyz");
        getBranchTask.setState("abc");
        try {
            getBranchTask.doExecute();
        } catch (WorkflowException e) {
            assertEquals("Invalid state 'abc', valid states are draft, published, unpublished", e.getMessage());
        }
    }

}