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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.fileupload.FileItem;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.IBehaviorListener;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.interpolator.MapVariableInterpolator;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.jquery.upload.AbstractFileUploadWidget;
import org.hippoecm.frontend.plugins.jquery.upload.FileUploadViolationException;
import org.hippoecm.frontend.plugins.jquery.upload.behaviors.AjaxFileUploadBehavior;
import org.hippoecm.frontend.plugins.jquery.upload.behaviors.FileUploadInfo;
import org.hippoecm.frontend.plugins.yui.upload.processor.FileUploadPreProcessorService;
import org.hippoecm.frontend.plugins.yui.upload.validation.FileUploadValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The single-file-upload widget using jquery-file-upload
 * @see AbstractFileUploadWidget
 */
public abstract class SingleFileUploadWidget extends AbstractFileUploadWidget {
    final Logger log = LoggerFactory.getLogger(SingleFileUploadWidget.class);

    private final String UPLOADING_SCRIPT_TEMPLATE = "$('#${componentMarkupId}').data('blueimp-fileupload').uploadFile();";

    private static final String WORKFLOW_CATEGORY = "cms";
    private static final String INTERACTION_TYPE = "resource";
    private static final String ACTION_UPLOAD = "upload";

    private SingleFileUploadBar fileUploadBar;
    private AjaxFileUploadBehavior ajaxFileUploadBehavior;

    public SingleFileUploadWidget(final String uploadPanel, final IPluginConfig pluginConfig,
                                  final FileUploadValidationService validator,
                                  final FileUploadPreProcessorService fileUploadPreProcessorService,
                                  final boolean autoUpload) {
        super(uploadPanel, pluginConfig, validator, fileUploadPreProcessorService);
        this.settings.setAutoUpload(autoUpload);

        createComponents();
    }

    public SingleFileUploadWidget(final String uploadPanel, final IPluginConfig pluginConfig,
                                  final FileUploadValidationService validator,
                                  final FileUploadPreProcessorService fileUploadPreProcessorService) {
        super(uploadPanel, pluginConfig, validator, fileUploadPreProcessorService);
        createComponents();
    }

    @Override
    public String getUploadScript() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("componentMarkupId", fileUploadBar.getMarkupId());

        return MapVariableInterpolator.interpolate(UPLOADING_SCRIPT_TEMPLATE, variables);
    }

    @Override
    protected void onBeforeRender() {
        // Obtain callback urls used for uploading files & notification
        String uploadUrl = urlFor(ajaxFileUploadBehavior, IBehaviorListener.INTERFACE, new PageParameters()).toString();
        settings.setUploadUrl(uploadUrl);

        super.onBeforeRender();
    }

    private void createComponents() {


        add(ajaxFileUploadBehavior = new AjaxFileUploadBehavior(this) {
            @Override
            protected void onBeforeUpload(final FileUploadInfo fileUploadInfo) {
                SingleFileUploadWidget.this.onBeforeUpload(fileUploadInfo);
            }

            @Override
            protected void process(final FileUpload fileUpload) throws FileUploadViolationException {
                SingleFileUploadWidget.this.process(fileUpload);
            }

            @Override
            protected void onAfterUpload(final FileItem file, final FileUploadInfo fileUploadInfo) {
                SingleFileUploadWidget.this.onAfterUpload(file, fileUploadInfo);
                final List<String> errorMessages = fileUploadInfo.getErrorMessages();
                // only fire event if there are no error messages
                if (errorMessages != null && errorMessages.size() == 0) {
                    MarkupContainer markupContainer = getParent();
                    BinaryContentEventLogger.fireUploadEvent(markupContainer, WORKFLOW_CATEGORY, INTERACTION_TYPE, ACTION_UPLOAD);
                }
            }

            protected void onUploadError(final FileUploadInfo fileUploadInfo) {
                SingleFileUploadWidget.this.onUploadError(fileUploadInfo);
            }

            @Override
            protected void validate(final FileUpload fileUpload) throws FileUploadViolationException {
                SingleFileUploadWidget.this.validate(fileUpload);
            }

            @Override
            protected FileUpload preProcess(FileItem fileItem, final FileUpload originalFileUpload) throws Exception {
                return SingleFileUploadWidget.this.preProcess(fileItem, originalFileUpload);
            }
        });

        fileUploadBar = new SingleFileUploadBar("fileUploadBar", settings) {
            protected void onChange(final AjaxRequestTarget target) {
                SingleFileUploadWidget.this.onChange(target);
            }
        };
        add(fileUploadBar);
    }

    protected void onChange(final AjaxRequestTarget target) {}
}
