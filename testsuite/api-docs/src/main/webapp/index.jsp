<%@ page language="java" %>
<%--
  Copyright 2017 Hippo B.V. (http://www.onehippo.com)

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

<%!
private static final String SWAGGER_API_URI = "/site/restservices/swagger.json";
private static final String SWAGGER_VERSION = "3.1.4";
private static final String SWAGGER_UI_PATH = "/webjars/swagger-ui/" + SWAGGER_VERSION + "/index.html?url=" + SWAGGER_API_URI;
%>

<%
response.sendRedirect(request.getContextPath() + SWAGGER_UI_PATH);
%>