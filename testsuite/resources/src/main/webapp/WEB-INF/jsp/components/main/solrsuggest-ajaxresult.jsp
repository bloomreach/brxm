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
<div>
<hr/>
<hst:link var="imglink" path="/images/icons/edit-16.png"/>
  &nbsp;<img src="${imglink}" onclick="simpleio_objectbyid('<hst:namespace/>query').value=simpleio_objectbyid('<hst:namespace/>collated').innerHTML"/><span id="<hst:namespace/>collated">${collated}</span><br/>
<hr/>

<c:forEach var="suggestion" items="${suggestions}">
<i>${suggestion.token}</i><br/>
  <c:forEach var="alternative" items="${suggestion.alternatives}" varStatus="counter">
    <c:if test="${counter.index < 7}">
      &nbsp;&nbsp;&nbsp;${alternative}<br/>
    </c:if>
  </c:forEach>
 </c:forEach>
</div>
