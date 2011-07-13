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
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>

<hst:defineObjects/>

<hst:headContribution keyHint="title"><title>${document.title}</title></hst:headContribution>
<hst:element name="script" var="yui3Elem">
  <hst:attribute name="type" value="text/javascript" />
  <hst:attribute name="src" value="http://yui.yahooapis.com/3.2.0/build/yui/yui-min.js" />
</hst:element>
<hst:headContribution keyHint="yui3" element="${yui3Elem}" />

<div id="<hst:namespace/>detailPane" class="yui-u">
<form name="theForm">
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
    Price : ${document.price}
  </p>
  <p>
    Tags : 
      <span id="<hst:namespace/>tagsLabel">${fn:escapeXml(fn:join(document.tags, ', '))}</span>
      <input id="<hst:namespace/>tagsText" 
             type="text" size="20" style="DISPLAY: none"
             title="Please enter comma separated tags here." 
             value="${fn:escapeXml(fn:join(document.tags, ', '))}"/>
      &nbsp;
      <a id="<hst:namespace/>tagsEdit" href="#" style="DISPLAY: none">Edit</a>
      <a id="<hst:namespace/>tagsSave" href="#" style="DISPLAY: none">Save</a>
      &nbsp;
      <a id="<hst:namespace/>tagsCancel" href="#" style="DISPLAY: none">Cancel</a>
      &nbsp;
      <c:if test="${isPreview and empty(hstRequest.userPrincipal)}">
        <span><em>(Authentication required to edit tags.)</em></span>
      </c:if>
  </p>
</form>
  <p>
    Image:
    <br/>
    <c:if test="${not empty document.image}">
      <a href="<hst:link hippobean="${document.image.original}"><hst:param name="t" value="<%=Long.toString(System.currentTimeMillis())%>"/></hst:link>" target="_blank">
        <img id="productImg" src="<hst:link hippobean="${document.image.thumbnail}"><hst:param name="t" value="<%=Long.toString(System.currentTimeMillis())%>"/></hst:link>" border="0"/>
      </a>
    </c:if>
    <br/>
    <c:if test="${isPreview and empty(hstRequest.userPrincipal)}">
      <span><em>(Authentication required to upload image.)</em></span>
    </c:if>
    <form id="uploadForm" method="POST" enctype="multipart/form-data">
      <input type="file" id="<hst:namespace/>uploadFile" name="file" style="DISPLAY: none" />
      <input type="button" id="<hst:namespace/>uploadButton" value="Upload" style="DISPLAY: none" />
    </form>
  </p>
</div>

<c:choose>
  <c:when test="${isPreview}">
    <a href="<hst:link hippobean="${document}" mountType="live"/>">View in Live</a>
  </c:when>
  <c:otherwise>
    <a href="<hst:link hippobean="${document}" mountType="preview"/>">Edit in Preview</a>
  </c:otherwise>
</c:choose>

<script language="javascript"> 
 
YUI().use('io-upload-iframe', 'json', 'node',
function(Y) {

  var tagsLabel = Y.one("#<hst:namespace/>tagsLabel");
  var tagsText = Y.one("#<hst:namespace/>tagsText");
  var tagsEditLink = Y.one("#<hst:namespace/>tagsEdit");
  var tagsSaveLink = Y.one("#<hst:namespace/>tagsSave");
  var tagsCancelLink = Y.one("#<hst:namespace/>tagsCancel");
  
  var uploadForm = Y.one("#uploadForm");
  var uploadFile = Y.one("#<hst:namespace/>uploadFile");
  var uploadButton = Y.one("#<hst:namespace/>uploadButton");
  
  var editTags = function(e) {
    tagsText.set("value", tagsLabel.get("text"));
    tagsLabel.setStyle("display", "none");
    tagsText.setStyle("display", "");
    tagsEditLink.setStyle("display", "none");
    tagsSaveLink.setStyle("display", "");
    tagsCancelLink.setStyle("display", "");
    e.halt();
  };

  var onSaveComplete = function(id, o, args) {
    if (o.status > 400) {
      alert("You are not authorized to edit the tags.");
      return;
    }
     
    var id = id;
    var data = o.responseText;
    var dataOut = null;

    try {
      dataOut = Y.JSON.parse(data);
      if (!dataOut) {
        Y.log("Error: no data found.");
        return;
      }

      var tags = dataOut.tags;
      if (!tags) {
        tagsLabel.set("innerHTML", "");
      } else {
        tagsLabel.set("innerHTML", encodeURIComponent(tags.join(", ")));
      } 
    } catch (e) {
      Y.log("Error: " + e.message);
      return;
    }
  };
  
  var saveTags = function(e) {
    var value = tagsText.get("value").replace(/^\s+/, "").replace(/\s+$/, "");
    if (!value) {
      return;
    }
    var tags = tagsText.get("value").split(/,/);
    for (var i = 0; i < tags.length; i++) {
      tags[i] = tags[i].replace(/^\s+/, "").replace(/\s+$/, "");
    }
    
    var data = {};
    data["brand"] = "${document.brand}";
    data["color"] = "${document.color}";
    data["product"] = "${document.product}";
    data["price"] = ${document.price};
    data["tags"] = tags;

    var uri = '<hst:link path="${hstRequest.requestContext.resolvedSiteMapItem.pathInfo}" mount="restapi"/>';
    var cfg = { 
          on: { complete: onSaveComplete },
          arguments: {},
          method: "PUT",
          headers: { 
              "Accept": "application/json, text/javascript, text/html, application/xhtml+xml, application/xml, text/xml", 
              "Content-Type": "application/json" 
                  },
          data: Y.JSON.stringify(data)
    };
    var request = Y.io(uri, cfg);
    
    tagsText.set("value", "");
    tagsLabel.setStyle("display", "");
    tagsText.setStyle("display", "none");
    tagsEditLink.setStyle("display", "");
    tagsSaveLink.setStyle("display", "none");
    tagsCancelLink.setStyle("display", "none");
    
    e.halt();
  };
  
  var cancelEditingTags = function(e) {
	tagsText.set("value", "");
    tagsLabel.setStyle("display", "");
    tagsText.setStyle("display", "none");
    tagsEditLink.setStyle("display", "");
    tagsSaveLink.setStyle("display", "none");
    tagsCancelLink.setStyle("display", "none");
    
    e.halt();
  };

  var refreshProductImage = function() {
    var productImg = Y.one("#productImg");
	if (productImg) {
	  var src = "" + productImg.get("src");
      if (src.indexOf('?') != -1) {
        src = src.substring(0, src.indexOf('?'));
      }
      src += "?t=" + new Date().getTime();
      productImg.set("src", src);
	}
  };
  
  var onUploadImageComplete = function(id, o, args) {
    refreshProductImage();
  };
  
  var uploadImageForm = function(e) {
    var cfg = {
          on: { complete: onUploadImageComplete },
          arguments: {},
          method: 'POST',
		  form: {
		    id: uploadForm,
		    upload: true
          }
    };
    
    var uri = '<hst:link hippobean="${document.image}" mount="restapi-gallery" subPath="original/content" />';
    var request = Y.io(uri, cfg);

    e.halt();
  };
  
<c:if test="${isPreview and not(empty(hstRequest.userPrincipal))}">
  tagsEditLink.setStyle("display", "");
  uploadFile.setStyle("display", "");
  uploadButton.setStyle("display", "");
</c:if>
  
  tagsEditLink.on("click", editTags);
  tagsSaveLink.on("click", saveTags);
  tagsCancelLink.on("click", cancelEditingTags);

  uploadButton.on("click", uploadImageForm);
});

</script>
