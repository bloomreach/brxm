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

<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>


<div>

    <form method="POST" name="myform" action="<hst:actionURL/>">
    <input type="hidden" name="previous" value="${form.previous}"/>
    <br/>
    <table>
        <tr>
            <td>Name</td>
            <td><input type="text" name="name" value="<c:out value="${form.value['name'].value}"/>" /></td>
            <td><font style="color:red">${form.message['name']}</font></td>
        </tr>
        <tr>
            <td>Email</td>
            <td><input type="text" name="email" value="<c:out value="${form.value['email'].value}"/>"/></td>
            <td><font style="color:red">${form.message['email']}</font></td>
        </tr>
        <tr>
            <td>Text</td>
            <td><textarea name="textarea"><c:out value="${form.value['textarea'].value}"/></textarea></td>
            <td><font style="color:red">${form.message['textarea']}</font></td>
        </tr>
        <tr>
            <td>
                <c:if test="${form.previous != null}">
                  <input type="submit" name="prev" value="prev"/>
                </c:if>
            </td>
            <td><input type="submit" value="send"/></td>
        </tr>
    </table>
    </form>
    

</div>
