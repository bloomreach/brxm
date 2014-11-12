/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.yui.upload;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.request.resource.CssResourceReference;
import org.hippoecm.frontend.plugins.yui.flash.FlashVersion;
import org.hippoecm.frontend.plugins.yui.upload.ajax.AjaxMultiFileUploadComponent;
import org.hippoecm.frontend.plugins.yui.upload.ajax.AjaxMultiFileUploadSettings;
import org.hippoecm.frontend.plugins.yui.upload.multifile.MultiFileUploadComponent;
import org.hippoecm.frontend.plugins.yui.upload.validation.DefaultUploadValidationService;
import org.hippoecm.frontend.plugins.yui.upload.validation.FileUploadValidationService;
import org.hippoecm.frontend.plugins.yui.webapp.WebAppBehavior;
import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.validation.ValidationException;
import org.hippoecm.frontend.validation.Violation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Widget for uploading files. This widget allows both flash and non-flash uploads based on the configuration. By
 * default the flash upload is used. For more configuration options please take a look at {@link
 * FileUploadWidgetSettings}.
 */
public class FileUploadWidget extends Panel {

    private static final long serialVersionUID = 1L;
    private static final CssResourceReference UPLOAD_WIDGET_STYLESHEET = new CssResourceReference(FileUploadWidget.class, "FileUploadWidget.css");

    final Logger log = LoggerFactory.getLogger(FileUploadWidget.class);

    private static final String COMPONENT_ID = "component";

    private Panel panel;
    private FileUploadWidgetSettings settings;
    private FileUploadValidationService validator;
    private List<Violation> violations;

    private final static FlashVersion VALID_FLASH = new FlashVersion(9, 0, 45);
    private FlashVersion detectedFlash;

    public FileUploadWidget(String id, FileUploadWidgetSettings settings) {
        this(id, settings, null);
    }

    public FileUploadWidget(String id, FileUploadWidgetSettings settings, FileUploadValidationService validator) {
        super(id);
        setOutputMarkupId(true);

        if (settings == null) {
           settings = new FileUploadWidgetSettings();
        }

        if (validator == null) {
            validator = new DefaultUploadValidationService();
        }

        String[] allowedExtensions = settings.getFileExtensions();
        if (allowedExtensions.length > 0) {
            log.warn("The allowed extensions configured in the upload plugin are used instead of those configured in the validator service for the sake of backwards compatibility.");
            validator.setAllowedExtensions(allowedExtensions);
        } else {
            settings.setFileExtensions(validator.getAllowedExtensions());
        }

        this.violations = new LinkedList<Violation>();
        this.settings = settings;
        this.validator = validator;

        add(panel = new EmptyPanel(COMPONENT_ID));
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(UPLOAD_WIDGET_STYLESHEET));
    }

    /**
     * Check if the client supports flash by looking up the WebAppBehavior in the page which test the client for flash
     * on the first load and saves the version.
     */
    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();

        if (settings.isFlashUploadEnabled()) {
            if (detectedFlash == null) {
                Page page = getPage();
                for (Behavior behavior : page.getBehaviors()) {
                    if (behavior instanceof WebAppBehavior) {
                        detectedFlash = ((WebAppBehavior) behavior).getFlash();
                    }
                }
            }
        }

        if (isFlashUpload()) {
            renderFlashUpload();
        } else {
            renderJavascriptUpload();
        }
    }

    /**
     * Traverse up the component tree in search for an IAjaxIndicatorAware component is used to indicate a busy state
     * while uploading.
     *
     * @return IAjaxIndicatorAware component or null of not found
     */
    protected String getAjaxIndicatorId() {
        Component c = this;
        while (c != null) {
            if (IAjaxIndicatorAware.class.isAssignableFrom(c.getClass())) {
                return ((IAjaxIndicatorAware) c).getAjaxIndicatorMarkupId();
            }
            c = c.getParent();
        }
        return null;
    }


    protected void renderFlashUpload() {
        AjaxMultiFileUploadSettings ajaxSettings = new AjaxMultiFileUploadSettings();
        ajaxSettings.setFileExtensions(settings.getFileExtensions());
        ajaxSettings.setAllowMultipleFiles(settings.getMaxNumberOfFiles() > 1);
        ajaxSettings.setUploadAfterSelect(settings.isAutoUpload());
        ajaxSettings.setClearAfterUpload(settings.isClearAfterUpload());
        ajaxSettings.setClearTimeout(settings.getClearTimeout());
        ajaxSettings.setHideBrowseDuringUpload(settings.isHideBrowseDuringUpload());
        ajaxSettings.setAjaxIndicatorId(getAjaxIndicatorId());
        ajaxSettings.setButtonWidth(settings.getButtonWidth());
        ajaxSettings.setSimultaneousUploadLimit(settings.getSimultaneousUploadLimit());
        ajaxSettings.setMaxNumberOfFiles(settings.getMaxNumberOfFiles());
        ajaxSettings.setAlwaysShowLabel(settings.isAlwaysShowLabel());
        replace(panel = new AjaxMultiFileUploadComponent(COMPONENT_ID, ajaxSettings) {

            @Override
            protected void onFileUpload(FileUpload fileUpload) {
                handleFileUpload(fileUpload);
            }

            @Override
            protected void onFinish(AjaxRequestTarget target) {
                FileUploadWidget.this.onFinishUpload();
                FileUploadWidget.this.onFinishAjaxUpload(target);
            }

        });
    }

    /**
     * Detect if flash is installed and if the correct version of the flash plugin is found.
     *
     * @return <code>true</code> if flash and the correct version is detected, <code>false</code> otherwise
     */
    public boolean isFlashUpload() {
        return detectedFlash != null && detectedFlash.isValid(VALID_FLASH);
    }

    /**
     * Components that embed a FileUploadWidget might have their own actions for triggering the upload, this method
     * returns the javascript call to initiate it.
     *
     * @return Javascript call that start the ajax upload
     */
    public String getStartAjaxUploadScript() {
        return "YAHOO.hippo.Upload.upload();";
    }

    protected void renderJavascriptUpload() {
        replace(panel = new MultiFileUploadComponent(COMPONENT_ID, settings));
    }

    protected void onFinishAjaxUpload(AjaxRequestTarget target) {
    }

    /**
     * The HTML4 upload collects the new files after the form has been fully posted to the server.
     *
     * When uploading files ajax-style, Wicket uses a hidden Iframe to handle the post and passes the
     * ajax response back to the originating {@link Page}. But even though the request is marked as ajax, no
     * {@link AjaxRequestTarget} is found in the {@link RequestCycle}. This leads to a redirect in the
     * response which in turn is handled by the hidden Iframe and renders the application in a locked state.
     * To fix this we simply ensure that a request marked as ajax has a corresponding {@link AjaxRequestTarget}.
     */
    public void onFinishHtmlUpload() {
        AjaxRequestTarget target = null;
        RequestCycle rc = RequestCycle.get();
        final WebRequest request = (WebRequest) rc.getRequest();
        if (request.isAjax() && rc.find(AjaxRequestTarget.class) == null) {
            WebApplication app = (WebApplication) getApplication();
            target = app.newAjaxRequestTarget(getPage());
            RequestCycle.get().scheduleRequestHandlerAfterCurrent(target);
        }

        onFinishUpload();

        if (target != null) {
            target.add(this);
        }
    }

    protected final void onFinishUpload() {
        if (!isFlashUpload()) {
            Collection<FileUpload> uploads = ((MultiFileUploadComponent) panel).getUploads();
            if (uploads != null) {
                for (FileUpload upload : uploads) {
                    handleFileUpload(upload);
                }
            }
        }
        handleViolations();
    }


    private void handleViolations() {
        if (violations.size() > 0) {
            for (Violation violation : violations) {
                error(violation.getMessage().getObject());
            }
            violations.clear();
        }
    }

    private void handleFileUpload(FileUpload fileUpload) {
        try {
            validator.validate(fileUpload);
            IValidationResult result = validator.getValidationResult();

            if (result.isValid()) {
                onFileUpload(fileUpload);
            } else {
                violations.addAll(result.getViolations());
            }
            //delete uploaded file after it is processed
            fileUpload.delete();

        } catch (ValidationException e) {
            log.error("Error while validating upload", e);
        }
    }

    /**
     * Hook method for subclasses to handle an upload once it has passed validation.
     *
     * @param fileUpload The uploaded file
     */
    protected void onFileUpload(FileUpload fileUpload) {
    }

    public String getAjaxIndicatorStopScript() {
        return "YAHOO.hippo.Upload.stopIndicator();";
    }

    public String hasFileSelectedScript() {
        return isFlashUpload() ?
                "YAHOO.hippo.Upload.latest.hasFilesSelected()" :
                "YAHOO.util.Dom.getElementsByClassName('wicket-mfu-row', 'div', '" + getMarkupId() + "').length > 0";
    }
}
