/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.components.info;

import org.hippoecm.hst.core.parameters.DocumentLink;
import org.hippoecm.hst.core.parameters.DropDownList;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;

/**
 * @version "$Id: EssentialsDocumentListComponentInfo.java 164011 2013-05-11 14:05:01Z mmilicevic $"
 */
/*@FieldGroupList({
        @FieldGroup(
                titleKey = "group.constraints",
                value = {"docType", "tags"}
        ),
        @FieldGroup(
                titleKey = "group.display",
                value = {"pageSize"}
        )
})*/
public interface EssentialsDocumentListComponentInfo {


    @Parameter(name = "path", required = false, displayName = "Documents path")
    @JcrPath(
            pickerConfiguration = "cms-pickers/documents",
            pickerSelectableNodeTypes = {"hippostd:folder"},
            pickerInitialPath = "/content/documents"
    )
    String getPath();


    @Parameter(name = "includeSubtypes", required = false, displayName = "Include document subtypes")
    Boolean getIncludeSubtypes();


    @Parameter(name = "documentTypes", required = true, displayName = "Document types (comma separated)")
    String getDocumentTypes();

    /*
    @Parameter(name = "listType", required = false, defaultValue = "search", displayName = "List type", description = "Repository or search type of list")
    @DropDownList(value = {"search", "repository"})
    String getListType();
    */

    @Parameter(name = "sortField", required = false, displayName = "Sort field")
    String getSortField();

    @Parameter(name = "sortOrder", required = false, defaultValue = "desc", displayName = "Sort order", description = "Order results ascending or descending")
    @DropDownList(value = {"asc", "desc"})
    String getSortOrder();


    @Parameter(name = "pageSize", required = true, defaultValue = "10", displayName = "Page size", description = "Nr of items per page")
    int getPageSize();

    @Parameter(name = "showPagination", required = false, displayName = "Show pagination")
    Boolean getShowPagination();


}
