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
    <link rel="stylesheet" href="${request.contextPath}/repository-servlet.css" />
  </head>

  <body>
    <div class="hippo-header">
      <#if jcrSession??>
        <div class="username-box"><noscript>Logged in as:</noscript>${jcrSession.userID}</div>
      </#if>
      <div class="logout">
        <a href="${request.contextPath}/repository/?logout">Log out</a>
      </div>
    </div>

    <div>
      <h3>Search by ...</h3>
      <form name="queryForm" method="get" action="" accept-charset="UTF-8">

        <#assign searchType = request.getParameter('search-type')!'xpath'/>
        <div class="search-type-selector">

          <!-- Lynx compatibility (no css, no javascript) -->
          <noscript><div>Please select operation:</div></noscript>

          <input class="typeInput" id="uuid-select" type="radio" value="uuid" name="search-type" <#if searchType == 'uuid'>checked="checked"</#if>>
          <label class="typeLabel" for="uuid-select">UUID</label>
          <input class="typeInput" id="text-select" type="radio" value="text" name="search-type" <#if searchType == 'text'>checked="checked"</#if>>
          <label class="typeLabel" for="text-select">Text</label>
          <input class="typeInput" id="xpath-select" type="radio" value="xpath" name="search-type" <#if searchType == 'xpath'>checked="checked"</#if>>
          <label class="typeLabel" for="xpath-select">XPath</label>
          <input class="typeInput" id="sql-select" type="radio" value="sql" name="search-type" <#if searchType == 'sql'>checked="checked"</#if>>
          <label class="typeLabel" for="sql-select">SQL</label>
          <hr>

          <div class="search-params">
            <#--UUID-->
            <div class="uuid-tab">
              <noscript>UUID:&nbsp;</noscript>
              <input name="uuid" type="text" size="60" value="${request.getParameter('uuid')!}" placeholder="UUID"/>
            </div>

            <#--FREE TEXT-->
            <div class="text-tab">
              <noscript><div>&nbsp;</div>Text:&nbsp;</noscript>
              <input name="text" type="text" size="60" value="${request.getParameter('text')!}" placeholder="Text search"/><br/>
              Limit<noscript> (text search)</noscript>: <input name="text-limit" type="text" size="5" value="${request.getParameter('text-limit')!1000?c}"/>
            </div>

            <#--XPATH-->
            <div class="xpath-tab">
              <noscript><div>&nbsp;</div>XPath:</noscript>
              <input name="xpath" type="text" size="60" value="${request.getParameter('xpath')!}" placeholder="XPath query"/><br/>
              Limit<noscript> (XPath query)</noscript>: <input name="xpath-limit" type="text" size="5" value="${request.getParameter('xpath-limit')!1000?c}"/>
            </div>

            <#--SQL2-->
            <div class="sql-tab">
              <noscript><div>&nbsp;</div>SQL:&nbsp;&nbsp;</noscript>
              <input name="sql" type="text" size="60" value="${request.getParameter('sql')!}" placeholder="SQL query"/><br/>
              Limit<noscript> (SQL query)</noscript>: <input name="sql-limit" type="text" size="5" value="${request.getParameter('sql-limit')!1000?c}"/>
            </div>
          </div>

          <noscript><div>&nbsp;</div></noscript>
          <input type="submit" value="Search"/>
        </div>
      </form>
    </div>

    <#if exception??>
      <div id="error">
        ERROR: <blockquote>${exception}</blockquote>
      </div>
    </#if>

    <hr>

    <#if currentNode??>
        <h3>Referenced node</h3>
        Accessing node:&nbsp;
        <code>
          <#assign baseRelPath = "./">
          <#if currentNode.isSame(rootNode)>/<a href="${baseRelPath}">root</a>/
          <#else>
            <#assign distance = ancestorNodes?size>
            <#assign baseRelPath = "">
            <#list 0..distance as d>
              <#assign baseRelPath = "../${baseRelPath}">
            </#list>
            /<a href="${baseRelPath}">root</a>/<#t>
            <#assign distance = ancestorNodes?size>
            <#list ancestorNodes as ancestor>
              <#assign ancestorLink = "">
              <#list 1..distance as d>
                <#assign ancestorLink = "../${ancestorLink}">
              </#list>
              <a href="${ancestorLink}">${ancestor.name!html}</a>/<#t>
              <#assign distance = distance - 1>
            </#list>
            <a href="./">${currentNode.name!html}</a>/<#t>
          </#if>
        </code>


        <ul>
          <#list currentNode.nodes as child>
            <li type="circle">
              <#assign childLink = "${child.name}">
              <#if child.index gt 1>
                <#assign childLink = "${childLink}[${child.index}]">
              </#if>
              <a href="./${childLink}/">
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
        <#if searchType == 'text'>
          Text query:&nbsp;
        <#elseif searchType == 'xpath'>
          XPath query:&nbsp;
        <#elseif searchType == 'sql'>
          SQL2 query:&nbsp;
        </#if>

        ${originalQuery?html}

      </blockquote>

      <#assign queryResultNodes = queryResult.nodes>
      Number of results found: ${queryResultTotalSize!-1}

      <ol>
        <#list queryResultNodes as node>
          <#if node??>
            <li>
              <a href="${baseRelPath}${node.path?substring(1)!url}">${node.path}</a>
    <#-- TODO
              writer.println("<a class=\"node-link\" title=\"Open node in new cms console window\" target=\"_blank\" href=\"" + req.getContextPath() + "/console/?path=" + resultNode.getPath() + "\">c</a>");
              writer.println("<a class=\"node-link\" title=\"Open node in new cms window\" target=\"_blank\" href=\"" + req.getContextPath() + "/?path=" + resultNode.getPath() + "\">cms</a>");
              writer.println("<a class=\"node-link\" title=\"Open node in repository\" href=\"" + req.getContextPath() + "/repository" + resultNode.getPath() + "\">r</a>");
    -->

            </li>
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
          <li>
            Found node: <a href="${baseRelPath}${nodeById.path?substring(1)!url}">${nodeById.path}</a>
            <#-- TODO
            writer.println("<a class=\"node-link\" title=\"Open node in new cms console window\" target=\"_blank\" href=\"" + req.getContextPath() + "/console/?path=" + n.getPath() + "\">c</a>");
            writer.println("<a class=\"node-link\" title=\"Open node in new cms window\" target=\"_blank\" href=\"" + req.getContextPath() + "/?path=" + n.getPath() + "\">cms</a>");
            writer.println("<a class=\"node-link\" title=\"Open node in repository\" href=\"" + req.getContextPath() + "/repository" + n.getPath() + "\">r</a>");-->
          </li>
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
