<%--
  Copyright 2009 Hippo

  Licensed under the Apache License, Version 2.0 (the  "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS"
  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>

<hst:element name="script" var="yui3Elem">
  <hst:attribute name="type" value="text/javascript" />
  <hst:attribute name="src" value="http://yui.yahooapis.com/3.2.0/build/yui/yui-min.js" />
</hst:element>
<hst:head-contribution keyHint="yui3" element="${yui3Elem}" />

<div id="ft">
  Par exemple français de bas de page
  <p>
    <a title="NewsRSS" href="<hst:link path="/rss.xml"/>">RSS <img src="<hst:link path="/images/rss.gif"/>" alt="RSS icon"/></a>
    &nbsp;
    <span id="<hst:namespace/>datetime"><hst:include ref="datetime"/></span>
  </p>
</div>

<script language="javascript"> 
YUI().use('io', 'node', 'async-queue',
function(Y) {
  var datetimePane = Y.one("#<hst:namespace/>datetime");
  var asyncQueue = new Y.AsyncQueue();
  var updateTimeout = 60000;

  var onUpdateTimeComplete = function(id, o, args) {
    datetimePane.set("innerHTML", o.responseText);
    asyncQueue.add({fn: function() {}, timeout: updateTimeout}, updateTime);
    asyncQueue.run();
  };
	  
  var updateTime = function(e) {
    var uri = '<hst:resourceURL/>';
    var cfg = { 
          on: { complete: onUpdateTimeComplete },
          arguments: {},
          method: "GET"
    };
    var request = Y.io(uri, cfg);
  };
  
  asyncQueue.add({fn: function() {}, timeout: updateTimeout}, updateTime);
  asyncQueue.run();
});
</script>