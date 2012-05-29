<%--
  Copyright 2012 Hippo

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

<hst:headContribution keyHint="title"><title>Faceted Navigation Add Products</title></hst:headContribution>

<div id="yui-u">
    <c:if test="${not empty message}">
        <h2>${message}</h2>
    </c:if>

    <h1>Index GOGREEN REST product Documents</h1>
    <p>
      Fill in an REST product folder url displaying 'documents' hrefs from GOGREEN, for example 
      <br/>
      <b>http://www.demo.onehippo.com/restapi/products/food/2011/11./documents?_type=xml</b>
    </p>
    <form action="<hst:actionURL/>" method="get">
       <input name="url" size="25" value="http://www.demo.onehippo.com/restapi/products/food/2011/11./documents?_type=xml"/>
       <input type="submit" value="Index product docs"/>
    </form>

</div>
