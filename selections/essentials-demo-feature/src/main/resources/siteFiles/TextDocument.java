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
        return getProperty("{{namespace}}:title");
    }

    public String getStaticDropdownValue() {
        return getProperty("{{namespace}}:staticdropdown");
    }

    public String getDynamicDropdownValue() {
        return getProperty("{{namespace}}:dynamicdropdown");
    }

    public String getDynamicDropdownObservableValue() {
        return getProperty("{{namespace}}:dynamicdropdown_observable");
    }

    public String getDynamicDropdownObserverValue() {
        return getProperty("{{namespace}}:dynamicdropdown_observer");
    }

    public String getCustomDynamicDropdownValue() {
        return getProperty("{{namespace}}:customdynamicdropdown");
    }

    public String getGroupedDropdownValue() {
        return getProperty("{{namespace}}:groupeddropdown");
    }

    public String getStringRadioGroupValue() {
        return getProperty("{{namespace}}:stringradiogroup");
    }

    public Boolean getBooleanDropdownValue() {
        return getProperty("{{namespace}}:booleanradiogroup");
    }

    public String[] getMultiSelectListValues() {
        return getProperty("{{namespace}}:multiselectlist");
    }

    public String[] getMultiCheckboxesValues() {
        return getProperty("{{namespace}}:multicheckboxes");
    }

    public String[] getMultiPaletteValues() {
        return getProperty("{{namespace}}:multipalette");
    }
}
