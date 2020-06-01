/**
 * Copyright 2012-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.diagnosis;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestTaskLogFormatUtils {

    private static Logger log = LoggerFactory.getLogger(TestTaskLogFormatUtils.class);

    private TestTask task;

    @Before
    public void setUp() throws Exception {
        task = new TestTask(null, "root");
        task.setDurationTimeMillis(10000);

        TestTask subtask_1 = (TestTask) task.startSubtask("subtask_1");
        subtask_1.setDurationTimeMillis(5000);

        TestTask subtask_1_1 = (TestTask) subtask_1.startSubtask("subtask_1_1");
        subtask_1_1.setDurationTimeMillis(3000);

        TestTask subtask_1_1_1 = (TestTask) subtask_1_1.startSubtask("subtask_1_1_1");
        subtask_1_1_1.setDurationTimeMillis(1000);

        TestTask subtask_1_1_1_1 = (TestTask) subtask_1_1_1.startSubtask("subtask_1_1_1_1");
        subtask_1_1_1_1.setDurationTimeMillis(500);

        TestTask subtask_1_1_1_2 = (TestTask) subtask_1_1_1.startSubtask("subtask_1_1_1_2");
        subtask_1_1_1_2.setDurationTimeMillis(10);

        TestTask subtask_1_1_1_3 = (TestTask) subtask_1_1_1.startSubtask("subtask_1_1_1_3");
        subtask_1_1_1_3.setDurationTimeMillis(400);

        TestTask subtask_1_1_1_4 = (TestTask) subtask_1_1_1.startSubtask("subtask_1_1_1_4");
        subtask_1_1_1_4.setDurationTimeMillis(10);

        TestTask subtask_1_1_1_5 = (TestTask) subtask_1_1_1.startSubtask("subtask_1_1_1_5");
        subtask_1_1_1_5.setDurationTimeMillis(80);

        TestTask subtask_1_1_2 = (TestTask) subtask_1.startSubtask("subtask_1_1_2");
        subtask_1_1_2.setDurationTimeMillis(1000);

        TestTask subtask_1_1_3 = (TestTask) subtask_1.startSubtask("subtask_1_1_3");
        subtask_1_1_3.setDurationTimeMillis(1000);

        TestTask subtask_1_2 = (TestTask) subtask_1.startSubtask("subtask_1_2");
        subtask_1_2.setDurationTimeMillis(2000);

        TestTask subtask_2 = (TestTask) task.startSubtask("subtask_2");
        subtask_2.setDurationTimeMillis(5000);
    }

    @Test
    public void testGetTaskLog_withDefaults() throws Exception {
        String logData = TaskLogFormatUtils.getTaskLog(task);

        assertTrue(logData.contains("- root (10000ms):"));
        assertTrue(logData.contains("|- subtask_1 (5000ms):"));
        assertTrue(logData.contains("|  |- subtask_1_1 (3000ms):"));
        assertTrue(logData.contains("|  |  `- subtask_1_1_1 (1000ms):"));
        assertTrue(logData.contains("|  |     |- subtask_1_1_1_1 (500ms):"));
        assertTrue(logData.contains("|  |     |- subtask_1_1_1_2 (10ms):"));
        assertTrue(logData.contains("|  |     |- subtask_1_1_1_3 (400ms):"));
        assertTrue(logData.contains("|  |     |- subtask_1_1_1_4 (10ms):"));
        assertTrue(logData.contains("|  |     `- subtask_1_1_1_5 (80ms):"));
        assertTrue(logData.contains("|  |- subtask_1_1_2 (1000ms):"));
        assertTrue(logData.contains("|  |- subtask_1_1_3 (1000ms):"));
        assertTrue(logData.contains("|  `- subtask_1_2 (2000ms):"));
        assertTrue(logData.contains("`- subtask_2 (5000ms):"));
    }

    @Test
    public void testGetTaskLog_withMaxDepth() throws Exception {
        String logData = TaskLogFormatUtils.getTaskLog(task, -1);

        assertTrue(logData.contains("- root (10000ms):"));
        assertTrue(logData.contains("|- subtask_1 (5000ms):"));
        assertTrue(logData.contains("|  |- subtask_1_1 (3000ms):"));
        assertTrue(logData.contains("|  |  `- subtask_1_1_1 (1000ms):"));
        assertTrue(logData.contains("|  |     |- subtask_1_1_1_1 (500ms):"));
        assertTrue(logData.contains("|  |     |- subtask_1_1_1_2 (10ms):"));
        assertTrue(logData.contains("|  |     |- subtask_1_1_1_3 (400ms):"));
        assertTrue(logData.contains("|  |     |- subtask_1_1_1_4 (10ms):"));
        assertTrue(logData.contains("|  |     `- subtask_1_1_1_5 (80ms):"));
        assertTrue(logData.contains("|  |- subtask_1_1_2 (1000ms):"));
        assertTrue(logData.contains("|  |- subtask_1_1_3 (1000ms):"));
        assertTrue(logData.contains("|  `- subtask_1_2 (2000ms):"));
        assertTrue(logData.contains("`- subtask_2 (5000ms):"));

        logData = TaskLogFormatUtils.getTaskLog(task, 4);

        assertTrue(logData.contains("- root (10000ms):"));
        assertTrue(logData.contains("|- subtask_1 (5000ms):"));
        assertTrue(logData.contains("|  |- subtask_1_1 (3000ms):"));
        assertTrue(logData.contains("|  |  `- subtask_1_1_1 (1000ms):"));
        assertTrue(logData.contains("|  |     |- subtask_1_1_1_1 (500ms):"));
        assertTrue(logData.contains("|  |     |- subtask_1_1_1_2 (10ms):"));
        assertTrue(logData.contains("|  |     |- subtask_1_1_1_3 (400ms):"));
        assertTrue(logData.contains("|  |     |- subtask_1_1_1_4 (10ms):"));
        assertTrue(logData.contains("|  |     `- subtask_1_1_1_5 (80ms):"));
        assertTrue(logData.contains("|  |- subtask_1_1_2 (1000ms):"));
        assertTrue(logData.contains("|  |- subtask_1_1_3 (1000ms):"));
        assertTrue(logData.contains("|  `- subtask_1_2 (2000ms):"));
        assertTrue(logData.contains("`- subtask_2 (5000ms):"));

        logData = TaskLogFormatUtils.getTaskLog(task, 3);

        assertTrue(logData.contains("- root (10000ms):"));
        assertTrue(logData.contains("|- subtask_1 (5000ms):"));
        assertTrue(logData.contains("|  |- subtask_1_1 (3000ms):"));
        assertTrue(logData.contains("|  |  `- subtask_1_1_1 (1000ms):"));
        assertFalse(logData.contains("|  |     |- subtask_1_1_1_1 (500ms):"));
        assertFalse(logData.contains("|  |     |- subtask_1_1_1_2 (10ms):"));
        assertFalse(logData.contains("|  |     |- subtask_1_1_1_3 (400ms):"));
        assertFalse(logData.contains("|  |     |- subtask_1_1_1_4 (10ms):"));
        assertFalse(logData.contains("|  |     `- subtask_1_1_1_5 (80ms):"));
        assertTrue(logData.contains("|  |- subtask_1_1_2 (1000ms):"));
        assertTrue(logData.contains("|  |- subtask_1_1_3 (1000ms):"));
        assertTrue(logData.contains("|  `- subtask_1_2 (2000ms):"));
        assertTrue(logData.contains("`- subtask_2 (5000ms):"));

        logData = TaskLogFormatUtils.getTaskLog(task, 2);

        assertTrue(logData.contains("- root (10000ms):"));
        assertTrue(logData.contains("|- subtask_1 (5000ms):"));
        assertTrue(logData.contains("|  |- subtask_1_1 (3000ms):"));
        assertFalse(logData.contains("|  |  `- subtask_1_1_1 (1000ms):"));
        assertTrue(logData.contains("|  `- subtask_1_2 (2000ms):"));
        assertTrue(logData.contains("`- subtask_2 (5000ms):"));

        logData = TaskLogFormatUtils.getTaskLog(task, 1);

        assertTrue(logData.contains("- root (10000ms):"));
        assertTrue(logData.contains("|- subtask_1 (5000ms):"));
        assertFalse(logData.contains("|  |- subtask_1_1 (3000ms):"));
        assertTrue(logData.contains("`- subtask_2 (5000ms):"));

        logData = TaskLogFormatUtils.getTaskLog(task, 0);

        assertTrue(logData.contains("- root (10000ms):"));
    }

    @Test
    public void testGetTaskLog_withMaxDepthAndSubtaskThresholdMillisec() throws Exception {
        String logData = TaskLogFormatUtils.getTaskLog(task, -1, -1);

        assertTrue(logData.contains("- root (10000ms):"));
        assertTrue(logData.contains("|- subtask_1 (5000ms):"));
        assertTrue(logData.contains("|  |- subtask_1_1 (3000ms):"));
        assertTrue(logData.contains("|  |  `- subtask_1_1_1 (1000ms):"));
        assertTrue(logData.contains("|  |     |- subtask_1_1_1_1 (500ms):"));
        assertTrue(logData.contains("|  |     |- subtask_1_1_1_2 (10ms):"));
        assertTrue(logData.contains("|  |     |- subtask_1_1_1_3 (400ms):"));
        assertTrue(logData.contains("|  |     |- subtask_1_1_1_4 (10ms):"));
        assertTrue(logData.contains("|  |     `- subtask_1_1_1_5 (80ms):"));
        assertTrue(logData.contains("|  |- subtask_1_1_2 (1000ms):"));
        assertTrue(logData.contains("|  |- subtask_1_1_3 (1000ms):"));
        assertTrue(logData.contains("|  `- subtask_1_2 (2000ms):"));
        assertTrue(logData.contains("`- subtask_2 (5000ms):"));

        logData = TaskLogFormatUtils.getTaskLog(task, -1, 10);

        assertTrue(logData.contains("- root (10000ms):"));
        assertTrue(logData.contains("|- subtask_1 (5000ms):"));
        assertTrue(logData.contains("|  |- subtask_1_1 (3000ms):"));
        assertTrue(logData.contains("|  |  `- subtask_1_1_1 (1000ms):"));
        assertTrue(logData.contains("|  |     |- subtask_1_1_1_1 (500ms):"));
        assertTrue(logData.contains("|  |     |- subtask_1_1_1_2 (10ms):"));
        assertTrue(logData.contains("|  |     |- subtask_1_1_1_3 (400ms):"));
        assertTrue(logData.contains("|  |     |- subtask_1_1_1_4 (10ms):"));
        assertTrue(logData.contains("|  |     `- subtask_1_1_1_5 (80ms):"));
        assertTrue(logData.contains("|  |- subtask_1_1_2 (1000ms):"));
        assertTrue(logData.contains("|  |- subtask_1_1_3 (1000ms):"));
        assertTrue(logData.contains("|  `- subtask_1_2 (2000ms):"));
        assertTrue(logData.contains("`- subtask_2 (5000ms):"));

        logData = TaskLogFormatUtils.getTaskLog(task, -1, 80);

        assertTrue(logData.contains("- root (10000ms):"));
        assertTrue(logData.contains("|- subtask_1 (5000ms):"));
        assertTrue(logData.contains("|  |- subtask_1_1 (3000ms):"));
        assertTrue(logData.contains("|  |  `- subtask_1_1_1 (1000ms):"));
        assertTrue(logData.contains("|  |     |- subtask_1_1_1_1 (500ms):"));
        assertTrue(logData.contains("|  |     |- subtask_1_1_1_3 (400ms):"));
        assertFalse(logData.contains("|  |     |- subtask_1_1_1_4 (10ms):"));
        assertTrue(logData.contains("|  |     `- subtask_1_1_1_5 (80ms):"));
        assertTrue(logData.contains("|  |- subtask_1_1_2 (1000ms):"));
        assertTrue(logData.contains("|  |- subtask_1_1_3 (1000ms):"));
        assertTrue(logData.contains("|  `- subtask_1_2 (2000ms):"));
        assertTrue(logData.contains("`- subtask_2 (5000ms):"));

        logData = TaskLogFormatUtils.getTaskLog(task, -1, 400);

        assertTrue(logData.contains("- root (10000ms):"));
        assertTrue(logData.contains("|- subtask_1 (5000ms):"));
        assertTrue(logData.contains("|  |- subtask_1_1 (3000ms):"));
        assertTrue(logData.contains("|  |  `- subtask_1_1_1 (1000ms):"));
        assertTrue(logData.contains("|  |     |- subtask_1_1_1_1 (500ms):"));
        assertTrue(logData.contains("|  |     |- subtask_1_1_1_3 (400ms):"));
        assertFalse(logData.contains("|  |     |- subtask_1_1_1_4 (10ms):"));
        assertFalse(logData.contains("|  |     `- subtask_1_1_1_5 (80ms):"));
        assertTrue(logData.contains("|  |- subtask_1_1_2 (1000ms):"));
        assertTrue(logData.contains("|  |- subtask_1_1_3 (1000ms):"));
        assertTrue(logData.contains("|  `- subtask_1_2 (2000ms):"));
        assertTrue(logData.contains("`- subtask_2 (5000ms):"));

        logData = TaskLogFormatUtils.getTaskLog(task, -1, 500);

        assertTrue(logData.contains("- root (10000ms):"));
        assertTrue(logData.contains("|- subtask_1 (5000ms):"));
        assertTrue(logData.contains("|  |- subtask_1_1 (3000ms):"));
        assertTrue(logData.contains("|  |  `- subtask_1_1_1 (1000ms):"));
        assertTrue(logData.contains("|  |     |- subtask_1_1_1_1 (500ms):"));
        assertTrue(logData.contains("|  |- subtask_1_1_2 (1000ms):"));
        assertTrue(logData.contains("|  |- subtask_1_1_3 (1000ms):"));
        assertTrue(logData.contains("|  `- subtask_1_2 (2000ms):"));
        assertTrue(logData.contains("`- subtask_2 (5000ms):"));

        logData = TaskLogFormatUtils.getTaskLog(task, -1, 1000);

        assertTrue(logData.contains("- root (10000ms):"));
        assertTrue(logData.contains("|- subtask_1 (5000ms):"));
        assertTrue(logData.contains("|  |- subtask_1_1 (3000ms):"));
        assertTrue(logData.contains("|  |  `- subtask_1_1_1 (1000ms):"));
        assertFalse(logData.contains("|  |     |- subtask_1_1_1_1 (500ms):"));
        assertTrue(logData.contains("|  |- subtask_1_1_2 (1000ms):"));
        assertTrue(logData.contains("|  |- subtask_1_1_3 (1000ms):"));
        assertTrue(logData.contains("|  `- subtask_1_2 (2000ms):"));
        assertTrue(logData.contains("`- subtask_2 (5000ms):"));

        logData = TaskLogFormatUtils.getTaskLog(task, -1, 2000);

        assertTrue(logData.contains("- root (10000ms):"));
        assertTrue(logData.contains("|- subtask_1 (5000ms):"));
        assertTrue(logData.contains("|  |- subtask_1_1 (3000ms):"));
        assertTrue(logData.contains("|  `- subtask_1_2 (2000ms):"));
        assertTrue(logData.contains("`- subtask_2 (5000ms):"));

        logData = TaskLogFormatUtils.getTaskLog(task, -1, 3000);

        assertTrue(logData.contains("- root (10000ms):"));
        assertTrue(logData.contains("|- subtask_1 (5000ms):"));
        assertTrue(logData.contains("|  |- subtask_1_1 (3000ms):"));
        assertTrue(logData.contains("`- subtask_2 (5000ms):"));

        logData = TaskLogFormatUtils.getTaskLog(task, -1, 5000);

        assertTrue(logData.contains("- root (10000ms):"));
        assertTrue(logData.contains("|- subtask_1 (5000ms):"));
        assertTrue(logData.contains("`- subtask_2 (5000ms):"));

        logData = TaskLogFormatUtils.getTaskLog(task, -1, 10000);

        assertTrue(logData.contains("- root (10000ms):"));

        logData = TaskLogFormatUtils.getTaskLog(task, -1, Long.MAX_VALUE);

        assertTrue(logData.contains("- root (10000ms):"));
    }

    private static class TestTask extends DefaultTaskImpl {

        private long durationTimeMillis = -1L;

        TestTask(Task parentTask, String name) {
            super(parentTask, name);
        }

        @Override
        public long getDurationTimeMillis() {
            return durationTimeMillis;
        }

        public void setDurationTimeMillis(long durationTimeMillis) {
            this.durationTimeMillis = durationTimeMillis;
        }

        @Override
        protected Task createSubtask(final Task parentTask, final String name) {
            return new TestTask(parentTask, name);
        }
    }
}
