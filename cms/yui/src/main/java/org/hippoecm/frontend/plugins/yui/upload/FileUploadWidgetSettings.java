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

import org.apache.wicket.IClusterable;

public class FileUploadWidgetSettings implements IClusterable{
    final static String SVN_ID = "$Id$";

    private String[] fileExtensions;
    private int maxNumberOfFiles;
    private boolean autoUpload;
    private boolean clearAfterUpload;
    private int clearTimeout;
    private boolean hideBrowseDuringUpload;
    private String buttonWidth;
    private String buttonHeight;

    public FileUploadWidgetSettings() {
        fileExtensions = new String[0];
        maxNumberOfFiles = 1;
        clearTimeout = 1000;
    }

    public void setFileExtensions(String[] fileExtensions) {
        this.fileExtensions = fileExtensions;
    }

    public String[] getFileExtensions() {
        return fileExtensions;
    }

    public void setMaxNumberOfFiles(int nr) {
        maxNumberOfFiles = nr;
    }

    public int getMaxNumberOfFiles() {
        return maxNumberOfFiles;
    }

    public void setAutoUpload(boolean set) {
        autoUpload = set;
    }

    public boolean isAutoUpload() {
        return autoUpload;
    }

    public void setClearAfterUpload(boolean clear) {
        this.clearAfterUpload = clear;
    }

    public boolean isClearAfterUpload() {
        return clearAfterUpload;
    }

    public int getClearTimeout() {
        return clearTimeout;
    }

    public void setClearTimeout(int clearTimeout) {
        this.clearTimeout = clearTimeout;
    }

    public void setHideBrowseDuringUpload(boolean hideBrowseDuringUpload) {
        this.hideBrowseDuringUpload = hideBrowseDuringUpload;
    }

    public boolean isHideBrowseDuringUpload() {
        return hideBrowseDuringUpload;
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
