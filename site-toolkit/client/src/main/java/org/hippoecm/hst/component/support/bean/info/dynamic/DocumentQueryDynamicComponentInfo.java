/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.component.support.bean.info.dynamic;

import org.hippoecm.hst.configuration.components.DynamicComponentInfo;
import org.hippoecm.hst.core.parameters.DropDownList;
import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;

@FieldGroupList({
        @FieldGroup(value = {"scope", "includeSubtypes", "documentTypes"}, titleKey = "list.group"),
        @FieldGroup(value = {"sortField", "sortOrder"}, titleKey = "sorting.group"),
        @FieldGroup(value = {"hidePastItems", "hideFutureItems", "dateField", "pageSize"}, titleKey = "filter.group")
})
public interface DocumentQueryDynamicComponentInfo extends DynamicComponentInfo {

    String ASC = "asc";
    String DESC = "desc";

    @Parameter(name = "scope")
    @JcrPath(
            isRelative = true,
            pickerSelectableNodeTypes = {"hippostd:folder"}
    )
    String getScope();

    @Parameter(name = "includeSubtypes", defaultValue = "true")
    Boolean getIncludeSubtypes();

    @Parameter(name = "documentTypes", required = true)
    String getDocumentTypes();

    @Parameter(name = "sortField")
    String getSortField();

    @Parameter(name = "sortOrder", defaultValue = DESC)
    @DropDownList(value = {ASC, DESC})
    String getSortOrder();

    /**
     * Boolean flag which indicates if items that happened in the past will not be shown.
     *
     * @return {@code true} if items should be hidden, {@code false} otherwise
     */
    @Parameter(name = "hidePastItems", defaultValue = "false")
    Boolean getHidePastItems();

    /**
     * Boolean flag which indicates if items that lay in the future will not be shown.
     *
     * @return {@code true} if items should be hidden, {@code false} otherwise
     */
    @Parameter(name = "hideFutureItems", defaultValue = "false")
    Boolean getHideFutureItems();
    
    @Parameter(name = "dateField")
    String getDateField();

    @Parameter(name = "pageSize", defaultValue = "10")
    int getPageSize();
}
