<%--
  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)

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

<hst:headContribution keyHint="title"><title>${document.title}</title></hst:headContribution>

<c:if test="${not empty goBackLink}">
<div class="right">
  <a href="<hst:link path="${goBackLink}"/>">
    <img src="<hst:link path="/images/goback.jpg"/>" class="noborder" alt="Go back"/>
  </a>
</div>
</c:if>

<div class="yui-u">
  <p>
  <hst:componentRenderingURL var="componentRenderingURL"/>
  <a href="${componentRenderingURL}" target="_blank">Render only this component</a>
  </p>
  <div id="editable_cont" class="inline-editor-editable-container">
    <div><span style="color:red">Note that this PAGE shows the thumbnail variants in the HTML field!! This page shows an example
    of the &lt;hst:imagevariant&gt; tag</span></div>
    
    <h2>${document.title}</h2>
    <p>
       ${document.summary}
    </p>
    <div>
        <hst:html hippohtml="${document.html}">
          <!-- Note that replace="hippogallery:original" is optional and can be left out -->
          <hst:imagevariant name="hippogallery:thumbnail" replaces="hippogallery:original" fallback="false"/>
        </hst:html>
    </div>
  </div>

  
  <c:if test="${not empty document.resource}">
      <h2>resource link:</h2>
      <hst:link var="resource" hippobean="${document.resource}" />
      <a href="${resource}">${document.resource.name}</a>
      <br/><br/>
  </c:if>


 <hst:link var="thumbnail" hippobean="${document.image.thumbnail}"/>
 <hst:link var="original" hippobean="${document.image.original}"/>
 <hst:headContribution category="jsInline">
    <script type="text/javascript" language="javascript">
      var thumbnailSrc = '${thumbnail}';
      var originalSrc = '${original}';
      var current = thumbnailSrc;
      function <hst:namespace/>_toggle(){
         if (current == thumbnailSrc) {
           document.getElementById("<hst:namespace/>toggleImg").src = originalSrc;
           current = originalSrc;
         } else {
           document.getElementById("<hst:namespace/>toggleImg").src = thumbnailSrc;
           current = thumbnailSrc;
         }
      }

  </script>
</hst:headContribution>

  <c:if test="${not empty document.image}">
      <span color="#ff0000">Click the image to change it size!</span><br/>

      <a href="#" onclick="<hst:namespace/>_toggle(); return false;"><img src="${thumbnail}" id="<hst:namespace/>toggleImg"/></a>
  </c:if>
    
  <div>
      <c:forEach var="comment" items="${comments}">
         <div style="border:1px solid black; padding:15px;">
             <b>${comment.title}</b>
             <br/>
             <hst:html hippohtml="${comment.html}"/>
         </div>
        </c:forEach>
   
  </div>
  
</div>

