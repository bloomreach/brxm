<%--
  Copyright 2014-2014 Hippo B.V. (http://www.onehippo.com)

  Licensed under the Apache License, Version 2.0 (the  "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS"
  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
--%>
<%@ page language="java" session="false" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="hst" uri="http://www.hippoecm.org/jsp/hst/core" %>
<html>
<head>
<title>ESI Single Page Example</title>
</head>
<body>

<h1>ESI Single Page Example</h1>
<hr/>

<form>
  <p>Random String to cause response buffer flushed before proceeding ESI Includes:</p>
  <div>
    <textarea rows="10" cols="80"><%=org.apache.commons.lang.RandomStringUtils.random(8192 * 2, true, true)%></textarea>
  </div>
</form>

<hr/>

<esi:include src="<hst:link path='/examples/poweredby.jsp'/>"/>
<esi:remove>
  Powered By Hippo
</esi:remove>

<hr/>

<esi:include src="<hst:link path='/examples/sessioninfo.jsp'/>"/>
<esi:remove>
  Session Information
</esi:remove>

</body>
</html>