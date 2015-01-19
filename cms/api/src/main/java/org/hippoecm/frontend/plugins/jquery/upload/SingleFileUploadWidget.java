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

package org.hippoecm.frontend.plugins.jquery.upload;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.AbstractFileUploadWidget;
import org.hippoecm.frontend.plugins.yui.upload.validation.FileUploadValidationService;

/**
 * @author cngo
 * @version $Id$
 * @since 2015-01-05
 */
public abstract class SingleFileUploadWidget extends AbstractFileUploadWidget {

    public static final String COMPONENT_FILE_INPUT_ID = "fileInput";
    private final FileUploadField fileUploadField;

    /**
     * @param id
     * @see org.apache.wicket.Component#Component(String)
     */
    public SingleFileUploadWidget(final String id, final IPluginConfig pluginConfig, final FileUploadValidationService validator) {
        super(id, pluginConfig, validator);

        add(fileUploadField = new FileUploadField(COMPONENT_FILE_INPUT_ID));

        // Supporting auto upload feature
        fileUploadField.add(new AjaxFormSubmitBehavior("change") {});
    }

    /**
     * Invoke to handle submit files
     * @throws FileUploadViolationException
     */
    public void onSubmit() throws FileUploadViolationException {
        final List<FileUpload> uploads = fileUploadField.getFileUploads();
        if (uploads != null) {
            for (FileUpload upload : uploads) {
                process(upload);
            }
        }
    }
}
