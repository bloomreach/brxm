/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.essentials.components.info;

import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;

@FieldGroupList({
        @FieldGroup(value = {"postScope", "pageSize", "sortField"}, titleKey = "group.blogauthorposts")
})
public interface EssentialsBlogAuthorPostsComponentInfo {
    @Parameter(name = "postScope")
    @JcrPath(pickerSelectableNodeTypes = {"hippostd:folder"})
    String getScope();

    @Parameter(name = "pageSize", required = true, defaultValue = "10", description = "Nr of items to show")
    int getPageSize();

    @Parameter(name = "sortField", required = true, defaultValue = "hippostdpubwf:publicationDate")
    String getSortField();
}
