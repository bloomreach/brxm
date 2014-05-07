/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.intranet.componentsinfo;

import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.Parameter;

@FieldGroupList({
        @FieldGroup(
                titleKey = "group.content",
                value = {"title", "pageSize", "docType"}
        ),
        @FieldGroup(
                titleKey = "group.sorting",
                value = {"sortBy", "sortOrder"}
        )
})
public interface GeneralListInfo  {

    @Parameter(name = "title", displayName = "The title of the page", defaultValue="Overview")
    String getTitle();

    @Parameter(name = "pageSize", displayName = "Page Size", defaultValue="10")
    int getPageSize();

    @Parameter(name = "docType", displayName = "Document Type", defaultValue="intranet:basedocument")
    String getDocType();

    @Parameter(name = "sortBy", displayName = "Sort By Property")
    String getSortBy();

    @Parameter(name = "sortOrder", displayName = "Sort Order", defaultValue="descending")
    String getSortOrder();

}
