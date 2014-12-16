<%--
    Copyright 2010-2014 Hippo

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
--%>
<%@ page language="java" %>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>
<hst:headContribution><title>${document.title}</title></hst:headContribution>
<div>
  <p><b>${document.title}</b></p>
  <p>Available value list identifiers: ${valueListIdentifiers}.
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
      <td><c:choose>
            <c:when test="${document.booleanDropdownValue}">Yes</c:when>
            <c:otherwise>No</c:otherwise>
          </c:choose>
      </td>
    </tr>
    <tr>
      <td>DynamicMultiSelect - List</td>
      <td>
        <c:forEach var="item" items="${document.multiSelectListValues}">
          ${valueList1Values[item]}<br />
        </c:forEach>
      </td>
    </tr>
    <tr>
      <td>DynamicMultiSelect - Checkboxes</td>
      <td>
        <c:forEach var="item" items="${document.multiCheckboxesValues}">
          ${valueList1Values[item]}<br />
        </c:forEach>
      </td>
    </tr>
    <tr>
      <td>DynamicMultiSelect - Palette</td>
      <td>
        <c:forEach var="item" items="${document.multiPaletteValues}">
          ${valueList1Values[item]}<br />
        </c:forEach>
      </td>
    </tr>
  </table>
</div>
