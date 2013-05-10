/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.channelmanager;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.container.HstComponentWindow;

/**
 * When a request is in {@link org.hippoecm.hst.core.request.HstRequestContext#isCmsRequest()} mode, extra info, for
 * example html comments or response headers, can be rendered per component window. The {@Link #append}
 * gets invoked before the actual {@link HstComponentWindow} writes to the {@link javax.servlet.http.HttpServletResponse}
 */
public interface ComponentWindowResponseAppender {

    /**
     * if any info needs to be added, like response headers or html comments, it can be written to the <code>response</code>
     * below
     */
    void process(final HstComponentWindow rootWindow,
                final HstComponentWindow rootRenderingWindow,
                final HstComponentWindow window,
                final HstRequest request,
                final HstResponse response);
}
