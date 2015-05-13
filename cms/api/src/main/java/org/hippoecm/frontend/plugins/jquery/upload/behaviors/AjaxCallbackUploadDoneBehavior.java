/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.frontend.plugins.jquery.upload.behaviors;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.wicket.ajax.json.JSONException;
import org.apache.wicket.ajax.json.JSONObject;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.TextRequestHandler;
import org.hippoecm.frontend.plugins.jquery.upload.FileUploadWidgetSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AjaxCallbackUploadDoneBehavior extends AbstractAjaxBehavior {
    private static final Logger log = LoggerFactory.getLogger(AjaxCallbackUploadDoneBehavior.class);

    public static final String APPLICATION_JSON = "application/json";

    private enum ResponseType {
        OK,
        FAILED
    }

    private final FileUploadWidgetSettings settings;

    public AjaxCallbackUploadDoneBehavior(final FileUploadWidgetSettings settings) {
        this.settings = settings;
    }

    /**
     * Handle notification from the file upload dialog. The notification contains number of files to be uploaded
     * in the following JSON format:
     * {
     *     total: #numberOfFiles
     * }
     *
     * The response is either of following:
     * {
     *     status: 'OK'|'FAILED'
     * }
     */
    @Override
    public void onRequest() {
        HttpServletRequest request = (HttpServletRequest) RequestCycle.get().getRequest().getContainerRequest();
        try {
            // The 'total' key contains expected #files uploaded.
            JSONObject json = new JSONObject(IOUtils.toString(request.getReader()));
            int numberOfFiles = json.getInt("total");
            if (numberOfFiles < 0 || numberOfFiles > settings.getMaxNumberOfFiles()) {
                log.error("Invalid notification parameter from jquery file upload dialog: numberOfFiles={}", numberOfFiles);
                response(ResponseType.FAILED);
                return;
            }
            log.debug("Number of files to be uploaded:{}", numberOfFiles);
            response(ResponseType.OK);
            onNotify(numberOfFiles);
        } catch (IOException | JSONException e) {
            log.error("Failed to process the close notification from jquery file upload dialog", e);
            response(ResponseType.FAILED);
        }
    }

    /**
     * Override this method to receive notification when uploading has done
     * @param numberOfFiles number of uploaded files
     */
    protected abstract void onNotify(final int numberOfFiles);

    private void response(final ResponseType responseType) {
        String content = String.format("{\"status\":\"%s\"}", responseType.name());
        TextRequestHandler textRequestHandler = new
                TextRequestHandler(APPLICATION_JSON, "UTF-8", content);
        RequestCycle.get().scheduleRequestHandlerAfterCurrent(textRequestHandler);
    }
}
