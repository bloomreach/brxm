/*
 *  Copyright 2014-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.autoreload;

import org.hippoecm.hst.core.channelmanager.AbstractComponentWindowResponseAppender;
import org.hippoecm.hst.core.channelmanager.ComponentWindowResponseAppender;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.container.HstComponentWindow;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.autoreload.AutoReloadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class AutoReloadResponseAppender extends AbstractComponentWindowResponseAppender implements ComponentWindowResponseAppender {

    private static final String AUTO_RELOAD_HEAD_KEY_HINT = AutoReloadResponseAppender.class.getName() + ".autoreload";

    private static final Logger log = LoggerFactory.getLogger(AutoReloadResponseAppender.class);

    @Override
    public void process(final HstComponentWindow rootWindow,
                        final HstComponentWindow rootRenderingWindow,
                        final HstComponentWindow window,
                        final HstRequest request,
                        final HstResponse response) {


        final boolean isComponentRenderingRequest = (request.getRequestContext().getBaseURL().getComponentRenderingWindowReferenceNamespace() != null);
        if (isComponentRenderingRequest) {
            log.debug("No auto-reload head element will be added for component rendering request");
            return;
        }

        final AutoReloadService autoReload = HippoServiceRegistry.getService(AutoReloadService.class);

        if (autoReload == null) {
            log.debug("No auto-reload service available, do not append auto-reload script to response");
            return;
        }
        if (!autoReload.isEnabled()) {
            log.debug("Auto-reload service is disabled, do not append auto-reload script to response");
            return;
        }

        // only process it for top window
        if (!isTopHstResponse(rootWindow, rootRenderingWindow, window)) {
            return;
        }

        if (!response.containsHeadElement(AUTO_RELOAD_HEAD_KEY_HINT)) {
            final Element autoReloadScript = getAutoReloadScriptElement(request, response, autoReload);
            if (autoReloadScript != null) {
                log.info("Append auto-reload script");
                response.addHeadElement(autoReloadScript, AUTO_RELOAD_HEAD_KEY_HINT);
            }
        }
    }

    private Element getAutoReloadScriptElement(final HstRequest request, final HstResponse response, final AutoReloadService autoReload) {
        try {
            final String script = autoReload.getJavaScript(request.getContextPath());
            final Element headElement = response.createElement("script");
            headElement.setAttribute("type", "text/javascript");
            headElement.setTextContent(script);
            return headElement;
        } catch (IllegalArgumentException e) {
            log.warn("Failed to append auto-reload script", e);
        }
        return null;
    }

}
