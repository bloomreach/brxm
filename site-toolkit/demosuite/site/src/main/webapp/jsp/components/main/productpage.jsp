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

<hst:head-contribution keyHint="title"><title>${document.title}</title></hst:head-contribution>
<hst:element name="script" var="yui3Elem">
  <hst:attribute name="type" value="text/javascript" />
  <hst:attribute name="src" value="http://yui.yahooapis.com/3.2.0/build/yui/yui-min.js" />
</hst:element>
<hst:head-contribution keyHint="yui3" element="${yui3Elem}" />

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
      <span id="<hst:namespace/>tagsLabel">${fn:join(document.tags, ', ')}</span>
      <input id="<hst:namespace/>tagsText" 
             type="text" size="20" style="DISPLAY: none"
             title="Please enter comma separated tags here." 
             value="${fn:join(document.tags, ', ')}"/>
      &nbsp;
      <a id="<hst:namespace/>tagsEdit" href="#" style="DISPLAY: none">Edit</a>
      <a id="<hst:namespace/>tagsSave" href="#" style="DISPLAY: none">Save</a>
      &nbsp;
      <a id="<hst:namespace/>tagsCancel" href="#" style="DISPLAY: none">Cancel</a>
  </p>
</form>
</div>

<script language="javascript"> 
 
YUI().use('io', 'json', 'node',
function(Y) {

  var tagsLabel = Y.one("#<hst:namespace/>tagsLabel");
  var tagsText = Y.one("#<hst:namespace/>tagsText");
  var tagsEditLink = Y.one("#<hst:namespace/>tagsEdit");
  var tagsSaveLink = Y.one("#<hst:namespace/>tagsSave");
  var tagsCancelLink = Y.one("#<hst:namespace/>tagsCancel");

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
        tagsLabel.set("innerHTML", tags.join(", "));
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
    
    var uri = "/site/preview/restapi/products/opel.html";
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
  
  tagsEditLink.setStyle("display", "");
  
  tagsEditLink.on("click", editTags);
  tagsSaveLink.on("click", saveTags);
  tagsCancelLink.on("click", cancelEditingTags);
  
});

</script>