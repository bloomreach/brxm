/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.hst.core.component.HstResponseState;

/**
 * Page model aggregation valve, to write a JSON model from the aggregated data for a page request.
 */
public class SelectiveRenderingAggregationValve extends AggregationValve {

    /**
     * Overrides <code>AggregationValve#processWindowsRender()</code> to remove any {@link HstResponseState} preamble or
     * epilogue comments since for the selective rendering we do not want those in the output
     */
    @Override
    protected void writeAggregatedOutput(final ValveContext context, final HstComponentWindow rootRenderingWindow)
            throws ContainerException {

        clearPreamblesAndEpilogues(rootRenderingWindow);
        super.writeAggregatedOutput(context, rootRenderingWindow);
    }

    private void clearPreamblesAndEpilogues(final HstComponentWindow window) {

        final HstResponseState responseState = window.getResponseState();
        responseState.clearPreambleComments();
        responseState.clearEpilogueComments();

        for (HstComponentWindow child : window.getChildWindowMap().values()) {
            clearPreamblesAndEpilogues(child);
        }
    }

}