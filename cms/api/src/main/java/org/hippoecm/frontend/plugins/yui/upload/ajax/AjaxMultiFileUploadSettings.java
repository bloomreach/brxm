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
package org.hippoecm.frontend.plugins.yui.upload.ajax;

import org.hippoecm.frontend.plugins.yui.AjaxSettings;

import java.util.HashMap;
import java.util.Map;

public class AjaxMultiFileUploadSettings extends AjaxSettings {

    //URL of the Flash file
    private String flashUrl;

    //URL that is used to upload files; must include a session ID
    private String uploadUrl;

    //Allowed file extensions; format is [ "*.jpg", "*.gif" ] etc
    private String[] fileExtensions;

    //Set to true if Browse dialog should allow multiple file selection
    private boolean allowMultipleFiles = true;

    //Max number of simultaneous uploads
    private int simultaneousUploadLimit = 3;

    //Id of the ajaxIndicatorObject
    private String ajaxIndicatorId;

    //If set every file selected will be directly uploaded
    private boolean uploadAfterSelect;

    //Clear dialog after all files are uploaded
    private boolean clearAfterUpload;

    //Timeout after upload before clearing dialog
    private int clearTimeout;

    //Hide browse button during upload
    private boolean hideBrowseDuringUpload;

    //Map containing translations used on the client
    private Map<String, String> translations = new HashMap<String, String>();

    //Specify the width of the button
    private String buttonWidth;

    //Specify the height of the button
    private String buttonHeight;

    public boolean isAllowMultipleFiles() {
        return allowMultipleFiles;
    }

    public void setAllowMultipleFiles(boolean allowMultipleFiles) {
        this.allowMultipleFiles = allowMultipleFiles;
    }

    public int getSimultaneousUploadLimit() {
        return simultaneousUploadLimit;
    }

    public void setSimultaneousUploadLimit(int simultaneousUploadLimit) {
        this.simultaneousUploadLimit = simultaneousUploadLimit;
    }

    public String[] getFileExtensions() {
        return fileExtensions;
    }

    public void setFileExtensions(String[] fileExtensions) {
        this.fileExtensions = fileExtensions;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }

    public String getFlashUrl() {
        return flashUrl;
    }

    public void setFlashUrl(String flashUrl) {
        this.flashUrl = flashUrl;
    }

    public String getAjaxIndicatorId() {
        return ajaxIndicatorId;
    }

    public void setAjaxIndicatorId(String ajaxIndicatorId) {
        this.ajaxIndicatorId = ajaxIndicatorId;
    }

    public void setUploadAfterSelect(boolean uploadAfterSelect) {
        this.uploadAfterSelect = uploadAfterSelect;
    }

    public boolean isUploadAfterSelect() {
        return uploadAfterSelect;
    }

    public void setClearAfterUpload(boolean clearAfterUpload) {
        this.clearAfterUpload = clearAfterUpload;
    }

    public boolean isClearAfterUpload() {
        return clearAfterUpload;
    }

    public void setClearTimeout(int clearTimeout) {
        this.clearTimeout = clearTimeout;
    }

    public int getClearTimeout() {
        return clearTimeout;
    }

    public boolean isHideBrowseDuringUpload() {
        return hideBrowseDuringUpload;
    }

    public void setHideBrowseDuringUpload(boolean hideBrowseDuringUpload) {
        this.hideBrowseDuringUpload = hideBrowseDuringUpload;
    }

    public Map<String, String> getTranslations() {
        return translations;
    }

    public void setTranslations(Map<String, String> translations) {
        this.translations = translations;
    }

    public void addTranslation(String key, String value) {
        this.translations.put(key, value);
    }

    public String getButtonWidth() {
        return buttonWidth;
    }

    public void setButtonWidth(String buttonWidth) {
        this.buttonWidth = buttonWidth;
    }

    public String getButtonHeight() {
        return buttonHeight;
    }

    public void setButtonHeight(String buttonHeight) {
        this.buttonHeight = buttonHeight;
    }

}
