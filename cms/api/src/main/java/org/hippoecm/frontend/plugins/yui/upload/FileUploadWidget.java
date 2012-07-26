/*
 *  Copyright 2010 Hippo.
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

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugins.yui.flash.FlashVersion;
import org.hippoecm.frontend.plugins.yui.upload.ajax.AjaxMultiFileUploadComponent;
import org.hippoecm.frontend.plugins.yui.upload.ajax.AjaxMultiFileUploadSettings;
import org.hippoecm.frontend.plugins.yui.upload.multifile.MultiFileUploadComponent;
import org.hippoecm.frontend.plugins.yui.webapp.WebAppBehavior;

/**
 * Widget for uploading files. This widget allows both flash and non-flash uploads based on the configuration.
 * By default the flash upload is used. For more configuration options please take a look at
 * {@link FileUploadWidgetSettings}.
 */
public class FileUploadWidget extends Panel {

    private static final long serialVersionUID = 1L;


    private static final String COMPONENT_ID = "component";

    private FileUploadWidgetSettings settings;
    private Panel panel;

    private final static FlashVersion VALID_FLASH = new FlashVersion(9, 0, 45);
    private FlashVersion detectedFlash;

    public FileUploadWidget(String id, FileUploadWidgetSettings settings) {
        super(id);

        setOutputMarkupId(true);
        this.settings = settings;
        add(panel = new EmptyPanel(COMPONENT_ID));
    }

    protected void onFileUpload(FileUpload fileUpload) {
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();

        if(settings.isFlashUploadEnabled()) {
            if (detectedFlash == null) {
                Page page = getPage();
                for (IBehavior behavior : page.getBehaviors()) {
                    if (behavior instanceof WebAppBehavior) {
                        WebAppBehavior webapp = (WebAppBehavior) behavior;
                        detectedFlash = webapp.getFlash();
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
     * Detect if flash is installed and if the correct version of the flash plugin is found.
     * @return <code>true</code> if flash and the correct version is detected, <code>false</code> otherwise
     */
    public boolean isFlashUpload() {
        return detectedFlash != null && detectedFlash.isValid(VALID_FLASH);
    }

    protected void renderFlashUpload() {
        AjaxMultiFileUploadSettings ajaxMultiFileUploadSettings = new AjaxMultiFileUploadSettings();
        ajaxMultiFileUploadSettings.setFileExtensions(settings.getFileExtensions());
        ajaxMultiFileUploadSettings.setAllowMultipleFiles(settings.getMaxNumberOfFiles() > 1);
        ajaxMultiFileUploadSettings.setUploadAfterSelect(settings.isAutoUpload());
        ajaxMultiFileUploadSettings.setClearAfterUpload(settings.isClearAfterUpload());
        ajaxMultiFileUploadSettings.setClearTimeout(settings.getClearTimeout());
        ajaxMultiFileUploadSettings.setHideBrowseDuringUpload(settings.isHideBrowseDuringUpload());
        ajaxMultiFileUploadSettings.setAjaxIndicatorId(getAjaxIndicatorId());
        ajaxMultiFileUploadSettings.setButtonWidth(settings.getButtonWidth());
        replace(panel = new AjaxMultiFileUploadComponent(COMPONENT_ID, ajaxMultiFileUploadSettings) {

            @Override
            protected void onFileUpload(FileUpload fileUpload) {
                FileUploadWidget.this.onFileUpload(fileUpload);
            }

            @Override
            protected void onFinish(AjaxRequestTarget target) {
                FileUploadWidget.this.onFinishAjaxUpload(target);
            }

            @Override
            protected void onUploadSuccess() {
            }
        });
    }

    protected void renderJavascriptUpload() {
        int max = settings.isAutoUpload() ? 1 : settings.getMaxNumberOfFiles();
        replace(panel = new MultiFileUploadComponent(COMPONENT_ID, max));
    }

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

    protected void onFinishAjaxUpload(AjaxRequestTarget target) {
    }

    public void handleNonFlashSubmit() {
        if (!isFlashUpload()) {
            Collection<FileUpload> uploads = ((MultiFileUploadComponent) panel).getUploads();
            if (uploads != null) {
                for (FileUpload upload : uploads) {
                    if (fileUploadIsValid(upload)) {
                        onFileUpload(upload);
                    }
                }
            }
        }
    }

    private boolean fileUploadIsValid(FileUpload upload) {
        if (settings.getFileExtensions() == null || settings.getFileExtensions().length == 0) {
            return true;
        }

        String fileName = upload.getClientFileName();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            String msg = new StringResourceModel("extension.not.found", this, null,
                    new Object[]{fileName, getReadableFileExtensions()}).getObject();
            error(msg);
            return false;
        }
        String uploadExt = fileName.substring(dotIndex + 1).toLowerCase();
        for (String extension : settings.getFileExtensions()) {
            extension = extension.toLowerCase();
            int extDotIndex = extension.lastIndexOf('.');
            if (extDotIndex > -1) {
                extension = extension.substring(extDotIndex + 1);
            }
            if (uploadExt.equals(extension)) {
                return true;
            }
        }
        String msg = new StringResourceModel("extension.not.allowed", this, null,
                new Object[]{getReadableFileExtensions()}).getObject();
        error(msg);
        return false;
    }

    private String getReadableFileExtensions() {
        StringBuilder sb = new StringBuilder();
        for (String extension : settings.getFileExtensions()) {
            sb.append(extension).append(" ");
        }
        return sb.toString();
    }

    public String getStartAjaxUploadScript() {
        return "YAHOO.hippo.Upload.upload();";
    }

    public String getAjaxIndicatorStopScript() {
        return "YAHOO.hippo.Upload.stopIndicator();";
    }
}
