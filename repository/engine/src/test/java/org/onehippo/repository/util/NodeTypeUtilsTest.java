/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.repository.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.onehippo.repository.testutils.slf4j.LoggerRecordingWrapper;
import org.slf4j.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NodeTypeUtilsTest extends RepositoryTestCase {

    private Logger bootstrapLogger = NodeTypeUtils.log;
    private LoggerRecordingWrapper loggingRecorder;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        loggingRecorder = new LoggerRecordingWrapper(bootstrapLogger);
        NodeTypeUtils.log = loggingRecorder;
    }

    @Override
    @After
    public void tearDown() throws Exception {
        NodeTypeUtils.log = bootstrapLogger;
        super.tearDown();
    }

    @Test
    public void testReplaceNodeTypeDefinition() throws Exception {
        NodeTypeUtils.initializeNodeTypes(session, getClass().getResourceAsStream("/bootstrap/test.cnd"), "test.cnd");
        assertEquals(2, loggingRecorder.getDebugMessages().size());
        assertTrue(loggingRecorder.getDebugMessages().get(1).startsWith("Replacing"));
    }

}
