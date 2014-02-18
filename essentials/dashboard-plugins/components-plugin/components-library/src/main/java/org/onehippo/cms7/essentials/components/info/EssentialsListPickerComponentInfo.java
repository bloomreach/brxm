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

import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;

/**
 * @version "$Id$"
 */

@FieldGroupList({
        @FieldGroup(
                value = {
                        "document1",
                        "document2",
                        "document3",
                        "document4",
                        "document5",
                        "document6",
                        "document7",
                        "document8",
                        "document9",
                        "document10"
                }
        )
})
public interface EssentialsListPickerComponentInfo extends EssentialsPageable {


    @Parameter(name = "document1", required = false, displayName = "Document item 1")
    @JcrPath(
            pickerConfiguration = "cms-pickers/documents",
            pickerSelectableNodeTypes = {"hippo:document"},
            pickerInitialPath = "/content/documents"
    )
    String getDocumentItem1();

    @Parameter(name = "document2", required = false, displayName = "Document item 2")
    @JcrPath(pickerConfiguration = "cms-pickers/documents", pickerSelectableNodeTypes = {"hippo:document"}, pickerInitialPath = "/content/documents")
    String getDocumentItem2();

    @Parameter(name = "document3", required = false, displayName = "Document item 3")
    @JcrPath(pickerConfiguration = "cms-pickers/documents", pickerSelectableNodeTypes = {"hippo:document"}, pickerInitialPath = "/content/documents")
    String getDocumentItem3();

    @Parameter(name = "document4", required = false, displayName = "Document item 4")
    @JcrPath(pickerConfiguration = "cms-pickers/documents", pickerSelectableNodeTypes = {"hippo:document"}, pickerInitialPath = "/content/documents")
    String getDocumentItem4();

    @Parameter(name = "document5", required = false, displayName = "Document item 5")
    @JcrPath(pickerConfiguration = "cms-pickers/documents", pickerSelectableNodeTypes = {"hippo:document"}, pickerInitialPath = "/content/documents")
    String getDocumentItem5();

    @Parameter(name = "document6", required = false, displayName = "Document item 6")
    @JcrPath(pickerConfiguration = "cms-pickers/documents", pickerSelectableNodeTypes = {"hippo:document"}, pickerInitialPath = "/content/documents")
    String getDocumentItem6();

    @Parameter(name = "document7", required = false, displayName = "Document item 7")
    @JcrPath(pickerConfiguration = "cms-pickers/documents", pickerSelectableNodeTypes = {"hippo:document"}, pickerInitialPath = "/content/documents")
    String getDocumentItem7();

    @Parameter(name = "document8", required = false, displayName = "Document item 8")
    @JcrPath(pickerConfiguration = "cms-pickers/documents", pickerSelectableNodeTypes = {"hippo:document"}, pickerInitialPath = "/content/documents")
    String getDocumentItem8();

    @Parameter(name = "document9", required = false, displayName = "Document item 9")
    @JcrPath(pickerConfiguration = "cms-pickers/documents", pickerSelectableNodeTypes = {"hippo:document"}, pickerInitialPath = "/content/documents")
    String getDocumentItem9();

    @Parameter(name = "document10", required = false, displayName = "Document item 10")
    @JcrPath(pickerConfiguration = "cms-pickers/documents", pickerSelectableNodeTypes = {"hippo:document"}, pickerInitialPath = "/content/documents")
    String getDocumentItem10();


}
