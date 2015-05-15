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

package org.hippoecm.frontend.plugins.jquery.upload.single;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.panel.Panel;
import org.hippoecm.frontend.dialog.HippoForm;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.jquery.upload.FileUploadViolationException;
import org.hippoecm.frontend.plugins.jquery.upload.behaviors.FileUploadInfo;
import org.hippoecm.frontend.plugins.yui.upload.validation.FileUploadValidationService;

/**
 * The panel utilizing {@link JQuerySingleFileUploadWidget} to upload a single file automatically upon selection.
 */
public abstract class FileUploadPanel extends Panel {
    private static final long serialVersionUID = 1L;

    public FileUploadPanel(String id, final IPluginConfig pluginConfig, final FileUploadValidationService validator) {
        super(id);

        final HippoForm form = new HippoForm("form");
        form.setMultiPart(true);

        form.add(new JQuerySingleFileUploadWidget("fileUpload", pluginConfig, validator, true) {
            private static final long serialVersionUID = 1L;
            @Override
            protected void onFileUpload(FileUpload fileUpload) throws FileUploadViolationException {
                form.clearFeedbackMessages();
                FileUploadPanel.this.onFileUpload(fileUpload);
            }

            @Override
            protected void onUploadError(final FileUploadInfo fileUploadInfo) {
                form.clearFeedbackMessages();
                final List<String> errorMessages = fileUploadInfo.getErrorMessages();
                if (!errorMessages.isEmpty()) {
                    errorMessages.forEach(form::error);
                    log.error("file {} contains errors: {}", fileUploadInfo.getFileName(), StringUtils.join(errorMessages, ";"));
                }
            }
        });
        add(form);
    }

    public abstract void onFileUpload(final FileUpload fileUpload) throws FileUploadViolationException;
}
