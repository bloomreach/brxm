<#include "../include/imports.ftl">
<#--
  Copyright 2019 Hippo B.V. (http://www.onehippo.com)

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<@hst.setBundle basename="essentials.homepage"/>
<div>
  <h1><@fmt.message key="homepage.title" var="title"/>${title?html}</h1>
  <p><@fmt.message key="homepage.text" var="text"/>${text?html}</p>
  <h1>Selections demo</h1>
  <#--<p>Available value list identifiers: ${valueListIdentifiers}.-->
  <table cellspacing="0" cellpadding="4" border="1px">
    <tr>
      <td>StaticDropdown</td>
      <td>${staticOptions1[document.staticDropdownValue]}</td>
    </tr>
    <tr>
      <td>DynamicDropdown</td>
      <td>${valueList1Values[document.dynamicDropdownValue]}</td>
    </tr>
    <tr>
      <td>DynamicDropdown - Observable with continents</td>
      <td>${continentValues[document.dynamicDropdownObservableValue]}</td>
    </tr>
    <tr>
      <td>DynamicDropdown - Observer of continents</td>
      <td>${chainedValues[document.dynamicDropdownObserverValue]}</td>
    </tr>
    <tr>
      <td>DynamicDropdown with custom ValueListProvider</td>
      <td>${document.customDynamicDropdownValue}</td>
    </tr>
    <tr>
      <td>DynamicDropdown showing grouped options</td>
      <td>${carOptions1[document.groupedDropdownValue]}</td>
    </tr>
    <tr>
      <td>RadioGroup</td>
      <td>${valueList1Values[document.stringRadioGroupValue]}</td>
    </tr>
    <tr>
      <td>BooleanRadioGroup</td>
      <td>
        <#if document.booleanDropdownValue??>
          YES
        <#else>
          NO
        </#if>
      </td>
    </tr>
    <tr>
      <td>DynamicMultiSelect - List</td>
      <td>
        <#list document.multiSelectListValues as item>
          ${item}
          <br/>
        </#list>
      </td>
    </tr>
    <tr>
      <td>DynamicMultiSelect - Checkboxes</td>
      <td>
        <#list document.multiCheckboxesValues as item>
          ${item}
          <br/>
        </#list>
      </td>
    </tr>
    <tr>
      <td>DynamicMultiSelect - Palette</td>
      <td>
        <#list document.multiPaletteValues as item>
          ${item}
          <br/>
        </#list>
      </td>
    </tr>
  </table>

</div>
<div>
  <@hst.include ref="container"/>
</div>