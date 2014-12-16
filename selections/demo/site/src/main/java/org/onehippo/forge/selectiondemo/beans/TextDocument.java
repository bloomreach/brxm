/*
 *  Copyright 2009-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.selectiondemo.beans;

import org.hippoecm.hst.content.beans.Node;

/**
  Bean representing [selectiondemo:textdocument] > selectiondemo:basedocument
 */
@Node(jcrType="selectiondemo:textdocument")
public class TextDocument extends BaseDocument {

    public String getTitle() {
        return getProperty("selectiondemo:title");
    }

    public String getStaticDropdownValue() {
        return getProperty("selectiondemo:staticdropdown");
    }

    public String getDynamicDropdownValue() {
        return getProperty("selectiondemo:dynamicdropdown");
    }

    public String getDynamicDropdownObservableValue() {
        return getProperty("selectiondemo:dynamicdropdown_observable");
    }

    public String getDynamicDropdownObserverValue() {
        return getProperty("selectiondemo:dynamicdropdown_observer");
    }

    public String getCustomDynamicDropdownValue() {
        return getProperty("selectiondemo:customdynamicdropdown");
    }

    public String getGroupedDropdownValue() {
        return getProperty("selectiondemo:groupeddropdown");
    }

    public String getStringRadioGroupValue() {
        return getProperty("selectiondemo:stringradiogroup");
    }

    public Boolean getBooleanDropdownValue() {
        return getProperty("selectiondemo:booleanradiogroup");
    }

    public String[] getMultiSelectListValues() {
        return getProperty("selectiondemo:multiselectlist");
    }

    public String[] getMultiCheckboxesValues() {
        return getProperty("selectiondemo:multicheckboxes");
    }

    public String[] getMultiPaletteValues() {
        return getProperty("selectiondemo:multipalette");
    }
}
