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
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:if test="${not empty goBackLink}">
<div align="right">
  <a href="<hst:link path="${goBackLink}"/>">
    <img src="<hst:link path="/images/goback.jpg"/>" border="0"/>
  </a>
</div>
</c:if>

<div class="yui-u">
    <h2>${document.title}</h2>
    <p>${document.summary}</p>
    <p><hst:html hippohtml="${document.html}"/></p>
    <c:if test="${not empty document.image}">
        <img src="<hst:link hippobean="${document.image.picture}"/>"/>
    </c:if>
    
  <hst:actionURL var="addURL">
    <hst:param name="type" value="add"/>
  </hst:actionURL>
  
  <div>
      <c:forEach var="comment" items="${comments}">
         <div style="border:1px solid black; padding:15px;">
             <b>${comment.title}</b>
             <br/>
             <hst:html hippohtml="${comment.html}"/>
         </div>
        </c:forEach>
   
  </div>
  
  <div>
    <form method="POST" action="${addURL}">
      <h4>Enter your comment here:</h4>
      <table>
        <tr>
          <th>Title:</th>
          <td><input type="text" name="title" value="" /></td>
        </tr>
        <tr>
          <th valign="top">Comment:</th>
          <td><textarea name="comment" rows="4" cols="40"></textarea></td>
        </tr>
        <tr>
          <td colspan="2">
            <input type="submit" value="Submit"/>
            <input type="reset" value="Reset"/>
          </td>
         </tr>
      </table>
    </form>
  </div>
  
</div>

