package org.hippoecm.frontend.plugins.yui.widget.tree;

import org.hippoecm.frontend.plugins.yui.widget.WidgetSettings;

public class TreeWidgetSettings extends WidgetSettings {

    //nicer names..
//    private String setWidthToElementWithClassname;
//    private String useWidthFromElementWithClassname;
//    private boolean autoWidth = true;

    private String useWidthFromClassname;
    private String setWidthToClassname = "hippo-tree";

    private boolean treeAutoWidth = true;
    private boolean bindToLayoutUnit = true;
    private boolean workflowEnabled = true;

    //get/set

    public boolean isBindToLayoutUnit() {
        return bindToLayoutUnit;
    }

    public void setBindToLayoutUnit(boolean bindToLayoutUnit) {
        this.bindToLayoutUnit = bindToLayoutUnit;
    }

    public String getSetWidthToClassname() {
        return setWidthToClassname;
    }

    public void setSetWidthToClassname(String setWidthToClassname) {
        this.setWidthToClassname = setWidthToClassname;
    }

    public boolean isTreeAutoWidth() {
        return treeAutoWidth;
    }

    public void setTreeAutoWidth(boolean treeAutoWidth) {
        this.treeAutoWidth = treeAutoWidth;
    }

    public String getUseWidthFromClassname() {
        return useWidthFromClassname;
    }

    public void setUseWidthFromClassname(String useWidthFromClassname) {
        this.useWidthFromClassname = useWidthFromClassname;
    }

    public boolean isWorkflowEnabled() {
        return workflowEnabled;
    }

    public void setWorkflowEnabled(boolean workflowEnabled) {
        this.workflowEnabled = workflowEnabled;
    }
}
