/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
public interface EssentialsDocumentListComponentInfo extends EssentialsPageable, EssentialsSortable {


    @Parameter(name = "path", required = false, displayName = "Documents path")
    @JcrPath(
            isRelative = true,
            pickerConfiguration = "cms-pickers/documents",
            pickerSelectableNodeTypes = {"hippostd:folder"},
            pickerInitialPath = "/content/documents"
    )
    String getPath();


    @Parameter(name = "includeSubtypes", required = false, displayName = "Include document subtypes")
    Boolean getIncludeSubtypes();


    @Parameter(name = "documentTypes", required = true, displayName = "Document types (comma separated)")
    String getDocumentTypes();



}
