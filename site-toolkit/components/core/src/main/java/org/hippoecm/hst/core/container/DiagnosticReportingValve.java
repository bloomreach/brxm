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
package org.hippoecm.hst.core.container;

import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.hosting.VirtualHostsService;
import org.hippoecm.hst.diagnosis.Task;
import org.hippoecm.hst.diagnosis.TaskLogFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiagnosticReportingValve extends AbstractDiagnosticReportingValve {

    private static Logger log = LoggerFactory.getLogger(DiagnosticReportingValve.class);

    protected void logDiagnosticSummary(final ValveContext context, final Task rootTask) {
        if (log.isInfoEnabled()) {

            final VirtualHosts virtualHosts = context.getRequestContext().getResolvedMount().getMount().getVirtualHost().getVirtualHosts();
            if (virtualHosts instanceof VirtualHostsService) {
                final VirtualHostsService virtualHostsService = (VirtualHostsService) virtualHosts;
                final long threshold = virtualHostsService.getDiagnosticsThresholdMillis();
                if (threshold > -1) {
                    // only log when threshold exceeded
                    if (rootTask.getDurationTimeMillis() < threshold) {
                        log.debug("Skipping task '{}' because took only '{}' ms.", rootTask.getName(), rootTask.getDurationTimeMillis());
                        return;
                    }
                }

                final int diagnosticsDepth = virtualHostsService.getDiagnosticsDepth();
                log.info("Diagnostic Summary:\n{}", TaskLogFormatUtils.getTaskLog(rootTask, diagnosticsDepth));
            } else {
                log.info("Diagnostic Summary:\n{}", TaskLogFormatUtils.getTaskLog(rootTask));
            }
        }
    }

}
