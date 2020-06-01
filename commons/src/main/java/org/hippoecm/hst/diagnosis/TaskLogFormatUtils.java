/*
 *  Copyright 2012-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.diagnosis;

import java.util.BitSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to get a pretty printed hierarchical task log.
 */
public class TaskLogFormatUtils {

    private static final Logger log = LoggerFactory.getLogger(TaskLogFormatUtils.class);

    /**
     * Returns a nicely formatted string showing all the tasks hierarchically.
     * @return returns the <code>task</code> nicely hierarchical formatted
     */
    public static String getTaskLog(Task task) {
        return getTaskLog(task, -1);
    }

    /**
     * Returns a formatted string from the <code>task</code> including its descendant tasks.
     * The log should include descendant tasks only in <code>maxDepth</code> level at max.
     * When <code>maxDepth</code> is set to a negative value, all the descendant tasks will be included.
     * @param task the task to log
     * @param maxDepth the maximum depth until how deep child tasks should be logged. A negative <code>maxDepth</code> value
     *                 will log all descendant tasks, <code>maxDepth</code> of <code>0</code> only the rootTask, <code>maxDepth</code>
     *                 of <code>1</code> the rootTask plus its direct children, etc.
     * @return
     */
    public static String getTaskLog(Task task, final int maxDepth) {
        StringBuilder sb = new StringBuilder(256);
        appendTaskLog(sb, task, 0, new BitSet(0), false, maxDepth, -1);
        return sb.toString();
    }

    /**
     * Returns a formatted string from the <code>task</code> including its descendant tasks.
     * The log should include descendant tasks only in <code>maxDepth</code> level at max.
     * When <code>maxDepth</code> is set to a negative value, all the descendant tasks will be included.
     * Also, the log should include descendant tasks only a descendant task takes more time than
     * <code>subtaskThresholdMillisec</code> in milliseconds.
     * When <code>subtaskThresholdMillisec</code> is set to a non-negative value, only descendant tasks which take not less
     * time than <code>taskThresholdMillisec</code> will be included.
     * @param task the task to log
     * @param maxDepth the maximum depth until how deep child tasks should be logged. A negative <code>maxDepth</code> value
     *                 will log all descendant tasks, <code>maxDepth</code> of <code>0</code> only the rootTask, <code>maxDepth</code>
     *                 of <code>1</code> the rootTask plus its direct children, etc.
     * @param subtaskThresholdMillisec the threshold time milliseconds to include logs of subtasks under <code>task</code>.
     * @return
     */
    public static String getTaskLog(Task task, final int maxDepth, final long subtaskThresholdMillisec) {
        StringBuilder sb = new StringBuilder(256);
        appendTaskLog(sb, task, 0, new BitSet(0), false, maxDepth, subtaskThresholdMillisec);
        return sb.toString();
    }

    private static void appendTaskLog(final StringBuilder sb,
                                      final Task task,
                                      final int depth,
                                      final BitSet bitset,
                                      final boolean lastChild,
                                      final int maxDepth,
                                      final long subtaskThresholdMillisec) {
        if (maxDepth > -1 && depth > maxDepth) {
            return;
        }

        final long durationTimeMillis = task.getDurationTimeMillis();

        if (depth > 0 && subtaskThresholdMillisec > -1) {
            if (durationTimeMillis < subtaskThresholdMillisec) {
                return;
            }
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
            String msg = "- " + task.getName() + " (" + durationTimeMillis + "ms): " + task.getAttributeMap();
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
                appendTaskLog(sb, childTask, depth + 1,hidePipeAt, true, maxDepth, subtaskThresholdMillisec);
            } else {
                appendTaskLog(sb, childTask, depth + 1,hidePipeAt, false, maxDepth, subtaskThresholdMillisec);
            }
        }
    }
}
