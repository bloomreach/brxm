<%--
  Copyright 2017 Hippo B.V. (http://www.onehippo.com)

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
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<hst:defineObjects />

<table border="2">
  <tr><th>Begin</th><td>${randomStringHolder.formattedBeginTimestamp}</td></tr>
  <tr><th>End</th><td>${randomStringHolder.formattedEndTimestamp}</td></tr>
  <tr><th>Duration</th><td>${randomStringHolder.durationMillis} ms</td></tr>
  <tr><th>Result Random String</th><td>${randomStringHolder.randomString}</td></tr>
</table>
