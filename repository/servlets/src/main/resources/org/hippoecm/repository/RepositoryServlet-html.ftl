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
    <title>BloomReach Repository Browser</title>
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
          background: #ffffff url("data:image/svg+xml;utf8,<svg width='32px' height='33px' viewBox='1 1 32 33' version='1.1' xmlns='http://www.w3.org/2000/svg' xmlns:xlink='http://www.w3.org/1999/xlink'><defs><polygon id='path-1' points='15.9984818 0.0172846715 0.0140145985 0.0172846715 0.0140145985 31.9859854 31.9829489 31.9859854 31.9829489 0.0172846715'></polygon> </defs><g id='Page-1-Copy' stroke='none' stroke-width='1' fill='none' fill-rule='evenodd'><g id='Artboard' transform='translate(-347.000000, -573.000000)'><g id='CMS_32' transform='translate(346.973633, 573.441406)'><g id='Group-3'><mask id='mask-2' fill='white'><use xlink:href='%23path-1'></use></mask><g id='Clip-2'></g><path d='M15.9913577,0.0172846715 C7.15982482,0.0172846715 0,7.17640876 0,16.0086423 C0,21.0062482 2.29208759,25.4649927 5.88309489,28.397781 L5.88309489,6.4920292 L10.1061606,6.4920292 L10.1061606,16.0731095 C11.3490219,15.353927 12.7908905,14.939562 14.3303942,14.939562 C14.4798832,14.939562 14.6279708,14.9437664 14.7758248,14.9519416 C14.8223066,14.9554453 14.8683212,14.9591825 14.9148029,14.9626861 C15.0159416,14.969927 15.1154453,14.976 15.2147153,14.9867445 C15.268438,14.9930511 15.320292,15.0009927 15.3735474,15.0072993 C15.4658102,15.0189781 15.5578394,15.0297226 15.6491679,15.0430365 C15.7021898,15.0509781 15.7540438,15.0617226 15.8070657,15.0717664 C15.8974599,15.0885839 15.9869197,15.1028321 16.0763796,15.1215182 C16.1284672,15.1331971 16.1789197,15.1448759 16.2298394,15.1565547 C16.3197664,15.1780438 16.4085255,15.1985985 16.4972847,15.2217226 C16.5447007,15.2350365 16.5925839,15.2485839 16.6407007,15.2635328 C16.729927,15.2887591 16.8219562,15.3146861 16.9100146,15.3431825 C16.9541606,15.3564964 16.998073,15.373781 17.0443212,15.3887299 C17.133781,15.4200292 17.2260438,15.4503942 17.3155036,15.483562 C17.3559124,15.4996788 17.3953869,15.5164964 17.4378978,15.5316788 C17.5289927,15.5692847 17.6226569,15.6059562 17.7128175,15.6451971 C17.747854,15.6603796 17.7845255,15.6790657 17.8204964,15.6951825 C17.9136934,15.7381606 18.0073577,15.782073 18.0991533,15.8276204 C18.1311533,15.8425693 18.1636204,15.8596204 18.1935182,15.8773723 C18.2906861,15.9257226 18.3848175,15.9766423 18.477781,16.0284964 C18.5058102,16.0446131 18.5315036,16.0597956 18.5576642,16.0749781 C18.6557664,16.1322044 18.7512993,16.1910657 18.8461314,16.2501606 C18.8671533,16.2634745 18.8902774,16.2786569 18.9112993,16.2929051 C19.009635,16.3552701 19.1058686,16.4206715 19.2018686,16.4895766 C19.2191533,16.5010219 19.2357372,16.5127007 19.2541898,16.5260146 C19.3534599,16.5974891 19.4506277,16.6717664 19.547562,16.7467445 C19.5587737,16.7565547 19.5727883,16.7654307 19.5851679,16.7771095 C19.6863066,16.8555912 19.7839416,16.9378102 19.8801752,17.0209635 C19.8874161,17.0289051 19.8965255,17.0352117 19.9044672,17.0424526 C20.0049051,17.1309781 20.1032409,17.2209051 20.1985401,17.3157372 C20.2027445,17.3183066 20.2067153,17.3220438 20.2111533,17.325781 C20.3111241,17.4220146 20.4089927,17.5219854 20.5026569,17.6240584 L20.5234453,17.6462482 C21.9228029,19.1535182 22.7783942,21.1697518 22.7783942,23.3887299 C22.7783942,28.0539562 18.9965547,31.835562 14.3303942,31.835562 C13.9816642,31.835562 13.638073,31.8122044 13.2996204,31.7694599 C14.1741314,31.9175474 15.0738686,32 15.9913577,32 C24.8226569,32 31.9829489,24.8406423 31.9829489,16.0086423 C31.9829489,7.17640876 24.8226569,0.0172846715 15.9913577,0.0172846715' id='Fill-1' fill='%23000000' mask='url(%23mask-2)'></path></g><path d='M17.1933431,20.2877664 C17.0954745,20.1976058 16.9927007,20.1111825 16.8852555,20.0305985 C16.8719416,20.0189197 16.856292,20.0084088 16.8406423,19.9967299 C16.7425401,19.9243212 16.6418686,19.8556496 16.5355912,19.7902482 C16.5134015,19.7762336 16.4893431,19.7624526 16.466219,19.7475036 C16.3690511,19.6912117 16.2672117,19.6384234 16.1649051,19.5868029 C16.1336058,19.5716204 16.1018394,19.556438 16.0693723,19.5419562 C15.9722044,19.4982774 15.8734015,19.4588029 15.7722628,19.4214307 C15.7330219,19.4081168 15.6949489,19.3910657 15.6561752,19.3786861 C15.5578394,19.3473869 15.4588029,19.3205255 15.3588321,19.2945985 C15.3144526,19.2829197 15.2712409,19.2714745 15.2254599,19.2607299 C15.122219,19.2392409 15.0178102,19.2231241 14.912,19.2088759 C14.8683212,19.2018686 14.8274453,19.1918248 14.7837664,19.1883212 C14.6345109,19.1722044 14.4833869,19.1642628 14.3303942,19.1642628 C11.9971971,19.1642628 10.1061606,21.054365 10.1061606,23.3887299 C10.1061606,25.7214599 11.9971971,27.6127299 14.3303942,27.6127299 C16.6635912,27.6127299 18.5548613,25.7214599 18.5548613,23.3887299 C18.5548613,22.3250219 18.1601168,21.3573139 17.5114745,20.6161752 C17.4175766,20.5077956 17.3155036,20.4040876 17.2108613,20.3048175 C17.2045547,20.2978102 17.1984818,20.2945401 17.1933431,20.2877664' id='Fill-4' fill='%23000000'></path><path d='M22.7783942,23.3887299 C22.7783942,21.1697518 21.9228029,19.1535182 20.5234453,17.6462482 L20.5026569,17.6240584 L17.5114745,20.6161752 C18.1601168,21.3573139 18.5548613,22.3250219 18.5548613,23.3887299 C18.5548613,25.7214599 16.6635912,27.6127299 14.3303942,27.6127299 C11.9971971,27.6127299 10.1061606,25.7214599 10.1061606,23.3887299 L10.1061606,30.701781 C10.6069489,30.9930511 11.1409051,31.2289635 11.6986861,31.4109197 C12.2221314,31.5585401 12.7556204,31.678365 13.2996204,31.7694599 C13.638073,31.8122044 13.9816642,31.835562 14.3303942,31.835562 C18.9965547,31.835562 22.7783942,28.0539562 22.7783942,23.3887299' id='Fill-6' fill='%2300BCDC'></path><path d='M10.1061606,16.0731095 L10.1061606,6.4920292 L5.88309489,6.4920292 L5.88309489,23.3887299 C5.88309489,20.2623066 7.58189781,17.5336642 10.1061606,16.0731095' id='Fill-7' fill='%2300BCDC'></path></g></g></g></svg>") no-repeat 0px 0px/25px 25px;
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
    <link href="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAB4AAAAgCAYAAAAFQMh/AAAAAXNSR0IArs4c6QAAA8BJREFUSA3FV0loU1EUPe9n6GA6WMRKVLSK81Bx2Dgg1C50oTvFhVJE0UWXrsSVC0EQQURwKQiCKAgudKN1UaqoWEoHh+KAY6XUFtMxSZt8z/nJ10y12pJ44fT9f+/Lvffd6f0aTEbPe0oRCq+leD0QXwGY+YAd5FoJGyXkF8PwCQjz7xifB/jcQ3yFZb+C5e3AnEUvsMZEycsik8Zp+RREZPwwbHs3+VsJf5r8X1+MGaOuZhjrLkp817F1oZxzKGG46TNPEz3PE+2n915X6KV0e0UxNpcXoTZQhJWzfKj0elBOQbnHwgTPOxiLY3Aijr5oDJ0jUbQPRdESGsPLkXFXjbtGYMxVlPhPywGDpg8bYcce0miFu0Pr0WAZzi6pQnXRLz9SxVM+tw5GcOJ1H1qHIul7DVPh9e4wePDuGY1uSZUGPAahnTWwTHomUvf8zfPDgVHsavuWvdWYWxa5qzMlwzEbBzp78XY0K1yZW3O+27aN+/2jaOz+nlNO5iqdeJgnnjXZjtqA/3eOS5ljn4UKr4UyN8fMr/Ls5HiYOR6O4HEojK+R2GQq2Rima8oEtjvK1BFDkyuahkSh/i9kwdivC27ZRjcNexo5dZjnApExvfDhlIW6mqeAtZYJv82sx/Nm3mDcGSC+0nXYufSNGlXt9NIx2PR+EUdcA6t8D51gb9ueGTqiqnwMC/cQt66hfklvUh/bCWgm9hE/kszE8qS/HCOhDXxZz0tiOR2ZT6eC9Ho2ecXORZG4JMb4Hib6icQlYbNuvLwkAv52bA6Okp9KC/hyRQzlVz84REzZXtwzXSrmDxsJXRSdUiLDut4EzTdeFqgjioiZUikVMG24TMiga6dToZbhXJNL4esiOohuIhHGRErc8Or3OkkJUUUECd50WEUwRU79+LhmUpd+qJEUyJTk+b1Lk6stz0ZyqXdssmJ5R/6Ov5uHfK2alNXyRuGuJC4SCnu+DKrdzhCqB4dSPwLKyDlC3CS0caZOqEuuEQcJFaFLm3RaVe1e4qPLTa6SLSZUnUqHqlVVmxggCc/lmFvhctStfIVTer8QmSR9l8RUOwmnCCnNF82j4nOEPsKyBoi8v0E0EDrhTKmGCo4Tdwh9R7mp++MA4T70EgrZdAdIlZTkIGeADFKgoiokdWiAtBTSYtLWI61KupLtxj/fazNt/RrRGuQniQ9Evgy/ou5jhNo0ixT6euIC0U5MENN1RG3zjDhLbCPSKKf1lB1+Pi8j1BZKyVxC/2PprtYkklNhQkb0BaMuEN4lIcdz0k/hdpmlzPxT8gAAAABJRU5ErkJggg==" rel="icon" type="image/png" />
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
