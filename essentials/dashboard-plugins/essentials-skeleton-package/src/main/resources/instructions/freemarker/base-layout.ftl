<!doctype html>
<#--
  Copyright 2014 Hippo B.V. (http://www.onehippo.com)

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
{{#repositoryBased}}
    <#include "../../hst:default/hst:templates/imports.ftl">
{{/repositoryBased}}
{{#fileBased}}
    <#include "/WEB-INF/freemarker/include/imports.ftl">
{{/fileBased}}
<html lang="en">
<head>
    <meta charset="utf-8"/>
    <link rel="stylesheet" href="<@hst.link  path="/css/bootstrap.css"/>" type="text/css"/>
    <@hst.defineObjects/>
    <#if hstRequest.requestContext.cmsRequest>
      <link rel="stylesheet" href="<@hst.link  path="/css/cms-request.css"/>" type="text/css"/>
    </#if>
<@hst.headContributions categoryIncludes="componentsCss" xhtml=true/>
<@hst.headContributions categoryIncludes="globalJavascript" xhtml=true/>
</head>
<body>
<div class="container">
    <div class="row">
        <div class="col-md-6 col-md-offset-3">
        <@hst.include ref="top"/>
        <@hst.include ref="menu"/>
        </div>
    </div>
    <div class="row">
        <@hst.include ref="main"/>
    </div>
    <div class="row">
        <@hst.include ref="footer"/>
    </div>
</div>
<@hst.headContributions categoryIncludes="componentsJavascript" xhtml=true/>
</body>
</html>