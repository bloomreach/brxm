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
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hc'%>

<hc:response-property name="hippo-widget-collection-dom">
<script language="javascript" src="http://www.onehippo.org/ajax/widget-collection.js"><![CDATA[
<!--
//alert("test");
//-->
]]></script>
</hc:response-property>
         
<script language="javascript">
<!--
function <hc:namespace/>showPopup() {
    alert("Hello from body component!");
}
//-->
</script>


<div>
    <h1>${document.page.title}</h1>
    <h3>${document.page.summary}</h3>
    <h3>${document.page.date.time}</h3>
</div>

<div>

Up: 
<c:url var="parentUrl" value="${parent.link}" /> 
<a href="${parentUrl}">
${parent.name}  
</a>    


</div>

<div>
Folders:
<ol>
    <c:forEach var="folder" items="${folders}">
        <li style="background-color:white">  
            <c:url var="folderUrl" value="${folder.link}" /> 
            <a href="${folderUrl}">
            ${folder.name}  
            </a>
        </li>
    </c:forEach>
</ol>

</div>

<div>
	Documents:
	<ul>
	    <c:forEach var="doc" items="${documents}">
	        <li>    
	            <a href="/site/content/${doc.link}">
	            ${doc.page.title}  
	            </a>
	        </li>
	    </c:forEach>
	</ul>
	
	 <a href="javascript:<hc:namespace/>showPopup();">Show</a>

    <hc:renderURL var="defaultUrl">
      <hc:param name="page" />
      <hc:param name="sortpage" />
    </hc:renderURL>
    <hc:renderURL var="firstUrl">
      <hc:param name="page" value="1" />
    </hc:renderURL>
    <hc:renderURL var="lastUrl">
      <hc:param name="page" value="9" />
    </hc:renderURL>
    <hc:actionURL var="actionUrl">
      <hc:param name="sort" value="descending" />
    </hc:actionURL>
    <hc:actionURL var="redirectActionUrl">
      <hc:param name="redirect" value="http://www.google.com" />
    </hc:actionURL>
    <hc:resourceURL var="resourceUrl" resourceId="/images/onehippo.gif">
    </hc:resourceURL>
        
    <a href="${defaultUrl}">Default</a>
    <a href="${firstUrl}">First</a>
    <a href="${lastUrl}">Last</a>
    <a href="${actionUrl}">Sort by descending order</a>
    <a href="${redirectActionUrl}">Redirect page</a>
    
    <div>
        body parameters: <%=request.getParameterMap()%>
        <img src="${resourceUrl}" />
    </div>

</div>
