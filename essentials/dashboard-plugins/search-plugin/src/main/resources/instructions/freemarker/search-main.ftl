<#include "/WEB-INF/freemarker/include/imports.ftl">
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
<#-- @ftlvariable name="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable" -->
<#if pageable??>
  <#if pageable.total == 0>
    <h3>No results for: ${query}</h3>
  <#else>
    <#list pageable.items as item>
      <@hst.link var="link" hippobean=item />
      <article>
        <h3><a href="${link}">${item.title}</a></h3>
      </article>
    </#list>
    <#if pageable.showPagination??>
      <#include "/WEB-INF/freemarker/include/pagination.ftl">
    </#if>
  </#if>
<#else>
  <h3>Please fill in a search term.</h3>
</#if>