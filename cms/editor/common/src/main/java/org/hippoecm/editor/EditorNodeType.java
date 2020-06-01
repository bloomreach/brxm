/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.editor;

/*
 * This file has to be kept in sync with:
 * src/main/resources/editor.cnd
 */

/**
 * This interface defines the node types and item names that are in use by
 * the Hippo frontend editor.
 */

public interface EditorNodeType {

    //--- Hippo Editor NodeTypes ---//
    String NT_TEMPLATESET = "editor:templateset";
    String NT_EDITABLE = "editor:editable";

    //--- Hippo Editor Item Names ---//
    String EDITOR_TEMPLATES = "editor:templates";
    String EDITOR_TEMPLATE = "editor:template";

}
