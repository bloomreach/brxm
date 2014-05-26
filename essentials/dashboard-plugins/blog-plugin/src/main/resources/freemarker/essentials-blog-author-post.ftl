{{#repositoryBased}}
    <#include "../../hst:default/hst:templates/imports">
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
<#-- @ftlvariable name="item" type="{{beansPackage}}.Blogpost" -->
<#-- @ftlvariable name="author" type="{{beansPackage}}.Author" -->
<#-- @ftlvariable name="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable" -->

<div class="panel panel-default">
<#if pageable??>
    <div class="panel-heading">
        <h3 class="panel-title">More by ${author.fullName}</h3>
    </div>
    <#if pagebale?? && pageable.total > 0>
        <div class="panel-body">
            <#list pageable.items as item>
                <@hst.link hippobean=item var="link"/>
                <p><a href="${link}">${item.title}</a></p>
            </#list>
        </div>
    <#else>
        <div class="panel-body">
            <p>No other posts found.</p>
        </div>
    </#if>
</#if>
</div>