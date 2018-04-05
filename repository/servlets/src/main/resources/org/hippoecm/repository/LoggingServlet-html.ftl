<#ftl output_format="HTML">
<!DOCTYPE html>
<#--
  Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)

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

    .filter {
        padding: 10px 30px 30px;
    }
    .filter input, .filter select {
        font-size: 16px;
        height: 28px;
        width: 800px;

    }
    .button {
        align-self: center;
        padding: 8px 20px 8px 20px;
        margin: 20px;
        color: #fff;
        font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;
        font-size: 16px;
        letter-spacing: 1px;
        text-transform: uppercase;
        text-align: center;
        background: #3e92d2;
        border-radius: 4px;
        transition: all 0.1s ease-in-out;
        cursor: pointer;
        border: 0;
    }

    .button:hover {
        background: #f66f6f;
    }

    .button:active {
        transform: scale(1.025);
    }
  </style>
    <script type="application/javascript">
        function filter(value) {
            if (value) {
                value = value.replace(/[^A-Za-z0-9_$.]+/g, "");
            }
            var fieldValue = document.getElementById('filter');
            if (value && value !== fieldValue) {
                document.getElementById('filter').value = value;
            }
            var rows = document.getElementById("loggers").getElementsByTagName("TR");
            if (value && value.length > 0) {
                var size = rows.length;
                var show = [];
                var hide = [];
                for (var i = 0; i < size; i++) {
                    var row = rows[i];
                    var el = row.getElementsByTagName("SELECT");
                    if (el && el[0]) {
                        var select = el[0];
                        if (select.id && select.id.indexOf(value) !== -1) {
                            show.push(row);
                        } else {
                            hide.push(row);
                        }
                    }

                }
                showHide(show, false);
                showHide(hide, true);

            } else {
                showHide(rows, false);
            }

        }

        function setLevel() {
            var dbg = document.getElementById('debugLevel');
            var debugValue = dbg.options[dbg.selectedIndex].value;

            var rows = document.getElementById("loggers").getElementsByTagName("TR");
            var size = rows.length;
            for (var i = 0; i < size; i++) {
                var row = rows[i];
                if (row.style.display === "") {
                    var el = row.getElementsByTagName("SELECT");
                    if (el && el[0]) {
                        var select = el[0];
                        var opts = select.options;
                        for (var j = 0; j < opts.length; j++) {
                            var opt = opts[j];
                            if (opt && opt.value && opt.value.indexOf(debugValue) !== -1) {
                                opt.selected = true;
                                break;
                            }

                        }
                    }
                }

            }
        }

        function showHide(rows, hide) {
            var size = rows.length;
            for (var i = 0; i < size; i++) {
                var element = rows[i];
                element.style.display = hide ? "none" : "";

            }
        }

        function getParam(name) {
            var url = window.location.href;
            name = name.replace(/[\[\]]/g, "\\$&");
            var regex = new RegExp("[?&]" + name + "(=([^&#]*)|&|#|$)"),
                    results = regex.exec(url);
            if (!results) {
                return null;
            }
            if (!results[2]) {
                return '';
            }
            return decodeURIComponent(results[2].replace(/\+/g, " "));
        }
    </script>
</head>
<body onload="filter(getParam('query'))">

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
    <div class="filter">
        <p>Filter loggers which contain:</p>
        <form action="#" method="get">
            <input id="filter" type="search" value="" name="query"/>
            <a class="button" onclick="filter(document.getElementById('filter').value)">Filter</a>
        </form>

    </div>
    <div class="filter">
        <p>Set logging level to filtered loggers:</p>
        <form action="#" method="get">
            <select name="debugLevel" id="debugLevel">
                <option value="TRACE">TRACE</option>
                <option value="ALL">ALL</option>
                <option value="ERROR">ERROR</option>
                <option value="INFO">INFO</option>
                <option value="FATAL">FATAL</option>
                <option value="DEBUG">DEBUG</option>
                <option value="OFF">OFF</option>
                <option value="WARN" selected="selected">WARN</option>
            </select>
            <a class="button" onclick="setLevel()">Set level</a>
        </form>
    </div>
  <h3>Loggers</h3>

  <form method="POST">

  <input class="button" type="submit" value="Apply" />

      <table id="loggers" summary="searchresult" border="1">
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

  <input type="submit" class="button" value="Apply" />

  </form>

</#if>

<br/>

</body>
</html>
