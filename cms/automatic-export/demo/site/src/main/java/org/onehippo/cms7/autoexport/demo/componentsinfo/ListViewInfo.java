package org.onehippo.cms7.autoexport.demo.componentsinfo;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.parameters.Color;
import org.hippoecm.hst.core.parameters.Parameter;

public interface ListViewInfo extends GeneralListInfo {

    /**
     * Returns the scope to search below. Leading and trailing slashes do not have meaning and will be skipped when using the scope. The scope
     * is always relative to the current {@link Mount#getContentPath()}, even if it starts with a <code>/</code>
     * @return the scope to search below
     */
    @Parameter(name = "scope", defaultValue="/", displayName = "Scope")
    String getScope();

    @Override
    @Parameter(name = "title", displayName = "The title of the page", defaultValue="List")
    String getTitle();
    
    @Parameter(name = "cssclass", defaultValue="lightgrey", displayName = "Css Class")
    String getCssClass();

    @Parameter(name = "bgcolor", defaultValue="", displayName = "Background Color")
    @Color
    String getBgColor();

}
