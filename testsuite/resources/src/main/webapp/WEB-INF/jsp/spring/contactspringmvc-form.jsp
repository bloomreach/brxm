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
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<div>

<!-- Support for Spring errors object -->
<spring:bind path="contactMessage.*">
  <c:forEach var="error" items="${status.errorMessages}">
    <B><FONT color=RED>
      <BR><c:out value="${error}"/>
    </FONT></B>
  </c:forEach>
</spring:bind>

<form method="POST" action="<hst:actionURL/>">
	<table>
	    <tr>
	        <td>Name</td>
	        <td>
	           <spring:bind path="contactMessage.name">
    	           <input type="text" name="${status.expression}" value="<c:out value='${status.value}'/>" />
    	       </spring:bind>
	        </td>
	    </tr>
	    <tr>
	        <td>Email</td>
            <td>
               <spring:bind path="contactMessage.email">
                   <input type="text" name="${status.expression}" value="<c:out value='${status.value}'/>" />
               </spring:bind>
            </td>
	    </tr>
	    <tr>
	        <td>Text</td>
            <td>
               <spring:bind path="contactMessage.message">
                   <textarea name="${status.expression}"><c:out value='${status.value}'/></textarea>
               </spring:bind>
            </td>
	    </tr>
	    <tr>
	        <td colspan="2"><input type="submit" value="send"/></td>
	    </tr>
	</table>
</form>

</div>
