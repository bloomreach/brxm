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

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.parameters.Color;
import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.Parameter;

@FieldGroupList({
        @FieldGroup(
                titleKey = "group.content",
                value = {"scope"}
        ),
        @FieldGroup(
                titleKey = "group.sorting",
                value = {}
        ),
        @FieldGroup(
                titleKey = "group.layout",
                value = {"cssClass", "bgColor"}
        )
})
public interface ListViewInfo extends GeneralListInfo {

    /**
     * Returns the scope to search below. Leading and trailing slashes do not have meaning and will be skipped when using the scope. The scope
     * is always relative to the current {@link org.hippoecm.hst.configuration.hosting.Mount#getContentPath()}, even if it starts with a <code>/</code>
     * @return the scope to search below
     */
    @Parameter(name = "scope", defaultValue="/")
    String getScope();

    @Override
    @Parameter(name = "title", defaultValue="List")
    String getTitle();
    
    @Parameter(name = "cssClass", defaultValue="lightgrey")
    String getCssClass();

    @Parameter(name = "bgColor", defaultValue="")
    @Color
    String getBgColor();

}
