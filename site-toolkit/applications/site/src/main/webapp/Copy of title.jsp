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
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<%@ taglib uri='/WEB-INF/hst-core.tld' prefix='hc'%>

<%
System.out.println("Console out from title.jsp");
%>

<div class="title">

<h1>News Title</h1>


<script language="javascript">
<!--
function <hc:namespace/>showPopup() {
    alert("Hello from title component!");
}
//-->
</script>

<div>

    <a href="javascript:<hc:namespace/>showPopup();">Show</a>
    
    <hc:url var="prevUrl" type="render">
      <hc:param name="page" value="prev" />
    </hc:url>
    <hc:url var="nextUrl" type="render">
      <hc:param name="page" value="next" />
    </hc:url>
    
    <a href="<%=prevUrl%>">Previous</a>
    <a href="<%=nextUrl%>">Next</a>
    
    <div>
        title parameters: <%=request.getParameterMap()%>
    </div>
    
</div>

</div>