<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ page isELIgnored="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst" prefix="h" %>
<%--
    Copyright 2007 Hippo
    
    Licensed under the Apache License, Version 2.0 (the  "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
    http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS"
    BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
--%>

<div id="block">

  <div background="lightblue" width="100%">
    <ul>
    <c:forEach var="item" items="${global['navigation/navigation']}">
      <c:forEach var="doc" items="${item}">
        <li><c:url var="url" value="${doc._path}"/><a href="${url}"/>${doc['sample:pageLabel']}</a>
      </c:forEach>
    </c:forEach>
    </ul>
  </div>

</div>
