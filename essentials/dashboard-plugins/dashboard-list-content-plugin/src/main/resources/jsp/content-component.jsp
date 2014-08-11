<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>
<%--
  Copyright 2014 Hippo B.V. (http://www.onehippo.com)

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  --%>

<%--@elvariable id="document" type="{{beansPackage}}.BaseDocument"--%>
<hst:defineObjects/>
<c:if test="${hstRequest.requestContext.cmsRequest}">
  <h5>[Content component]</h5>
  <sub>
    This is the rendering template for the generic Content component,
    which makes the document identified by the requested URL available as
    'document'.
    As the content that you wish to display here is not generic, it is up
    to you to adjust this rendering template to correctly interact with the
    kind of content you want to display. Multiple instances of the generic
    content component may require different rendering templates.
  </sub>
</c:if>
