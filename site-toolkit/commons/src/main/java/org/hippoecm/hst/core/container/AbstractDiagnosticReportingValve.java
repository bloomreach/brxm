/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.hst.container.valves.AbstractOrderableValve;
import org.hippoecm.hst.diagnosis.HDC;
import org.hippoecm.hst.diagnosis.Task;

public abstract class AbstractDiagnosticReportingValve extends AbstractOrderableValve {

    @Override
    public void invoke(ValveContext context) throws ContainerException {
        if (context.getServletRequest().getAttribute(ContainerConstants.HST_FORWARD_PATH_INFO) != null) {
            // continue
            context.invokeNext();
            return;
        }

        if (HDC.isStarted()) {
            HDC.getRootTask().stop();
            logDiagnosticSummary(context, HDC.getRootTask());
        }

        // continue
        context.invokeNext();
    }

    protected abstract void logDiagnosticSummary(final ValveContext context, final Task rootTask);

}
