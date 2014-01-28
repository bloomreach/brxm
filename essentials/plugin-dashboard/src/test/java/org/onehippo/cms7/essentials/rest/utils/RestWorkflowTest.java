/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.rest.utils;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;
import org.onehippo.cms7.essentials.rest.exc.RestException;

import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class RestWorkflowTest extends BaseRepositoryTest{

    @Test
    public void testAddCompoundType() throws Exception {


        final RestWorkflow workflow = new RestWorkflow(getContext().getSession(), getContext());
        boolean success = workflow.addContentBlockCompound("testing");
        assertTrue("Expected to be able to add document", success);

    }

    @Test(expected = RestException.class)
    public void testAddCompoundTypeFail() throws Exception {


        final RestWorkflow workflow = new RestWorkflow(getContext().getSession(), "nonexistingNamesace",getContext());
        workflow.addContentBlockCompound("testing");

    }
}
