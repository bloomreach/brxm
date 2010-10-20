<%--
  Copyright 2008-2009 Hippo

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

<hst:head-contribution keyHint="title"><title>${document.title}</title></hst:head-contribution>
<hst:element name="script" var="yui3Elem">
  <hst:attribute name="type" value="text/javascript" />
  <hst:attribute name="src" value="http://yui.yahooapis.com/3.2.0/build/yui/yui-min.js" />
</hst:element>
<hst:head-contribution keyHint="yui3" element="${yui3Elem}" />

<div class="yui-u">
  
  <h2>${document.title}</h2>
  <p>
    ${document.summary}
  </p>
  
  <p>
    Product : ${document.product}
  </p>
  <p>
    Brand : ${document.brand}
  </p>
  <p>
    Type : ${document.type}
  </p>
  <p>
    Color : ${document.color}
  </p>
  <p>
    Tags : 
      <c:forEach var="tag" items="${document.tags}">
        ${tag}&nbsp;
      </c:forEach>
      &nbsp;<a id="edittags" href="#">Edit tags</a>
  </p>
  
</div>

<script language="javascript"> 
 
YUI().use("node",
function(Y) {
  
  var editTags = function(e) {
    e.preventDefault();
    Y.log("You clicked on the edit tags link.", "info", "example");
    alert("You clicked on the edit tags link.");

    // TODO: change tags via the new rest api.
  };
  
  Y.on("click", editTags, "#edittags");
});
 
</script>