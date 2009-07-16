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

<%
String hstPortletTitle = "News Demo";
String hstServletPath = "/preview";
String hstPathInfo = "/news";

Map prefValues = (Map) request.getAttribute("prefValues");

if (prefValues.containsKey("hstPortletTitle")) {
    hstPortletTitle = (String) prefValues.get("hstPortletTitle");
}
if (prefValues.containsKey("hstServletPath")) {
    hstServletPath = (String) prefValues.get("hstServletPath");
}
if (prefValues.containsKey("hstPathInfo")) {
    hstPathInfo = (String) prefValues.get("hstPathInfo");
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
        <th>Type:</th>
        <td>
          <select name="hstServletPath">
            <option value="/preview" <%=("/preview".equals(hstServletPath) ? "selected" : "")%>>Preview</option>
            <option value="/live" <%=("/live".equals(hstServletPath) ? "selected" : "")%>>Live</option>
          </select>
        </td>
      </tr>
      <tr>
        <th>Path:</th>
        <td>
          <input type="text" name="hstPathInfo" value="<%=hstPathInfo%>"/>
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