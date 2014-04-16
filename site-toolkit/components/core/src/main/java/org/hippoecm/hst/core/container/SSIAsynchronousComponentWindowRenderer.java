/**
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.hst.core.component.HstURL;
import org.w3c.dom.Comment;

/**
 * Asynchronous component window rendering implementation rending SSI include comments.
 */
public class SSIAsynchronousComponentWindowRenderer implements AsynchronousComponentWindowRenderer {

    @Override
    public void processWindowBeforeRender(HstComponentWindow window, HstRequest request, HstResponse response) {
        HstURL compUrl = response.createComponentRenderingURL();
        final Comment ssiComment = response.createComment("#include virtual=\"" + compUrl + "\" ");
        response.addPreamble(ssiComment);
    }
}
