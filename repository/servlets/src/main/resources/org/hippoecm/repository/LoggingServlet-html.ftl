<!DOCTYPE html>
<#--
  Copyright 2014 Hippo B.V. (http://www.onehippo.com)

  Licensed under the Apache License, Version 2.0 (the  "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS"
  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

${response.setContentType("text/html;charset=UTF-8")}

<html>

<head>
  <title>Hippo Logging Configuration Browser</title>
  <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
  <style type="text/css">
    body { background-color: #efefef }
    h3 {margin:2px}
    table.params {font-size:small}
    td.header {text-align: left; vertical-align: top; padding: 10px;}
    td {text-align: left}
    th {text-align: left}
    #infotable { background-color: #cfcfcf }
    #error { background-color: #efef00; font-size: large; padding: 10px }
  </style>
</head>

<body>

<table id="infotable" width="100%">
  <tr>
    <td class="header">
      <h3>Request Information</h3>
      <table style="params" summary="request parameters">
        <tr>
          <th>Name</th>
          <th>Value</th>
        </tr>
        <tr>
          <td>Context Path : </td>
          <td><code>${request.contextPath!}</code></td>
        </tr>
        <tr>
          <td>Servlet Path : </td>
          <td><code>${request.servletPath!}</code></td>
        </tr>
        <tr>
          <td>Request URI : </td>
          <td><code>${request.requestURI!}</code></td>
        </tr>
      </table>
    </td>
  </tr>
</table>

<#if exception??>
  <div id="error">
    ERROR: <blockquote>${exception}</blockquote>
  </div>
</#if>

<#if loggerInUse??>
    <hr>
    <h3>Logging</h3>
    <p>Logger in use : <code>${loggerInUse.class.name}</code></p>
</#if>

<#if loggerLevelInfosMap??>
  <hr>
  <h3>Loggers</h3>

  <form method="POST">

  <input type="submit" value="Apply" />

  <table summary="searchresult" border="1">
    <tr>
      <th>Logger</th>
      <th>Current Level</th>
      <th>New Level</th>
    </tr>
    <#list loggerLevelInfosMap?keys as loggerName>
      <#assign loggerLevelInfo = loggerLevelInfosMap[loggerName]>
      <#assign curLogLevel = loggerLevelInfo.effectiveLogLevel>
      <tr>
        <td>${loggerName!}</td>
        <td>
          <#if loggerLevelInfo.logLevel??>
            ${curLogLevel!}
          <#else>
            ${curLogLevel!} (unset)
          </#if>
        </td>
        <td>
          <select name="ll" id="ll_${loggerName!}">
            <#list logLevels as logLevel>
              <#if "${curLogLevel!}" == "${logLevel!}">
                <option value="${loggerName!}:${logLevel!}" selected="true">${logLevel!}</option>
              <#else>
                <option value="${loggerName!}:${logLevel!}">${logLevel!}</option>
              </#if>
            </#list>
          </select>
        </td>
      </tr>
    </#list>
  </table>

  <input type="submit" value="Apply" />

  </form>

</#if>

<br/>

</body>
</html>
