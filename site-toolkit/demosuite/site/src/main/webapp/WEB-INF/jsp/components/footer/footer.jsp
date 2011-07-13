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

<hst:link var="simpleiopath" path="/javascript/simple-io.js"/>
<hst:element name="script" var="simpleio">
  <hst:attribute name="type" value="text/javascript" />
  <hst:attribute name="src" value="${simpleiopath}" />
</hst:element>
<hst:headContribution keyHint="simpleio" element="${simpleio}" />

<div id="ft">
  <p>
    <a title="NewsRSS" href="<hst:link path="/rss.xml"/>">RSS <img src="<hst:link path="/images/rss.gif"/>" alt="RSS icon"/></a>
    &nbsp;
    <span id="<hst:namespace/>datetime"><hst:include ref="datetime"/></span>
  </p>
</div>

<script language="javascript">
<!--
function <hst:namespace/>mycb(req) {
    var text = req.responseText;
    if (text) {
    	simpleio_objectbyid("<hst:namespace/>datetime").innerHTML = text;
    }
    window.setTimeout("<hst:namespace/>refreshDateTime();", 60000, "javascript");
}
function <hst:namespace/>refreshDateTime() {
	simpleio_sendrequest("<hst:resourceURL/>", <hst:namespace/>mycb, null);
}
window.setTimeout("<hst:namespace/>refreshDateTime();", 60000, "javascript");
//-->
</script>
