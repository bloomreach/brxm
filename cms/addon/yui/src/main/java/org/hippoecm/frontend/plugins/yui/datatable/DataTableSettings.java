package org.hippoecm.frontend.plugins.yui.datatable;

import org.hippoecm.frontend.plugins.yui.widget.WidgetSettings;

public class DataTableSettings extends WidgetSettings {
    final static String SVN_ID = "$Id$";

    private boolean cacheEnabled = true;
    private String autoWidthColumnClassname;

    public String getAutoWidthColumnClassname() {
        return autoWidthColumnClassname;
    }

    public void setAutoWidthColumnClassname(String autoWidthColumnClassname) {
        this.autoWidthColumnClassname = autoWidthColumnClassname;
    }

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }
}
