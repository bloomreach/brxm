/*
 * Copyright 2015-2020 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.jquery.upload.single;

import java.util.List;

import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.panel.Panel;
import org.hippoecm.frontend.dialog.HippoForm;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.jquery.upload.FileUploadViolationException;
import org.hippoecm.frontend.plugins.jquery.upload.behaviors.FileUploadInfo;
import org.hippoecm.frontend.plugins.yui.upload.processor.FileUploadPreProcessorService;
import org.hippoecm.frontend.plugins.yui.upload.validation.FileUploadValidationService;

/**
 * The panel utilizing {@link SingleFileUploadWidget} to upload a single file automatically upon selection. It can be
 * reused by putting either in a div or a wicket:container (recommended because it removed a redundant div tag).
 */
public abstract class FileUploadPanel extends Panel {

    public FileUploadPanel(String id, final IPluginConfig pluginConfig, final FileUploadValidationService validator,
                           final FileUploadPreProcessorService fileUploadPreProcessorService) {
        super(id);

        final HippoForm form = new HippoForm("form");
        form.setMultiPart(true);

        form.add(new SingleFileUploadWidget("fileUpload", pluginConfig, validator,
                fileUploadPreProcessorService, true) {
            @Override
            protected void onBeforeUpload(final FileUploadInfo fileUploadInfo) {
                form.clearFeedbackMessages();
                FileUploadPanel.this.onBeforeUpload(fileUploadInfo);
            }

            @Override
            protected void onFileUpload(FileUpload fileUpload) throws FileUploadViolationException {
                FileUploadPanel.this.onFileUpload(fileUpload);
            }

            @Override
            protected void onUploadError(final FileUploadInfo fileUploadInfo) {
                final List<String> errorMessages = fileUploadInfo.getErrorMessages();
                if (!errorMessages.isEmpty()) {
                    // Added as info message: the file was not uploaded so the document is not in an invalid state.
                    // Saving the document is not blocked by this info message.
                    errorMessages.forEach(form::info);
                    log.debug("file {} contains errors: {}", fileUploadInfo.getFileName(), String.join(";", errorMessages));
                }
            }
        });
        add(form);
    }

    /**
     * The event is fired before processing the uploaded file.
     */
    protected void onBeforeUpload(final FileUploadInfo fileUploadInfo) {
    }

    public abstract void onFileUpload(final FileUpload fileUpload) throws FileUploadViolationException;
}
