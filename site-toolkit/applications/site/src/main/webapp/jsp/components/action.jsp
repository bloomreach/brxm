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

<hst:head-contribution keyHint="hippo-widget-collection-dom">
<script language="javascript" src="http://www.onehippo.org/ajax/widget-collection.js"><![CDATA[
<!--
//alert("test");
//-->
]]></script>
</hst:head-contribution>
         
<script language="javascript">
<!--
function <hst:namespace/>showPopup() {
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

    <div style="background-color:green; color:white; width:400px;">
    <hst:link var="link" hippobean="${parent}"/>
    <a href="${link}">
    PARENT : ${parent.name}      
    </a>
    
    </div>  
    <div style="background-color:blue; color:white; width:400px;">
    <hst:link var="link" hippobean="${current}"/>
    <a href="${link}">
    CURRENT:    ${current.name}  
    </a>
    </div>   
    <div style="background-color:yellow; width:400px;">
    <ol >
    <c:forEach var="folder" items="${collections}">
        <li>  
            <hst:link var="link" hippobean="${folder}"/>
            <a href="${link}">
             ${folder.name}
             </a>
        </li> 
    </c:forEach>
    </ol>
   </div>

    <div style="background-color:green; width:400px;">
    <ol >
    <c:forEach var="document" items="${documents}">
        <li >  
        <hst:link var="link" hippobean="${document}"/>
        <a href="${link}">
        ${document.title}
        </a>
        <br/>
        ${document.summary}
        <br/>
        ${document.date.time}
        
        <div style="background-color:red" >
        
        ${document.html.content}
        </div>
        </li>
        
        
        
    </c:forEach>
    </ol>
    </div>

<div>

    <a href="javascript:<hst:namespace/>showPopup();">Show</a>

    <hst:renderURL var="defaultUrl">
      <hst:param name="page" />
      <hst:param name="sortpage" />
    </hst:renderURL>
    <hst:renderURL var="firstUrl">
      <hst:param name="page" value="1" />
    </hst:renderURL>
    <hst:renderURL var="lastUrl">
      <hst:param name="page" value="9" />
    </hst:renderURL>
    <hst:actionURL var="actionUrl">
      <hst:param name="sort" value="descending" />
    </hst:actionURL>
    <hst:actionURL var="redirectActionUrl">
      <hst:param name="redirect" value="http://www.google.com" />
    </hst:actionURL>
        
    <a href="${defaultUrl}">Default</a>
    <a href="${firstUrl}">First</a>
    <a href="${lastUrl}">Last</a>
    <a href="${redirectActionUrl}">Redirect page</a>
    <a href="${actionUrl}">Sort by descending order</a>
    
    <form method="POST" action="<hst:actionURL/>">
      <input type="radio" name="sort" value="ascending" checked="true" />Ascending
      <input type="radio" name="sort" value="descending" />Descending
      <input type="submit" />
    </form>
    
    <div>
        body parameters: <%=request.getParameterMap()%>
        <img src="<hst:resourceURL resourceId='static:/images/onehippo.gif'/>" />
    </div>
    
    <div>
        <form method="POST" enctype="multipart/form-data" action="<hst:actionURL/>">
          Caption: <input type="text" name="caption" value="caption" />
          File: <input type="file" name="file" />
          <input type="submit" />
        </form>
    </div>

</div>
