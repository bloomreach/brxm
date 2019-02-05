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
 <h1>Selections demo multi</h1>
  <p><b>${document.title}</b></p>
  <table cellspacing="0" cellpadding="4" border="1px">
    <tr>
      <td>DynamicDropdown</td>
      <td>${multilingualValues[document.dynamicDropdownValue]}</td>
    </tr>
  </table>
</div>
<div>
  <@hst.include ref="container"/>
</div>