/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.channelmanager.channeleditor;

import java.util.Map;
import java.util.stream.Collectors;

import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;

/**
 * Manages the picker dialog for imagelink fields. The dialog is used to select an image.
 * The behavior can be called by the frontend to open the dialog.
 * When done the method 'ChannelEditor#onImagePicked' is called.
 * Cancelling the dialog calls 'ChannelEditor#onImagePickCancelled'.
 */
class ImagePickerManager extends PickerManager {

    private static final IPluginConfig DEFAULT_PICKER_CONFIG = new JavaPluginConfig();

    private final AbstractAjaxBehavior behavior;

    ImagePickerManager(final IPluginContext context, final String channelEditorId) {
        super(DEFAULT_PICKER_CONFIG);
        behavior = new GalleryPickerDialogBehavior(context);
    }

    AbstractAjaxBehavior getBehavior() {
        return behavior;
    }

    private class GalleryPickerDialogBehavior extends AbstractAjaxBehavior {

        private final IPluginContext context;

        public GalleryPickerDialogBehavior(final IPluginContext context) {
            this.context = context;
        }

        @Override
        public void onRequest() {
            Map<String, String> params = getParameters();
            initPicker(params);

            // TODO: show the gallery picker dialog
        }

        protected Map<String, String> getParameters() {
            final Request request = RequestCycle.get().getRequest();
            final IRequestParameters parameters = request.getPostParameters();
            return parameters.getParameterNames()
                    .stream()
                    .collect(Collectors.toMap(
                            name -> name,
                            name -> parameters.getParameterValue(name).toString()
                    ));
        }
    }

}
