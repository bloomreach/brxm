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
 */
public class TaskLogFormatter {

    private static final Logger log = LoggerFactory.getLogger(TaskLogFormatter.class);

    /**
     * @return returns the <code>task</code> nicely hierarchical formatted
     */
    public static String getTaskLog(Task task) {
        StringBuilder sb = new StringBuilder(256);
        appendTaskLog(sb, task, 0, new BitSet(0), false);
        return sb.toString();
    }

    private static void appendTaskLog(StringBuilder sb, Task task, int depth, final BitSet bitset, boolean lastChild) {
        
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
                appendTaskLog(sb, childTask, depth + 1,hidePipeAt, true);
            } else {
                appendTaskLog(sb, childTask, depth + 1,hidePipeAt, false);
            }
        }
    }
}
