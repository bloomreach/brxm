/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.container;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;

/**
 * AsynchronousComponentWindowRenderer
 * <P>
 * Component implementing this interface is used in page aggregation phase.
 * For example, <CODE>AggregationValve</CODE> may look up an AsynchronousComponentWindowRenderer for the specific
 * asynchronous rendering mode when rendering an HST component window and invoke methods of this interface
 * to render some tags for asynchronous rendering (e.g., AJAX script based or ESI based).
 * </P>
 */
public interface AsynchronousComponentWindowRenderer {

    /**
     * Invoked when processing the doBeforeRender phase of a component window which should be asynchronously rendered.
     * @param window
     * @param request
     * @param response
     */
    public void processWindowBeforeRender(HstComponentWindow window, HstRequest request, HstResponse response);

}
