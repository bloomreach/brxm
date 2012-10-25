/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.hst.core.container;

import org.hippoecm.hst.diagnosis.HDC;
import org.hippoecm.hst.diagnosis.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * DiagnosticReportingValve
 */
public class DiagnosticReportingValve extends AbstractValve {

    private static Logger log = LoggerFactory.getLogger(DiagnosticReportingValve.class);

    @Override
    public void invoke(ValveContext context) throws ContainerException {
        if (HDC.isStarted()) {
            HDC.getRootTask().stop();
            logDiagnosticSummary();
        }

        // continue
        context.invokeNext();
    }

    private void logDiagnosticSummary() {
        if (log.isInfoEnabled()) {
            StringBuilder sb = new StringBuilder(256);
            Task rootTask = HDC.getRootTask();
            appendTaskLog(sb, rootTask, 0);

            log.info("Diagnostic Summary:\n{}", sb.toString());
        }
    }

    private void appendTaskLog(StringBuilder sb, Task task, int depth) {
        for (int i = 0; i < depth; i++) {
            sb.append("  ");
        }

        String log = "- " + task.getName() + " (" + task.getDurationTimeMillis() + "ms): " + task.getAttributeMap();
        sb.append(log).append('\n');

        for (Task childTask : task.getChildTasks()) {
            appendTaskLog(sb, childTask, depth + 1);
        }
    }

}
