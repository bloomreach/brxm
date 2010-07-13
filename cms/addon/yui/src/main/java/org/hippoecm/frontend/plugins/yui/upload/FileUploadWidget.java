/*
 * Copyright 2010 Hippo.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.plugins.yui.upload;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.hippoecm.frontend.plugins.yui.flash.ProbeFlashBehavior;
import org.hippoecm.frontend.plugins.yui.upload.ajax.AjaxMultiFileUploadComponent;
import org.hippoecm.frontend.plugins.yui.upload.ajax.AjaxMultiFileUploadSettings;
import org.hippoecm.frontend.plugins.yui.upload.multifile.MultiFileUploadComponent;

import java.util.Collection;

public class FileUploadWidget extends Panel implements ProbeFlashBehavior.IProbeFlashHandler {
    final static String SVN_ID = "$Id$";

    private static final String COMPONENT_ID = "component";

    private FileUploadWidgetSettings settings;
    private boolean isFlash;

    private Panel panel;

    ProbeFlashBehavior probeBehavior;

    public FileUploadWidget(String id, FileUploadWidgetSettings settings) {
        super(id);
        setOutputMarkupId(true);

        this.settings = settings;

        add(probeBehavior = new ProbeFlashBehavior(this) {

            @Override
            protected boolean isValid(Flash flash) {
                return flash.isAvailable() && flash.isValid(9, 0, 45);
            }
        });
        add(panel = new EmptyPanel(COMPONENT_ID));
    }

    protected void onFileUpload(FileUpload fileUpload) {
    }

    public boolean isFlash() {
        return isFlash;
    }

    public void handleFlash(AjaxRequestTarget target) {
        isFlash = true;

        AjaxMultiFileUploadSettings ajaxMultiFileUploadSettings = new AjaxMultiFileUploadSettings();
        ajaxMultiFileUploadSettings.setFileExtensions(settings.getFileExtensions());
        ajaxMultiFileUploadSettings.setAllowMultipleFiles(settings.getMaxNumberOfFiles() > 1);
        ajaxMultiFileUploadSettings.setUploadAfterSelect(settings.isAutoUpload());
        ajaxMultiFileUploadSettings.setClearAfterUpload(settings.isClearAfterUpload());
        ajaxMultiFileUploadSettings.setClearTimeout(settings.getClearTimeout());
        ajaxMultiFileUploadSettings.setHideBrowseDuringUpload(settings.isHideBrowseDuringUpload());
        ajaxMultiFileUploadSettings.setAjaxIndicatorId(getAjaxIndicatorId());
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

        remove(probeBehavior);
        target.addComponent(this);
    }

    protected String getAjaxIndicatorId() {
        Component c = this;
        while(c != null) {
            if(IAjaxIndicatorAware.class.isAssignableFrom(c.getClass())) {
                return ((IAjaxIndicatorAware)c).getAjaxIndicatorMarkupId();
            }
            c = c.getParent();
        }
        return null;
    }

    protected void onFinishAjaxUpload(AjaxRequestTarget target) {
    }

    public void handleJavascript(AjaxRequestTarget target) {
        isFlash = false;
        replace(panel = new MultiFileUploadComponent(COMPONENT_ID));

        remove(probeBehavior);
        target.addComponent(this);
    }

    public void handleNonFlashSubmit() {
        if (!isFlash) {
            Collection<FileUpload> uploads = ((MultiFileUploadComponent) panel).getUploads();
            if (uploads != null) {
                for (FileUpload upload : uploads) {
                    if(fileUploadIsValid(upload)) {
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
        if(dotIndex == -1 || dotIndex == fileName.length()-1) {
            error("No extension found on uploaded file " + fileName + ". Extension allowed: " + settings.getFileExtensions());
            return false;
        }
        String uploadExt = fileName.substring(dotIndex + 1).toLowerCase();
        for (String extension : settings.getFileExtensions()) {
            extension = extension.toLowerCase();
            int extDotIndex = extension.lastIndexOf('.');
            if(extDotIndex > -1) {
                extension = extension.substring(extDotIndex + 1);
            }
            if(uploadExt.equals(extension)) {
                return true;
            }
        }
        StringBuilder sb = new StringBuilder();
        for (String extension : settings.getFileExtensions()) {
            sb.append(extension).append(" ");
        }
        error("The file you've uploaded contains an extension we don't allow. Allowed extensions are: " + sb.toString());
        return false;
    }

    public String getStartAjaxUploadScript() {
        return "YAHOO.hippo.Upload.upload();";
    }
}
