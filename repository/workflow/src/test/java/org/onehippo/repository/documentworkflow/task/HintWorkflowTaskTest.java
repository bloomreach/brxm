/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.documentworkflow.task;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.reviewedactions.HippoStdPubWfNodeType;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.documentworkflow.MockWorkflowContext;
import org.onehippo.repository.mock.MockNode;

/**
 * HintWorkflowTaskTest
 */
public class HintWorkflowTaskTest {

    private HintTask task;
    private DocumentHandle dm;

    @Before
    public void before() throws Exception {
        MockNode handle = MockNode.root().addMockNode("test", HippoNodeType.NT_HANDLE);
        MockNode liveVariant = handle.addMockNode(handle.getName(), HippoStdPubWfNodeType.HIPPOSTDPUBWF_DOCUMENT);
        liveVariant.setProperty(HippoStdNodeType.HIPPOSTD_STATE, "published");
        dm = new DocumentHandle(new MockWorkflowContext("testuser"), liveVariant);

        task = new HintTask();
        task.setDataModel(dm);
    }

    @Test
    public void testExecution() throws Exception {
        task.setHint("hint1");
        task.setValue("value1");
        task.execute();

        assertEquals("value1", dm.getHints().get("hint1"));

        task.setHint("hint1");
        task.setValue(null);
        task.execute();

        assertFalse(dm.getHints().containsKey("hint1"));
    }

}
