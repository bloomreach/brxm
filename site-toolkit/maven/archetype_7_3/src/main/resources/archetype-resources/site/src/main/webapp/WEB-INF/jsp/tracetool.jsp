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

<%@ page language="java" import="org.hippoecm.hst.logging.*, org.hippoecm.hst.site.HstServices" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>
<%
LogEventBuffer traceLogEventBuffer = (LogEventBuffer) HstServices.getComponentManager().getComponent("hstTraceToolLogEventBuffer");
String traceLogLevelName = traceLogEventBuffer.getLevelName();
%>

<hst:element var="hstTraceToolStyles" name="style">
  <hst:attribute name="id" value="hstTraceToolStyles" />
  <hst:attribute name="type" value="text/css" />
  @import url('<hst:link path="/javascript/dojo/dijit/themes/tundra/tundra.css"/>');
</hst:element>
<hst:head-contribution keyHint="hstTraceToolStyles" element="${hstTraceToolStyles}" />

<hst:link var="dojoPath" path="/javascript/dojo/dojo/dojo.js"/>
<hst:element var="hstTraceToolDojoInclude" name="script">
  <hst:attribute name="id" value="hstTraceToolDojoInclude" />
  <hst:attribute name="language" value="javascript" />
  <hst:attribute name="type" value="text/javascript" />
  <hst:attribute name="src" value="${dojoPath}" />
  <hst:attribute name="djConfig" value="parseOnLoad: true" />
</hst:element>
<hst:head-contribution keyHint="hstTraceToolDojoInclude" element="${hstTraceToolDojoInclude}" />

<hst:element var="hstTraceToolDojoRequires" name="script">
  <hst:attribute name="language" value="javascript" />
  <hst:attribute name="type" value="text/javascript" />
  dojo.require("dojo.parser");
  dojo.require("dijit.layout.TabContainer");
  dojo.require("dijit.layout.ContentPane");
</hst:element>
<hst:head-contribution keyHint="hstTraceToolDojoRequires" element="${hstTraceToolDojoRequires}" />

<hst:resourceURL var="logResourcePath" resourceId="/WEB-INF/jsp/tracetool-log.jsp" />

<a href="javascript:toggleHstTraceWindow()">>> HST Traces</a>
<div id="hstTraceTabContainer" dojoType="dijit.layout.TabContainer" class="tundra" style="DISPLAY: 'none'; WIDTH: 100%; HEIGHT: 400px">
  <div dojoType="dijit.layout.ContentPane" title="Logs"
       href="${logResourcePath}" preventCache="true" refreshOnShow="true"
       style="font-size: 10px; white-space: pre">
  </div>
  <div dojoType="dijit.layout.ContentPane" title="Settings">
    <form name="theForm">
      <div>
        Log Level: 
        <select id="<hst:namespace/>logLevel">
          <option value="DEBUG" <%=("DEBUG".equals(traceLogLevelName) ? "selected" : "")%>>DEBUG</option>
          <option value="INFO" <%=("INFO".equals(traceLogLevelName) ? "selected" : "")%>>INFO</option>
          <option value="WARN" <%=("WARN".equals(traceLogLevelName) ? "selected" : "")%>>WARN</option>
          <option value="ERROR" <%=("ERROR".equals(traceLogLevelName) ? "selected" : "")%>>ERROR</option>
        </select>
        <input id="<hst:namespace/>logLevelSave" type="button" value="Save"/>
      </div>
    </form>
  </div>
</div>

<hst:resourceURL var="logLevelUrl" resourceId="/WEB-INF/jsp/tracetool-level.jsp" />

<script type="text/javascript" language="javascript">
function toggleHstTraceWindow() {
    var hstTraceTabContainer = document.getElementById("hstTraceTabContainer");
    if (hstTraceTabContainer.style.display == "") {
        hstTraceTabContainer.style.display = "none";
    } else {
        hstTraceTabContainer.style.display = "";
    }
}
dojo.addOnLoad(function() {
    var btnSaveNode = dojo.byId("<hst:namespace/>logLevelSave");
    dojo.connect(btnSaveNode, "onclick", function() {
        var logLevel = dojo.byId("<hst:namespace/>logLevel").value;
        var logLevelUrl = "${logLevelUrl}" + ("${logLevelUrl}".indexOf("?") >= 0 ? "&" : "?") + "level=" + logLevel;

        var xhrArgs = {
            url: logLevelUrl,
            handleAs: "text",
            load: function(data) {
                var arr = eval(data.replace(/^\s+/g, ""));
                if (arr[0] != "OK") {
                    alert("Failed to apply the log level. " + arr[1]);
                } else {
                    alert("The log level has been applied.");
                }
            },
            error: function(error) {
                alert("An unexpected error occurred: " + error);
            }
        };

        var deferred = dojo.xhrGet(xhrArgs);
    });
});
</script>
