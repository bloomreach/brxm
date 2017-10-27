<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"> 
<%--
  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)

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
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>

<html  xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
    <hst:headContributions categoryExcludes="jsInline,jsExternal,scripts" />
    <script type="text/javascript">
    // Example custom asynchronous components rendering callback function.
    function registerAsyncComponentsRenderingCallback(cb) {
      var cf = confirm("Custom Asynchronous Component Windows Rendering call example\nWill you proceed to load Asynchronous components?");
      if (cf) {
        cb();
      }
    }
    </script>
  </head>
  <body>
      <div id="custom-doc" class="yui-t6">
        <hst:include ref="header"/>
        <hst:include ref="body"/>
        <hst:include ref="footer"/>
      </div>
      <hst:headContributions categoryIncludes="jsExternal"/>
      <hst:headContributions categoryIncludes="jsInline"/>
      <hst:headContributions categoryIncludes="scripts"/>
  </body>
</html>
