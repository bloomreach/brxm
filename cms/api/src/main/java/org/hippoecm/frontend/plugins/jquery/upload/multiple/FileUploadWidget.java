/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.jquery.upload.multiple;

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.IBehaviorListener;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.interpolator.MapVariableInterpolator;
import org.apache.wicket.util.upload.FileItem;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.jquery.upload.AbstractFileUploadWidget;
import org.hippoecm.frontend.plugins.jquery.upload.FileUploadViolationException;
import org.hippoecm.frontend.plugins.jquery.upload.behaviors.AjaxCallbackUploadDoneBehavior;
import org.hippoecm.frontend.plugins.jquery.upload.behaviors.AjaxFileUploadBehavior;
import org.hippoecm.frontend.plugins.jquery.upload.behaviors.FileUploadInfo;
import org.hippoecm.frontend.plugins.yui.upload.validation.FileUploadValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The multi-files upload widget.
 * @see AbstractFileUploadWidget
 */
public abstract class FileUploadWidget extends AbstractFileUploadWidget {

    private static final Logger log = LoggerFactory.getLogger(FileUploadWidget.class);

    private static final String UPLOADING_SCRIPT_TEMPLATE = "$('#${componentMarkupId}').data('blueimp-fileupload').uploadFiles()";

    private FileUploadBar fileUploadBar;

    private AbstractDefaultAjaxBehavior ajaxCallbackUploadDoneBehavior;
    private AjaxFileUploadBehavior ajaxFileUploadBehavior;
    private AbstractDefaultAjaxBehavior ajaxCallbackSelectionChangeBehavior;

    protected void onFileUploadResponse(final ServletWebRequest request, final Map<String, FileUploadInfo> uploadedFiles) {
    }

    public FileUploadWidget(final String uploadPanel, final IPluginConfig pluginConfig, final FileUploadValidationService validator) {
        super(uploadPanel, pluginConfig, validator);

        createComponents();
    }

    public String getUploadScript() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("componentMarkupId", fileUploadBar.getMarkupId());

        return MapVariableInterpolator.interpolate(UPLOADING_SCRIPT_TEMPLATE, variables);
    }

    private void createComponents() {
        add(ajaxFileUploadBehavior = new AjaxFileUploadBehavior(this) {
            @Override
            protected void onBeforeUpload(final FileUploadInfo fileUploadInfo) {
                FileUploadWidget.this.onBeforeUpload(fileUploadInfo);
            }

            @Override
            protected void process(final FileUpload fileUpload) throws FileUploadViolationException {
                FileUploadWidget.this.process(fileUpload);
            }

            @Override
            protected void onResponse(final ServletWebRequest request, final Map<String, FileUploadInfo> uploadedFiles) {
                FileUploadWidget.this.onFileUploadResponse(request, uploadedFiles);
            }

            @Override
            protected void onAfterUpload(final FileItem file, final FileUploadInfo fileUploadInfo) {
                FileUploadWidget.this.onAfterUpload(file, fileUploadInfo);
            }

            protected void onUploadError(final FileUploadInfo fileUploadInfo) {
                FileUploadWidget.this.onUploadError(fileUploadInfo);
            }

        });

        add(ajaxCallbackUploadDoneBehavior = new AjaxCallbackUploadDoneBehavior() {
            @Override
            protected void onNotify(final AjaxRequestTarget target, final int numberOfFiles, final boolean error) {
                FileUploadWidget.this.onFinished(target, numberOfFiles, error);
                // backward compatible call
                FileUploadWidget.this.onFinished();
            }
        });

        add(ajaxCallbackSelectionChangeBehavior = new AbstractDefaultAjaxBehavior() {
            @Override
            protected void respond(final AjaxRequestTarget target) {
                final int numberOfFiles = RequestCycle.get().getRequest().getRequestParameters().getParameterValue("numberOfFiles").toInt();
                FileUploadWidget.this.onSelectionChange(target, numberOfFiles);
            }
        });

        // The buttons toolbar. Mandatory
        fileUploadBar = new FileUploadBar("fileUploadBar", settings);
        add(fileUploadBar);

        // The template used by jquery.fileupload-ui.js to show the files
        // scheduled for upload (i.e. the added files).
        // Optional
        FileUploadTemplate uploadTemplate = new FileUploadTemplate("uploadTemplate");
        add(uploadTemplate);

        // The template used by jquery.fileupload-ui.js to show the uploaded files
        // Optional
        FileDownloadTemplate downloadTemplate = new FileDownloadTemplate("downloadTemplate");
        add(downloadTemplate);
    }

    @Override
    protected void onBeforeRender() {
        // Obtain callback urls used for uploading files & notification
        final String uploadUrl = urlFor(ajaxFileUploadBehavior, IBehaviorListener.INTERFACE, new PageParameters()).toString();
        settings.setUploadUrl(uploadUrl);

        final String uploadDoneNotificationUrl = ajaxCallbackUploadDoneBehavior.getCallbackUrl().toString();
        settings.setUploadDoneNotificationUrl(uploadDoneNotificationUrl);

        final String selectionChangeNotificationUrl = ajaxCallbackSelectionChangeBehavior.getCallbackUrl().toString();
        settings.setSelectionChangeNotificationUrl(selectionChangeNotificationUrl);
        super.onBeforeRender();
    }

    @Override
    protected void onAfterUpload(final FileItem file, final FileUploadInfo fileUploadInfo) {
        if (log.isDebugEnabled()) {
            log.debug("Uploaded file: #{} {}", fileUploadInfo.getFileName());
        }
    }

    @Override
    protected void onSelectionChange(final AjaxRequestTarget target, final int numberOfFiles) {
    }
}
