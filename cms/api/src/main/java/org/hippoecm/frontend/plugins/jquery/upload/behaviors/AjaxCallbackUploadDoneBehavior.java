/*
 * Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.cycle.RequestCycle;

public class AjaxCallbackUploadDoneBehavior extends AbstractDefaultAjaxBehavior {
    @Override
    protected void respond(final AjaxRequestTarget target) {
        final IRequestParameters requestParameters = RequestCycle.get().getRequest().getRequestParameters();
        final int numberOfUploadedFiles = requestParameters.getParameterValue("numberOfFiles").toInt(0);
        final boolean error = requestParameters.getParameterValue("error").toBoolean(false);

        onNotify(target, numberOfUploadedFiles, error);
    }

    /**
     * Override this method to receive notification when uploading has been done.
     * @param target
     * @param numberOfFiles
     * @param error
     */
    protected void onNotify(final AjaxRequestTarget target, final int numberOfFiles, final boolean error) {}
}
