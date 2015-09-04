/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.util;

import java.util.BitSet;

import org.hippoecm.hst.diagnosis.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to get a pretty printed hierarchical task log
 *
 * @deprecated Use {@link org.hippoecm.hst.diagnosis.TaskLogFormatUtils} instead.
 */
@Deprecated
public class TaskLogFormatter {

    private static final Logger log = LoggerFactory.getLogger(TaskLogFormatter.class);

    /**
     * @return returns the <code>task</code> nicely hierarchical formatted
     */
    public static String getTaskLog(Task task) {
        return getTaskLog(task, -1);
    }

    /**
     *
     * @param task the task to log
     * @param maxDepth the maximum depth until how deep child tasks should be logged. <code>maxDepth</code> of <code>-1</code>
     *                 will log all descendant tasks, <code>maxDepth</code> of <code>0</code> only the rootTask, <code>maxDepth</code>
     *                 of <code>1</code> the rootTask plus its direct children, etc.
     * @return
     */
    public static String getTaskLog(Task task, final int maxDepth) {
        StringBuilder sb = new StringBuilder(256);
        appendTaskLog(sb, task, 0, new BitSet(0), false, maxDepth);
        return sb.toString();
    }

    private static void appendTaskLog(final StringBuilder sb,
                                      final Task task,
                                      final int depth,
                                      final BitSet bitset,
                                      final boolean lastChild,
                                      final int maxDepth) {
        if (maxDepth > -1 && depth > maxDepth) {
            return;
        }

        BitSet hidePipeAt = new BitSet(depth);
        hidePipeAt.or(bitset);
        for (int i = 0; i < depth; i++) {
            if (i > 0) {
                if (hidePipeAt.get(i)) {
                    sb.append(" ");
                } else {
                    sb.append("|");
                }
            }

            sb.append("  ");
        }
        if (depth > 0) {
            if (lastChild) {
                sb.append("`");
                hidePipeAt.set(depth);
            } else {
                sb.append("|");
            }
        }
        try {
            String msg = "- " + task.getName() + " (" + task.getDurationTimeMillis() + "ms): " + task.getAttributeMap();
            sb.append(msg).append('\n');
        } catch (Throwable e) {
            if (log.isDebugEnabled()) {
                log.warn("Exception during writing task", e);
            } else {
                log.warn("Exception during writing task : {}", e.toString());
            }
        }

        int count = 0;
        for (Task childTask : task.getChildTasks()) {
            count++;
            if (count == task.getChildTasks().size()) {
                appendTaskLog(sb, childTask, depth + 1,hidePipeAt, true, maxDepth);
            } else {
                appendTaskLog(sb, childTask, depth + 1,hidePipeAt, false, maxDepth);
            }
        }
    }
}
