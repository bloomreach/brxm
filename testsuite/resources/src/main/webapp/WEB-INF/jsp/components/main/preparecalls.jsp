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
<%@ page import="org.hippoecm.hst.demo.util.SimpleTimeKeeper" %>

<hst:defineObjects />

<h2>An example demonstrating HstComponent#prepareBeforeRender() calls</h2>

<hr/>

<p>
This 'parent' component includes four child components, each of which generates a random string (40 chars) in an asynchronous
job by creating a <code>java.util.concurrent.Future</code> instance on <code>HstComponent#prepareBeforeRender(HstRequest, HstResponse)</code>
method invocation and getting the result of the <code>java.util.concurrent.Future</code> instance on
on <code>HstComponent#doBeforeRender(HstRequest, HstResponse)</code> method invocation.
</p>

<p>
By executing business service invocations in parallel in <code>HstComponent#prepareBeforeRender(HstRequest, HstResponse)</code> method
(in this simple example, each <code>java.util.concurrent.Callable</code> job just waits for 500ms for demonstration purpose
without having to invoke real external services for simplicity), the whole page can be rendered usually faster than
sequential executions by only traditional <code>HstComponent#doBeforeRender(HstRequest, HstResponse)</code> method invocations.
</p>

<p>
Compare the following demo result.
</p>

<hr/>

<h3>Parent component's time records as a whole</h3>

<%
SimpleTimeKeeper timeKeeper = (SimpleTimeKeeper) request.getAttribute("timeKeeper");
if (timeKeeper != null) {
    timeKeeper.end();
}
%>

<div>
  <table border="2">
    <tr><th>Begin<th><td>${timeKeeper.formattedBeginTimestamp}</td></tr>
    <tr><th>End<th><td>${timeKeeper.formattedEndTimestamp}</td></tr>
    <tr><th>Duration<th><td>${timeKeeper.durationMillis} ms</td></tr>
  </table>
</div>

<hr/>

<h3>Each Child component's time records</h3>
<hr/>

<c:forEach var="childContentName" items="${hstResponse.childContentNames}">
  <div>
    <h4>Child component: ${childContentName}</h4>
    <div>
        <hst:include ref="${childContentName}" />
    </div>
  </div>
  <hr/>
</c:forEach>
