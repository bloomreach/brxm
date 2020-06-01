/*
 * Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
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
package {{beansPackage}};

import org.hippoecm.hst.content.beans.Node;

/**
  Bean representing [{{namespace}}:textdocument] > {{namespace}}:basedocument
 */
@Node(jcrType="{{namespace}}:textdocument")
public class TextDocument extends BaseDocument {

    public String getTitle() {
        return getSingleProperty("{{namespace}}:title");
    }

    public String getStaticDropdownValue() {
        return getSingleProperty("{{namespace}}:staticdropdown");
    }

    public String getDynamicDropdownValue() {
        return getSingleProperty("{{namespace}}:dynamicdropdown");
    }

    public String getDynamicDropdownObservableValue() {
        return getSingleProperty("{{namespace}}:dynamicdropdown_observable");
    }

    public String getDynamicDropdownObserverValue() {
        return getSingleProperty("{{namespace}}:dynamicdropdown_observer");
    }

    public String getCustomDynamicDropdownValue() {
        return getSingleProperty("{{namespace}}:customdynamicdropdown");
    }

    public String getGroupedDropdownValue() {
        return getSingleProperty("{{namespace}}:groupeddropdown");
    }

    public String getStringRadioGroupValue() {
        return getSingleProperty("{{namespace}}:stringradiogroup");
    }

    public Boolean getBooleanDropdownValue() {
        return getSingleProperty("{{namespace}}:booleanradiogroup");
    }

    public String[] getMultiSelectListValues() {
        return getMultipleProperty("{{namespace}}:multiselectlist");
    }

    public String[] getMultiCheckboxesValues() {
        return getMultipleProperty("{{namespace}}:multicheckboxes");
    }

    public String[] getMultiPaletteValues() {
        return getMultipleProperty("{{namespace}}:multipalette");
    }
}
