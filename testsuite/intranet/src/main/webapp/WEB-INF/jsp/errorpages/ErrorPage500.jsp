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
<!doctype html>
<%@ include file="/WEB-INF/jspf/htmlTags.jspf" %>
<%@ page isErrorPage="true" %>
<% response.setStatus(500); %>

<html lang="en">
  <head>
    <meta charset="utf-8"/>
    <title>500 error</title>
  </head>
  <body>
    <h1>Server error</h1>
    <% out.println("<!-- An unexcepted error occurred. The name of the exception is:"); %>
    <%= exception %>
    <% out.println("-->"); %>
  </body>
</html>