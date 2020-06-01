<%--
  Copyright 2014 Hippo B.V. (http://www.onehippo.com)

  Licensed under the Apache License, Version 2.0 (the  "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS"
  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License. --%>
<%@ include file="/WEB-INF/jspf/htmlTags.jspf" %>

<hst:defineObjects/>

<c:set var="pageTitle" value="${hstRequestContext.resolvedSiteMapItem.pageTitle}"/>

<c:if test="${not empty pageTitle}">
  <hst:element var="headTitle" name="title">
    <c:out value="${pageTitle}"/>
  </hst:element>
  <hst:headContribution keyHint="headTitle" element="${headTitle}"/>
</c:if>

<div class="row-fluid">
  <div class="span6">
    <hst:include ref="left-container"/>
  </div>
  <div class="span6">
    <hst:include ref="right-container"/>
  </div>
</div>
