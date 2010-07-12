package org.hippoecm.frontend.plugins.yui.upload;

import org.apache.wicket.IClusterable;

public class FileUploadWidgetSettings implements IClusterable{
    final static String SVN_ID = "$Id$";

    private String[] fileExtensions;
    private int maxNumberOfFiles;
    private boolean autoUpload;
    private boolean clearAfterUpload;

    private int clearTimeout;

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
}
