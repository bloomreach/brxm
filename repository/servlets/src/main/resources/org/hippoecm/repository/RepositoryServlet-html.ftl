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
  <title>Hippo Repository Browser</title>
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
      <h3>Search by ...</h3>
      <table style="params" summary="searching">
        <form method="get" action="" accept-charset="UTF-8">
        <tr>
          <th>UUID: </th>
          <td>
              <input name="uuid" type="text" size="80" value="${request.getParameter('uuid')!}"/>
              <input type="submit" value="Fetch"/>
          </td>
        </tr>
        </form>
        <form method="get" action="" accept-charset="UTF-8">
        <tr>
          <th>XPath: </th>
          <td>
              <input name="xpath" type="text" size="120" value="${request.getParameter('xpath')!}"/>
          </td>
        </tr>
        <tr>
          <td>Limit: </td>
          <td>
              <input name="limit" type="text" size="5" value="${request.getParameter('limit')!1000?c}"/>
              <input type="submit" value="Search"/>
          </td>
        </tr>
        </form>
        <form method="get" action="" accept-charset="UTF-8">
        <tr>
          <th>SQL: </th>
          <td>
              <input name="sql" type="text" size="120" value="${request.getParameter('sql')!}"/>
          </td>
        </tr>
        <tr>
          <td>Limit: </td>
          <td>
              <input name="limit" type="text" size="5" value="${request.getParameter('limit')!1000?c}"/>
              <input type="submit" value="Search"/>
          </td>
        </tr>
        </form>
      </table>
    </td>
    <td class="header">
      <h3>Request Information</h3>
      <table style="params" summary="request parameters">
        <tr>
          <th>Name</th>
          <th>Value</th>
        </tr>
        <tr>
          <td>Servlet Path : </td>
          <td><code>${request.servletPath!}</code></td>
        </tr>
        <tr>
          <td>Request URI : </td>
          <td><code>${request.requestURI!}</code></td>
        </tr>
        <tr>
          <td>Relative Path : </td>
          <td><code>${currentNodePath!}</code></td>
        </tr>
      </table>
    </td>
    <td class="header">
      <h3>Login Information</h3>
      <table style="params" summary="login parameters">
        <tr>
          <th>Logged in as : </th>
          <td>
              <#if jcrSession??>
                <code>${jcrSession.userID}</code>
              </#if>
          </td>
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

<hr>

<#if currentNode??>
    <h3>Referenced node</h3>
    Accessing node : &nbsp;&nbsp;

    <code>
      <#assign baseRelPath = "./">
      <#if currentNode.isSame(rootNode)>
        / <a href="${baseRelPath}">root</a> /
      <#else>
        <#assign distance = ancestorNodes?size>
        <#assign baseRelPath = "">
        <#list 0..distance as d>
          <#assign baseRelPath = "../${baseRelPath}">
        </#list>
        / <a href="${baseRelPath}">root</a> /

        <#assign distance = ancestorNodes?size>
        <#list ancestorNodes as ancestor>
          <#assign ancestorLink = "">
          <#list 1..distance as d>
            <#assign ancestorLink = "../${ancestorLink}">
          </#list>
          <a href="${ancestorLink}">${ancestor.name!html}</a>
          /
          <#assign distance = distance - 1>
        </#list>
        <a href="./">${currentNode.name!html}</a>
        /
      </#if>
    </code>

    <ul>
      <#list currentNode.nodes as child>
        <li type="circle">
          <#assign childLink = "${child.name}">
          <#if child.index gt 1>
            <#assign childLink = "${childLink}[${child.index}]">
          </#if>
          <a href="./${childLink?url}/">
            ${child.name?html}
            <#if child.hasProperty("hippo:count")>
            [${child.getProperty("hippo:count").long}]
            </#if>
          </a>
        </li>
      </#list>
      <#list currentNode.properties as prop>
        <li type="disc">
          [name="${prop.name}"] =
          <#if prop.definition.multiple>
            [
              <#list prop.values as value>
                <#if value.type != 2>
                  ${value.string!?html},
                <#else>
                  ${prop.length} bytes.
                </#if>
              </#list>
            ]
          <#else>
            <#if prop.type != 2>
              ${prop.string!?html}
            <#else>
              ${prop.length} bytes.
            </#if>
          </#if>
        </li>
      </#list>
    </ul>
</#if>
<hr>

<#if queryResult??>
  <h3>Query executed</h3>

  <blockquote>
    <#if request.getParameter("xpath")??>
      ${request.getParameter("xpath")!?html}
    <#else>
      ${request.getParameter("sql")!?html}
    </#if>
  </blockquote>

  <#assign queryResultNodes = queryResult.nodes>
  Number of results found: ${queryResultTotalSize!-1}

  <ol>
    <#list queryResultNodes as node>
      <#if node??>
        <li><a href="${baseRelPath}${node.path?substring(1)!url}">${node.path}</a></li>
      </#if>
    </#list>
  </ol>

  <hr/>

  <table summary="searchresult" border="1">
    <tr>
      <th>#</th>
      <#list queryResult.columnNames as columnName>
        <th>${columnName}</th>
      </#list>
    </tr>
    <#list queryResult.rows as row>
      <#if row??>
        <tr>
          <td>${row_index + 1}</td>
          <#assign values = row.values>
          <#if values??>
            <#list row.values as value>
              <#if value?? && value.type != 2>
                <td>${value.string!}</td>
              <#else>
                <td></td>
              </#if>
            </#list>
          </#if>
        </tr>
      </#if>
    </#list>
  </table>
</#if>

<#if repositoryMap??>
  <h3>Repository as map</h3>
  <blockquote>
    _name = ${repositoryMap.get("_name")!}<br/>
    _location = ${repositoryMap.get("_location")!}<br/>
    _path = ${repositoryMap.get("_path")!}<br/>
    _index = ${repositoryMap.get("_index")!}<br/>
    _size = ${repositoryMap.get("_size")!}<br/>
    <#list repositoryMap?keys as key>
      ${key} = ${repositoryMap.get(key)!}
    </#list>
  </blockquote>
</#if>

<#if nodeById??>

  <#if request.getParameter("uuid")??>

    <h3>Get node by UUID</h3>
    <blockquote>
      UUID = ${request.getParameter("uuid")!}
    </blockquote>
    <ol>
      <li>Found node: <a href="${baseRelPath}${nodeById.path?substring(1)!url}">${nodeById.path}</a></li>
    </ol>

  <#elseif request.getParameter("deref")??>

    <h3>Getting nodes having a reference to </h3>
    <blockquote>
      UUID = ${request.getParameter("uuid")!}
      ( <a href="${baseRelPath}${nodeById.path?substring(1)!url}">${nodeById.path}</a> )
    </blockquote>
    <hr>
    <table>
      <tr>
        <th align="left">Node path</th>
        <th align="left">Property reference name</th>
      </tr>
      <#list nodeById.references as prop>
        <tr>
          <td>${prop.parent.path!}</td>
          <td>${prop.name!}</td>
        </tr>
      </#list>
    </table>

  </#if>

</#if>

<br/>

</body>
</html>
