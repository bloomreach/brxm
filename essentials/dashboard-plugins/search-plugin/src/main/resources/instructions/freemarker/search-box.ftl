{{#repositoryBased}}
    <#include "../../hst:default/hst:templates/imports.ftl">
{{/repositoryBased}}
{{#fileBased}}
    <#include "/WEB-INF/freemarker/include/imports.ftl">
{{/fileBased}}
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
<#-- @ftlvariable name="query" type="java.lang.String" -->
<form class="navbar-form" role="search" action="<@hst.link path="/search" />" method="get">
    <div class="input-group">
        <input type="text" class="form-control" placeholder="Search" name="query">
        <div class="input-group-btn">
            <button class="btn btn-default" type="submit"><i class="glyphicon glyphicon-search"></i></button>
        </div>
    </div>
</form>
