<#ftl output_format="HTML">
<!DOCTYPE html>
<#--
  Copyright 2014-2021 Hippo B.V. (http://www.onehippo.com)

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
    <title>Bloomreach Repository Browser</title>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <style>
        * {font-family: tahoma, arial, helvetica, sans-serif;}
        * {font-size: 14px;}
        body { background-color: #efefef }
        h3 {margin:12px 2px 2px 2px}
        td {text-align: left}
        th {text-align: left}
        tt, code, code *, kbd, samp { font-family: monospace; font-size: 13px;}
        form {margin-top: 10px}
        a {text-decoration: none;}

        #error { background-color: #efef00; font-size: large; padding: 10px }
  
        .hippo-header {
          height: 31px;
          background-color: #C8C8C8;
        }
        .hippo-header .logo {
          background: #ffffff url("data:image/svg+xml;utf8,<svg width='28px' height='28px' viewBox='0 0 28 28' version='1.1' xmlns='http://www.w3.org/2000/svg' xmlns:xlink='http://www.w3.org/1999/xlink'><g stroke='none' stroke-width='1' fill='none' fill-rule='evenodd'><circle fill= '%23FFD500' fill-rule='nonzero' cx='14' cy='14' r='14'></circle><path fill= '%23002840' fill-rule='nonzero' d='M16.9363368,11.2224064 L16.242042,12.9724775 C16.9930774,13.4509726 17.4401201,14.2740357 17.4254991,15.1513741 C17.4254991,16.5126346 16.2986809,17.6161542 14.9086802,17.6161542 C13.5186796,17.6161542 12.3918614,16.5126346 12.3918614,15.1513741 C12.3619709,14.496338 12.6006638,13.856774 13.0548257,13.3749987 C13.5089877,12.8932234 14.1409396,12.6092067 14.8100588,12.5861484 L15.4609603,10.8090342 C14.2023973,10.6080828 12.9172847,10.9594377 11.9470069,11.7697628 C10.9767291,12.5800879 10.4195119,13.7673503 10.4233577,15.0161589 L10.4233577,15.0161589 L10.4233577,19.4782609 L12.3958062,19.4782609 L12.3958062,18.7674152 C13.1478968,19.2347016 14.021984,19.4788937 14.9126251,19.4705343 C16.9905273,19.4985133 18.8105189,18.1118772 19.2938096,16.1325471 C19.7771002,14.1532171 18.7945029,12.1102816 16.9284471,11.2146798 L16.9363368,11.2224064 Z'></path><path fill= '%23002840' fill-rule='nonzero' d='M10.4233577,10.7536232 C11.0064842,10.1996182 11.7009686,9.75811003 12.4671533,9.4543104 L12.4671533,6.69565217 L10.4233577,6.69565217 L10.4233577,10.7536232 Z'></path></g></svg>") no-repeat 0px 0px/23px 23px;
          border-radius: 50%;
          width: 23px;
          height: 23px;
          float: left;
          margin: 5px;
        }

        .username-box { float:left; color: #505050; font-weight: bold; font-size: small; padding: 7px 0 0 7px;}
        .logout { float: right; padding: 6px 12px 0 0;}
        .logout a { color: #505050; font-size: small;}

        .query-input {width: 100%;box-sizing: border-box;}

        .search-params {padding: 20px 0 10px 0;}
        .search-params input { margin-bottom: 4px}

        .search-type-selector {position:relative; margin-bottom: 20px;}
        .search-type-selector hr { height: 1px; margin: 0;}
        .search-type-selector .typeInput {position:absolute;left:-9999px}
        .search-type-selector .typeLabel {display:inline-block; padding:8px 10px 5px 10px; cursor:pointer; min-width: 40px; text-align: center;}
        .search-type-selector .typeInput:checked+.typeLabel { font-weight:bold; border-bottom: 4px solid #C8C8C8;}

        .search-type-selector #uuid-select:checked ~ .search-params .text-tab {display:none}
        .search-type-selector #uuid-select:checked ~ .search-params .xpath-tab {display:none}
        .search-type-selector #uuid-select:checked ~ .search-params .sql-tab {display:none}

        .search-type-selector #text-select:checked ~ .search-params .uuid-tab {display:none}
        .search-type-selector #text-select:checked ~ .search-params .xpath-tab {display:none}
        .search-type-selector #text-select:checked ~ .search-params .sql-tab {display:none}

        .search-type-selector #xpath-select:checked ~ .search-params .uuid-tab {display:none}
        .search-type-selector #xpath-select:checked ~ .search-params .text-tab {display:none}
        .search-type-selector #xpath-select:checked ~ .search-params .sql-tab {display:none}

        .search-type-selector #sql-select:checked ~ .search-params .uuid-tab {display:none}
        .search-type-selector #sql-select:checked ~ .search-params .text-tab {display:none}
        .search-type-selector #sql-select:checked ~ .search-params .xpath-tab {display:none}
    </style>
    <link href="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAAA7lJREFUWAm9V2lI1EEUf7ua4dplh2WJlJ1CdApthxqUmUiXn4KSqOiAgqAo8EN+iiKCsOhDfYiig4guKDtIkA5UMBKygsLO1dZuK492t12333Ob+c/O7uYetQ9+zHtv3pv3Zv7zZuZvor+TCd05wDIgHxgFpANdgB14AVQC14CPwD+lxRjtEeANA79gcwzg5GKmfhjhMhBOYN2mA36rYskgE86PQwa3pHyl4RlNNCDVTmazO6Qd0d5wk+BvLIhnXgdMFoqedtiIN1S6tYZyi7zUf1Bf2edxe6ix3knnjo6hhpo8qTeYXWAPGGJwTk2Al32FNDOZu2ndjgu0chORyWSW+mDMi6edtGtNIf1oU/dAN0yLgNvBXIROJMAb7qZQYnk9tO/EGZo5zyJ1vTGd7U7aWGylD+/GKabPwfOK8ucKSjwzTmK/X+/W8vMRBWfnlP596fClWkq2fFfGmgh+vSIHsJwA1/kU2ZM59gktLe0j5UiYIcOSae32G5pLrwnwIWPQlvKHhhAFt6zUTJaUNsWTJzhSkf1YXoF8qemT1EXTrMZOlx0RMAmJZsrJe6B48CcOViU9JpwAH68+yhjTRAmJCUKMup0++7Pma8TQOjgBo3SGjlCXTjONQExL13e9EUMbhhPgi8VHLkd0m0/4i9bjEZxonYLRW06AbzUffWodKtiY2pbXSZr/J02WIifAV6qPWpsnEB8osVJdNd8pKjWqgspzApVS4fWa6NbFRClHw7xv6aInD62KK0+oVpH9WE6AHxPGRztVUUAuF9/v0dHBsizydvO4gviOcQhBb9mQXzLHZUdnxxAq3xiybKRdMOb+rZ/UUJurdVVosp8oap5Pv82Ab/PYbaOp1WajuYscuAnFheXnGCDcue6gPdtW4x2j2p+B3ZEAW0UhEuiA7i1QIvtePRtPNVVumja7hQamhi7P9m8OOrR7IJ2sKNGWnofaAtjkmEEYNVvu3guU+dmZTF7Knl5PBSte0qSpLkodbCany0vNL4nuXk+j6soF5HHrZSeG4M+7EOBXVti0E5a8KfX3XrQyH8szwo7+x3AR2mdApEFPw2cD4NJ8v0FWSxNi78TnAd5jVA/w8ypUMlxiZ4FZgKAlYFiv+rRDDrgV9T0gBtBbLksuL27TAZ4hH6/831AH/AR0KoTiCpCsdPC9sxyoUnT/lZ2P0Xnm6krwyhQDcaM5iMTvRDUJXkGj5OOQSg5ifNGS+BXvJKYiIJ8L6krYIceVshGNg4okcJrFn7IQ8ipwD7D+BrZbLK6dz8HeAAAAAElFTkSuQmCC" rel="icon" type="image/png" />
  </head>

  <body>
    <div class="hippo-header">
      <div class="logo"></div>
      <#if jcrSession??>
        <div class="username-box"><noscript>Logged in as: </noscript>${jcrSession.userID}</div>
      </#if>
      <div class="logout">
        <a href="${rootRelativePath}?logout">Log out</a>
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
              <input class="query-input" type="text"  name="xpath" placeholder="XPath query" value="${request.getParameter('xpath')!}"/><br/>
              Limit<noscript> (XPath query)</noscript>: <input name="xpath-limit" type="text" size="5" value="${request.getParameter('xpath-limit')!1000?c}"/>
            </div>

            <#--SQL-->
            <div class="sql-tab">
              <noscript><div>&nbsp;</div>SQL:&nbsp;&nbsp;</noscript>
              <input class="query-input" type="text"  name="sql" placeholder="SQL query" value="${request.getParameter('sql')!}"/><br/>
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
              <a href="${ancestorLink}">${ancestor.name}</a>/<#t>
              <#assign distance = distance - 1>
            </#list>
            <a href="./">${currentNode.name}</a>/<#t>
          </#if>
        </code>


        <ul>
          <#list currentNode.nodes as child>
            <li type="circle">
              <#assign childLink = "${child.name}">
              <#if child.index gt 1>
                <#assign childLink = "${childLink}[${child.index}]">
              </#if>
              <a href="./${childLink?url_path}/">
                ${child.name}
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
                      ${value.string!},
                    <#else>
                      ${prop.length} bytes.
                    </#if>
                  </#list>
                ]
              <#else>
                <#if prop.type != 2>
                  ${prop.string!}
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
          SQL query:&nbsp;
        </#if>

        ${originalQuery}

      </blockquote>

      <#assign queryResultNodes = queryResult.nodes>
      Number of results found: ${queryResultTotalSize!-1}

      <ol>
        <#list queryResultNodes as node>
          <#if node??>
            <li>
              <a href="${baseRelPath}${node.path?substring(1)?url_path}">${node.path}</a>
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
          <#list columnNames as columnName>
            <th>${columnName}</th>
          </#list>
        </tr>
        <#list data.rows as row>
            <#if row??>
            <tr>
                <td>${row_index + 1}</td>
              <#if row.data??>
                  <#list row.data as value>
                      <#if value??>
                        <td>${value!}</td>
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
          UUID: ${request.getParameter("uuid")!}
        </blockquote>
        <ol>
          <li>
            Found node: <a href="${baseRelPath}${nodeById.path?substring(1)?url_path}">${nodeById.path}</a>
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
          ( <a href="${baseRelPath}${nodeById.path?substring(1)?url_path}">${nodeById.path}</a> )
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
