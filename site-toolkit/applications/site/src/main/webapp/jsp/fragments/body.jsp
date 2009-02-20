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

<script language="javascript">
<!--
function <hc:namespace/>showPopup() {
    alert("Hello from body component!");
}
//-->
</script>

<div>

    <a href="javascript:<hc:namespace/>showPopup();">Show</a>

    <hc:url var="firstUrl" type="render">
      <hc:param name="page" value="1" />
    </hc:url>
    <hc:url var="lastUrl" type="render">
      <hc:param name="page" value="9" />
    </hc:url>
    
    <a href="<%=firstUrl%>">First</a>
    <a href="<%=lastUrl%>">Last</a>
    
    <div>
        header parameters: <%=request.getParameterMap()%>
    </div>
    
</div>

</div>