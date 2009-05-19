<%--
  Copyright 2008 Hippo

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

<hst:link var="dojoPath" path="/staticresource/javascript/dojo-1.3.0"/>

<hst:element var="hstTraceToolStyles" name="style">
  <hst:attribute name="type" value="text/css" />
  @import url("${dojoPath}/dijit/themes/tundra/tundra.css");
  @import url("${dojoPath}/dojox/layout/resources/FloatingPane.css");
  @import url("${dojoPath}/dojox/layout/resources/ResizeHandle.css");
</hst:element>
<hst:head-contribution keyHint="hstTraceToolStyles" element="${hstTraceToolStyles}" />

<hst:element var="hstTraceToolDojoInclude" name="script">
  <hst:attribute name="language" value="javascript" />
  <hst:attribute name="type" value="text/javascript" />
  <hst:attribute name="src" value="${dojoPath}/dojo/dojo.js" />
  <hst:attribute name="djConfig" value="parseOnLoad: true" />
</hst:element>
<hst:head-contribution keyHint="hstTraceToolDojoInclude" element="${hstTraceToolDojoInclude}" />

<hst:element var="hstTraceToolDojoRequires" name="script">
  <hst:attribute name="language" value="javascript" />
  <hst:attribute name="type" value="text/javascript" />
  dojo.require("dojo.parser");
  dojo.require("dojox.layout.FloatingPane");
  dojo.require("dijit.layout.TabContainer");
  dojo.require("dijit.layout.ContentPane");
</hst:element>
<hst:head-contribution keyHint="hstTraceToolDojoRequires" element="${hstTraceToolDojoRequires}" />

<hst:resourceURL var="logResourcePath" resourceId="log" />

<div dojoType="dojox.layout.FloatingPane" id="hstTrace" title="HST Trace" class="tundra"
     resizable="true" dockable="true" maxable="true" closable="true"
     style="width: 600px; height: 400px; visibility: hidden;">
  <div id="tabContainer" dojoType="dijit.layout.TabContainer">
    <div dojoType="dijit.layout.ContentPane" title="Logs"
         href="${logResourcePath}" preventCache="true" refreshOnShow="true"
         style="font-size: 10px; white-space: pre">
    </div>
    <div dojoType="dijit.layout.ContentPane" title="Settings">
      
    </div>
  </div>
</div>
