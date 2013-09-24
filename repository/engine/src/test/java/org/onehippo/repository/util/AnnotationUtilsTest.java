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
package org.onehippo.repository.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Map;

import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.junit.Test;
import org.onehippo.repository.api.annotation.WorkflowAction;
import org.onehippo.repository.util.AnnotationUtils;

/**
 * AnnotationUtilsTest
 */
public class AnnotationUtilsTest {

    @Test
    public void testFindWorkflowActionAnnotation() throws Exception {
        Workflow workflow = new WorkflowImpl() {
            @Override
            public Map<String,Serializable> hints() {
                return null;
            }
        };

        Method hintsMethod = workflow.getClass().getMethod("hints", null);
        WorkflowAction wfActionAnno = hintsMethod.getAnnotation(WorkflowAction.class);
        assertNull(wfActionAnno);
        wfActionAnno = AnnotationUtils.findMethodAnnotation(hintsMethod, WorkflowAction.class);
        assertNotNull(wfActionAnno);
        assertFalse(wfActionAnno.loggable());
    }

}
