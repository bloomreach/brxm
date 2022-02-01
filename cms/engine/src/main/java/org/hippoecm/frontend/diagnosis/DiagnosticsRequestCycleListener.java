/*
 * Copyright 2015-2022 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.diagnosis;

import org.apache.wicket.Application;
import org.apache.wicket.request.cycle.IRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;
import org.hippoecm.frontend.Main;
import org.hippoecm.hst.diagnosis.HDC;
import org.hippoecm.hst.diagnosis.Task;
import org.hippoecm.hst.diagnosis.TaskLogFormatUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default {@link IRequestCycleListener} implementation to set diagnosis context and report monitoring logs.
 */
public class DiagnosticsRequestCycleListener implements IRequestCycleListener {

    private static final Logger log = LoggerFactory.getLogger(DiagnosticsRequestCycleListener.class);

    @Override
    public void onBeginRequest(RequestCycle cycle) {
        final DiagnosticsService diagnosticsService = HippoServiceRegistry.getService(DiagnosticsService.class);

        if (diagnosticsService != null) {
            final Main application = (Main) Application.get();

            if (diagnosticsService.isEnabledFor(cycle.getRequest())) {
                if (HDC.isStarted()) {
                    log.error("HDC was not cleaned up properly in previous request cycle for some reason. So clean up HDC to start new one.");
                    HDC.cleanUp();
                }

                Task rootTask = HDC.start(application.getPluginApplicationName());
                rootTask.setAttribute("request", cycle.getRequest().getUrl().toString());
            }
        }
    }

    @Override
    public void onEndRequest(RequestCycle cycle) {
        if (HDC.isStarted()) {
            try {
                final Task rootTask = HDC.getRootTask();
                rootTask.stop();

                final DiagnosticsService diagnosticsService = HippoServiceRegistry.getService(DiagnosticsService.class);
                final long threshold = diagnosticsService != null ? diagnosticsService.getThresholdMillisec() : -1;
                final int depth = diagnosticsService != null ? diagnosticsService.getDepth() : -1;
                final long unitThreshold = diagnosticsService != null ? diagnosticsService.getUnitThresholdMillisec() : -1;

                if (threshold > -1L && rootTask.getDurationTimeMillis() < threshold) {
                    log.debug("Skipping task '{}' because took only '{}' ms.",
                              rootTask.getName(), rootTask.getDurationTimeMillis());
                } else {
                    log.info("Diagnosis Summary:\n{}", TaskLogFormatUtils.getTaskLog(rootTask, depth, unitThreshold));
                }
            } finally {
                HDC.cleanUp();
            }
        }
    }
}
