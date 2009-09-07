<%--
  Copyright 2008 Hippo

  Licensed under the Apache License, Version 2.0 (the  "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS"
  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License. --%>
<%@ page language="java" import="java.util.Map" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>
<%@ taglib uri="http://java.sun.com/portlet" prefix="portlet"%>

<%
String hstPortletTitle = "News Demo";
String hstServletPath = "/preview";
String hstPathInfo = "/news";

Map prefValues = (Map) request.getAttribute("prefValues");

if (prefValues.containsKey("hstPortletTitle")) {
    hstPortletTitle = ((String []) prefValues.get("hstPortletTitle"))[0];
}
if (prefValues.containsKey("hstServletPath")) {
    hstServletPath = ((String []) prefValues.get("hstServletPath"))[0];
}
if (prefValues.containsKey("hstPathInfo")) {
    hstPathInfo = ((String []) prefValues.get("hstPathInfo"))[0];
}
%>

<h1>Edit Preferences</h1>

<hr/>

<form method="POST" action="<hst:actionURL/>">
  <table>
    <tbody>
      <tr>
        <th>Title</th>
        <td><input type="text" name="hstPortletTitle" value="<%=hstPortletTitle%>"/></td>
      </tr>
      <tr>
        <th>Path:</th>
        <td>
          <select name="hstPathInfo2" onchange="this.form.hstPathInfo.value = this.value; return true;">
            <option value="">(Choose One)</option>
            <c:forEach var="menuItem" items="${hstSiteMenu.menuItems}">
              <option value="/${menuItem.hstLink.path}">${menuItem.name}</option>
            </c:forEach>
          </select>
          &nbsp;
          Manual Configuration: <input type="text" name="hstPathInfo" value="<%=hstPathInfo%>" />
        </td>
      </tr>
      <tr>
        <td colspan="2">
          <input type="submit" value="Save"/>
          <input type="reset" value="Reset"/>
        </td>
      </tr>
    </tbody>
  </table>
</form>