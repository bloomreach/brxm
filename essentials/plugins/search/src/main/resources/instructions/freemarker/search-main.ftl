<#include "../include/imports.ftl">

<#-- @ftlvariable name="query" type="java.lang.String" -->
<#-- @ftlvariable name="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable" -->
<#if pageable??>
  <#if pageable.total == 0>
    <h3>No results for: ${query?html}</h3>
  <#else>
    <#list pageable.items as item>
      <#if item.title??>
        <@hst.link var="link" hippobean=item />
        <article>
          <@hst.cmseditlink hippobean=item/>
          <h3><a href="${link}">${item.title?html}</a></h3>
        </article>
      </#if>
    </#list>
    <#if cparam.showPagination>
    <#include "../include/pagination.ftl">

    </#if>
  </#if>
<#else>
  <h3>Please fill in a search term.</h3>
</#if>