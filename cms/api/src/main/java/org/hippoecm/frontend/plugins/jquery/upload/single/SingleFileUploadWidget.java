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

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.behavior.IBehaviorListener;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.interpolator.MapVariableInterpolator;
import org.apache.wicket.util.upload.FileItem;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.jquery.upload.AbstractFileUploadWidget;
import org.hippoecm.frontend.plugins.jquery.upload.FileUploadViolationException;
import org.hippoecm.frontend.plugins.jquery.upload.behaviors.AjaxCallbackUploadDoneBehavior;
import org.hippoecm.frontend.plugins.jquery.upload.behaviors.AjaxFileUploadBehavior;
import org.hippoecm.frontend.plugins.jquery.upload.behaviors.FileUploadInfo;
import org.hippoecm.frontend.plugins.jquery.upload.multiple.FileUploadWidget;
import org.hippoecm.frontend.plugins.yui.upload.validation.FileUploadValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The single-file-upload widget using jquery-file-upload
 */
public abstract class SingleFileUploadWidget extends AbstractFileUploadWidget {
    final Logger log = LoggerFactory.getLogger(SingleFileUploadWidget.class);

    private final String UPLOADING_SCRIPT_TEMPLATE = "$('#${componentMarkupId}').data('blueimp-fileupload').uploadFile();";

    private SingleFileUploadBar fileUploadBar;
    private AbstractAjaxBehavior ajaxCallbackDoneBehavior;
    private AjaxFileUploadBehavior ajaxFileUploadBehavior;

    public SingleFileUploadWidget(final String uploadPanel, final IPluginConfig pluginConfig, final FileUploadValidationService validator,
                                  final boolean autoUpload) {
        super(uploadPanel, pluginConfig, validator);
        this.settings.setAutoUpload(autoUpload);

        createComponents();
    }

    public SingleFileUploadWidget(final String uploadPanel, final IPluginConfig pluginConfig, final FileUploadValidationService validator) {
        super(uploadPanel, pluginConfig, validator);
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

        String uploadDoneNotificationUrl = ajaxCallbackDoneBehavior.getCallbackUrl().toString();
        settings.setUploadDoneNotificationUrl(uploadDoneNotificationUrl);
        super.onBeforeRender();
    }

    private void createComponents() {


        add(ajaxFileUploadBehavior = new AjaxFileUploadBehavior(this) {
            @Override
            protected void process(final FileUpload fileUpload) throws FileUploadViolationException {
                SingleFileUploadWidget.this.process(fileUpload);
            }

            @Override
            protected void onAfterUpload(final FileItem file, final FileUploadInfo fileUploadInfo) {
                SingleFileUploadWidget.this.onAfterUpload(file, fileUploadInfo);
            }

            protected void onUploadError(final FileUploadInfo fileUploadInfo) {
                SingleFileUploadWidget.this.onUploadError(fileUploadInfo);
            }
        });

        add(ajaxCallbackDoneBehavior = new AjaxCallbackUploadDoneBehavior(settings) {
            @Override
            protected void onNotify(final int numberOfUploadedFiles) {
                if (numberOfUploadedFiles != 1) {
                    log.error("The widget '{}' should be used for uploading only a single file. Please use {} for multiple file uploads",
                            SingleFileUploadWidget.class.getName(), FileUploadWidget.class.getName());
                }
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
