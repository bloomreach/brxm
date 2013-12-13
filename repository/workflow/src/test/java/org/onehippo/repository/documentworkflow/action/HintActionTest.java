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
package org.onehippo.repository.documentworkflow.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.commons.scxml2.Context;
import org.apache.commons.scxml2.Evaluator;
import org.apache.commons.scxml2.SCXMLExpressionException;
import org.apache.commons.scxml2.env.jexl.JexlContext;
import org.apache.commons.scxml2.env.jexl.JexlEvaluator;
import org.apache.commons.scxml2.model.ModelException;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.reviewedactions.HippoStdPubWfNodeType;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.documentworkflow.MockWorkflowContext;
import org.onehippo.repository.mock.MockNode;

/**
 * HinthintActionTest
 */
public class HintActionTest {

    private DocumentHandle dm;

    private Context context = new JexlContext();
    private Evaluator evaluator = new JexlEvaluator();
    
    private class MockHintAction extends HintAction {
        private static final long serialVersionUID = 1L;
        @Override
        protected Context getContext() {
            return context;
        }
        @SuppressWarnings("unchecked")
        @Override
        public <T> T eval(String expr) throws ModelException, SCXMLExpressionException {
            return (T) evaluator.eval(context, expr);
        }
    }

    @Before
    public void before() throws Exception {
        MockNode handle = MockNode.root().addMockNode("test", HippoNodeType.NT_HANDLE);
        MockNode liveVariant = handle.addMockNode(handle.getName(), HippoStdPubWfNodeType.HIPPOSTDPUBWF_DOCUMENT);
        liveVariant.setProperty(HippoStdNodeType.HIPPOSTD_STATE, "published");
        dm = new DocumentHandle(new MockWorkflowContext("testuser"), liveVariant);

        context.set("dm", dm);
        context.set("value1", "value1");
    }

    @Test
    public void testBasic() throws Exception {
        HintAction hintAction = new MockHintAction();
        hintAction.setHint("hint1");
        hintAction.setValue("value1");
        hintAction.execute(null, null, null, null, null);

        assertEquals("value1", dm.getHints().get("hint1"));

        dm.getHints().clear();

        hintAction = new MockHintAction();
        hintAction.setHint("hint1");
        hintAction.setValue(null);
        hintAction.execute(null, null, null, null, null);

        assertNull(dm.getHints().get("hint1"));
    }
}
